package ru.yandex.market.stat.dicts.parsers;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.stat.dicts.loaders.DictionaryLoadIterator;
import ru.yandex.market.stat.dicts.records.CatalogerDictionaryRecord;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.makeIterator;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public class CatalogerDictionaryParserTest {

    @Test
    public void test() throws Exception {
        // Given
        DictionaryParser<CatalogerDictionaryRecord> parser = new CatalogerDictionaryRecord.CatalogerDictionaryParser();

        // When and Then
        long count = 0;
        int checkedRecords = 0;

        try (DictionaryLoadIterator<CatalogerDictionaryRecord> iterator = makeIterator(parser, "/parsers/cataloger.catalog_dump.xml.gz")) {
            for (CatalogerDictionaryRecord record : (Iterable<CatalogerDictionaryRecord>) () -> iterator) {
                if (record.getId() == 90555L) {
                    assertThat(record, equalTo(
                        CatalogerDictionaryRecord.builder()
                            .id(90555L)
                            .parent(944108L)
                            .name("Наушники и Bluetooth-гарнитуры")
                            .uniq_name("Наушники и Bluetooth-гарнитуры")
                            .description("")
                            .output_type("guru")
                            .market_view("list")
                            .visual(false)
                            .visual_show_with_childs(true)
                            .output_category_id(119075L)
                            .model_list_id(119075L)
                            .position(100L)
                            .special(0L)
                            .related_categories(
                                Arrays.stream("91107,2724669,6368403".split(","))
                                    .map(String::trim).map(Long::parseLong).collect(Collectors.toList())
                            )
                            .build()
                    ));
                    checkedRecords++;
                }
                if (record.getId() == 90764L) {
                    assertThat(record, equalTo(
                        CatalogerDictionaryRecord.builder()
                            .id(90764L)
                            .parent(90401L)
                            .name("Детские товары")
                            .uniq_name("Детские товары")
                            .description("Детские товары на Яндекс.Маркете. Выбирайте и покупайте игрушки, товары для малышей, все для школы и детского творчества.")
                            .output_type("clusters")
                            .market_view("list")
                            .visual(true)
                            .visual_show_with_childs(true)
                            .output_category_id(10831931L)
                            .model_list_id(0L)
                            .position(100L)
                            .special(0L)
                            .related_categories(
                                Arrays.stream("90680,90787,91528,91548,966984,91572,91543".split(","))
                                    .map(String::trim).map(Long::parseLong).collect(Collectors.toList())
                            )
                            .build()
                    ));
                    checkedRecords++;
                }
                count++;
            }
        }

        assertThat(count, equalTo(2721L));
        assertThat(checkedRecords, equalTo(2));
    }
}
