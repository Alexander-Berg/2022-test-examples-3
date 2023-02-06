package ru.yandex.market.markup2.tasks.card_duplicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.markup2.utils.cards.CardType;
import ru.yandex.market.markup2.utils.report.ReportCard;
import ru.yandex.market.markup2.utils.top.QueriesType;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author sergtru
 * @since 19.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CardDuplicatesPayloadTest {
    @Test
    public void appendTest() throws Exception {
        CardGenerator cards = new CardGenerator();
        CardDuplicatesPayload expected = cards.generatePayload(1);
        CardDuplicatesPayload addition = cards.generatePayload(2);
        expected.append(addition);
        Assert.assertEquals(new HashSet<>(Arrays.asList(101L, 102L)), expected.getSerpIds());
    }

    @Test
    public void serializationTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CardGenerator cards = new CardGenerator();

        CardDuplicatesPayload expected1 = cards.generatePayload(1, QueriesType.SEARCH);
        String encoded1 = mapper.writeValueAsString(expected1);
        CardDuplicatesPayload actual1 = mapper.readValue(encoded1, CardDuplicatesPayload.class);
        Assert.assertEquals(encoded1, expected1, actual1); //identity only
        Assert.assertEquals(encoded1, expected1.getSerpIds(), actual1.getSerpIds());
        Assert.assertEquals(encoded1, expected1.getQuery(), actual1.getQuery());
        Assert.assertEquals(encoded1, expected1.getGlFilters(), actual1.getGlFilters());
        Assert.assertEquals(encoded1, expected1.getPage(), actual1.getPage());
        Assert.assertEquals(encoded1, expected1.getQueryType(), actual1.getQueryType());
        Assert.assertTrue(EqualsBuilder.reflectionEquals(expected1.getHitmanRequest(), actual1.getHitmanRequest()));

        CardDuplicatesPayload expected2 = cards.generatePayload(2, QueriesType.FILTERS);
        String encoded2 = mapper.writeValueAsString(expected2);
        CardDuplicatesPayload actual2 = mapper.readValue(encoded2, CardDuplicatesPayload.class);
        Assert.assertEquals(encoded2, expected2, actual2); //identity only
        Assert.assertEquals(encoded2, expected2.getSerpIds(), actual2.getSerpIds());
        Assert.assertEquals(encoded2, expected2.getQuery(), actual2.getQuery());
        Assert.assertEquals(encoded2, expected2.getGlFilters(), actual2.getGlFilters());
        Assert.assertEquals(encoded2, expected2.getPage(), actual2.getPage());
        Assert.assertEquals(encoded2, expected2.getQueryType(), actual2.getQueryType());
        Assert.assertTrue(EqualsBuilder.reflectionEquals(expected2.getHitmanRequest(), actual2.getHitmanRequest()));

        CardDuplicatesPayload expected3 = cards.generatePayload(3, QueriesType.CATEGORY_PAGES);
        String encoded3 = mapper.writeValueAsString(expected3);
        CardDuplicatesPayload actual3 = mapper.readValue(encoded3, CardDuplicatesPayload.class);
        Assert.assertEquals(encoded3, expected3, actual3); //identity only
        Assert.assertEquals(encoded3, expected3.getSerpIds(), actual3.getSerpIds());
        Assert.assertEquals(encoded3, expected3.getQuery(), actual3.getQuery());
        Assert.assertEquals(encoded3, expected3.getGlFilters(), actual3.getGlFilters());
        Assert.assertEquals(encoded3, expected3.getPage(), actual3.getPage());
        Assert.assertEquals(encoded3, expected3.getQueryType(), actual3.getQueryType());
        Assert.assertTrue(EqualsBuilder.reflectionEquals(expected3.getHitmanRequest(), actual3.getHitmanRequest()));
    }

    static class CardGenerator {
        private final int testPageNumber = 10;
        int i = 0;

        ReportCard next() {
            ReportCard result = new ReportCard(10 + i, "title " + i, "http://ya.ru/" + i,
                    "description " + i, "bikes", 507, CardType.CLUSTER);
            i++;
            return result;
        }

        CardDuplicatesPayload generatePayload(int id, QueriesType queryType) {
            CardDuplicatesPayload.Builder builder = CardDuplicatesPayload.Builder.newBuilder();
            builder.setCard1(next())
                .setCard2(next())
                .setInstruction("instruction " + id)
                .setSerpId(100 + id)
                .setQueryType(queryType.name());
            switch (queryType) {
                case SEARCH:
                    builder.setQuery("query " + id);
                    break;
                case FILTERS:
                    builder.setGlFilters("glfilters " + id);
                    break;
                case CATEGORY_PAGES:
                    builder.setPage(testPageNumber);
                    break;
                default:
                    throw new RuntimeException("Unknown queryType: " + queryType.name());
            }
            return builder.build();
        }

        CardDuplicatesPayload generatePayload(int id) {
            return generatePayload(id, QueriesType.SEARCH);
        }
    }
}
