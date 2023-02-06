package ru.yandex.market.sc.core.test;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
public class TestDatabaseConfiguration {

    @Autowired
    Environment environment;

    @Value("${market.tpl.sc.postgres.use-recipe:false}")
    Boolean useRecipe;

    @Value("${sc.jdbc.schema:public}")
    String schema;

    @SneakyThrows
    @Bean(name = {"dataSource", "tmsDataSource", "longTimeoutDataSource"})
    DataSource dataSource() {
        DataSource dataSource = useRecipe ? ciDataSource() : embeddedDataSource();

        var proxyDataSource = new ProxyDataSource(dataSource);
        proxyDataSource.setDataSourceName("default");
        proxyDataSource.addListener(new DataSourceQueryCountListener());

        return proxyDataSource;
    }

    @SneakyThrows
    private void runLiquibase(Connection connection) {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new liquibase.Liquibase(
                "changelog.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts());
    }

    @SneakyThrows
    private DataSource embeddedDataSource() {
        var provider = PreparedDbProvider.forPreparer(LiquibasePreparer.forClasspathLocation("changelog.xml"));
        var connectionInfo = provider.createNewDatabase();
        return provider.createDataSourceFromConnectionInfo(connectionInfo);
    }

    @SneakyThrows
    private DataSource ciDataSource() {
        int port = Integer.parseInt(System.getenv("PG_LOCAL_PORT"));
        String database = System.getenv("PG_LOCAL_DATABASE");
        String userName = System.getenv("PG_LOCAL_USER");
        String password = System.getenv("PG_LOCAL_PASSWORD");

        String jdbcUrl = String.format(
                "jdbc:postgresql://localhost:%s/%s?user=%s",
                port, database, userName);

        var dataSource = new BasicDataSource();
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setDefaultSchema(schema);
        dataSource.setConnectionInitSqls(List.of("CREATE SCHEMA IF NOT EXISTS " + schema));

        try (var connection = dataSource.getConnection()) {
            runLiquibase(connection);
        }

        return dataSource;
    }

}
