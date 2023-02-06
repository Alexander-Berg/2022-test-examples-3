package ru.yandex.market.pers.tms.yt.yql.author;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

/**
 * @author grigor-vlad
 * 20.09.2021
 */
public class AgitationBaseTablesYqlTest extends AbstractPersYqlTest {

    @Test
    public void testOrderedModelsForGrade() {
        runTest(
            loadScript("/yql/author/agitation_grade_order.sql").requestProperty("ordered_models"),
            "/order/ordered_models_expected.json",
            "/order/agitation_grade_order.mock"
        );
    }

    @Test
    public void testAgitationVideoOrder() {
        runTest(
            loadScript("/yql/author/agitation_video_order.sql").requestProperty("yql"),
            "/order/agitation_video_order_expected.json",
            "/order/agitation_video_order.mock"
        );
    }


    @Test
    public void testAgitationIndexerModels() {
        runTest(
            loadScript("/yql/author/agitation_indexer_models.sql").requestProperty("yql"),
            "/author/base/agitation_indexer_models_expected.json",
            "/author/base/agitation_indexer_models.mock"
        );
    }

}
