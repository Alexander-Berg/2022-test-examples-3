package ru.yandex.market.ir.tms.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import java.util.Arrays;
import java.util.HashSet;

public class ScGenerationDiffHelperTest {

    @Test
    public void getYqlForDiff() {
        Assertions.assertThat(
            ScGenerationDiffHelper.getYqlForDiff(
                new HashSet<>(Arrays.asList("category_id", "cluster_id", "classifier_magic_id")),
                YPath.simple("//home/oldTable"),
                YPath.simple("//home/newTable"),
                YPath.simple("//home/diffTablePath"),
                1
            ))
            .isEqualTo("" +
                "pragma SimpleColumns='true';\n" +
                "pragma yt.ExpirationInterval='1d';\n" +
                "\n" +
                "$mbo_hash_s = ($row) -> {\n" +
                "    return Digest::Md5Hex(String::JoinFromList(AsList(\n" +
                "        $row.classifier_magic_id,\n" +
                "        CAST($row.category_id AS String),\n" +
                "        CAST($row.cluster_id AS String)\n" +
                "    ), ''));\n" +
                "};\n" +
                "$mbo_hash_f = ($row) -> {\n" +
                "    return Digest::Md5Hex($mbo_hash_s($row));\n" +
                "};\n" +
                "\n" +
                "$s_old = (select $mbo_hash_f(TableRow()) as mbo_hash, t.* from `//home/oldTable` as t);\n" +
                "$s_new = (select $mbo_hash_f(TableRow()) as mbo_hash, t.* from `//home/newTable` as t);\n" +
                "insert into `//home/diffTablePath`\n" +
                "  select \n" +
                "    if (t0.mbo_hash == t1.mbo_hash, t0.classifier_magic_id, t1.classifier_magic_id) " +
                "as classifier_magic_id,\n" +
                "    if (t0.mbo_hash == t1.mbo_hash, t0.category_id, t1.category_id) as category_id,\n" +
                "    if (t0.mbo_hash == t1.mbo_hash, t0.cluster_id, t1.cluster_id) as cluster_id\n" +
                "  from $s_old as t0\n" +
                "    right join $s_new as t1 on t0.classifier_magic_id = t1.classifier_magic_id;");
    }
}
