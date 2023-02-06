package ru.yandex.market.stat.dicts.parsers;

import com.google.common.collect.Streams;
import org.junit.Test;
import ru.yandex.market.stat.dicts.records.WhiteHidNidRecord;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.makeIterator;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public class CatalogerWhiteHidNidParserTest {

    @Test
    public void test() throws IOException {
        // Given
        DictionaryParser<WhiteHidNidRecord> parser = new WhiteHidNidRecord.WhiteHidNidParser();

        // When and Then
        List<WhiteHidNidRecord> WhiteHidNidRecordList = Streams.stream(makeIterator(parser, "/parsers/cataloger.navigation.xml.gz"))
                .collect(Collectors.toList());
        assertThat(WhiteHidNidRecordList, hasSize(7485));

        Map<Long, WhiteHidNidRecord> recordMap = WhiteHidNidRecordList.stream()
                .collect(Collectors.toMap(WhiteHidNidRecord::getId, Function.identity()));

        assertThat(recordMap.keySet(), hasSize(7485));

        WhiteHidNidRecord стружкоотсосы = new WhiteHidNidRecord();
        стружкоотсосы.setId(70389L);
        стружкоотсосы.setHid(14910819L);
        стружкоотсосы.setParent_id(70387L);
        стружкоотсосы.setName("Стружкоотсосы");
        стружкоотсосы.setUnique_name("Стружкоотсосы");
        стружкоотсосы.setGuru_category_id(0L);
        стружкоотсосы.setIs_primary(true);
        стружкоотсосы.setIs_hidden(false);
        стружкоотсосы.setIs_show_in_menu(true);
        стружкоотсосы.setIs_green(true);
        стружкоотсосы.setIs_blue(false);
        стружкоотсосы.setIs_promo(false);
        стружкоотсосы.setChildren_output_type("gurulight");
        стружкоотсосы.setOutput_type("gurulight");
        стружкоотсосы.setHas_promo(false);
        стружкоотсосы.setTouch_hide(false);
        стружкоотсосы.setApplication_hide(false);

        assertThat(recordMap.get(70389L), is(стружкоотсосы));
    }
}
