package ru.yandex.market.sqb.test.db;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Syntax;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.hsqldb.cmdline.SqlFile;

import ru.yandex.market.sqb.test.db.datasource.DbDataSourceFactory;
import ru.yandex.market.sqb.test.db.datasource.DbDataSourceHolder;
import ru.yandex.market.sqb.test.db.misc.CustomOperationLister;
import ru.yandex.market.sqb.test.db.misc.DbOperationSet;

import static ru.yandex.market.sqb.test.db.datasource.DbDataSourceHolder.runWithDataSource;

/**
 * Утилитный класс для работы с DbUnit'ом.
 *
 * @author Vladislav Bauer
 */
public final class DbUnitUtils {

    @Syntax("SQL")
    private static final String SQL_SHUTDOWN = "SHUTDOWN";


    private DbUnitUtils() {
        throw new UnsupportedOperationException();
    }


    public static void runWithDb(
            @Nullable final Supplier<String> schemaSqlReader,
            @Nullable final Supplier<String> dataSetXmlReader,
            @Nonnull final DbOperationSet operationSet
    ) throws Exception {
        final DataSource dataSource = DbDataSourceFactory.createTestDataSource();

        runWithDataSource(dataSource, () -> {
            final IDatabaseTester dbTester = onBefore(schemaSqlReader, dataSetXmlReader);
            try {
                operationSet.process();
            } finally {
                onAfter(dbTester);
            }
        });
    }


    private static IDataSet createDataSet(final Supplier<String> configReader) throws Exception {
        if (configReader == null) {
            return new DefaultDataSet();
        }

        try (StringReader reader = new StringReader(configReader.get())) {
            FlatXmlDataSet flatXmlDataSet = new FlatXmlDataSetBuilder().build(reader);
            final ReplacementDataSetBuilder replacementDataSetBuilder = new ReplacementDataSetBuilder(flatXmlDataSet);
            replacementDataSetBuilder.withSystimestampReplacement();
            return replacementDataSetBuilder.build();
        }
    }

    private static IDatabaseTester createDbTester(final IDataSet dataSet) throws Exception {
        final IDatabaseTester dbTester = new JdbcDatabaseTester(
                DbDataSourceFactory.TEST_DRIVER_CLASS,
                DbDataSourceFactory.TEST_CONNECTION_URL,
                DbDataSourceFactory.TEST_USERNAME,
                DbDataSourceFactory.TEST_PASSWORD
        );
        dbTester.setOperationListener(new CustomOperationLister());
        dbTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        dbTester.setDataSet(dataSet);
        return dbTester;
    }


    private static IDatabaseTester onBefore(
            final Supplier<String> schemaSqlReader,
            final Supplier<String> dataSetXmlReader
    ) throws Exception {
        if (schemaSqlReader != null) {
            final String content = schemaSqlReader.get();
            exec(content);
        }

        final IDataSet dataSet = createDataSet(dataSetXmlReader);
        final IDatabaseTester dbTester = createDbTester(dataSet);
        dbTester.onSetup();

        return dbTester;
    }

    private static void onAfter(final IDatabaseTester dbTester) throws Exception {
        DbUtils.execQuery(SQL_SHUTDOWN);

        if (dbTester != null) {
            dbTester.onTearDown();
        }
    }

    private static void exec(@Nonnull final String sqlQuery) throws Exception {
        final String prefix = String.valueOf(RandomUtils.nextLong(0, Integer.MAX_VALUE));
        final String suffix = String.valueOf(System.currentTimeMillis());

        final File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();

        FileUtils.write(tempFile, sqlQuery, StandardCharsets.UTF_8);

        final DataSource dataSource = DbDataSourceHolder.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            final SqlFile sqlFile = new SqlFile(tempFile);
            sqlFile.setContinueOnError(false);
            sqlFile.setConnection(connection);
            sqlFile.execute();
        }
    }

}
