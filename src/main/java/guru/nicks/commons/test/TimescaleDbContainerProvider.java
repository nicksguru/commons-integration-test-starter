package guru.nicks.commons.test;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.containers.TimescaleDBContainerProvider;
import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Copy-pasted from Testcontainers' {@link TimescaleDBContainerProvider} in order to replace TimescaleDB with
 * TimescaleDB-HA. The only difference from the original class is {@link #DEFAULT_IMAGE} + {@link #getImageTag()}.
 *
 * @see <a href="https://github.com/timescale/timescaledb-docker-ha">TimescaleDB-HA on GitHub</a>
 */
public class TimescaleDbContainerProvider extends JdbcDatabaseContainerProvider {

    public static final String DEFAULT_IMAGE_TAG = "pg18.4-ts2.29.2";

    public static final String USER_PARAM = "user";

    public static final String PASSWORD_PARAM = "password";

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName
            .parse("timescale/timescaledb-ha")
            .asCompatibleSubstituteFor("postgres");

    private static final String NAME = "timescaledb";

    /**
     * NOTE: subclasses should ensure that this image version matches that in their local Docker environment.
     *
     * @return image tag to use for the container ({@value #DEFAULT_IMAGE_TAG})
     */
    public String getImageTag() {
        return DEFAULT_IMAGE_TAG;
    }

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(NAME);
    }

    @Override
    public JdbcDatabaseContainer<?> newInstance() {
        return newInstance(getImageTag());
    }

    @Override
    public JdbcDatabaseContainer<?> newInstance(String tag) {
        return new PostgreSQLContainer(DEFAULT_IMAGE.withTag(tag));
    }

    @Override
    public JdbcDatabaseContainer<?> newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }

}
