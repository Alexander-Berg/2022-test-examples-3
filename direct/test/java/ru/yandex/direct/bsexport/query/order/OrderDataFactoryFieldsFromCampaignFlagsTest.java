package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDataFactoryFieldsFromCampaignFlagsTest extends BsExportSnapshotTestBase {
    private OrderDataFactory orderDataFactory;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);
    }

    @Test
    void getFieldsFromCampaignFlags_AllFlagsFalseTest() {
        var campaign = createCampaign();
        setAllFlagsAsFalse(campaign);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.getIsVirtual()).isEqualTo(0);
        assertThat(orderBuilder.hasRequireFiltrationByDontShowDomains()).isFalse();
        assertThat(orderBuilder.hasAllowAloneTrafaret()).isFalse();
        assertThat(orderBuilder.hasHasTurboApp()).isFalse();
        assertThat(orderBuilder.hasUseTurboUrl()).isFalse();
    }

    @Test
    void getFieldsFromCampaignFlags_isVirtualTest() {
        var campaign = createCampaign();
        setAllFlagsAsFalse(campaign);
        campaign.withIsVirtual(true);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.getIsVirtual()).isEqualTo(1);
    }

    @Test
    void getFieldsFromCampaignFlags_AloneTrafaretAllowedTest() {
        var campaign = createCampaign();
        setAllFlagsAsFalse(campaign);
        campaign.withIsAloneTrafaretAllowed(true);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.hasAllowAloneTrafaret()).isTrue();
        assertThat(orderBuilder.getAllowAloneTrafaret()).isEqualTo(1);
    }


    @Test
    void getFieldsFromCampaignFlags_RequireFiltrationByDontShowDomainsTest() {
        var campaign = createCampaign();
        setAllFlagsAsFalse(campaign);
        campaign.withRequireFiltrationByDontShowDomains(true);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.hasRequireFiltrationByDontShowDomains()).isTrue();
        assertThat(orderBuilder.getRequireFiltrationByDontShowDomains()).isEqualTo(1);
    }

    @Test
    void getFieldsFromCampaignFlags_HasTurboAppTest() {
        var campaign = createCampaign();
        setAllFlagsAsFalse(campaign);
        campaign.withHasTurboApp(true);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.hasHasTurboApp()).isTrue();
        assertThat(orderBuilder.getHasTurboApp()).isTrue();
    }

    @Test
    void getFieldsFromCampaignFlags_UseTurboUrl_PerformanceTest() {
        var campaign = new SmartCampaign()
                .withId(1L)
                .withType(CampaignType.PERFORMANCE);
        setAllFlagsAsFalse(campaign);
        campaign.withHasTurboSmarts(true);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.hasUseTurboUrl()).isTrue();
        assertThat(orderBuilder.getUseTurboUrl()).isEqualTo(1);
    }

    @Test
    void getFieldsFromCampaignFlags_NotUseTurboUrl_PerformanceTest() {
        var campaign = new SmartCampaign()
                .withId(1L)
                .withType(CampaignType.PERFORMANCE);
        setAllFlagsAsFalse(campaign);
        campaign.withHasTurboSmarts(false);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.hasUseTurboUrl()).isTrue();
        assertThat(orderBuilder.getUseTurboUrl()).isEqualTo(0);
    }

    @Test
    void getFieldsFromCampaignFlags_UseTurboUrl_NotPerformanceTest() {
        var campaign = createCampaign();
        campaign.setType(CampaignType.TEXT);
        setAllFlagsAsFalse(campaign);
        campaign.withHasTurboApp(true);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addFieldsFromCampaignFlags(orderBuilder, campaign);
        assertThat(orderBuilder.hasUseTurboUrl()).isFalse();
    }

    private void setAllFlagsAsFalse(CommonCampaign campaign) {
        campaign.withIsVirtual(false);
        campaign.withIsAloneTrafaretAllowed(false);
        campaign.withRequireFiltrationByDontShowDomains(false);
        campaign.withHasTurboApp(false);
        campaign.withHasTurboSmarts(false);
    }
}
