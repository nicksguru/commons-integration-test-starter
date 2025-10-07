package guru.nicks.test;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.requireNonNull;

/**
 * Creates a WireMock stub. Neither request variables nor body are matched (i.e. any request having the same URL and
 * HTTP method returns the same response) because different content types require different comparison logic (WireMock
 * has {@link com.github.tomakehurst.wiremock.matching.EqualToJsonPattern} and others).
 * <p>
 * HTTP methods recognized: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD.
 * <p>
 * Recommended call chain:
 * <pre>
 *  builder()
 *      .request()
 *          .[request builder methods]
 *      .and()
 *      .response()
 *          .[response builder methods]
 *      .and()
 *          .mock()
 * </pre>
 *
 * @see #mock()
 */
@Builder(buildMethodName = "_build", toBuilder = true) // rename default method to create custom build() method
public class WireMockCommand {

    private static final ResourcePatternResolver RESOURCE_RESOLVER = new PathMatchingResourcePatternResolver(
            MethodHandles.lookup().lookupClass().getClassLoader());

    private static final List<HttpHeader> DEFAULT_RESPONSE_CONTENT_TYPE_HEADER = List.of(new HttpHeader(
            org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    private static final Map<HttpMethod, Function<String, MappingBuilder>> HTTP_METHOD_MAPPERS = Map.of(
            HttpMethod.GET, path -> get(urlPathEqualTo(path)),
            HttpMethod.POST, path -> post(urlPathEqualTo(path)),
            HttpMethod.PUT, path -> put(urlPathEqualTo(path)),
            HttpMethod.DELETE, path -> delete(urlPathEqualTo(path)),
            HttpMethod.PATCH, path -> patch(urlPathEqualTo(path)),
            HttpMethod.OPTIONS, path -> options(urlPathEqualTo(path)),
            HttpMethod.HEAD, path -> head(urlPathEqualTo(path))
    );

    private final Request request;
    private final Response response;

    /**
     * Builds command and sends it to WireMock server.
     *
     * @return command; {@link WireMockCommand#toBuilder()} can be used to create another one based on this one, and if
     *         the same URL is used, the new stub overwrites the previous one
     */
    public WireMockCommand mock() {
        requireNonNull(request, "request");
        requireNonNull(request.getHttpMethod(), "request -> HTTP method");
        requireNonNull(request.getPath(), "request -> path");
        requireNonNull(response, "response");
        requireNonNull(response.getHttpStatus(), "response -> HTTP status");

        Function<String, MappingBuilder> requestPathMapper = HTTP_METHOD_MAPPERS.get(request.getHttpMethod());
        if (requestPathMapper == null) {
            throw new NotImplementedException("Unsupported HTTP method: " + request.getHttpMethod());
        }

        MappingBuilder httpMethodAndPath = requestPathMapper.apply(request.getPath());
        List<HttpHeader> responseHeaders = fixResponseHeaders();

        stubFor(httpMethodAndPath.willReturn(
                aResponse()
                        .withStatus(response.getHttpStatus().value())
                        .withHeaders(new HttpHeaders(responseHeaders))
                        // WireMock throws exception if response body is null
                        .withBody(Optional.ofNullable(response.getBody()).orElse(""))));
        return this;
    }

    /**
     * Adds a default content type header to the end of the list if it's not already present.
     *
     * @return response headers with a default content type header if it was not present
     */
    private List<HttpHeader> fixResponseHeaders() {
        List<HttpHeader> responseHeaders = Optional
                .ofNullable(response.getHeaders())
                .orElseGet(Collections::emptyList);

        // make sure content type is present
        if (responseHeaders.stream().noneMatch(header ->
                header.keyEquals(org.springframework.http.HttpHeaders.CONTENT_TYPE))) {
            responseHeaders = Stream.concat(
                    responseHeaders.stream(),
                    DEFAULT_RESPONSE_CONTENT_TYPE_HEADER.stream()
            ).toList();
        }

        return responseHeaders;
    }

    public static class WireMockCommandBuilder {

        private Request.RequestBuilder<WireMockCommandBuilder> requestBuilder;
        private Response.ResponseBuilder<WireMockCommandBuilder> responseBuilder;

        /**
         * Convenience method which combines {@link #build()} and {@link WireMockCommand#mock()}.
         */
        public WireMockCommand mock() {
            WireMockCommand command = build();
            command.mock();
            return command;
        }

        public WireMockCommand build() {
            if (requestBuilder != null) {
                request(requestBuilder.build());
            }

            if (responseBuilder != null) {
                response(responseBuilder.build());
            }

            // call Lombok builder
            return _build();
        }

        /**
         * Creates a request builder or returns the existing one, therefore can be called multiple times. Contrary to
         * what the method name implies, creates a builder - to let configure the nested object and then return to the
         * upper level with {@link Request.RequestBuilder#and()}.
         *
         * @return request builder
         */
        public Request.RequestBuilder<WireMockCommandBuilder> request() {
            if (requestBuilder == null) {
                requestBuilder = Request.builder(this);
            }

            return requestBuilder;
        }

        /**
         * Called from {@link #build()} internally. The presence of a method with exactly this signature and logic is
         * required by Lombok.
         *
         * @param request request
         * @return {@code this}
         */
        private WireMockCommandBuilder request(Request request) {
            this.request = request;
            return this;
        }

        /**
         * Creates a response builder or returns the existing one, therefore can be called multiple times. Contrary to
         * what the method name implies, creates a builder - to let configure the nested object and then return to the
         * upper level with {@link Response.ResponseBuilder#and()}.
         *
         * @return response builder
         */
        public Response.ResponseBuilder<WireMockCommandBuilder> response() {
            if (responseBuilder == null) {
                responseBuilder = Response.builder(this);
            }

            return responseBuilder;
        }

        /**
         * Called from {@link #build()} internally. The presence of a method with exactly this signature and logic is
         * required by Lombok.
         *
         * @param response response
         * @return {@code this}
         */
        private WireMockCommandBuilder response(Response response) {
            this.response = response;
            return this;
        }

    }

    @Value
    @NonFinal
    @Builder
    public static class Request {

        HttpMethod httpMethod;
        String path;

        public static <P> RequestBuilder<P> builder(P parentBuilder) {
            return new RequestBuilder<>(parentBuilder);
        }

        /**
         * Overridden Lombok-generated method to make it inaccessible.
         */
        @SuppressWarnings("unused")
        private static <P> RequestBuilder<P> builder() {
            throw new NotImplementedException("Don't use no-arg method");
        }

        /**
         * Lombok builder augmented with some additional logic.
         *
         * @param <P> parent builder type
         */
        public static class RequestBuilder<P> extends NestedBuilder<Request, P> {

            public RequestBuilder(P parentBuilder) {
                super(parentBuilder);
            }

        }

    }

    @Value
    @NonFinal
    @Builder
    public static class Response {

        /**
         * HTTP status to return, default is {@link HttpStatus#OK}.
         */
        @Builder.Default
        HttpStatus httpStatus = HttpStatus.OK;

        @Singular
        List<HttpHeader> headers;

        /**
         * Response body to return ({@code null} is replaced with '' internally).
         */
        String body;

        public static <P> ResponseBuilder<P> builder(P parentBuilder) {
            return new ResponseBuilder<>(parentBuilder);
        }

        /**
         * Overridden Lombok-generated method to make it inaccessible.
         */
        @SuppressWarnings("unused")
        private static <P> ResponseBuilder<P> builder() {
            throw new NotImplementedException("Don't use no-arg method");
        }

        /**
         * Lombok builder augmented with some additional logic.
         *
         * @param <P> parent builder type
         */
        public static class ResponseBuilder<P> extends NestedBuilder<Response, P> {

            public ResponseBuilder(P parentBuilder) {
                super(parentBuilder);
            }

            /**
             * Loads response body from the classpath.
             *
             * @param path path to the response file ('classpath:' is prepended automatically)
             * @return {@code this}
             * @throws IllegalArgumentException error reading resource
             */
            public ResponseBuilder<P> bodyFromClasspath(String path) {
                String content;

                try {
                    content = RESOURCE_RESOLVER
                            .getResource("classpath:" + path)
                            .getContentAsString(StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error reading resource: " + e.getMessage(), e);
                }

                return body(content);
            }

        }

    }

}
