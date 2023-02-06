package ru.yandex.market.deliverycalculator.indexer.job;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.service.OutletImportService;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryOptionEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;
import ru.yandex.market.deliverycalculator.storage.model.OutletType;
import ru.yandex.market.deliverycalculator.storage.model.RegularPickupTariff;
import ru.yandex.market.deliverycalculator.storage.model.ShopOutlet;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorStorageService;
import ru.yandex.market.deliverycalculator.storage.util.StorageUtils;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@DbUnitDataSet
class OutletImporterJobTest extends FunctionalTest {
    private static final long SHOP_ID_1 = 11;
    private static final long SHOP_ID_2 = 22;
    private static final long SHOP_ID_3 = 33;
    private static final long SHOP_ID_4 = 44;

    private static final String RUR = "RUR";

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DeliveryCalculatorStorageService storageService;

    @Autowired
    private OutletImportService outletImportService;

    @Test
    void basicTestWorkingWithZippedFile() throws IOException {
        createShop(SHOP_ID_1, RUR);

        runOutletImporterJob("basicTestGz.xml.gz");

        Assertions.assertEquals(1, getAllShopOutlets().size());
    }

    @Test
    void shopOutletsTest() throws Exception {
        createShop(SHOP_ID_1, RUR);

        importOutlets("basicTest.xml");

        final List<ShopOutlet> shopOutlets = getAllShopOutlets();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, shopOutlets.size()),
                () -> {
                    final ShopOutlet outlet = shopOutlets.get(0);
                    Assertions.assertEquals(1, outlet.getId());
                    Assertions.assertEquals(OutletType.DEPOT, outlet.getType());
                    Assertions.assertEquals(213, outlet.getRegionId());
                }
        );
    }

    @Test
    void updateShopOutletTest() throws Exception {
        DeliveryShop shop1 = new DeliveryShop();
        shop1.setId(SHOP_ID_1);
        shop1.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop1);

        ShopOutlet outlet11 = WorkflowTestUtils.createShopOutlet(shop1, 1, OutletType.DEPOT, 213);
        outlet11.setRule(new DeliveryRuleEntity());
        outlet11.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet11.getRule(), 3000, 1, 5, 15);
        storageService.persistOutlet(outlet11);

        DeliveryShop shop2 = new DeliveryShop();
        shop2.setId(SHOP_ID_2);
        shop2.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop2);

        ShopOutlet outlet22 = WorkflowTestUtils.createShopOutlet(shop2, 2, OutletType.DEPOT, 213);
        outlet22.setRule(new DeliveryRuleEntity());
        outlet22.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet22.getRule(), 1000, 1, 5, 15);
        storageService.persistOutlet(outlet22);

        shop1 = storageService.getShopBriefInfo(SHOP_ID_1);
        shop2 = storageService.getShopBriefInfo(SHOP_ID_2);

        storageService.markShopPickupTariffExportedIfNotChanged(shop1.getId(), shop1.getPickupUpdateTime(), 10);
        storageService.markShopPickupTariffExportedIfNotChanged(shop2.getId(), shop2.getPickupUpdateTime(), 10);

        importOutlets("updateShopOutletTest.xml");

        shop1 = storageService.getShopBriefInfo(SHOP_ID_1);
        shop2 = storageService.getShopBriefInfo(SHOP_ID_2);

        Assertions.assertEquals(10, shop1.getPickupGenerationId().longValue());
        Assertions.assertNull(shop2.getPickupGenerationId());
    }

    @Test
    @DbUnitDataSet(
            before = "OutletImporter/ShopOutletsUpdateDeliveryOptionsTest.before.csv"
    )
    @DisplayName("Проверка, что у существующего аутлета обновляются опции доставки")
    void updateShopOutletDeliveryOptionsTest() throws Exception {
        assertDeliveryOptionForOutlet(SHOP_ID_1, 1, 0, 2, 3, 5);

        importOutlets("updateShopOutletDeliveryOptionsTest.xml");

        assertDeliveryOptionForOutlet(SHOP_ID_1, 1, 2200, 1, 4, 15);
    }

    private void assertDeliveryOptionForOutlet(long shopId, long outletId,
                                               int cost, int minDaysCount, int maxDaysCount, int orderBefore) {
        DeliveryOptionEntity option = extractDeliveryOption(storageService.getRegularPickupTariff(shopId, true),
                outletId);

        assertThat(option.getCost(), is(cost));
        assertThat(option.getMinDaysCount(), is(minDaysCount));
        assertThat(option.getMaxDaysCount(), is(maxDaysCount));
        assertThat(option.getOrderBefore(), is(orderBefore));
    }

    private DeliveryOptionEntity extractDeliveryOption(RegularPickupTariff actualRegularPickupTariff, long outletId) {
        ShopOutlet shopOutlet = actualRegularPickupTariff.getOutlets().stream()
                .filter(outlet -> outlet.getId() == outletId)
                .findFirst()
                .orElseThrow();
        assertThat(shopOutlet.getRule(), is(notNullValue()));
        assertThat(shopOutlet.getRule().getOptions(), is(notNullValue()));

        Set<DeliveryOptionEntity> options = shopOutlet.getRule().getOptions();
        assertThat(options, hasSize(1));
        return options.iterator().next();
    }

    @Test
    void deleteShopOutletTest() throws Exception {
        DeliveryShop shop1 = new DeliveryShop();
        shop1.setId(SHOP_ID_1);
        shop1.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop1);

        ShopOutlet outlet11 = WorkflowTestUtils.createShopOutlet(shop1, 1, OutletType.DEPOT, 213);
        outlet11.setRule(new DeliveryRuleEntity());
        outlet11.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet11.getRule(), 3000, 1, 5, 15);
        storageService.persistOutlet(outlet11);

        ShopOutlet outlet12 = WorkflowTestUtils.createShopOutlet(shop1, 12, OutletType.DEPOT, 213);
        outlet12.setRule(new DeliveryRuleEntity());
        outlet12.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet12.getRule(), 3000, 1, 5, 15);
        storageService.persistOutlet(outlet12);

        ShopOutlet outlet13 = WorkflowTestUtils.createShopOutlet(shop1, 13, OutletType.DEPOT, 213);
        outlet13.setRule(new DeliveryRuleEntity());
        outlet13.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet13.getRule(), 3000, 1, 5, 15);
        storageService.persistOutlet(outlet13);

        DeliveryShop shop2 = new DeliveryShop();
        shop2.setId(SHOP_ID_2);
        shop2.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop2);

        ShopOutlet outlet22 = WorkflowTestUtils.createShopOutlet(shop2, 2, OutletType.DEPOT, 213);
        outlet22.setRule(new DeliveryRuleEntity());
        outlet22.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet22.getRule(), 1000, 1, 5, 15);
        storageService.persistOutlet(outlet22);

        shop1 = storageService.getShopBriefInfo(SHOP_ID_1);
        shop2 = storageService.getShopBriefInfo(SHOP_ID_2);

        storageService.markShopPickupTariffExportedIfNotChanged(shop1.getId(), shop1.getPickupUpdateTime(), 10);
        storageService.markShopPickupTariffExportedIfNotChanged(shop2.getId(), shop2.getPickupUpdateTime(), 10);

        List<ShopOutlet> shop1Outlets = storageService.getOutletsByShopId(shop1.getId());
        List<ShopOutlet> shop2Outlets = storageService.getOutletsByShopId(shop2.getId());

        Assertions.assertEquals(3, shop1Outlets.size());
        Assertions.assertEquals(1, shop2Outlets.size());

        importOutlets("updateShopOutletTest.xml");

        shop1 = storageService.getShopBriefInfo(SHOP_ID_1);
        shop2 = storageService.getShopBriefInfo(SHOP_ID_2);

        Assertions.assertNull(shop1.getPickupGenerationId());
        Assertions.assertNull(shop2.getPickupGenerationId());

        shop1Outlets = storageService.getOutletsByShopId(shop1.getId());
        shop2Outlets = storageService.getOutletsByShopId(shop2.getId());

        Assertions.assertEquals(1, shop1Outlets.size());
        Assertions.assertEquals(1, shop2Outlets.size());

        Assertions.assertEquals(outlet11.getId(), shop1Outlets.get(0).getId());
        Assertions.assertEquals(outlet22.getId(), shop2Outlets.get(0).getId());
    }

    @Test
    void updateOutletTariffCurrencyTest() throws Exception {
        DeliveryShop shop1 = new DeliveryShop();
        shop1.setId(SHOP_ID_1);
        shop1.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop1);
        DeliveryShop shop2 = new DeliveryShop();
        shop2.setId(SHOP_ID_2);
        shop2.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop2);
        DeliveryShop shop3 = new DeliveryShop();
        shop3.setId(SHOP_ID_3);
        shop3.setOutletTariffCurrency("BYN");
        storageService.insertShop(shop3);
        DeliveryShop shop4 = new DeliveryShop();
        shop4.setId(SHOP_ID_4);
        storageService.insertShop(shop4);

        importOutlets("updateOutletTariffCurrencyTest.xml");

        shop1 = storageService.getShopBriefInfo(SHOP_ID_1);
        shop2 = storageService.getShopBriefInfo(SHOP_ID_2);
        shop3 = storageService.getShopBriefInfo(SHOP_ID_3);
        shop4 = storageService.getShopBriefInfo(SHOP_ID_4);

        Assertions.assertEquals("BYN", shop1.getOutletTariffCurrency());
        Assertions.assertEquals("RUR", shop2.getOutletTariffCurrency());
        Assertions.assertEquals("KZT", shop3.getOutletTariffCurrency());
        Assertions.assertEquals("UAH", shop4.getOutletTariffCurrency());
    }

    @Test
    void updateOutletRegionTest() throws Exception {
        createShopsForUpdateRegionTest();

        importOutlets("updateOutletRegionTest.xml");

        List<ShopOutlet> shop1Outlets = storageService.getOutletsByShopId(SHOP_ID_1);
        List<ShopOutlet> shop2Outlets = storageService.getOutletsByShopId(SHOP_ID_2);

        Assertions.assertEquals(1, shop1Outlets.size());
        Assertions.assertEquals(1, shop2Outlets.size());

        Assertions.assertEquals(213, shop1Outlets.get(0).getRegionId());
        Assertions.assertEquals(213, shop2Outlets.get(0).getRegionId());
    }

    @Test
    @DisplayName("Проверка удаления аутлетов, если в выгрузке у магазина нет аутлетов")
    @DbUnitDataSet(
            before = "OutletImporter/ShopOutletsDeleteOutdatedTest.before.csv",
            after = "OutletImporter/ShopOutletsDeleteOutdatedTest.after.csv"
    )
    void testDeleteOutdatedShopOutlets() throws Exception {
        importOutlets("deleteOutdatedShopOutlets.xml");
    }

    @Test
    void updateOutletRegionPickupGenerationTest() throws Exception {
        createShopsForUpdateRegionTest();

        var shop1 = storageService.getShopBriefInfo(SHOP_ID_1);
        var shop2 = storageService.getShopBriefInfo(SHOP_ID_2);
        storageService.markShopPickupTariffExportedIfNotChanged(shop1.getId(), shop1.getPickupUpdateTime(), 10);
        storageService.markShopPickupTariffExportedIfNotChanged(shop2.getId(), shop2.getPickupUpdateTime(), 10);

        importOutlets("updateOutletRegionTest.xml");

        Assertions.assertEquals(10, storageService.getShopBriefInfo(SHOP_ID_1).getPickupGenerationId().longValue());
        // у точки изменился регион, следовательно необходимо сбросить generation_id, чтобы тариф переварился
        Assertions.assertNull(storageService.getShopBriefInfo(SHOP_ID_2).getPickupGenerationId());
    }

    private void createShopsForUpdateRegionTest() {
        DeliveryShop shop1 = new DeliveryShop();
        shop1.setId(SHOP_ID_1);
        shop1.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop1);
        ShopOutlet outlet12 = WorkflowTestUtils.createShopOutlet(shop1, 12, OutletType.DEPOT, 213);
        outlet12.setRule(new DeliveryRuleEntity());
        outlet12.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet12.getRule(), 1000, 1, 5, 15);
        storageService.persistOutlet(outlet12);

        DeliveryShop shop2 = new DeliveryShop();
        shop2.setId(SHOP_ID_2);
        shop2.setOutletTariffCurrency("RUR");
        storageService.insertShop(shop2);
        ShopOutlet outlet22 = WorkflowTestUtils.createShopOutlet(shop2, 22, OutletType.DEPOT, 2);
        outlet22.setRule(new DeliveryRuleEntity());
        outlet22.getRule().setMaxPrice(100.0);
        StorageTestUtils.createOption(outlet22.getRule(), 1000, 1, 5, 15);
        storageService.persistOutlet(outlet22);
    }

    private void runOutletImporterJob(String filename) throws IOException {
        URL outletsURL = new ClassPathResource("OutletImporterJobTest/" + filename).getURL();
        new OutletImporterJob(outletsURL, outletImportService).doJob(null);
    }

    private void importOutlets(final String filename) throws Exception {
        try (InputStream in = this.getClass().getResourceAsStream("/OutletImporterJobTest/" + filename)) {
            outletImportService.importOutlets(in);
        }
    }

    private DeliveryShop createShop(final long shopId, final String outletCurrency) {
        final DeliveryShop shop = new DeliveryShop();
        shop.setId(shopId);
        shop.setOutletTariffCurrency(outletCurrency);
        storageService.insertShop(shop);
        return shop;
    }

    private List<ShopOutlet> getAllShopOutlets() {
        return StorageUtils.doInEntityManager(transactionTemplate, entityManager -> {
            final String hql = "select outlet from ShopOutlet outlet order by outlet.id";
            return entityManager.createQuery(hql, ShopOutlet.class).getResultList();
        });
    }
}
