package ru.yandex.market.sqb.test.db;

import javax.annotation.Nonnull;

/**
 * Утилитный класс для работы с SQL.
 *
 * @author Vladislav Bauer
 */
public final class DbSqlUtils {

    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_SIZE = 500;


    private DbSqlUtils() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static String limitedQuery(@Nonnull final String query) {
        return limitedQuery(query, DEFAULT_OFFSET, DEFAULT_SIZE);
    }

    @Nonnull
    public static String limitedQuery(@Nonnull final String query, final int offset, final int size) {
        return String.format("%s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", query, offset, size);
    }

}
