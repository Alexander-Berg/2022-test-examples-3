package ru.yandex.market.ff.util;

import java.util.Set;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YqlOverPgUtils {
    private static final Set<String> FOLDERS = Set.of(
            "//home/cdc/market/production/mstat/delivery_tracker/delivery_checkpoints/",
            "//home/market/production/checkouter/cdc/checkouter_main/"
    );
    private static boolean transformYqlToSql = true;

    private YqlOverPgUtils() {
    }

    public static boolean isTransformYqlToSql() {
        return transformYqlToSql;
    }

    public static void setTransformYqlToSql(boolean transformYqlToSql) {
        YqlOverPgUtils.transformYqlToSql = transformYqlToSql;
    }

    public static String convertYqlToPgSql(String yqlSql) {
        if (!transformYqlToSql) {
            return yqlSql;
        }

        String sql = yqlSql;
        sql = replaceIgnoringCase(sql, "pragma [^ ]+='[^ ]+';", "");
        for (String folder : FOLDERS) {
            sql = replaceIgnoringCase(sql, folder, "tmp_yt_");
        }
        sql = sql.replace("`", "");

        // data types
        sql = replaceIgnoringCase(sql, "Int32", "integer");
        sql = replaceIgnoringCase(sql, "UInt32", "integer");
        sql = replaceIgnoringCase(sql, "Int64", "bigint");
        sql = replaceIgnoringCase(sql, "Uint64", "bigint");

        if (!yqlSql.equals(sql)) {
            log.info("\n" +
                    "--------------- SQL WAS CHANGED! ---------------\n" +
                    "---------------------- FROM --------------------\n" +
                    "" + yqlSql + "\n" +
                    "----------------------- TO ---------------------\n" +
                    "" + sql + "\n" +
                    "----------------------- END --------------------\n"
            );
        }
        return sql;
    }

    private static String replaceIgnoringCase(String sql, String regex, String replacement) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sql).replaceAll(replacement);
    }
}

