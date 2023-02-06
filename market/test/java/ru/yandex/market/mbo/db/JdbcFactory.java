package ru.yandex.market.mbo.db;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author moskovkin@yandex-team.ru
 * @since 23.11.18
 */
public class JdbcFactory {
    private JdbcFactory() {
    }

    public static NamedParameterJdbcOperations createH2NamedJdbcOperations(Mode mode, String... sqlFiles) {
        BasicDataSource dataSource = createH2DataSource(mode, sqlFiles);
        return new NamedParameterJdbcTemplate(dataSource);
    }

    public static BasicDataSource createH2DataSource(Mode mode, String... sqlFiles) {
        String init = Arrays.stream(sqlFiles).map(sqlFile -> "RUNSCRIPT FROM '" + sqlFile + "'")
            .collect(Collectors.joining("\\;"));

        String dbName = UUID.randomUUID().toString();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=" + init +
                ";MODE=" + mode.getMode()
        );
        return dataSource;
    }

    public enum Mode {
        POSTGRES("PostgreSQL"),
        ORACLE("Oracle");

        private final String mode;

        Mode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }
}
