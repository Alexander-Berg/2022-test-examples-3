package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_PRICE;

class OrderDataFactoryAuctionPriorityTest extends BsExportSnapshotTestBase {
    private OrderDataFactory orderDataFactory;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);
    }

    @ParameterizedTest
    @EnumSource(CampaignType.class)
    void getStartTimeTest(CampaignType campaignType) {
        var campaign = createCampaign();
        campaign.withType(campaignType);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addAuctionPriority(orderBuilder, campaign);
        if (CPM_PRICE.equals(campaignType)) {
            assertThat(orderBuilder.hasAuctionPriority()).isTrue();
            assertThat(orderBuilder.getAuctionPriority()).isEqualTo(10);
        } else {
            assertThat(orderBuilder.hasAuctionPriority()).isFalse();
        }
    }
}
