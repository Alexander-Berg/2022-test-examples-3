package ru.yandex.market.stat.dicts.parsers;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.stat.dicts.records.CompatibilityDictionaryRecord;
import ru.yandex.market.stat.dicts.records.CompatibilityDictionaryRecord.CompatibilityDictionaryParser;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * @author Max Samoylov <max-samoylov@yandex-team.ru>
 */
public class CompatibilityDictionaryParserTest {

    @Test
    public void test() throws IOException {
        final List<CompatibilityDictionaryRecord> expected = asList(
                CompatibilityDictionaryRecord.builder()
                        .model_id_1(1026977L)
                        .model_id_2(1714984418L)
                        .direction("FORWARD")
                        .build(),
                CompatibilityDictionaryRecord.builder()
                        .model_id_1(14224366L)
                        .model_id_2(1714984233L)
                        .direction("BOTH")
                        .build()
        );
        final List<CompatibilityDictionaryRecord> actual = loadRecords(new CompatibilityDictionaryParser(), "/compatibilites.xml");
        assertEquals(expected, actual);
    }
}
