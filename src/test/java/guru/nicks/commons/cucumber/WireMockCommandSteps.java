package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.test.WireMockCommand;
import guru.nicks.commons.test.WireMockConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Steps for testing {@link WireMockCommand} and {@link WireMockConfig} against a real WireMock server started on a
 * random port. Exception state is shared via {@link TextWorld} to let common steps assert it.
 */
@RequiredArgsConstructor
public class WireMockCommandSteps {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // DI
    private final TextWorld textWorld;

    @Value("${" + WireMockConfig.WIREMOCK_SELF_TEST_PROPERTY + "}")
    private String endpoint;

    // accepts any content type, not only JSON
    private static final RestTemplate REST_TEMPLATE = buildRestTemplate();

    private WireMockCommand command;
    private ResponseEntity<?> response;

    /**
     * Cucumber has no '{httpMethod}' parameter type built-in. This method registers it.
     *
     * @param httpMethod HTTP method name, such as 'GET'
     * @return HTTP method
     */
    @ParameterType("GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD")
    public HttpMethod httpMethod(String httpMethod) {
        return HttpMethod.valueOf(httpMethod);
    }

    /**
     * Builds a RestTemplate able to read responses of any content type (such as 'image/png'), not only JSON. Connection
     * reuse is disabled because WireMock's Jetty closes keep-alive connections, which breaks the shared client's
     * connection pool.
     *
     * @return RestTemplate
     */
    private static RestTemplate buildRestTemplate() {
        var converter = new MappingJackson2HttpMessageConverter();
        // will be called for 'image/png' etc., just to avoid the 'Message converter not found' error
        converter.setSupportedMediaTypes(List.of(MediaType.ALL));

        // never reuse connections - reusing one already closed by WireMock's Jetty fails the request
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionReuseStrategy((request, response, context) -> false)
                .build();

        return new RestTemplateBuilder()
                .messageConverters(converter)
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

    /**
     * Creates a stub with request HTTP method and path, and response HTTP status.
     *
     * @param httpMethod request HTTP method
     * @param path request path
     * @param httpStatus response HTTP status
     */
    @Given("a WireMock stub for {httpMethod} {string} returning {int}")
    public void aWireMockStubForReturning(HttpMethod httpMethod, String path, int httpStatus) {
        command = WireMockCommand.builder()
                .request()
                .httpMethod(httpMethod).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.valueOf(httpStatus))
                //
                .and().mock();
    }

