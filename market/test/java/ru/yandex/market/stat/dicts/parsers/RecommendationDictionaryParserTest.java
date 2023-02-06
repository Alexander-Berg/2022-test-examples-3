package ru.yandex.market.stat.dicts.parsers;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.junit.Test;
import ru.yandex.market.stat.dicts.records.RecommendationDictionaryRecord;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;


/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public class RecommendationDictionaryParserTest {

    @Test
    public void test() throws IOException {
        // Given
        DictionaryParser<RecommendationDictionaryRecord> parser =
            new RecommendationDictionaryRecord.RecommendationDictionaryParser();

        // When
        List<RecommendationDictionaryRecord> records = loadRecords(parser, "/parsers/recommendation_rules.xml");
        Multimap<Long, RecommendationDictionaryRecord> id2Recommendations =
            Multimaps.index(records, RecommendationDictionaryRecord::getId);

        assertThat(records.size(), equalTo(32));
        assertThat(id2Recommendations.size(), equalTo(32));

        assertThat(id2Recommendations.get(2777L), equalTo(Collections.singletonList(
            RecommendationDictionaryRecord.builder()
                .id(2777L)
                .main_category_id(90565L)
                .linked_category_id(7718435L)
                .name("Климатизаторы")
                .link_type("BUY_WITH_IT")
                .direction("FORWARD")
                .weight(100.0)
                .rule_id(2778L)
                .rule_type("CATEGORY")
                .main_recipe("{\"id\":2779}")
                .linked_recipe("{\"id\":2780}")
                .is_published(true)
                .build()
        )));

        assertThat(id2Recommendations.get(10587L), equalTo(asList(
            RecommendationDictionaryRecord.builder()
                .id(10587L)
                .main_category_id(10755995L)
                .linked_category_id(989043L)
                .name("Колыбели_и_люльки")
                .link_type("BUY_WITH_IT")
                .direction("FORWARD")
                .weight(100.0)
                .rule_id(10588L)
                .rule_type("CATEGORY")
                .main_recipe("{\"id\":10589}")
                .linked_recipe("{\"id\":10590}")
                .is_published(true)
                .build(),
            RecommendationDictionaryRecord.builder()
                .id(10587L)
                .main_category_id(10755995L)
                .linked_category_id(989043L)
                .name("Колыбели_и_люльки")
                .link_type("BUY_WITH_IT")
                .direction("FORWARD")
                .weight(100.0)
                .rule_id(10591L)
                .rule_type("GOODS")
                .main_recipe("{\"id\":10592,\"filter\":[{\"param_id\":\"10790087\",\"param_type\":\"ENUM\",\"value_id\":\"10790090\"}]}")
                .linked_recipe("{\"id\":10593}")
                .is_published(true)
                .build()
        )));

        Collection<RecommendationDictionaryRecord> actual = id2Recommendations.get(2785L);
        List<RecommendationDictionaryRecord> expected = asList(
            RecommendationDictionaryRecord.builder()
                .id(2785L)
                .main_category_id(90578L)
                .linked_category_id(671243L)
                .name("Комплектующие")
                .link_type("ACCESSORY_HARDWARE_CONSUMABLE")
                .direction("FORWARD")
                .weight(100.0)
                .rule_id(2786L)
                .rule_type("CATEGORY")
                .main_recipe("{\"id\":2787}")
                .linked_recipe("{\"id\":2788}")
                .is_published(false)
                .build(),
            RecommendationDictionaryRecord.builder()
                .id(2785L)
                .main_category_id(90578L)
                .linked_category_id(671243L)
                .name("Комплектующие")
                .link_type("ACCESSORY_HARDWARE_CONSUMABLE")
                .direction("FORWARD")
                .weight(100.0)
                .rule_id(2789L)
                .rule_type("GOODS")
                .main_recipe("{\"id\":2790,\"filter\":[{\"param_id\":\"7534180\",\"param_type\":\"ENUM\",\"value_id\":\"12105076\"},{\"param_id\":\"7893318\",\"param_type\":\"ENUM\",\"value_id\":\"153082\"}]}")
                .linked_recipe("{\"id\":2791,\"filter\":[{\"param_id\":\"7745065\",\"param_type\":\"ENUM\",\"value_id\":\"7745069\"},{\"param_id\":\"7893318\",\"param_type\":\"ENUM\"}]}")
                .is_published(false)
                .build(),
            RecommendationDictionaryRecord.builder()
                .id(2785L)
                .main_category_id(90578L)
                .linked_category_id(671243L)
                .name("Комплектующие")
                .link_type("ACCESSORY_HARDWARE_CONSUMABLE")
                .direction("FORWARD")
                .weight(100.0)
                .rule_id(2792L)
                .rule_type("GOODS")
                .main_recipe("{\"id\":2793,\"filter\":[{\"param_id\":\"7534180\",\"param_type\":\"ENUM\",\"value_id\":\"12105077\"},{\"param_id\":\"7893318\",\"param_type\":\"ENUM\",\"value_id\":\"153061\"}]}")
                .linked_recipe("{\"id\":2794,\"filter\":[{\"param_id\":\"7745065\",\"param_type\":\"ENUM\",\"value_id\":\"7745068\"},{\"param_id\":\"7893318\",\"param_type\":\"ENUM\"}]}")
                .is_published(false)
                .build()
        );

        assertThat(actual, equalTo(expected));
    }
}
