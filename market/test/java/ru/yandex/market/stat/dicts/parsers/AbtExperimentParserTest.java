package ru.yandex.market.stat.dicts.parsers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.stat.dicts.records.AbtDictionaryRecord;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class AbtExperimentParserTest {
    @Test
    public void test() throws IOException {
        // Given
        DictionaryParser<AbtDictionaryRecord> parser = new AbtDictionaryRecord.AbtDictionaryParser();

        // When
        List<AbtDictionaryRecord> records = loadRecords(parser, "/parsers/abt.json.gz");
        Map<Integer, AbtDictionaryRecord> idToRecord = Maps.uniqueIndex(records, AbtDictionaryRecord::getTest_id);

        // Then
        assertThat(records.size(), equalTo(59));
        assertThat(idToRecord.get(45020), equalTo(
            AbtDictionaryRecord.builder()
                .test_id(45020)
                .title("Обратный эксперимент с саджестом тача. Строчный.")
                .rfactors(Collections.emptyMap())
                .date_modified(LocalDate.of(2017, Month.JUNE, 1))
                .ticket("EXPERIMENTS-14112")
                .build()
        ));
        assertThat(idToRecord.get(44953), equalTo(
            AbtDictionaryRecord.builder()
                .test_id(44953)
                .title("Параметрический поиск в гуре test")
                .rfactors(ImmutableMap.of("market_disable_guru_parametric_search_for_first_kind_filters", "1"))
                .date_modified(LocalDate.of(2017, Month.JUNE, 1))
                .ticket("EXPERIMENTS-14094")
                .build()
        ));
    }
}