    /**
     * Creates a stub returning given status with explicit Content-Type and ETag headers and a JSON body derived from a
     * single-key map.
     *
     * @param httpMethod request HTTP method
     * @param path request path
     * @param httpStatus response HTTP status
     * @param contentType response Content-Type header value
     * @param etag response ETag header value
     * @param bodyKey JSON body key
     * @param bodyValue JSON body value
     * @throws JsonProcessingException error serializing JSON body
     */
    @Given("a WireMock stub for {httpMethod} {string} returning {int} with content type {string} and ETag {string}"
            + " and JSON body containing key {string} with value {string}")
    public void aWireMockStubForReturningWithHeadersAndJsonBody(HttpMethod httpMethod, String path, int httpStatus,
            String contentType, String etag, String bodyKey, String bodyValue) throws JsonProcessingException {
        // derive JSON from DTO
        String responseBody = OBJECT_MAPPER.writeValueAsString(Map.of(bodyKey, bodyValue));

        command = WireMockCommand.builder()
                .request()
                .httpMethod(httpMethod).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.valueOf(httpStatus))
                .header(new HttpHeader(HttpHeaders.CONTENT_TYPE, contentType))
                .header(new HttpHeader(HttpHeaders.ETAG, etag))
                .body(responseBody)
                //
                .and().mock();
    }

    /**
     * Creates a stub with response body loaded from the classpath.
     *
     * @param httpMethod request HTTP method
     * @param path request path
     * @param httpStatus response HTTP status
     * @param classpathLocation response body location on the classpath
     */
    @Given("a WireMock stub for {httpMethod} {string} returning {int} with body from classpath {string}")
    public void aWireMockStubForReturningWithBodyFromClasspath(HttpMethod httpMethod, String path, int httpStatus,
            String classpathLocation) {
        command = WireMockCommand.builder()
                .request()
                .httpMethod(httpMethod).path(path)
                //
                .and().response()
                .httpStatus(HttpStatus.valueOf(httpStatus)).bodyFromClasspath(classpathLocation)
                //
                .and().mock();
    }

    /**
     * Creates a stub calling request and response builder sections repeatedly to let later sections overwrite earlier
     * ones.
     *
     * @param httpMethod request HTTP method
     * @param path request path
     * @param finalStatus response HTTP status set by the last response section
     */
    @Given("a WireMock stub built with repeated request and response sections for {httpMethod} {string}"
            + " with final status {int}")
    public void aWireMockStubBuiltWithRepeatedSections(HttpMethod httpMethod, String path, int finalStatus) {
        command = WireMockCommand.builder()
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
                .httpStatus(HttpStatus.valueOf(finalStatus))
                //
                .and().mock();
    }

    /**
     * Re-mocks the URL of the stub created earlier using {@link WireMockCommand#toBuilder()}.
     *
     * @param httpStatus new response HTTP status
     */
    @Given("the stub is rebuilt with toBuilder returning {int}")
    public void theStubIsRebuiltWithToBuilderReturning(int httpStatus) {
        // change HTTP status for already mocked URL
        command = command.toBuilder()
                .response()
                .httpStatus(HttpStatus.valueOf(httpStatus))
                //
                .and().mock();
    }

    /**
     * Sends a request with the given HTTP method to the given path on the WireMock server.
     *
     * @param httpMethod request HTTP method
     * @param path request path
     */
    @When("a {httpMethod} request is sent to {string}")
    public void aRequestIsSentTo(HttpMethod httpMethod, String path) {
        textWorld.setLastException(catchThrowable(() ->
                response = REST_TEMPLATE.exchange(endpoint + path, httpMethod, HttpEntity.EMPTY, Map.class)));
    }

    /**
     * Asserts that the request failed with {@link HttpClientErrorException} (any 4xx or 5xx status, including
     * subclasses such as {@code NotFound}).
     */
    @Then("a HttpClientErrorException should be thrown")
    public void aHttpClientErrorExceptionShouldBeThrown() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isInstanceOf(HttpClientErrorException.class);
    }

    /**
     * Asserts response HTTP status.
     *
     * @param httpStatus expected response HTTP status
     */
    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int httpStatus) {
        assertThat(response.getStatusCode())
                .as("response HTTP status")
                .isEqualTo(HttpStatus.valueOf(httpStatus));
    }

    /**
     * Asserts response Content-Type header.
     *
     * @param contentType expected response Content-Type header value
     */
    @Then("the response content type should be {string}")
    public void theResponseContentTypeShouldBe(String contentType) {
        assertThat(response.getHeaders().getContentType())
                .as("response Content-Type header")
                .isEqualTo(MediaType.parseMediaType(contentType));
    }

    /**
     * Asserts response ETag header.
     *
     * @param etag expected response ETag header value
     */
    @Then("the response ETag should be {string}")
    public void theResponseETagShouldBe(String etag) {
        assertThat(response.getHeaders().getETag())
                .as("response ETag header")
                .isEqualTo(etag);
    }

    /**
     * Asserts that response body is JSON containing the given key-value pair.
     *
     * @param key expected JSON body key
     * @param value expected JSON body value
     */
    @SuppressWarnings("unchecked")
    @Then("the response body should contain key {string} with value {string}")
    public void theResponseBodyShouldContainKeyWithValue(String key, String value) {
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertThat(body)
                .as("response body")
                .containsEntry(key, value);
    }

}
