package ru.yandex.direct.bsexport.query.order;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.exception.ObjectNotFoundInSnapshotException;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.bsexport.snapshot.model.QueuedCampaign;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderDataFactoryGetQueueSetTimeTest extends BsExportSnapshotTestBase {

    private OrderDataFactory orderDataFactory;
    private BaseCampaign campaign;
    private QueuedCampaign queuedCampaign;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);

        campaign = createCampaign();

        queuedCampaign = new QueuedCampaign()
                .withId(campaign.getId());
        putQueuedCampaignToSnapshot(queuedCampaign);
    }


    @Test
    void testQueueSetTimeFormat1() {
        queuedCampaign.setQueueTime(LocalDateTime.of(2019, 8, 14, 20, 45, 20));
        assertThat(orderDataFactory.getQueueSetTime(campaign)).isEqualTo("20190814204520");
    }

    @Test
    void testQueueSetTimeFormat2() {
        queuedCampaign.setQueueTime(LocalDateTime.of(2019, 11, 6, 0, 4, 2));
        assertThat(orderDataFactory.getQueueSetTime(campaign)).isEqualTo("20191106000402");
    }

    @Test
    void testQueueSetTimeFormat3() {
        queuedCampaign.setQueueTime(LocalDateTime.of(2020, 2, 29, 23, 49, 55));
        assertThat(orderDataFactory.getQueueSetTime(campaign)).isEqualTo("20200229234955");
    }

    @Test
    void testQueueSetTimeFormat4() {
        queuedCampaign.setQueueTime(LocalDateTime.of(2019, 12, 30, 20, 16, 47));
        assertThat(orderDataFactory.getQueueSetTime(campaign)).isEqualTo("20191230201647");
    }

    @Test
    void queuedCampaignNotFoundInSnapshot_throwsException() {
        // практически невозможный сценарий
        removeQueuedCampaignFromSnapshot(campaign.getId());

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> orderDataFactory.getQueueSetTime(campaign));
    }
}
