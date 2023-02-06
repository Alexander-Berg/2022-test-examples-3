package ru.yandex.market.partner.mvc.controller.feed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;

/**
 * Функциональные тесты на {@link FeedController}.
 *
 * @author fbokovikov
 */
class FeedControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    /**
     * Тест на {@link FeedController#removeFeed(long, PartnerDefaultRequestHandler.PartnerHttpServRequest) удаление
     * фида}
     */
    @Test
    @DbUnitDataSet(before = "dataset/FeedControllerFunctionalTest.csv")
    @DbUnitDataSet(before = "testFeedRemove.before.csv", after = "testFeedRemove.after.csv")
    void testFeedRemove() {
        mockCamunda();
        FunctionalTestHelper.delete(baseUrl + buildFeedUrl(79620, 10));
    }

    private String buildFeedUrl(long datasourceId, long validationId) {
        return String.format("/feed?datasourceId=%d&validation_id=%d&feed_id=15&_user_id=12345", datasourceId,
                validationId);
    }

    private void mockCamunda() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        Mockito.when(mbiBpmnClient.postProcess(any())).thenReturn(response);
    }

    /**
     * Тест на /feed {@link FeedController} взятие фидов
     */
    @Test
    @DbUnitDataSet(before = "dataset/FeedControllerFunctionalTest.csv")
    @DbUnitDataSet(before = "testGetFeed.before.csv")
    @DisplayName("Проверка, что берется фид")
    void testGetFeed() {
        var response = FunctionalTestHelper.get(baseUrl + "/feeds?_user_id=12345&_remote_ip=78.140.5" +
                ".12&campaign_id=1888&need_actual_state=true");
        JsonTestUtil.assertEquals(response, getClass(), "get_feed.json");
    }

    /**
     * Тест на /feed {@link FeedController} взятие фидов
     */
    @Test
    @DbUnitDataSet(before = "dataset/DbsFeedControllerFunctionalTest.csv")
    @DisplayName("Проверка, что берется дбсный виртуальный фид")
    void testGetDbsVirtualFeed() {
        var response = FunctionalTestHelper.get(baseUrl + "/feeds?_user_id=12345&_remote_ip=78.140.5" +
                ".12&campaign_id=1&need_actual_state=true");
        JsonTestUtil.assertEquals(response, getClass(), "get_dbs_virt_feed.json");
    }

    @Test
    @DbUnitDataSet(before = "feedCategoryChildrenTest.before.csv")
    @DisplayName("Проверяем получение рутовых категорий если нет с null parent_category_id")
    void testGetFeedRootCategories() {
        ResponseEntity<String> res = FunctionalTestHelper.get(baseUrl + "/feed/children-category?feed_id=2326");
        JsonTestUtil.assertEquals(res, this.getClass(), "feedControllerFeedRootCategories.json");
    }

    @Test
    @DbUnitDataSet(before = "feedCategoryChildrenTest.before.csv")
    @DisplayName("Проверяем получение рутовых категорий если есть с null parent_category_id")
    void testGetNullFeedRootCategories() {
        ResponseEntity<String> res = FunctionalTestHelper.get(baseUrl + "/feed/children-category?feed_id=2327");
        JsonTestUtil.assertEquals(res, this.getClass(), "feedControllerNullFeedRootCategories.json");
    }

    @Test
    @DbUnitDataSet(before = "feedCategoryChildrenTest.before.csv")
    @DisplayName("Проверяем получение дочерних категорий по родительской")
    void testGetFeedChildCategories() {
        ResponseEntity<String> res = FunctionalTestHelper.get(baseUrl + "/feed/children-category?feed_id=2326" +
                "&parent_category_ids=1013");
        JsonTestUtil.assertEquals(res, this.getClass(), "feedControllerFeedCategories.json");
    }
}
