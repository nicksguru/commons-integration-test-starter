package guru.nicks.test;

import com.github.tomakehurst.wiremock.core.Options;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to run integration tests. It:
 * <ul>
 *  <li>sets Spring profile to 'local'</li>
 *  <li>enables WireMock on a random port</li>
 *  <li>sets application properties sufficient to use Spring beans in integration tests</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("local")
@AutoConfigureWireMock(port = Options.DYNAMIC_PORT)
@TestPropertySource(properties = {
        //"logging.level.root=DEBUG",

        // needed for component scanners, such as Spring Data
        "app.rootPackage=guru.nicks",
        "app.timeZone=Etc/UTC",
        "app.customEpoch=2024-08-24T00:00:00Z",
        "app.errorDictionaryDefaultLocale=en",
        "app.errorDictionary.BAD_REQUEST.en=Bad Request",

        "app.request-statistics.max-request-uris=1000",
        "app.request-statistics.retention-period=PT1H",

        // conditional bean creation is based on feature enablement
        "togglz.feature-enums=${app.rootPackage}.feature.domain.ProjectFeature",
        "togglz.console.path=/dummy/togglz-console",

        // each cache manager corresponds to a certain TTL
        "cache.inMemory.maxEntriesPerCacheManager=50000",

        // don't try to fetch remote config
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.server.vault.token=N/A",
        // don't download remote configs from Vault/Git (fix needed only if Config Server is embedded in app)
        "spring.profiles.active=native",

        // disable Eureka client and attempts to register app / fetch app config from Eureka
        "spring.cloud.config.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.serviceUrl.registerWithEureka=false",
        "eureka.client.serviceUrl.fetchRegistry=false",

        "spring.jackson.write-dates-as-timestamps=false",
        // don't create MeteredClient - there are no underlying beans for that
        "spring.cloud.openfeign.micrometer.enabled=false",

        "security.url-auth-by-role.ROLE_INTERNAL=/internal/**",
        "security.url-auth-by-role.ROLE_USER=/**",
        "security.url-auth-skip=/actuator/health",
        "security.basicAuthRealm=Credentials",
        //
        "security.actuatorUser.username=test-actuator-user",
        "security.actuatorUser.password=test-actuator-password",
        //
        "security.internalUser.username=test-internal-user",
        "security.internalUser.password=test-internal-password",
        //
        "camunda.bpm.admin-user.id=test",
        "camunda.bpm.admin-user.password=test",
        // disable async jobs, otherwise Camunda tests will stop at the first async boundary
        "camunda.bpm.job-execution.enabled.false=",
        "camunda.bpm.generic-properties.properties.historyTimeToLive=P1D",
        //
        "spring.datasource.my.host=N/A",
        "spring.datasource.my.port=3306",
        // to be overwritten by TestContainers
        "spring.datasource.my.database=N/A",
        "spring.datasource.my.options=?useUnicode=true&characterEncoding=utf8",
        //
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=validate",
        "spring.jpa.properties.hibernate.check_nullability=true",
        // fix for '50 vs 1' sequence-dependent primary keys
        "spring.jpa.properties.hibernate.id.sequence.increment_size_mismatch_strategy=fix",
        "spring.jpa.show-sql=true",
        // log SQL placeholder values
        "logging.level.org.hibernate.type.descriptor.sql=TRACE",
        "logging.level.org.hibernate.type=TRACE",
        "logging.level.org.hibernate.orm.jdbc.bind=TRACE",

        "spring.data.mongodb.my.scheme=mongodb",
        // to be overwritten by TestContainers
        "spring.data.mongodb.my.seedList=<dummy>",
        "spring.data.mongodb.my.database=<dummy>",

        // Unified changelog location for all microservices (the file should exist, at least empty).
        // YAML is supported too, but it has no validation and autocompletion during editing.
        "spring.liquibase.change-log=classpath:/db/changelog/root.xml",

        // empty (comma-separated) list means any AZP/AUD JWT claim is allowed
        "jwt.resource-server.onlyJwtAudience=",
        "jwt.resource-server.initialRolesForAutoImportedExternalProfiles=ROLE_USER",

        "hash-ids-mapper.salt=testtest",
        "hash-ids-mapper.alphabet=12345678abcdefgh",
        "hash-ids-mapper.minHashLength=3",

        "order.id.sqlSequence=order_id_seq",
        // WARNING: not for production use!
        "order.id.encryption.key=00000000000000000000000000000000",
        "order.id.encryption.tweak=00000000000000"
})
public @interface WithTestApplicationProperties {
}
