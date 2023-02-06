package ru.yandex.market.stat.dicts.parsers;

import com.google.common.collect.Streams;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import ru.yandex.market.stat.dicts.records.AllHidNidRecord;

import java.io.IOException;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.makeIterator;

/**
 * @author Sergei Stradomsky <sstradomsky@yandex-team.ru>
 */

public class AllHidNidParserTest {

    @Test
    public void testAllHidNidParserFromFile() throws IOException {

        DictionaryParser<AllHidNidRecord> parser =
                new AllHidNidRecord.AllHidNidParser();

        List<AllHidNidRecord> records = loadRecords(parser, "/parsers/cataloger_hid_nid_all.xml");

        AllHidNidRecord allHidNidRecordFirst = new AllHidNidRecord();
        allHidNidRecordFirst.setNavigation_tree_id(16326774L);
        allHidNidRecordFirst.setNavigation_tree_code("fmcg");
        allHidNidRecordFirst.setId(16318515L);
        allHidNidRecordFirst.setHid(90401L);
        allHidNidRecordFirst.setParent_id(0L);
        allHidNidRecordFirst.setName("Все товары");
        allHidNidRecordFirst.setUnique_name("Все товары");
        allHidNidRecordFirst.setGuru_category_id(0L);
        allHidNidRecordFirst.setIs_primary(true);
        allHidNidRecordFirst.setIs_hidden(true);
        allHidNidRecordFirst.setIs_show_in_menu(true);
        allHidNidRecordFirst.setIs_green(true);
        allHidNidRecordFirst.setIs_blue(false);
        allHidNidRecordFirst.setIs_promo(false);
        allHidNidRecordFirst.setChildren_output_type("mixed");
        allHidNidRecordFirst.setOutput_type("gurulight");
        allHidNidRecordFirst.setHas_promo(false);
        allHidNidRecordFirst.setTouch_hide(true);
        allHidNidRecordFirst.setApplication_hide(true);

        AllHidNidRecord allHidNidRecordSecond = new AllHidNidRecord();
        allHidNidRecordSecond.setNavigation_tree_id(16326774L);
        allHidNidRecordSecond.setNavigation_tree_code("fmcg");
        allHidNidRecordSecond.setId(16321241L);
        allHidNidRecordSecond.setHid(91384L);
        allHidNidRecordSecond.setParent_id(16321236L);
        allHidNidRecordSecond.setName("Овощи");
        allHidNidRecordSecond.setUnique_name("Овощи");
        allHidNidRecordSecond.setGuru_category_id(91384L);
        allHidNidRecordSecond.setIs_primary(true);
        allHidNidRecordSecond.setIs_hidden(false);
        allHidNidRecordSecond.setIs_show_in_menu(true);
        allHidNidRecordSecond.setIs_green(false);
        allHidNidRecordSecond.setIs_blue(false);
        allHidNidRecordSecond.setIs_promo(false);
        allHidNidRecordSecond.setChildren_output_type("guru");
        allHidNidRecordSecond.setOutput_type("guru");
        allHidNidRecordSecond.setHas_promo(false);
        allHidNidRecordSecond.setTouch_hide(false);
        allHidNidRecordSecond.setApplication_hide(false);

        AllHidNidRecord allHidNidRecordThird = new AllHidNidRecord();
        allHidNidRecordThird.setNavigation_tree_id(88063L);
        allHidNidRecordThird.setNavigation_tree_code("red");
        allHidNidRecordThird.setId(83835L);
        allHidNidRecordThird.setHid(90401L);
        allHidNidRecordThird.setParent_id(0L);
        allHidNidRecordThird.setName("Все товары");
        allHidNidRecordThird.setUnique_name("Все товары");
        allHidNidRecordThird.setGuru_category_id(0L);
        allHidNidRecordThird.setIs_primary(true);
        allHidNidRecordThird.setIs_hidden(true);
        allHidNidRecordThird.setIs_show_in_menu(true);
        allHidNidRecordThird.setIs_green(true);
        allHidNidRecordThird.setIs_blue(false);
        allHidNidRecordThird.setIs_promo(false);
        allHidNidRecordThird.setChildren_output_type("mixed");
        allHidNidRecordThird.setOutput_type("gurulight");
        allHidNidRecordThird.setHas_promo(false);
        allHidNidRecordThird.setTouch_hide(true);
        allHidNidRecordThird.setApplication_hide(true);

        AllHidNidRecord allHidNidRecordFourth = new AllHidNidRecord();
        allHidNidRecordFourth.setNavigation_tree_id(88063L);
        allHidNidRecordFourth.setNavigation_tree_code("red");
        allHidNidRecordFourth.setId(88026L);
        allHidNidRecordFourth.setHid(91461L);
        allHidNidRecordFourth.setParent_id(87899L);
        allHidNidRecordFourth.setName("Телефоны и умные часы");
        allHidNidRecordFourth.setUnique_name("Телефоны и умные часы");
        allHidNidRecordFourth.setGuru_category_id(0L);
        allHidNidRecordFourth.setIs_primary(true);
        allHidNidRecordFourth.setIs_hidden(false);
        allHidNidRecordFourth.setIs_show_in_menu(true);
        allHidNidRecordFourth.setIs_green(true);
        allHidNidRecordFourth.setIs_blue(true);
        allHidNidRecordFourth.setIs_promo(false);
        allHidNidRecordFourth.setChildren_output_type("mixed");
        allHidNidRecordFourth.setOutput_type("gurulight");
        allHidNidRecordFourth.setHas_promo(false);
        allHidNidRecordFourth.setTouch_hide(false);
        allHidNidRecordFourth.setApplication_hide(false);

        List<AllHidNidRecord> expected = asList(
            allHidNidRecordFirst, allHidNidRecordSecond, allHidNidRecordThird, allHidNidRecordFourth
        );

        assertThat(records, equalTo(expected));
    }

    @Test
    public void testEmptyHidNidNavigationTree() throws IOException {
        DictionaryParser<AllHidNidRecord> parser =
                new AllHidNidRecord.AllHidNidParser();

        List<AllHidNidRecord> records = loadRecords(parser, "/parsers/empty_hid_nid_navigation_tree.xml");

        List<AllHidNidRecord> expected = Collections.emptyList();

        assertThat(records, equalTo(expected));
    }

    @Test
    public void testBigHidNidNavigationTree() throws IOException {
        DictionaryParser<AllHidNidRecord> parser =
                new AllHidNidRecord.AllHidNidParser();

        List<AllHidNidRecord> records = Streams.stream(makeIterator(parser, "/parsers/cataloger_hid_nid_all_big.xml.gz"))
                .collect(Collectors.toList());

        assertThat(records.size(), equalTo(19026));
    }

}
