package ru.yandex.market.deliverycalculator.workflow.regularpickup;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;
import ru.yandex.market.deliverycalculator.storage.model.OutletType;
import ru.yandex.market.deliverycalculator.storage.model.ShopOutlet;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorStorageService;
import ru.yandex.market.deliverycalculator.workflow.test.AbstractTariffWorkflowTest;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;
import ru.yandex.market.deliverycalculator.workflow.util.XmlUtils;

import static ru.yandex.market.deliverycalculator.workflow.abstractworkflow.AbstractRegularTariffWorkflow.SHOP_CAMPAIGN_TYPE;

class RegularPickupTariffWorkflowTest extends AbstractTariffWorkflowTest {

    @Autowired
    private DeliveryCalculatorStorageService storageService;

    @Autowired
    @Qualifier("regularPickupTariffIndexerWorkflow")
    private RegularPickupTariffWorkflow indexerWorkflow;

    @Autowired
    @Qualifier("regularPickupTariffSearchEngineWorkflow")
    private RegularPickupTariffWorkflow searchWorkflow;

    @Test
    void basicTest() {
        long shopId = 774;
        long feedId = 55335;
        long retailOutletId = 419556;
        long depotOutletId = 419549;
        long depotOutletId2 = 719549;
        long depotOutletId3 = 919549;
        long depotOutletId4 = 999999;
        long mixedOutletId = 419550;
        double depotOutletMaxPrice = 100;
        int depotOutletPrice = 25000;
        int mixedOutletPrice = 35000;
        long generationId = 45;
        String bucketsUrl = "fakeUrl";
        DeliveryShop shop = new DeliveryShop();
        shop.setCampaignType(SHOP_CAMPAIGN_TYPE);
        shop.setId(shopId);
        shop.setOutletTariffCurrency("RUR");
        WorkflowTestUtils.createFeed(shop, feedId);
        storageService.insertShop(shop);

        ShopOutlet retailOutlet = WorkflowTestUtils.createShopOutlet(shop, retailOutletId, OutletType.RETAIL, 213);
        storageService.persistOutlet(retailOutlet);

        ShopOutlet depotOutlet = WorkflowTestUtils.createShopOutlet(shop, depotOutletId, OutletType.DEPOT, 2);
        depotOutlet.setRule(WorkflowTestUtils.createPriceRuleEntity(null, null, depotOutletMaxPrice));
        StorageTestUtils.createOption(depotOutlet.getRule(), depotOutletPrice, 4, 5, 15);
        storageService.persistOutlet(depotOutlet);

        ShopOutlet depotOutlet2 = WorkflowTestUtils.createShopOutlet(shop, depotOutletId2, OutletType.DEPOT, 2);
        depotOutlet2.setRule(WorkflowTestUtils.createPriceRuleEntity(null, null, depotOutletMaxPrice));
        StorageTestUtils.createOption(depotOutlet2.getRule(), depotOutletPrice, 4, 5, 15);
        storageService.persistOutlet(depotOutlet2);

        ShopOutlet depotOutlet3 = WorkflowTestUtils.createShopOutlet(shop, depotOutletId3, OutletType.DEPOT, 213);
        depotOutlet3.setRule(WorkflowTestUtils.createPriceRuleEntity(null, null, depotOutletMaxPrice));
        StorageTestUtils.createOption(depotOutlet3.getRule(), depotOutletPrice, 1, 3, 12);
        storageService.persistOutlet(depotOutlet3);

        ShopOutlet depotOutlet4 = WorkflowTestUtils.createShopOutlet(shop, depotOutletId4, OutletType.DEPOT, 213);
        depotOutlet4.setRule(WorkflowTestUtils.createPriceRuleEntity(null, null, depotOutletMaxPrice));
        StorageTestUtils.createOption(depotOutlet4.getRule(), depotOutletPrice, 6, 3, 12);
        storageService.persistOutlet(depotOutlet4);

        ShopOutlet mixedOutlet = WorkflowTestUtils.createShopOutlet(shop, mixedOutletId, OutletType.MIXED, 65);
        mixedOutlet.setRule(new DeliveryRuleEntity());
        StorageTestUtils.createOption(mixedOutlet.getRule(), mixedOutletPrice, 5, 6, 16);
        storageService.persistOutlet(mixedOutlet);

        Assertions.assertIterableEquals(Collections.singleton(shopId), indexerWorkflow.getNotExportedTariffIds());

        PreparedTariff preparedTariff = indexerWorkflow.prepareTariff(shopId);
        softlyAssertXmlEquals(
                XmlUtils.serialize(RegularPickupTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()),
                StringTestUtil.getString(RegularPickupTariffWorkflowTest.class, "tariff_shop_774.xml")
        );
        DeliveryCalcProtos.DeliveryOptions deliveryOptions = getDeliveryOptionsByFeed(preparedTariff);

        var deliveryOptionGroups = groupOptionGroupById(deliveryOptions);
        softlyAssertProtobufAsJson(deliveryOptionGroups, RegularPickupTariffWorkflowTest.class, "delivery_option_group_");

        var pickupBucketsById = groupPickupBucketsById(deliveryOptions);
        softlyAssertProtobufAsJson(pickupBucketsById, RegularPickupTariffWorkflowTest.class, "bucket_");

        var pickupOptionsBucketsById = groupPickupOptionsBucketsById(deliveryOptions);
        softlyAssertProtobufAsJson(pickupOptionsBucketsById, RegularPickupTariffWorkflowTest.class, "bucket_");

        Assertions.assertTrue(indexerWorkflow.isActual(shopId, preparedTariff));

        Generation gen = new Generation(generationId, generationId);
        indexerWorkflow.addToGeneration(gen, shopId, preparedTariff, bucketsUrl);

        indexerWorkflow.markTariffExported(shopId, preparedTariff, generationId);

        searchWorkflow.loadFromGeneration(gen);
    }
}
