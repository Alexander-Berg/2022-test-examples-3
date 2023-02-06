package ru.yandex.direct.bsexport.query.order;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDataFactoryStartTimeTest extends BsExportSnapshotTestBase {
    private OrderDataFactory orderDataFactory;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);
    }

    @Test
    void getStartTimeTest() {
        var campaign = createCampaign();
        campaign.setStartDate(LocalDate.of(2020, 5, 24));
        var startTime = orderDataFactory.getStartTime(campaign);
        assertThat(startTime).isEqualTo("20200524000000");
    }
}
