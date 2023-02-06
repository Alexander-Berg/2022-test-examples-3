package ru.yandex.market.deliverycalculator.workflow.mardopickup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffUpdatedInfo;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.LogisticsPickupPoint;
import ru.yandex.market.deliverycalculator.storage.service.LogisticsStorageService;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.workflow.FeedSource;
import ru.yandex.market.deliverycalculator.workflow.test.AbstractTariffWorkflowTest;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;
import ru.yandex.market.deliverycalculator.workflow.util.XmlUtils;

import static ru.yandex.market.deliverycalculator.storage.StorageTestUtils.initProviderMock;
import static ru.yandex.market.deliverycalculator.test.TestUtils.extractFileContent;
import static ru.yandex.market.deliverycalculator.workflow.util.XmlUtils.serialize;

class MardoPickupTariffWorkflowTest extends AbstractTariffWorkflowTest {

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Autowired
    private LogisticsStorageService logisticsStorageService;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    @Qualifier("mardoPickupTariffIndexerWorkflow")
    private MardoPickupTariffWorkflow indexerWorkflow;

    @Autowired
    @Qualifier("mardoPickupTariffSearchEngineWorkflow")
    private MardoPickupTariffWorkflow searchEngineWorkflow;

    @Test
    void basicTest() {
        // Сохранение тарифа в БД
        StorageTestUtils.initProviderMock(tariffInfoProvider, arg -> "tariff_1411.xml", getClass());
        YaDeliveryTariffUpdatedInfo tariff = WorkflowTestUtils.createMardoPickupTariff(1411, 123, 20.0, "RUR", Collections.emptySet(), Collections.emptySet());
        yaDeliveryTariffDbService.save(tariff);

        // Подготовка точек тарифа
        logisticsStorageService.savePickupPoints(List.of(
            new LogisticsPickupPoint(20000L, 123, "1"),
            new LogisticsPickupPoint(20001L, 123, "2"),
            new LogisticsPickupPoint(20002L, 123, "3"),
            new LogisticsPickupPoint(20003L, 123, "4")
        ));

        // Варка тарифа
        List<Long> tariffIds = indexerWorkflow.getNotExportedTariffIds();
        Assertions.assertEquals(1, tariffIds.size());
        long tariffId = tariffIds.get(0);
        PickupPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(tariffId);
        Generation generation = new Generation(1, 1);
        indexerWorkflow.addToGeneration(generation, tariffId, preparedTariff, "bucketsUrl");

        // Загрузка сваренного тарифа
        searchEngineWorkflow.loadFromGeneration(generation);
        Assertions.assertIterableEquals(Collections.singletonList("bucketsUrl"),
                searchEngineWorkflow.getBucketsResponseUrls(-1, FeedSource.UNKNOWN,
                        Collections.singletonList(DeliveryTariffProgramType.MARKET_DELIVERY.getSEProgramName()), 1));
    }

    @Test
    @DisplayName("Тариф самовывоза 'MARKET_DELIVERY' из тарификатора (forCustomer=false)")
    @DbUnitDataSet(before = "/tariffs/pickup/100062/forShop/market_delivery_from_tarifficator.csv")
    @DbUnitDataSet(before = "/pickuppoints/pickuppoints139.csv")
    void tariffFromTarifficatorForShop() {
        initProviderMock(tariffInfoProvider, filename -> "/tariffs/pickup/100062/forShop/tariff.xml", getClass());

        Long notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);
        PickupPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        var deliveryOptionsByFeed = getDeliveryOptionsByFeed(preparedTariff);
        var optionGroupId2OptionGroup = groupOptionGroupById(deliveryOptionsByFeed);
        var pickupBucketId2Bucket = groupPickupBucketsById(deliveryOptionsByFeed);

