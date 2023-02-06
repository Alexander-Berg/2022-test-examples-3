package ru.yandex.market.sc.load;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestDatabaseConfiguration {

    @Value("${market.tpl.sc.postgres.use-recipe:false}")
    Boolean useRecipe;

    @Value("${sc.jdbc.schema:public}")
    String schema;

    @Value("${sc.jdbc.changelog.path:liquibase/pg.schema.xml}")
    String changelog;

    @Bean
    DataSource dataSource() {
        return createDataSourceOrThrow();
    }

    private DataSource createDataSourceOrThrow() {
        try {
            if (!useRecipe) {
                var provider = PreparedDbProvider.forPreparer(LiquibasePreparer.forClasspathLocation(changelog));
                var connectionInfo = provider.createNewDatabase();
                return provider.createDataSourceFromConnectionInfo(connectionInfo);
            }

            int port = Integer.parseInt(System.getenv("PG_LOCAL_PORT"));
            String database = System.getenv("PG_LOCAL_DATABASE");
            String userName = System.getenv("PG_LOCAL_USER");
            String password = System.getenv("PG_LOCAL_PASSWORD");

            String jdbcUrl = String.format("jdbc:postgresql://localhost:%s/%s?user=%s", port, database, userName);

            BasicDataSource ds = new BasicDataSource();
            ds.setUsername(userName);
            ds.setPassword(password);
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUrl(jdbcUrl);
            ds.setDefaultSchema(schema);
            ds.setConnectionInitSqls(List.of("CREATE SCHEMA IF NOT EXISTS " + schema));

            try (var connection = ds.getConnection()) {
                runLiquibase(connection);
            }

            return ds;
        } catch (SQLException | LiquibaseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void runLiquibase(Connection connection) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(
                changelog, new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts());
    }

}
