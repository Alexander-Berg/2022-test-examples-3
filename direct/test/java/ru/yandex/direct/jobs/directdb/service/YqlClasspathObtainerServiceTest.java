package ru.yandex.direct.jobs.directdb.service;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@ExtendWith(SpringExtension.class)
@JobsTest
class YqlClasspathObtainerServiceTest {

    @Autowired
    private YqlClasspathObtainerService service;

    @Test
    void shouldReturnCollectionOfPairs() {
        String convert = "$getCurrencyRate = ($code) -> {\n" +
                "    RETURN\n" +
                "        CASE $code\n" +
                "            WHEN 'RUB' THEN 1.0\n" +
                "            WHEN 'YND_FIXED' THEN 30.0\n" +
                "            ELSE NULL\n" +
                "        END\n" +
                "};\n" +
                "$countCurrencySum = ($sum, $rate) -> {\n" +
                "    RETURN IF($rate IS NOT NULL, Math::Round(CAST($sum AS Double) * CAST($rate AS Double), -6), " +
                "NULL);\n" +
                "};\n" +
                "$convertToRub = ($sum, $code) -> {\n" +
                "    RETURN CAST($countCurrencySum($sum, $getCurrencyRate($code)) AS String);\n" +
                "};";

        Pair<String, String> yql1 = Pair.of(
                "test1.yql",
                "\nPRAGMA yt.Pool = 'home-direct-db-testing';\nUSE hahn;\n\n" + convert + "\nSELECT 1;\n"
        );
        Pair<String, String> yql2 = Pair.of(
                "test2.yql",
                "\nPRAGMA yt.Pool = 'home-direct-db-testing';\nUSE hahn;\n\n" + convert + "\nSELECT 1 + 1;\n"
        );

        Collection<Pair<String, String>> pairs = service.obtainYqlQueriesFromClassPath();

        assertThat(yql1).isIn(pairs);
        assertThat(yql2).isIn(pairs);
    }

}
