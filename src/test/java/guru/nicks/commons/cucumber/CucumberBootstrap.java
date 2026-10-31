package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.test.WireMockConfig;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Initializes Spring context for the whole test suite. Starts WireMock server on a random port and overrides
 * URL-related properties via {@link WireMockConfig}, letting {@code WireMockCommandSteps} send real HTTP requests
 * against it.
 * <p>
 * Please keep in mind that mocked Spring beans declared in step definition classes conflict with each other because all
 * the steps are part of the same test suite i.e. Spring context.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = {
        // Spring beans
        WireMockConfig.class,

        // scenario-scoped states
        TextWorld.class
}, webEnvironment = NONE)
// default port = 0 (dynamic), property exposed = 'wiremock.server.port' (same contract as the former
// @AutoConfigureWireMock from spring-cloud-contract-wiremock)
@EnableWireMock(@ConfigureWireMock)
public class CucumberBootstrap {
}
