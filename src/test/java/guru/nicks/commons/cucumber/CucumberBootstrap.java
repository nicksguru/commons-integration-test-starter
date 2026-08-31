package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.test.WireMockConfig;

import com.github.tomakehurst.wiremock.core.Options;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Initializes Spring context for the whole test suite. Starts WireMock server on a random port and overrides
 * URL-related properties via {@link WireMockConfig}, letting {@code WireMockCommandSteps} send real HTTP requests
 * against it.
 * <p>
 * Please keep in mind that mocked Spring beans declared in step definition classes conflict with each other because
 * all the steps are part of the same test suite i.e. Spring context.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = {
        // Spring beans
        WireMockConfig.class,

        // scenario-scoped states
        TextWorld.class
}, webEnvironment = NONE)
@AutoConfigureWireMock(port = Options.DYNAMIC_PORT)
public class CucumberBootstrap {
}
