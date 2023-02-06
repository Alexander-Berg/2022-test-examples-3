package ru.yandex.market.loyalty.core.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.core.utils.LoyaltyWithClearDatasourceTypeExecutorService;
import ru.yandex.market.loyalty.lightweight.WrappingExecutorService;

import static ru.yandex.market.loyalty.core.config.DatasourceType.READ_WRITE;

@Component
public class DbCleaner {
    private int betweenVacuumCounter = 0;
    private static final int VACUUM_PERIOD = 5;

    private static final Logger logger = LogManager.getLogger(DbCleaner.class);
    private static final List<String> LOCAL_JDBC_URL_PREFIXES = Arrays.asList(
            "jdbc:postgresql://127.",
            "jdbc:postgresql://0:0:0:0:0:0:0:1",
            "jdbc:postgresql://localhost",
            "jdbc:postgresql://docker.local"
    );

    private final JdbcTemplate jdbcTemplate;
    private final WrappingExecutorService vacuumService =
            LoyaltyWithClearDatasourceTypeExecutorService.createSingle();
    private final Semaphore vacuumResource = new Semaphore(1);
    private final List<Future<?>> vacuumCalls = new ArrayList<>();

    public DbCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clearDb() {
        logger.info("Clearing DB");
        Resource truncateTablesScript = new ClassPathResource("sql/truncate_tables.sql");
        jdbcTemplate.execute((Connection con) -> {
            DatabaseMetaData metaData = con.getMetaData();
            String url = metaData.getURL();
            boolean isLocalDatabase = LOCAL_JDBC_URL_PREFIXES.stream().noneMatch(url::startsWith);
            if (isLocalDatabase) {
                throw new RuntimeException(
                        "ARE YOU SURE TO RUN truncate_tables.sql and other test stuff ON THIS DB? " + metaData.getURL());
            }
            ScriptUtils.executeSqlScript(con, truncateTablesScript);
            return null;
        });
        ++betweenVacuumCounter;
        if (betweenVacuumCounter >= VACUUM_PERIOD) {
            betweenVacuumCounter = 0;
            if (vacuumResource.tryAcquire()) {
                vacuumCalls.add(vacuumService.submit(() -> READ_WRITE.within(() -> {
                    try {
                        jdbcTemplate.update("VACUUM");
                    } finally {
                        vacuumResource.release();
                    }
                })));
            }
        }
    }

    @PreDestroy
    public void shutdown() throws InterruptedException, ExecutionException, TimeoutException {
        vacuumService.shutdown();
        vacuumService.awaitTermination(5, TimeUnit.SECONDS);
        for (Future<?> vacuumCall : vacuumCalls) {
            vacuumCall.get(5, TimeUnit.SECONDS);
        }
    }
}
