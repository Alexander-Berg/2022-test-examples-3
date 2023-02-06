package ru.yandex.market.stat.dicts.parsers;

import com.google.common.collect.Maps;
import org.junit.Test;
import ru.yandex.market.stat.dicts.records.ModelRatingsDictionaryRecord;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public class ModelRatingsDictionaryParserTest {

    @Test
    public void test() throws IOException {
        // Given
        DictionaryParser<ModelRatingsDictionaryRecord> parser = new ModelRatingsDictionaryRecord.ModelRatingsDictionaryParser();

        // When
        List<ModelRatingsDictionaryRecord> records = loadRecords(parser, "/parsers/models_for_market.txt.gz");
        Map<Long, ModelRatingsDictionaryRecord> idToRecord = Maps.uniqueIndex(records, ModelRatingsDictionaryRecord::getModel_id);

        // Then
        assertThat(records.size(), equalTo(57043));

        assertThat(idToRecord.get(11010064L), equalTo(
            ModelRatingsDictionaryRecord.builder()
                .model_id(11010064L)
                .rating_value(3.0)
                .rating_total(5)
                .opinions_total(4)
                .reviews_total(23)
                .build()
        ));

        assertThat(idToRecord.get(10485605L), equalTo(
            ModelRatingsDictionaryRecord.builder()
                .model_id(10485605L)
                .opinions_total(2)
                .build()
        ));

        assertThat(idToRecord.get(10485596L), equalTo(
            ModelRatingsDictionaryRecord.builder()
                .model_id(10485596L)
                .build()
        ));
    }
}
