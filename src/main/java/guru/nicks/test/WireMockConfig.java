package guru.nicks.test;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Overrides URL-related properties ({@link #PROPERTIES_TO_STUB_WITH_WIREMOCK}) using {@link #WIREMOCK_URL_PREFIX} with
 * the (random) WireMock port appended. WireMock must already be configured, for example with
 * {@code @AutoConfigureWireMock(port = Options.DYNAMIC_PORT)}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WireMockConfig {

    /**
     * Dummy property for testing stubbing functionality as such.
     */
    public static final String WIREMOCK_SELF_TEST_PROPERTY =
            "spring.cloud.discovery.client.simple.instances.wiremock-self-test.uri[0]";

    /**
     * Add more URL-holding properties here - presumably in the form
     * {@code spring.cloud.discovery.client.simple.instances.some-service-name.uri[0]} because for the static service
     * discovery client, each service always has a list of URIs, not one URI.
     */
    public static final List<String> PROPERTIES_TO_STUB_WITH_WIREMOCK = List.of(WIREMOCK_SELF_TEST_PROPERTY);

    public static final String WIREMOCK_URL_PREFIX = "http://localhost:";

    // DI
    private final ConfigurableApplicationContext applicationContext;

    @Value("${wiremock.server.port}")
    private int wiremockPort;

    @PostConstruct
    private void init() {
        if (wiremockPort == 0) {
            throw new IllegalStateException("Wiremock server not initialized");
        }

        // append Wiremock URL to property names
        Set<String> mockedUrls = PROPERTIES_TO_STUB_WITH_WIREMOCK.stream()
                .map(property -> property + "=" + WIREMOCK_URL_PREFIX + wiremockPort)
                .collect(Collectors.toSet());

        log.info("Mocking URLs with Wiremock running on port {}: {}", wiremockPort, mockedUrls);
        TestPropertyValues testProps = TestPropertyValues.of(mockedUrls);
        testProps.applyTo(applicationContext);
    }

}
