package ru.yandex.market.pers.address.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class DbCleaner {
    private static final Logger logger = LogManager.getLogger(DbCleaner.class);

    private final JdbcTemplate jdbcTemplate;

    public DbCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clearDb() {
        logger.info("Clearing DB");
        Resource truncateTablesScript = new ClassPathResource("sql/truncate_tables.sql");
        jdbcTemplate.execute((Connection con) -> {
            DatabaseMetaData metaData = con.getMetaData();
            String url = metaData.getURL();
            if (!url.startsWith("jdbc:postgresql://127.0.0.1")
                    && !url.startsWith("jdbc:postgresql://127.0.1.1")
                    && !url.startsWith("jdbc:postgresql://0:0:0:0:0:0:0:1")
                    && !url.startsWith("jdbc:postgresql://localhost")) {
                throw new RuntimeException("ARE YOU SURE TO RUN truncate_tables.sql and other test stuff ON THIS DB? " + metaData.getURL());
            }
            ScriptUtils.executeSqlScript(con, truncateTablesScript);
            return null;
        });
    }
}
