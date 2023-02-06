package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDataFactoryGetInternalCampaignFieldsTest extends BsExportSnapshotTestBase {
    private OrderDataFactory orderDataFactory;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);
    }

    @Test
    void notInternalCampaign() {
        CommonCampaign campaign = new TextCampaign()
                .withId(getCampaignId())
                .withClientId(1L);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addInternalCampaignFields(orderBuilder, campaign);
        assertThat(orderBuilder.hasPlaceID()).isFalse();
        assertThat(orderBuilder.hasServiceName()).isFalse();
    }

    @Test
    void internalCampaignWithoutProduct() {
        CommonCampaign campaign = new InternalAutobudgetCampaign()
                .withId(getCampaignId())
                .withClientId(1L)
                .withType(CampaignType.INTERNAL_AUTOBUDGET)
                .withPlaceId(2L);
        var product = new InternalAdsProduct()
                .withClientId(ClientId.fromLong(3L)) // anotherClient
                .withName("browser-app");

        putInternalAdsProductToSnapshot(product);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addInternalCampaignFields(orderBuilder, campaign);
        assertThat(orderBuilder.getPlaceID()).isEqualTo(2L);
        assertThat(orderBuilder.hasServiceName()).isFalse();
    }

    @Test
    void internalCampaignWithProduct() {
        ClientId clientId = ClientId.fromLong(1L);
        CommonCampaign campaign = new InternalAutobudgetCampaign()
                .withId(getCampaignId())
                .withClientId(clientId.asLong())
                .withType(CampaignType.INTERNAL_AUTOBUDGET)
                .withPlaceId(2L);
        var product = new InternalAdsProduct()
                .withClientId(clientId)
                .withName("browser-app");

        putInternalAdsProductToSnapshot(product);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addInternalCampaignFields(orderBuilder, campaign);
        assertThat(orderBuilder.getPlaceID()).isEqualTo(2L);
        assertThat(orderBuilder.getServiceName()).isEqualTo("browser-app");
    }

}
