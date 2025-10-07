package guru.nicks.test;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.TimescaleDBContainerProvider;
import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Copy-pasted from Testcontainers' {@link TimescaleDBContainerProvider} in order to replace TimescaleDB with
 * TimescaleDB-HA. The only difference from the original class is {@link #DEFAULT_IMAGE} + {@value #DEFAULT_TAG}.
 *
 * @see <a href="https://github.com/timescale/timescaledb-docker-ha">TimescaleDB-HA on GitHub</a>
 */
public class TimescaleDbContainerProvider extends JdbcDatabaseContainerProvider {

    /**
     * WARNING: image version must be the same as in the local Docker environment.
     */
    public static final String DEFAULT_TAG = "pg17.6-ts2.22.0";

    public static final String USER_PARAM = "user";
    public static final String PASSWORD_PARAM = "password";
    public static final DockerImageName DEFAULT_IMAGE = DockerImageName
            .parse("timescale/timescaledb-ha")
            .asCompatibleSubstituteFor("postgres");
    private static final String NAME = "timescaledb";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new PostgreSQLContainer(DEFAULT_IMAGE.withTag(tag));
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }

}