        softly.assertThat(preparedTariff.getMetaTariff().getRules()).hasSize(3);
        softly.assertThat(deliveryOptionsByFeed.getPickupBucketsCount()).isEqualTo(3);
        softly.assertThat(deliveryOptionsByFeed.getDeliveryOptionGroupsCount()).isEqualTo(4);
        softlyAssertXmlEquals(
                serialize(MardoPickupTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()),
                extractFileContent("tariffs/pickup/100062/forShop/tariff_meta.xml")
        );
        softlyAssertProtobufAsJson(Map.of(
                pickupBucketId2Bucket.get(1L), "tariffs/pickup/100062/forShop/bucket_1.json",
                pickupBucketId2Bucket.get(2L), "tariffs/pickup/100062/forShop/bucket_2.json",
                pickupBucketId2Bucket.get(3L), "tariffs/pickup/100062/forShop/bucket_3.json"
        ));
        softlyAssertProtobufAsJson(Map.of(
                optionGroupId2OptionGroup.get(1L), "tariffs/pickup/100062/forShop/delivery_option_group_1.json",
                optionGroupId2OptionGroup.get(2L), "tariffs/pickup/100062/forShop/delivery_option_group_2.json",
                optionGroupId2OptionGroup.get(3L), "tariffs/pickup/100062/forShop/delivery_option_group_3.json",
                optionGroupId2OptionGroup.get(4L), "tariffs/pickup/100062/forShop/delivery_option_group_4.json"
        ));
    }

    @Test
    @DisplayName("Тариф самовывоза 'MARKET_DELIVERY' из тарификатора (forCustomer=true)")
    @DbUnitDataSet(before = "/tariffs/pickup/100062/forCustomer/market_delivery_from_tarifficator.csv")
    @DbUnitDataSet(before = "/pickuppoints/pickuppoints139.csv")
    void tariffFromTarifficatorForCustomer() {
        initProviderMock(tariffInfoProvider, filename -> "/tariffs/pickup/100062/forCustomer/tariff.xml", getClass());

        Long notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);
        PickupPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        var deliveryOptionsByFeed = getDeliveryOptionsByFeed(preparedTariff);
        var optionGroupId2OptionGroup = groupOptionGroupById(deliveryOptionsByFeed);
        var pickupBucketId2Bucket = groupPickupBucketsById(deliveryOptionsByFeed);

        softly.assertThat(preparedTariff.getMetaTariff().getRules()).hasSize(3);
        softly.assertThat(deliveryOptionsByFeed.getPickupBucketsCount()).isEqualTo(3);
        softly.assertThat(deliveryOptionsByFeed.getDeliveryOptionGroupsCount()).isEqualTo(4);
        softlyAssertXmlEquals(
            serialize(MardoPickupTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()),
            extractFileContent("tariffs/pickup/100062/forCustomer/tariff_meta.xml")
        );
        softlyAssertProtobufAsJson(Map.of(
            pickupBucketId2Bucket.get(1L), "tariffs/pickup/100062/forCustomer/bucket_1.json",
            pickupBucketId2Bucket.get(2L), "tariffs/pickup/100062/forCustomer/bucket_2.json",
            pickupBucketId2Bucket.get(3L), "tariffs/pickup/100062/forCustomer/bucket_3.json"
        ));
        softlyAssertProtobufAsJson(Map.of(
            optionGroupId2OptionGroup.get(1L), "tariffs/pickup/100062/forCustomer/delivery_option_group_1.json",
            optionGroupId2OptionGroup.get(2L), "tariffs/pickup/100062/forCustomer/delivery_option_group_2.json",
            optionGroupId2OptionGroup.get(3L), "tariffs/pickup/100062/forCustomer/delivery_option_group_3.json",
            optionGroupId2OptionGroup.get(4L), "tariffs/pickup/100062/forCustomer/delivery_option_group_4.json"
        ));
    }

    /**
     * Тест проверяет нарезку бакетов по карго-типам
     * <p>
     * bucket_id -> [outlets]
     * 1 -> [3]
     * 2 -> [1, 4]
     * 3 -> [2]
     * <p>
     * max_weights -> [regions] ->[outlets]
     * 6 -> [1,2,3] -> [1, 4]
     * 7 -> [2,3]   -> [2]
     * 8 -> [3]     -> [3]
     * <p>
     * cargo types blacklist -> [regions]
     * 11 -> [1]
     * 12 -> [2]
     */
    @Test
    @DbUnitDataSet(before = "mardoPickupTariffWorkflowTest.csv")
    void testCargoType() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "tariff_4207.xml", getClass());
        Long notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);
        PickupPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        Assertions.assertEquals(getExpectedSerializedTariffInfoForTestCargoType(), XmlUtils.serialize(
                MardoPickupTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()));

        DeliveryCalcProtos.FeedDeliveryOptionsResp feedDeliveryOptionsResp = preparedTariff.getFeedDeliveryOptionsResp(1, 1);
        DeliveryCalcProtos.DeliveryOptions deliveryOptions = feedDeliveryOptionsResp.getDeliveryOptionsByFeed();
        List<DeliveryCalcProtos.PickupBucket> pickupBuckets = deliveryOptions.getPickupBucketsList();

        Assertions.assertTrue(pickupBuckets.stream().allMatch(pickupBucket -> pickupBucket.getTariffId() == 4207));
        Assertions.assertEquals(Collections.singletonList(113L),
                Collections.singletonList(pickupBuckets.get(0).getDeliveryOptionGroupOutlets(0).getOutletId())
        );

        Assertions.assertEquals(Arrays.asList(114L, 111L),
                Arrays.asList(pickupBuckets.get(1).getDeliveryOptionGroupOutlets(0).getOutletId(),
                        pickupBuckets.get(1).getDeliveryOptionGroupOutlets(1).getOutletId())
        );

        Assertions.assertEquals(Collections.singletonList(112L),
                Collections.singletonList(pickupBuckets.get(2).getDeliveryOptionGroupOutlets(0).getOutletId()));
    }


    private String getExpectedSerializedTariffInfoForTestCargoType() {
        return "<tariff min-weight=\"-1.0\" max-weight=\"50.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\" volume-weight-coefficient=\"2.0E-4\">\n" +
                "   <programs>\n" +
                "      <program name-key=\"MARKET_DELIVERY\"/>\n" +
                "   </programs>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"-1.0\" max-weight=\"6.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11]\" min-weight=\"-1.0\" max-weight=\"6.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[12]\" min-weight=\"-1.0\" max-weight=\"6.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"3\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"6.0\" max-weight=\"7.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11]\" min-weight=\"6.0\" max-weight=\"7.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[12]\" min-weight=\"6.0\" max-weight=\"7.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"3\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"7.0\" max-weight=\"8.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11]\" min-weight=\"7.0\" max-weight=\"8.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11]\" min-weight=\"8.0\" max-weight=\"10.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "</tariff>";
    }

}
