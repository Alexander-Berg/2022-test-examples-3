package ru.yandex.market.sqb.test.db.datasource;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.google.common.base.Preconditions;

import ru.yandex.market.sqb.test.db.misc.DbOperationSet;

/**
 * Хранитель текущего {@link DataSource}.
 *
 * @author Vladislav Bauer
 */
public final class DbDataSourceHolder {

    private static final InheritableThreadLocal<DataSource> DATA_SOURCE_HOLDER = new InheritableThreadLocal<>();
    private static final String ERROR_MESSAGE = "Datasource must be defined for this thread";


    private DbDataSourceHolder() {
        throw new UnsupportedOperationException();
    }


    public static void runWithDataSource(
            @Nonnull final DataSource dataSource, @Nonnull final DbOperationSet dbOperationSet
    ) throws Exception {
        try {
            DATA_SOURCE_HOLDER.set(dataSource);
            dbOperationSet.process();
        } finally {
            DATA_SOURCE_HOLDER.remove();
        }
    }

    @Nonnull
    public static DataSource getDataSource() {
        final DataSource dataSource = DATA_SOURCE_HOLDER.get();
        return Preconditions.checkNotNull(dataSource, ERROR_MESSAGE);
    }

}
