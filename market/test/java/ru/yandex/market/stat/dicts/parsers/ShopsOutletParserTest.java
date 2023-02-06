package ru.yandex.market.stat.dicts.parsers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.stat.dicts.records.ShopsOutletDictionaryRecord;
import ru.yandex.market.stat.dicts.records.ShopsOutletSelfDeliveryRuleDictionaryRecord;
import ru.yandex.market.stat.dicts.records.ShopsOutletWorkingTimeDictionaryRecord;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 26.06.17.
 */
public class ShopsOutletParserTest {
    private static final Long POINT_ID = 326623L;

    @Test
    public void testShopOutletWithDeliveryService() throws IOException {
        // Given
        DictionaryParser<ShopsOutletDictionaryRecord> parser = new ShopsOutletDictionaryRecord.ShopsOutletParser();
        // When
        List<ShopsOutletDictionaryRecord> records = loadRecords(parser, "/parsers/shopsOutlet.v2.xml");
        // Then
        assertThat(records.size(), equalTo(7));
        assertThat(records.get(0), equalTo(ShopsOutletDictionaryRecord
            .builder()
            .point_id(POINT_ID)
            .shop_point_id("326623")
            .point_name("МонолитСнаб-СПб")
            .point_type("RETAIL")
            .is_main(true)
            .locality_name("Санкт-Петербург")
            .thoroughfare_name("Рижская")
            .premise_number("5")
            .estate("оф. 310")
            .block("1")
            .address_add("Под <sender>арку направо.</sender> Наши соседи -- магазины Кей и <strike>Лабива</strike>..")
            .region_id(2L)
            .delivery_id(103L)
            .shop_id(null)
            .build()
        ));
    }

    @Test
    public void testShopOutlet() throws IOException {
        // Given
        DictionaryParser<ShopsOutletDictionaryRecord> parser = new ShopsOutletDictionaryRecord.ShopsOutletParser();
        // When
        List<ShopsOutletDictionaryRecord> records = loadRecords(parser, "/parsers/shopsOutlet.v2.xml");
        // Then
        assertThat(records.size(), equalTo(7));

        assertThat(records.get(1), equalTo(ShopsOutletDictionaryRecord
            .builder()
            .point_id(540340L)
            .shop_point_id("540340")
            .point_name("Центральный офис")
            .point_type("MIXED")
            .is_main(true)
            .locality_name("Санкт-Петербург")
            .thoroughfare_name("Санкт-Петербург, Шафировский пр.,")
            .premise_number("17")
            .estate(null)
            .block(null)
            .address_add("офис 2")
            .region_id(2L)
            .delivery_id(null)
            .shop_id(393223L)
            .build()
        ));
    }

    @Test
    public void testShopOutletSelfDeliveryRules() throws IOException {
        // Given
        DictionaryParser<ShopsOutletSelfDeliveryRuleDictionaryRecord> parser = new ShopsOutletSelfDeliveryRuleDictionaryRecord.ShopsOutletSelfDeliveryRuleParser();
        // When
        List<ShopsOutletSelfDeliveryRuleDictionaryRecord> records = loadRecords(parser, "/parsers/shopsOutlet.v2.xml");
        // Then
        assertThat(records.size(), equalTo(7));
        ShopsOutletSelfDeliveryRuleDictionaryRecord record = new ShopsOutletSelfDeliveryRuleDictionaryRecord();
        record.setPrice_from(null);
        record.setPrice_to(null);
        record.setCost(BigDecimal.valueOf(0L));
        record.setMin_delivery_days(0);
        record.setMax_delivery_days(2);
        record.setUnspecified_delvery_interval(false);
        record.setWork_in_holiday(true);
        record.setDate_switch_hour(24);
        record.setShipper_id(99L);
        record.setShipper_name("Собственная служба");
        record.setShipper_human_readable_id("Self");
        record.setPoint_id(POINT_ID);
        assertThat(records.get(0), equalTo(record));
    }

    @Test
    public void testShopOutletWorkingTime() throws IOException {
        // Given
        DictionaryParser<ShopsOutletWorkingTimeDictionaryRecord> parser = new ShopsOutletWorkingTimeDictionaryRecord.ShopsOutletWorkingTimeParser();
        // When
        List<ShopsOutletWorkingTimeDictionaryRecord> records = loadRecords(parser, "/parsers/shopsOutlet.v2.xml");
        // Then
        assertThat(records.size(), equalTo(8));
        ShopsOutletWorkingTimeDictionaryRecord record = new ShopsOutletWorkingTimeDictionaryRecord();
        record.setWorking_days_from(1);
        record.setWorking_days_till(5);
        record.setWorking_hours_from("9:00");
        record.setWorking_hours_till("18:00");
        record.setPoint_id(POINT_ID);
        assertThat(records.get(0), equalTo(record));
    }
}
