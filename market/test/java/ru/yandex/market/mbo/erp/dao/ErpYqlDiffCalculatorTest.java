package ru.yandex.market.mbo.erp.dao;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;

public class ErpYqlDiffCalculatorTest {
    @Test
    public void testCorrectLambdaGeneration() {
        Assertions.assertThat(ErpYqlDiffCalculator.hash(Arrays.asList("category_id", "name")))
            .isEqualTo("($row) -> {\n" +
                "return Digest::FarmHashFingerprint64('salt' ||\n" +
                " String::HexEncode(COALESCE(CAST($row.category_id as String), 'null')) || '_' ||\n" +
                " String::HexEncode(COALESCE(CAST($row.name as String), 'null')) || '_');\n" +
                "};");
    }
}
