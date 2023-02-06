package ru.yandex.market.tpl.billing.config.dbunit.cleaner;

import java.net.InetAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.sql.DataSource;

import ru.yandex.market.tpl.billing.config.dbunit.configs.DatabaseCleanerConfig;
import ru.yandex.market.tpl.billing.config.dbunit.configs.SchemaCleanerConfig;
import ru.yandex.market.tpl.billing.config.dbunit.configs.SimpleSchemaCleanerConfig;
import ru.yandex.market.tpl.billing.config.dbunit.strategy.DatabaseCleanerStrategy;

public class GenericDatabaseCleaner implements DatabaseCleaner {

    private final DataSource dataSource;
    private final DatabaseCleanerConfig config;
    private final DatabaseCleanerStrategy strategy;

    public GenericDatabaseCleaner(
        DataSource dataSource,
        DatabaseCleanerConfig config,
        DatabaseCleanerStrategy strategy
    ) {
        this.dataSource = dataSource;
        this.config = config;
        this.strategy = strategy;

        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            InetAddress byName = InetAddress.getByName(new URI(url).getHost());
            if (!byName.isLoopbackAddress()) {
                throw new IllegalStateException("DataSource must be connection to local machine");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearDatabase() {
        try {
            Set<String> schemas = config.getSchemas();
            Collection<String> dbSchemas = getSchemas();
            schemas.stream()
                .filter(e -> !strategy.getMetaSchemas().contains(e))
                .filter(dbSchemas::contains)
                .forEach(this::doWithSchema);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void truncateAllTables(final DataSource dataSource, String schema) throws SQLException {
        clean(
            dataSource,
            schema,
            strategy::getFindTablesSQL,
            strategy::getBatchTruncateTablesSQL,
            strategy::getTruncateTableSQL
        );
    }

    public void resetAllSequences(final DataSource dataSource, String schema) throws SQLException {
        clean(
            dataSource,
            schema,
            strategy::getFindSequencesSQL,
            (schemaName, sequences) -> Optional.empty(),
            strategy::getResetSequenceSQL
        );
    }

    private void clean(
        DataSource dataSource,
        String schema,
        Function<String, String> findSubjectsFunction,
        BiFunction<String, Collection<String>, Optional<String>> batchCleanSubjectsFunction,
        BiFunction<String, String, String> cleanSubjectsFunction
    ) throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            Set<String> subjects = getSubjects(s, findSubjectsFunction.apply(schema), getSubjectPredicate(schema));
            if (subjects.isEmpty()) {
                return;
            }

            Optional<String> batchCleanSQL = batchCleanSubjectsFunction.apply(schema, subjects);
            if (batchCleanSQL.isPresent()) {
                s.execute(batchCleanSQL.get());
            } else {
                for (String subject : subjects) {
                    s.execute(cleanSubjectsFunction.apply(schema, subject));
                }
            }
        }
    }

    private Predicate<String> getSubjectPredicate(String schema) {
        SchemaCleanerConfig configForSchema = config.getConfigForSchema(schema);

        SimpleSchemaCleanerConfig.validate(configForSchema);
        Set<String> shouldBeTruncated = configForSchema.shouldBeTruncated();
        Set<String> shouldNotBeTruncated = configForSchema.shouldNotBeIgnored();

        if (!shouldBeTruncated.isEmpty()) {
            return e -> shouldBeTruncated.contains(e) && !shouldNotBeTruncated.contains(e);
        } else if (!shouldNotBeTruncated.isEmpty()) {
            return e -> !shouldNotBeTruncated.contains(e);
        } else {
            return e -> true;
        }
    }

    private Set<String> getSubjects(Statement s, String sql, Predicate<String> predicate) throws SQLException {
        Set<String> subjects = new HashSet<>();
        try (ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString(1);
                if (predicate.test(name)) {
                    subjects.add(name);
                }
            }
        }

        return subjects;
    }

    private Collection<String> getSchemas() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            return getSubjects(s, strategy.getSelectSchemasSQL(), e -> !strategy.getMetaSchemas().contains(e));
        }
    }

    private void doWithSchema(String schema) {
        try {
            truncateAllTables(dataSource, schema);
            if (Boolean.TRUE.equals(config.getConfigForSchema(schema).resetSequences())) {
                resetAllSequences(dataSource, schema);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

