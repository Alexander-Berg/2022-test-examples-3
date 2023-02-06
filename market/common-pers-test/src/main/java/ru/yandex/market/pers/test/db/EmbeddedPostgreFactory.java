package ru.yandex.market.pers.test.db;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 01.07.2021
 */
public class EmbeddedPostgreFactory {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedPostgreFactory.class);

    /**
     * Embedded postres or reciepe.
     * Starts db with url=jdbc:postgresql://localhost:#{embeddedPostgres.getPort()}/postgres
     * User/password = postgres.
     * <p>
     * Uses market.pers.use.pg.recipe environment to decide if recipe should be used (always active in ya.make).
     * <p>
     * Don't forget to close on destroy!
     */
    public static Closeable embeddedPostgres(
        Function<EmbeddedPostgres.Builder, EmbeddedPostgres.Builder> fun
    ) {
        String recipeEnv = System.getenv("market.pers.use.pg.recipe");
        LOG.info(String.format("Recipe pg flag = %s", recipeEnv));
        boolean useRecipe = recipeEnv != null && recipeEnv.equalsIgnoreCase("true");

        // initializes embedded/recipe postgres connection
        //market.pers.jdbc.writeUrl=jdbc:postgresql://localhost:#{embeddedPostgres.getPort()}/postgres
        //market.pers.jdbc.username=postgres
        //market.pers.jdbc.password=postgres
        if (useRecipe) {
            LOG.info("Running tests with postgres recipe configuration");
            return new RecipeAwarePostgres();
        }

        try {
            System.out.println("Running tests with postgres local configuration");
            return fun.apply(EmbeddedPostgres.builder()).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DataSource embeddedDatasource(Object portProvider, Map<String, String> props) {
        // portProvider should have int getPort method
        int port;
        try {
            Method getPort = portProvider.getClass().getMethod("getPort");
            port = (int) getPort.invoke(portProvider);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // datasource created as in EmbeddedPostgres.getDatabase
        final PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL("jdbc:postgresql://localhost:" + port + "/postgres");
        ds.setDatabaseName("postgres");
        ds.setUser("postgres");

        props.forEach((propertyKey, propertyValue) -> {
            try {
                ds.setProperty(propertyKey, propertyValue);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        return ds;
    }
}
