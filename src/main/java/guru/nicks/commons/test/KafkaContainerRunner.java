package guru.nicks.commons.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Starts a real Kafka broker (Apache Kafka official image) in a Docker container and wires it into the test application
 * context as the Spring Cloud Stream Kafka binder endpoint.
 * <p>
 * The container is a JVM-wide singleton (reused by context restarts), the bootstrap servers are exposed via
 * {@link #getBootstrapServers()} for raw producers, and the container is registered as a bean destroyed with
 * {@code stop()}.
 */
@Slf4j
public class KafkaContainerRunner implements ApplicationContextInitializer<GenericApplicationContext> {

    public static final String IMAGE_TAG = "apache/kafka:3.8.0";

    /**
     * 3.9.0 is broken: its entrypoint fails env-based {@code advertised.listeners} setup with 'advertised.listeners
     * cannot use the nonroutable meta-address 0.0.0.0'
     */
    private static final KafkaContainer CONTAINER = new KafkaContainer(IMAGE_TAG);

    private static boolean started;

    /**
     * Returns Kafka bootstrap servers of the running container.
     *
     * @return bootstrap servers, e.g. {@code localhost:32768}
     * @throws IllegalStateException container is not started yet
     */
    public static String getBootstrapServers() {
        if (!started) {
            throw new IllegalStateException("Kafka container is not started yet");
        }

        return CONTAINER.getBootstrapServers();
    }

    private static KafkaContainer createAndStartContainer(GenericApplicationContext applicationContext) {
        if (!started) {
            CONTAINER.start();
            started = true;
            log.info("Started Kafka container, bootstrap servers: {}", CONTAINER.getBootstrapServers());
        }

        TestPropertyValues testProps = TestPropertyValues.of(
                "spring.cloud.stream.kafka.binder.brokers=" + CONTAINER.getBootstrapServers()
        );

        testProps.applyTo(applicationContext.getEnvironment());
        return CONTAINER;

    }

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
        KafkaContainer container = createAndStartContainer(applicationContext);

        applicationContext.registerBean(KafkaContainer.class, () -> container,
                beanDefinition -> beanDefinition.setDestroyMethodName("stop"));
    }

}
