package ru.yandex.market.mbo.lightmapper.benchmark.state;

import javax.sql.DataSource;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.postgres.spring.configs.PGDatabaseConfig;
import ru.yandex.market.mbo.lightmapper.benchmark.config.DbTestConfiguration;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@State(Scope.Benchmark)
public class ConnectionState {
    private final TransactionTemplate transactionTemplate;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public ConnectionState() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        PGaaSZonkyInitializer pgInitializer = new PGaaSZonkyInitializer();
        pgInitializer.initialize(context);
        context.register(DbTestConfiguration.class);
        context.refresh();
        DbTestConfiguration config = context.getBean(DbTestConfiguration.class);
        PGDatabaseConfig datasourceConfig = context.getBean(PGDatabaseConfig.class);

        this.transactionTemplate = config.transactionTemplate();
        this.jdbcTemplate = config.namedParameterJdbcTemplate();
        this.dataSource = datasourceConfig.dataSource();
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Setup
    public void setup() {
        JdbcOperations operations = jdbcTemplate.getJdbcOperations();
        operations.execute("drop table if exists test");
        operations.execute("create unlogged table test (" +
                "   id serial primary key," +
                "   name text not null," +
                "   date timestamptz," +
                "   data jsonb," +
                "   mapping_id bigint" +
                ")");
    }

    @TearDown
    public void teardown() {
        jdbcTemplate.getJdbcOperations().execute("drop table if exists test");
    }
}
