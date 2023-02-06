package ru.yandex.market.sqb.test.db;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;

/**
 * Утилитные функции для HSQLDB.
 *
 * @author vbauer
 */
@SuppressWarnings("unused")
public final class DbFunctions {

    private DbFunctions() {
        throw new UnsupportedOperationException();
    }


    @Nullable
    public static String strSum(
            @Nullable final String in, @Nullable final Boolean flag,
            @Nonnull final String[] register, @Nonnull final Integer[] counter
    ) {
        if (Boolean.TRUE.equals(flag)) {
            return register[0];
        }
        if (in != null) {
            if (register[0] == null) {
                register[0] = in;
                counter[0] = 1;
            } else {
                register[0] += in;
                counter[0]++;
            }
        }
        return null;
    }

    @Nullable
    public static String jsonValue(@Nullable final String json, @Nonnull final String format) {
        final boolean empty = StringUtils.isEmpty(json);
        return empty ? json : JsonPath.read(json, format);
    }

}
