package ru.yandex.market.stat.dicts.parsers;

import com.google.common.collect.Lists;
import org.junit.Test;
import ru.yandex.market.stat.dicts.records.ShopRatingsPersDictionaryRecord;
import ru.yandex.market.stat.dicts.utils.ParserTestUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

public class ShopRatingsPersParserTest {

    @Test
    public void test() throws IOException {
        // Given
        DictionaryParser<ShopRatingsPersDictionaryRecord> parser =
            new ShopRatingsPersDictionaryRecord.ShopRatingPersParser();

        // When
        List<ShopRatingsPersDictionaryRecord> records = loadRecords(parser, "/parsers/shop_rating.txt");

        // Then
        assertThat(records.size(), equalTo(3));

        checkContains(records,
                ShopRatingsPersDictionaryRecord.builder()
                        .shop_id(91L)
                        .new_rating(0.0)
                        .new_rating_total(3.854095144678764)
                        .rating_old(4.0)
                        .rating_old_norm(0.8)
                        .new_rating_final(3.854095144678764)
                        .rating_type(2)
                        .skk_disabled(false)
                        .force_new(true)
                        .new_grades_count_3m(0)
                        .new_grades_count(4078)
                        .rec_and_nonrec_pub_count(8502)
                        .build());

        checkContains(records,
                ShopRatingsPersDictionaryRecord.builder()
                        .shop_id(152L)
                        .new_rating(0.0)
                        .new_rating_total(4.021367521367521)
                        .rating_old(4.0)
                        .rating_old_norm(0.8)
                        .new_rating_final(4.021367521367521)
                        .rating_type(2)
                        .skk_disabled(false)
                        .force_new(true)
                        .new_grades_count_3m(0)
                        .new_grades_count(936)
                        .rec_and_nonrec_pub_count(1743)
                        .build());

        checkContains(records,
                ShopRatingsPersDictionaryRecord.builder()
                        .shop_id(179L)
                        .new_rating(4.090909090909091)
                        .new_rating_total(4.583333333333333)
                        .rating_old(5.0)
                        .rating_old_norm(1.0)
                        .new_rating_final(4.090909090909091)
                        .rating_type(3)
                        .skk_disabled(false)
                        .force_new(true)
                        .new_grades_count_3m(22)
                        .new_grades_count(312)
                        .rec_and_nonrec_pub_count(414)
                        .build());
    }

    private void checkContains(List<ShopRatingsPersDictionaryRecord> records, ShopRatingsPersDictionaryRecord record) {
        assertTrue(records.contains(record));
    }
}
