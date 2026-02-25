package guru.nicks.commons.test.config;

import guru.nicks.commons.test.MongoContainerRunner;
import guru.nicks.commons.test.MySqlContainerRunner;
import guru.nicks.commons.test.PostgreSqlContainerRunner;
import guru.nicks.commons.test.RedisContainerRunner;
import guru.nicks.commons.test.TimescaleDbContainerProvider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for integration test utilities including WireMock, TestContainers, and database runners.
 * <p>
 * This configuration automatically registers beans for WireMock stubbing, TestContainers-based database runners, and
 * various database container providers (MySQL, PostgreSQL, MongoDB, Redis, TimescaleDB).
 * <p>
 * The configuration is activated when the required classes are present on the classpath and creates beans only if they
 * are not already defined by the user.
 */
@AutoConfiguration
@Slf4j
public class WireMockAutoConfiguration {

    /**
     * Creates a {@link MongoContainerRunner} bean if not already defined.
     * <p>
     * This bean provides MongoDB TestContainer integration for integration tests.
     *
     * @return MongoContainerRunner instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MongoContainerRunner.class)
    public MongoContainerRunner mongoContainerRunner() {
        log.debug("Registering MongoContainerRunner bean");
        return new MongoContainerRunner();
    }

    /**
     * Creates a {@link MySqlContainerRunner} bean if not already defined.
     * <p>
     * This bean provides MySQL TestContainer integration for integration tests.
     *
     * @return MySqlContainerRunner instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MySqlContainerRunner.class)
    public MySqlContainerRunner mySqlContainerRunner() {
        log.debug("Registering MySqlContainerRunner bean");
        return new MySqlContainerRunner();
    }

    /**
     * Creates a {@link PostgreSqlContainerRunner} bean if not already defined.
     * <p>
     * This bean provides PostgreSQL TestContainer integration for integration tests.
     *
     * @return PostgreSqlContainerRunner instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(PostgreSqlContainerRunner.class)
    public PostgreSqlContainerRunner postgreSqlContainerRunner() {
        log.debug("Registering PostgreSqlContainerRunner bean");
        return new PostgreSqlContainerRunner();
    }

    /**
     * Creates a {@link RedisContainerRunner} bean if not already defined.
     * <p>
     * This bean provides Redis TestContainer integration for integration tests.
     *
     * @return RedisContainerRunner instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisContainerRunner.class)
    public RedisContainerRunner redisContainerRunner() {
        log.debug("Registering RedisContainerRunner bean");
        return new RedisContainerRunner();
    }

    /**
     * Creates a {@link TimescaleDbContainerProvider} bean if not already defined.
     * <p>
     * This bean provides TimescaleDB TestContainer integration for integration tests.
     *
     * @return TimescaleDbContainerProvider instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(TimescaleDbContainerProvider.class)
    public TimescaleDbContainerProvider timescaleDbContainerProvider() {
        log.debug("Registering TimescaleDbContainerProvider bean");
        return new TimescaleDbContainerProvider();
    }

}
