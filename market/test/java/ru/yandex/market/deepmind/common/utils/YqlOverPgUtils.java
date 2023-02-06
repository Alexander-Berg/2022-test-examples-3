package ru.yandex.market.deepmind.common.utils;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.mboc.common.ReplicaCluster;

/**
 * @deprecated YqlOverPg не развивается и должен быть закопан в пользу yql-tests
 * Смотри https://st.yandex-team.ru/DEEPMIND-1961
 */
@Deprecated
public class YqlOverPgUtils {
    private static final Logger log = LoggerFactory.getLogger(YqlOverPgUtils.class);
    private static boolean transformYqlToSql = false;

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
        for (ReplicaCluster cluster : ReplicaCluster.getAllMdmClusters()) {
            sql = replaceIgnoringCase(sql, cluster + "\\.", "");
        }
        sql = sql.replace("`", "");
        sql = sql.replace("//", "mbo_category.");
        sql = sql.replace("/", "_");

        // data types
        sql = replaceIgnoringCase(sql, "Int32", "integer");
        sql = replaceIgnoringCase(sql, "UInt32", "integer");
        sql = replaceIgnoringCase(sql, "Int64", "bigint");
        sql = replaceIgnoringCase(sql, "Uint64", "bigint");

        if (!yqlSql.equals(sql)) {
            log.warn("\n" +
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
