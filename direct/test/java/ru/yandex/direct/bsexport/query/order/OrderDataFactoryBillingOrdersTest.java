package ru.yandex.direct.bsexport.query.order;

import com.google.common.truth.extensions.proto.FieldScope;
import com.google.common.truth.extensions.proto.FieldScopes;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.model.BillingAggregateRule;
import ru.yandex.direct.bsexport.model.BillingAggregates;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.core.entity.campaign.model.BillingAggregateCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.currency.CurrencyCode;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static ru.yandex.direct.bsexport.model.ProductType.AudioCreativeReach;
import static ru.yandex.direct.bsexport.model.ProductType.VideoCreativeReach;
import static ru.yandex.direct.bsexport.testing.data.TestOrder.cpmBannerWithUpdateInfo1Full;

class OrderDataFactoryBillingOrdersTest extends BsExportSnapshotTestBase {
    private static final Order ORDER_WITHOUT_BILLING_ORDERS = Order.newBuilder().buildPartial();
    private static FieldScope onlyBillingOrders;

    private OrderDataFactory orderDataFactory;

    @BeforeAll
    static void initializeScope() {
        Descriptors.FieldDescriptor fieldDescriptor = Order.getDescriptor().findFieldByName("BillingOrders");
        onlyBillingOrders = FieldScopes.allowingFieldDescriptors(fieldDescriptor);
    }

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);
    }

    @Test
    void campaignWithoutWallet() {
        CommonCampaign campaign = new TextCampaign()
                .withId(getCampaignId())
                .withWalletId(0L)
                .withClientId(1L);
        var orderBuilder = Order.newBuilder();

        orderDataFactory.addBillingOrders(orderBuilder, campaign);

        assertThat(orderBuilder.buildPartial())
                .withPartialScope(onlyBillingOrders)
                .isEqualTo(ORDER_WITHOUT_BILLING_ORDERS);
    }

    @Test
    void campaignWithNotAggregatedSumTest() {
        var campaignId = 1L;
        var walletId = 2L;
        CommonCampaign campaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withType(CampaignType.CPM_BANNER)
                .withWalletId(walletId)
                .withClientId(1L);

        WalletTypedCampaign walletCampaign = new WalletTypedCampaign()
                .withId(walletId)
                .withIsSumAggregated(false);
        putCampaignToSnapshot(campaign);
        putCampaignToSnapshot(walletCampaign);
        var orderBuilder = Order.newBuilder();

        orderDataFactory.addBillingOrders(orderBuilder, campaign);

        assertThat(orderBuilder.buildPartial())
                .withPartialScope(onlyBillingOrders)
                .isEqualTo(ORDER_WITHOUT_BILLING_ORDERS);
    }

    @Test
    void campaignWithOnlyDefaultProductTypeTest() {
        var campaignId = 1L;
        var walletId = 2L;
        Long billingAggregateId = 3L;
        CommonCampaign campaign = new TextCampaign()
                .withId(campaignId)
                .withWalletId(walletId)
                .withCurrency(CurrencyCode.RUB)
                .withType(CampaignType.TEXT)
                .withClientId(1L);

        WalletTypedCampaign walletCampaign = new WalletTypedCampaign()
                .withId(walletId)
                .withIsSumAggregated(true);

        var billingAggregate = new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(billingAggregateId);

        putCampaignToSnapshot(campaign);
        putCampaignToSnapshot(walletCampaign);
        putBillingAggregateToSnapshot(ProductType.TEXT, billingAggregate);
        var orderBuilder = Order.newBuilder();

        orderDataFactory.addBillingOrders(orderBuilder, campaign);

        BillingAggregates expected = BillingAggregates.newBuilder()
                .setDefault(billingAggregateId.intValue())
                .build();
        ProtoTruth.assertThat(orderBuilder.buildPartial())
                .withPartialScope(onlyBillingOrders)
                .isEqualTo(Order.newBuilder().setBillingOrders(expected).buildPartial());
    }

    @Test
    void campaignWithSpecialProductTypesTest() {
        var campaignId = 1L;
        var walletId = 2L;
        Long defaultBillingAggregateId = 3L;
        Long cpmVideoBillingAggregateId = 4L;
        Long cpmAudioBillingAggregateId = 5L;
        CommonCampaign campaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withWalletId(walletId)
                .withCurrency(CurrencyCode.RUB)
                .withType(CampaignType.CPM_BANNER)
                .withClientId(1L);

        WalletTypedCampaign walletCampaign = new WalletTypedCampaign()
                .withId(walletId)
                .withIsSumAggregated(true);

        var defaultBillingAggregate = new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(defaultBillingAggregateId);

        var cpmVideoBillingAggregate = new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(cpmVideoBillingAggregateId);

        var cpmAudioBillingAggregate = new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(cpmAudioBillingAggregateId);

        putCampaignToSnapshot(campaign);
        putCampaignToSnapshot(walletCampaign);
        putBillingAggregateToSnapshot(ProductType.CPM_BANNER, defaultBillingAggregate);
        putBillingAggregateToSnapshot(ProductType.CPM_VIDEO, cpmVideoBillingAggregate);
        putBillingAggregateToSnapshot(ProductType.CPM_AUDIO, cpmAudioBillingAggregate);
        var orderBuilder = Order.newBuilder();

        orderDataFactory.addBillingOrders(orderBuilder, campaign);

        BillingAggregates expected = BillingAggregates.newBuilder()
                .setDefault(defaultBillingAggregateId.intValue())
                .addRules(BillingAggregateRule.newBuilder()
                        .setResult(cpmVideoBillingAggregateId.intValue())
                        .addProductTypes(VideoCreativeReach)
                        .build())
                .addRules(BillingAggregateRule.newBuilder()
                        .setResult(cpmAudioBillingAggregateId.intValue())
                        .addProductTypes(AudioCreativeReach)
                        .build())
                .build();
        ProtoTruth.assertThat(orderBuilder.buildPartial())
                .withPartialScope(onlyBillingOrders)
                .isEqualTo(Order.newBuilder().setBillingOrders(expected).buildPartial());
    }

    @Test
    void campaignWithSpecialProductTypesSmokeTest() {
        Long walletId = 41603880L;
        CommonCampaign campaign = new CpmBannerCampaign()
                .withId(1L)
                .withWalletId(walletId)
                .withCurrency(CurrencyCode.RUB)
                .withType(CampaignType.CPM_BANNER)
                .withClientId(1L);
        putCampaignToSnapshot(campaign);

        WalletTypedCampaign walletCampaign = new WalletTypedCampaign()
                .withId(walletId)
                .withIsSumAggregated(true);
        putCampaignToSnapshot(walletCampaign);

        putBillingAggregateToSnapshot(ProductType.CPM_BANNER, new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(41603880L));
        putBillingAggregateToSnapshot(ProductType.CPM_VIDEO, new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(41603887L));
        putBillingAggregateToSnapshot(ProductType.CPM_AUDIO, new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(44798409L));
        putBillingAggregateToSnapshot(ProductType.CPM_INDOOR, new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(43921364L));
        putBillingAggregateToSnapshot(ProductType.CPM_OUTDOOR, new BillingAggregateCampaign()
                .withWalletId(walletId)
                .withId(41603894L));


        var orderBuilder = Order.newBuilder();
        orderDataFactory.addBillingOrders(orderBuilder, campaign);

        // реальный пример из логов
        ProtoTruth.assertThat(orderBuilder.buildPartial())
                .withPartialScope(onlyBillingOrders)
                .isEqualTo(cpmBannerWithUpdateInfo1Full);
    }
}
