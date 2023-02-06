package ru.yandex.autotests.testpers.misc;

import com.jcabi.jdbc.ListOutcome;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * User: lanwen
 * Date: 28.04.15
 * Time: 21:57
 */
public class OraPg {
    private String ora;
    private String pg;

    public OraPg(String ora, String pg) {
        this.ora = ora;
        this.pg = pg;
    }

    public String getOra() {
        return ora;
    }

    public String getPg() {
        return pg;
    }

    @Override
    public String toString() {
        return format("ora:%s -> pg:%s", ora, pg);
    }

    public static ListOutcome<OraPg> oraToPg() {
        return new ListOutcome<>(resultSet -> new OraPg(resultSet.getString(1), resultSet.getString(2)));
    }

    public static String replaceOraWithPg(List<OraPg> orapg, String resp, List<String> prefixes) {
        final String[] result = {resp};

        Stream.of(prefixes).forEach(prefix -> {
            for (OraPg next : orapg) {
                result[0] = result[0].replace(prefix + next.getOra(), prefix + next.getPg());
            }
        });

        return result[0];
    }

    public static String replaceOraWithPgWithoutPrefix(List<OraPg> orapg, String resp) {
        String result = resp;
        System.out.println(orapg.size() > 50 ? orapg.subList(0, 50) : orapg);
        for (OraPg next : orapg) {
            result = result.replace(next.getOra(), next.getPg());
        }
        return result;
    }
}
