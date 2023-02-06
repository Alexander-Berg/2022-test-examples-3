package ru.yandex.market.sqb.service.builder;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.service.config.ConfigurationModelServiceTest;
import ru.yandex.market.sqb.util.SqbGenerationUtils;
import ru.yandex.market.sqb.util.SqbRenderingUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.createReader;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.sqlFile;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.xmlFile;

/**
 * Unit-тесты для {@link QueryBuilderService}.
 *
 * @author Vladislav Bauer
 */
class QueryBuilderServiceTest {

    @Test
    void testPositive() throws Exception {
        checkQueryBuilder(ConfigurationModelServiceTest.CONFIG_CORRECT);
    }


    private void checkQueryBuilder(final String fileName) throws Exception {
        final String actualSQL = SqbGenerationUtils.generateSQL(createReader(xmlFile(fileName)));

        assertThat(actualSQL, not(emptyOrNullString()));
        assertThat(actualSQL, not(containsString(SqbRenderingUtils.DEFAULT_PREFIX)));
        assertThat(actualSQL, not(containsString(SqbRenderingUtils.DEFAULT_SUFFIX)));
        assertThat(actualSQL, not(containsString(String.valueOf(SqbRenderingUtils.DEFAULT_ESCAPE))));

        final String expectedSQL = readSQL(sqlFile(fileName));
        assertThat(expectedSQL, not(emptyOrNullString()));

        checkSQL(expectedSQL, actualSQL);
    }

    private void checkSQL(final String excepted, final String actual) throws Exception {
        final List<String> expectedLines = generateLines(excepted);
        final List<String> actualLines = generateLines(actual);

        final boolean equalCollection = CollectionUtils.isEqualCollection(expectedLines, actualLines);
        assertThat(
                String.format("%nActual:%n %s%n%nExpected: %s%n", actual, excepted),
                equalCollection, equalTo(true)
        );
    }

    private List<String> generateLines(final String text) throws IOException {
        final List<String> lines = IOUtils.readLines(new StringReader(text));

        return lines.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String readSQL(final String fileName) {
        final Supplier<String> reader = createReader(fileName);
        return reader.get();
    }

}
