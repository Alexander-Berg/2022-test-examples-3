package ru.yandex.market.core.delivery.tariff.event;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.tariff.error.DeliveryRegionCheckException;
import ru.yandex.market.core.delivery.tariff.service.DeliveryTariffService;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.event.FixTariffFeedDeletedListener;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author stani on 19.12.17.
 */
class FeedBeforeDeleteEventListenerTest extends FunctionalTest {

    private static final Long FEED_ID = 1L;
    private static final Long ACTION_ID = 101L;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Autowired
    DeliveryTariffService deliveryTariffService;

    @Autowired
    FixTariffFeedDeletedListener tariffDaoEventListener;

    @Autowired
    FeedService feedService;

    @Test
    @DbUnitDataSet(
            before = "deleteUnreferencedCategoryRulesOnRemoveFeedTest.before.csv",
            after = "deleteUnreferencedCategoryRulesOnRemoveFeedTest.after.csv"
    )
    void deleteUnreferencedCategoryRulesOnRemoveFeedTest() throws DeliveryRegionCheckException {
        mockCamunda();
        feedService.removeFeed(FEED_ID, ACTION_ID);
    }

    private void mockCamunda() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        Mockito.when(mbiBpmnClient.postProcess(any())).thenReturn(response);
    }
}
