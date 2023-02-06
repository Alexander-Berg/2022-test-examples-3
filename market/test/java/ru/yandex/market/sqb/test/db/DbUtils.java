package ru.yandex.market.sqb.test.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.market.sqb.test.db.datasource.DbDataSourceFactory;
import ru.yandex.market.sqb.test.db.datasource.DbDataSourceHolder;
import ru.yandex.market.sqb.test.db.misc.DbOperation;
import ru.yandex.market.sqb.test.db.misc.DbOperationSet;

import static ru.yandex.market.sqb.test.db.datasource.DbDataSourceHolder.runWithDataSource;

/**
 * Утилитный класс для БД.
 *
 * @author Vladislav Bauer
 */
public final class DbUtils {

    private DbUtils() {
        throw new UnsupportedOperationException();
    }


    public static void runWithDb(@Nonnull final DbOperationSet dbOperationSet) throws Exception {
        final DataSource dataSource = DbDataSourceFactory.createRealDataSource();
        runWithDataSource(dataSource, dbOperationSet);
    }

    @SuppressWarnings("unused")
    @Nonnull
    public static List<Map<String, Object>> execQuery(@Nonnull final String sqlQuery) throws SQLException {
        return Preconditions.checkNotNull(runWithStatement(
                statement -> {
                    final ResultSet resultSet = statement.executeQuery(sqlQuery);
                    return getRows(resultSet);
                }
        ));
    }

    @Nonnull
    public static List<Map<String, Object>> getRows(@Nonnull final ResultSet resultSet) throws SQLException {
        final List<String> columnNames = DbUtils.getColumnLabels(resultSet);
        final List<Map<String, Object>> rows = Lists.newArrayList();

        while (resultSet.next()) {
            final Map<String, Object> row = Maps.newLinkedHashMap();

            for (int i = 0; i < columnNames.size(); i++) {
                final int columnIndex = i + 1;
                final Object columnValue = resultSet.getObject(columnIndex);
                final String columnName = columnNames.get(i);

                row.put(columnName, columnValue);
            }

            rows.add(row);
        }
        return rows;
    }

    @Nullable
    public static <T> T runWithStatement(@Nonnull final DbOperation<T> operation) {
        final DataSource dataSource = DbDataSourceHolder.getDataSource();
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement(
                        ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY
                )
        ) {
            return operation.process(statement);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    public static List<String> getColumnLabels(@Nonnull final ResultSet resultSet) throws SQLException {
        final ResultSetMetaData metaData = resultSet.getMetaData();
        final int columnCount = metaData.getColumnCount();

        return IntStream.rangeClosed(1, columnCount)
                .mapToObj(index -> getColumnLabel(metaData, index))
                .collect(Collectors.toList());
    }


    private static String getColumnLabel(final ResultSetMetaData metaData, final int index) {
        try {
            final String columnName = metaData.getColumnLabel(index);
            return StringUtils.upperCase(columnName);
        } catch (final Exception ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

}
