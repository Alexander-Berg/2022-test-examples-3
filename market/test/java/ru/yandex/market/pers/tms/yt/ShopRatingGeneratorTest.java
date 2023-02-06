package ru.yandex.market.pers.tms.yt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.grade.core.db.GradeMasterJdbc;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.yt.dumper.dumper.YtExportHelper;

public class ShopRatingGeneratorTest extends MockedPersTmsTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(true);
    private static final Instant SOME_INSTANT = Instant.parse("2007-12-03T10:15:30.00Z");

    @Autowired
    ShopRatingGenerator shopRatingGenerator;
    @Autowired
    YtExportHelper ytExportHelper;
    @Autowired
    GradeMasterJdbc gradeMasterJdbc;

    @Captor
    ArgumentCaptor<YPath> srcYtPathCaptor;
    @Captor
    ArgumentCaptor<Function<JsonNode, Object[]>> mapperCaptor;
    @Captor
    ArgumentCaptor<Consumer<List<Object[]>>> consumerCaptor;

    @Test
    public void saveBatchToPostgresTest() {
        JsonNode shop1node1 = createShopRatingNode(1, SOME_INSTANT, 0);
        JsonNode shop2node1 = createShopRatingNode(2, SOME_INSTANT, 1);
        JsonNode shop1node2 = createShopRatingNode(1, SOME_INSTANT, 2);
        JsonNode shop1node3 = createShopRatingNode(1, SOME_INSTANT.plus(1, ChronoUnit.DAYS), 3);

        shopRatingGenerator.appendHistoryInPg();
        Mockito.verify(ytExportHelper.getHahnYtClient()).consumeTableBatched(
                srcYtPathCaptor.capture(),
                Mockito.anyInt(),
                mapperCaptor.capture(),
                consumerCaptor.capture()
        );
        Assert.assertEquals(
                "//home/market/junit-test/pers-grade/tables/shop_rating_full/current",
                srcYtPathCaptor.getValue().toString()
        );
        Function<JsonNode, Object[]> mapper = mapperCaptor.getValue();
        Consumer<List<Object[]>> consumer = consumerCaptor.getValue();
        List<Object[]> valuesToSave = Stream.of(shop1node1, shop2node1, shop1node2, shop1node3)
                .map(mapper)
                .collect(Collectors.toList());

        //Имитация сохранения первой пачки в первый день. Тут 2 записи по 2 разным магазинам. Все должны сохраниться.
        consumer.accept(valuesToSave.subList(0, 2));
        checkShopRatingHistorySize(2);
        checkSavedRatingTypes(Set.of(0, 1));

        //Имитация сохранения второй пачки в первый день, когда почему-то пришла ещё одна запись по первому магазину
        //Она не должна сохраниться, т.е. содержимое таблицы должно остаться неизменным.
        consumer.accept(valuesToSave.subList(2, 3));
        checkShopRatingHistorySize(2);
        checkSavedRatingTypes(Set.of(0, 1));

        //Имитация сохранения пачки на следующий день, Пришла очередная запись по магазину, по которому уже была запись
        //вчера, но поскольку дата новая - новая запись должна быть добавлена в таблицу
        consumer.accept(valuesToSave.subList(3, 4));
        checkShopRatingHistorySize(3);
        checkSavedRatingTypes(Set.of(0, 1, 3));
    }

    void checkShopRatingHistorySize(long expectedSize) {
        Assert.assertEquals(expectedSize, (long) gradeMasterJdbc.getPgJdbcTemplate().queryForObject(
                "select count(*) from shop_rating_history",
                Long.class
        ));
    }

    void checkSavedRatingTypes(Set<Integer> expectedSavedRatingTypes) {
        List<Integer> actualRatingTypes =
                gradeMasterJdbc.getPgJdbcTemplate().queryForList(
                "select distinct rating_type from shop_rating_history",
                Integer.class
        );
        Assert.assertEquals(expectedSavedRatingTypes, new HashSet<>(actualRatingTypes));
    }

    JsonNode createShopRatingNode(long shopId, Instant eventTime, int ratingType) {
        ObjectNode result = new ObjectNode(JSON_NODE_FACTORY);
        result.put("shop_id", shopId);
        result.put("event_time", eventTime.getEpochSecond());
        result.put("rating_type", ratingType);
        result.put("rating", 0.0);
        result.put("rating_near", 0.0);
        result.put("rating_total", 0.0);
        result.put("rating_old", 0.0);
        result.put("grade_cnt_verified", 0L);
        result.put("grade_cnt_unverified", 0L);
        result.put("grade_cnt_text", 0L);
        result.put("grade_cnt_notext", 0L);
        result.put("grade_cnt_1", 0L);
        result.put("grade_cnt_2", 0L);
        result.put("grade_cnt_3", 0L);
        result.put("grade_cnt_4", 0L);
        result.put("grade_cnt_5", 0L);
        result.put("grade_cnt_total_verified", 0L);
        result.put("grade_cnt_total_unverified", 0L);
        result.put("grade_cnt_total_text", 0L);
        result.put("grade_cnt_total_notext", 0L);
        result.put("grade_cnt_total_1", 0L);
        result.put("grade_cnt_total_2", 0L);
        result.put("grade_cnt_total_3", 0L);
        result.put("grade_cnt_total_4", 0L);
        result.put("grade_cnt_total_5", 0L);
        result.put("grade_cnt_text_v", 0L);
        result.put("grade_cnt_text_uv", 0L);
        result.put("grade_cnt_notext_v", 0L);
        result.put("grade_cnt_notext_uv", 0L);
        result.put("grade_cnt_total_text_v", 0L);
        result.put("grade_cnt_total_text_uv", 0L);
        result.put("grade_cnt_total_notext_v", 0L);
        result.put("grade_cnt_total_notext_uv", 0L);
        result.put("skk_disabled", 0);
        return result;
    }

}
