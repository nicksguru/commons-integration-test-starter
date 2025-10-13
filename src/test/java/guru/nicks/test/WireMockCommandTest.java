package guru.nicks.user;

import guru.nicks.test.WireMockCommand;
import guru.nicks.test.WireMockConfig;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.core.Options;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link WireMockCommand} and {@link WireMockConfig}
 */
@SpringBootTest(classes = WireMockConfig.class)
@AutoConfigureWireMock(port = Options.DYNAMIC_PORT)
class WireMockCommandTest {

    private static RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${" + WireMockConfig.WIREMOCK_SELF_TEST_PROPERTY + "}")
    private String endpoint;

    @BeforeAll
    static void beforeClass() {
        var converter = new MappingJackson2HttpMessageConverter();
        // will be called for 'image/png' etc., just to avoid the 'Message converter not found' error
        converter.setSupportedMediaTypes(List.of(MediaType.ALL));

        restTemplate = new RestTemplateBuilder()
                .messageConverters(converter)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/non/existing/url/1"})
    void givenNoStubs_whenCalled_then404(String path) {
        assertThatThrownBy(() -> restTemplate.headForHeaders(endpoint + path))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404 Not Found");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/non/existing/url/2"})
    void givenNotStubbedPath_whenCalled_then404(String path) {
        WireMockCommand.builder()
                .request()
                .httpMethod(HttpMethod.HEAD).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.BAD_REQUEST)
                //
                .and().mock();

        assertThatThrownBy(() -> restTemplate.headForHeaders(endpoint + path + "notfound"))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404 Not Found");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/non/existing/url/3"})
    void givenNotStubbedHttpMethod_whenCalled_thenError(String path) {
        WireMockCommand.builder()
                .request()
                .httpMethod(HttpMethod.DELETE).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.OK)
                //
                .and().mock();

        // the error is not '405 Method Not Allowed', alas
        assertThatThrownBy(() -> restTemplate.getForObject(endpoint + path, Void.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("Request was not matched");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/non/existing/url/4", "/some/other/url4"})
    void givenStubbedPath_whenGet_thenAccepted(String path) throws JsonProcessingException {
        // derive JSON from DTO
        Map<String, Object> responseDto = Map.of("responseFromGet", path);
        String responseBody = objectMapper.writeValueAsString(responseDto);

        WireMockCommand.builder()
                .request()
                .httpMethod(HttpMethod.GET).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.ACCEPTED)
                .header(new HttpHeader(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG.toString()))
                .header(new HttpHeader(HttpHeaders.ETAG, "test-etag"))
                .body(responseBody)
                //
                .and().mock();

        ResponseEntity<Map> response = restTemplate.getForEntity(endpoint + path, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getETag()).isEqualTo("test-etag");
        assertThat(response.getBody()).containsEntry("responseFromGet", path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/non/existing/url/5", "/some/other/url5"})
    void givenStubbedPath_whenPost_thenCreated(String path) {
        WireMockCommand.builder()
                .request()
                .httpMethod(HttpMethod.POST).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.CREATED).bodyFromClasspath("wiremock/self-test/response1.json")
                //
                .and().mock();

        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint + path, null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).containsEntry("testKey", "test value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/non/existing/url/6"})
    void givenUnauthorizedHttpStatus_whenCalled_thenError(String path) {
        WireMockCommand.builder()
                .request()
                .httpMethod(HttpMethod.PUT).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.UNAUTHORIZED)
                //
                .and()
                .mock();

        // exception is thrown on 4xx and 5xx statuses (not because of stubbing errors)
        assertThatThrownBy(() -> restTemplate.put(endpoint + path, null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("Unauthorized");
    }

    @ParameterizedTest
    @ValueSource(strings = "/non/existing/url/7")
    void givenRepeatedBuilder_whenBuild_thenOk(String path) {
        HttpMethod httpMethod = HttpMethod.PUT;

        WireMockCommand.builder()
                .request()
                .httpMethod(httpMethod)
                //
                // add request fields
                .and().request()
                .path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.OK)
                //
                // overwrite response fields
                .and().response()
                .httpStatus(HttpStatus.NO_CONTENT)
                //
                .and().mock();

        ResponseEntity<Void> response = restTemplate.exchange(endpoint + path, httpMethod, HttpEntity.EMPTY,
                Void.class, Collections.emptyMap());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @ParameterizedTest
    @ValueSource(strings = "/non/existing/url/8")
    void givenToBuilder_whenChangeHttpStatusForUrl_thenOk(String path) {
        HttpMethod httpMethod = HttpMethod.PUT;

        var command = WireMockCommand.builder()
                .request()
                .httpMethod(httpMethod).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.UNAUTHORIZED)
                //
                .and().mock();

        // change HTTP status for already mocked URL
        command.toBuilder()
                .response()
                .httpStatus(HttpStatus.PERMANENT_REDIRECT)
                //
                .and().mock();

        ResponseEntity<Void> response = restTemplate.exchange(endpoint + path, httpMethod, HttpEntity.EMPTY,
                Void.class, Collections.emptyMap());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
    }

}
