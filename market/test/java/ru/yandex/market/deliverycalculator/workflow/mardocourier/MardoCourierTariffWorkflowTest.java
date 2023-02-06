package ru.yandex.market.deliverycalculator.workflow.mardocourier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffUpdatedInfo;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.DeliveryTariffId;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.workflow.test.AbstractTariffWorkflowTest;
import ru.yandex.market.deliverycalculator.workflow.test.AssertionsTestUtils;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;
import ru.yandex.market.deliverycalculator.workflow.util.XmlUtils;

import static ru.yandex.market.deliverycalculator.storage.StorageTestUtils.initProviderMock;
import static ru.yandex.market.deliverycalculator.test.TestUtils.extractFileContent;
import static ru.yandex.market.deliverycalculator.workflow.util.XmlUtils.serialize;

class MardoCourierTariffWorkflowTest extends AbstractTariffWorkflowTest {

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    @Qualifier("mardoCourierTariffIndexerWorkflow")
    private MardoCourierTariffWorkflow indexerWorkflow;

    @Test
    @DbUnitDataSet
    void tariffWithSeveralLocationsFromTest() {
        long customerTariffId = 4365;
        long shopTariffId = 4366;

        StorageTestUtils.initProviderMock(tariffInfoProvider, arg -> "tariff_" + arg, getClass());

        YaDeliveryTariffUpdatedInfo customerTariff = WorkflowTestUtils.createCourierTariff(customerTariffId, 50, 200.0, true,
                ImmutableSet.of(1, 2), Collections.emptySet());
        YaDeliveryTariffUpdatedInfo shopTariff = WorkflowTestUtils.createCourierTariff(shopTariffId, 50, 200.0, false,
                Collections.emptySet(), Collections.emptySet());
        yaDeliveryTariffDbService.save(customerTariff);
        yaDeliveryTariffDbService.save(shopTariff);

        List<? extends DeliveryTariffId> notExportedTariffs = indexerWorkflow.getNotExportedTariffIds();
        Assertions.assertEquals(1, notExportedTariffs.size());
        Assertions.assertEquals(new DeliveryTariffId(customerTariffId, shopTariffId), notExportedTariffs.get(0));

        CourierPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariffs.get(0));

        Assertions.assertEquals(4, preparedTariff.getTariff().getRules().size());
        Assertions.assertEquals(getExpectedSerializedTariffInfoForTariffWithSeveralLocationsFromTest(),
                XmlUtils.serialize(MardoCourierTariffWorkflow.JAXB_CONTEXT, preparedTariff.getTariff()));
        DeliveryCalcProtos.FeedDeliveryOptionsResp feedDeliveryOptionsResp = preparedTariff.getFeedDeliveryOptionsResp(1, 1);
        Assertions.assertEquals(3, feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionGroupsCount());
        Assertions.assertEquals(3, feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionBucketsCount());
        Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> optionGroupId2OptionGroup = feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionGroupsList().stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsGroup::getDeliveryOptionGroupId, Function.identity()));
        Map<Long, DeliveryCalcProtos.DeliveryOptionsBucket> deliveryOptionsBucketId2Bucket = feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionBucketsList().stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsBucket::getDeliveryOptBucketId, Function.identity()));
        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(1L), customerTariffId,
                ImmutableMap.of(1, AssertionsTestUtils.buildDeliveryOption(9900, 3, 6, 13, 12200),
                        3, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24300)),
                optionGroupId2OptionGroup);
        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(2L), customerTariffId,
                ImmutableMap.of(1, AssertionsTestUtils.buildDeliveryOption(9900, 2, 4, 13, 30000)),
                optionGroupId2OptionGroup);
        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(3L), customerTariffId,
                ImmutableMap.of(3, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24300)),
                optionGroupId2OptionGroup);
    }

    @Test
    @DisplayName("Курьерский тариф 'MARKET_DELIVERY' из тарификатора")
    @DbUnitDataSet(before = {
            "/tariffs/courier/100061/customer.csv",
            "/tariffs/courier/100061/shop.csv"
    })
    void tariffFromTarifficator() {
        initProviderMock(tariffInfoProvider, filename -> "/tariffs/courier/100061/tariff.xml", getClass());

        DeliveryTariffId notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);
        CourierPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        var deliveryOptionsByFeed = getDeliveryOptionsByFeed(preparedTariff);
        var optionGroupId2OptionGroup = groupOptionGroupById(deliveryOptionsByFeed);
        var courierBucketId2Bucket = groupCourierBucketsById(deliveryOptionsByFeed);

        softly.assertThat(preparedTariff.getTariff().getRules()).hasSize(3);
        softly.assertThat(deliveryOptionsByFeed.getDeliveryOptionBucketsCount()).isEqualTo(3);
        softly.assertThat(deliveryOptionsByFeed.getDeliveryOptionGroupsCount()).isEqualTo(4);
        softlyAssertXmlEquals(
                serialize(MardoCourierTariffWorkflow.JAXB_CONTEXT, preparedTariff.getTariff()),
                extractFileContent("tariffs/courier/100061/tariff_meta.xml")
        );
        softlyAssertProtobufAsJson(Map.of(
                courierBucketId2Bucket.get(1L), "tariffs/courier/100061/bucket_1.json",
                courierBucketId2Bucket.get(2L), "tariffs/courier/100061/bucket_2.json",
                courierBucketId2Bucket.get(3L), "tariffs/courier/100061/bucket_3.json"
        ));
        softlyAssertProtobufAsJson(Map.of(
                optionGroupId2OptionGroup.get(1L), "tariffs/courier/100061/delivery_option_group_1.json",
                optionGroupId2OptionGroup.get(2L), "tariffs/courier/100061/delivery_option_group_2.json",
                optionGroupId2OptionGroup.get(3L), "tariffs/courier/100061/delivery_option_group_3.json",
                optionGroupId2OptionGroup.get(4L), "tariffs/courier/100061/delivery_option_group_4.json"
        ));
    }

    /**
     * Тест проверяет нарезку бакетов по карго-типам
     * <p>
     * bucket_id -> [regions]
     * 1 -> [1, 4]
     * 2 -> [2, 4]
     * 3 -> [3, 5]
     * 4 -> [5, 6]
     * 5 -> [6]
     * 6 -> [4, 5, 6]
     * 7 -> [1, 2, 4]
     * <p>
     * dim -> [regions]
     * {-1,6} -> [1,2,4]
     * {6,7} ->  [3, 5]
     * {7,8} ->  [6]
     * <p>
     * cargo types blacklist -> [regions]
     * 11 -> [1]
     * 12 -> [2]
     * 13 -> [3]
     */
    @Test
    @DbUnitDataSet(before = "mardoCourierTariffWorkflowTest.csv")
    void locationCargoTypesTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "tariff_4207.xml", getClass());
        DeliveryTariffId notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);
        CourierPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        Assertions.assertEquals(7, preparedTariff.getBuckets().getBucketIds().size());

        Assertions.assertEquals(getExpectedSerializedTariffInfoForTariffWithLocationCargoTypesTest(),
                XmlUtils.serialize(MardoCourierTariffWorkflow.JAXB_CONTEXT, preparedTariff.getTariff()));

        DeliveryCalcProtos.FeedDeliveryOptionsResp feedDeliveryOptionsResp = preparedTariff.getFeedDeliveryOptionsResp(1, 1);

        //Проверяем что в бакеты содержат подходящие регионы
        Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> optionGroupId2OptionGroup = feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionGroupsList().stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsGroup::getDeliveryOptionGroupId, Function.identity()));
        Map<Long, DeliveryCalcProtos.DeliveryOptionsBucket> deliveryOptionsBucketId2Bucket = feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionBucketsList().stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsBucket::getDeliveryOptBucketId, Function.identity()));

        long expectedTariffId = 4207;
        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(1L), expectedTariffId,
                ImmutableMap.of(4, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900),
                        5, AssertionsTestUtils.buildDeliveryOption(9900, 1, 3, 13, 24900),
                        6, AssertionsTestUtils.buildDeliveryOption(9900, 1, 4, 13, 24900)),
                optionGroupId2OptionGroup);

        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(2L), expectedTariffId,
                ImmutableMap.of(
                        1, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900),
                        4, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900))
                , optionGroupId2OptionGroup);

        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(3L), expectedTariffId,
                ImmutableMap.of(1, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900),
                        2, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900),
                        4, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900)),
                optionGroupId2OptionGroup);

        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(4L), expectedTariffId,
                ImmutableMap.of(
                        2, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900),
                        4, AssertionsTestUtils.buildDeliveryOption(9900, 1, 2, 13, 24900)),
                optionGroupId2OptionGroup);

        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(5L), expectedTariffId,
                ImmutableMap.of(3, AssertionsTestUtils.buildDeliveryOption(9900, 1, 3, 13, 24900),
                        5, AssertionsTestUtils.buildDeliveryOption(9900, 1, 3, 13, 24900)),
                optionGroupId2OptionGroup);

        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(6L), expectedTariffId,
                ImmutableMap.of(5, AssertionsTestUtils.buildDeliveryOption(9900, 1, 3, 13, 24900),
                        6, AssertionsTestUtils.buildDeliveryOption(9900, 1, 4, 13, 24900)),
                optionGroupId2OptionGroup);

        checkBucketDeliveryOptionsCorrectness(deliveryOptionsBucketId2Bucket.get(7L), expectedTariffId,
                ImmutableMap.of(6, AssertionsTestUtils.buildDeliveryOption(9900, 1, 4, 13, 24900)),
                optionGroupId2OptionGroup);
    }

    @Test
    @DisplayName("Поиск тарифов 'MARKET_DELIVERY' для магазина и покупателя из разных источников")
    @DbUnitDataSet(before = {
            "/tariffs/courier/4628/customer.csv",
            "/tariffs/courier/100001/customer.csv",
            "/tariffs/courier/100061/customer.csv",
            "/tariffs/courier/4629/shop.csv",
            "/tariffs/courier/100001/shop.csv",
            "/tariffs/courier/100061/shop.csv",
    })
    void searchCustormerAndShopTariffsFromDifferentSources() {
        List<? extends DeliveryTariffId> notExportedTariffIds = indexerWorkflow.getNotExportedTariffIds();
        softly.assertThat(notExportedTariffIds).isEqualTo(List.of(
                new DeliveryTariffId(4628L, 4629L),
                new DeliveryTariffId(100001L, 100000100001L),
                new DeliveryTariffId(100061L, 100000100061L)
        ));
    }

    private void checkBucketDeliveryOptionsCorrectness(DeliveryCalcProtos.DeliveryOptionsBucket bucket,
                                                       long expectedTariffId,
                                                       Map<Integer, DeliveryCalcProtos.DeliveryOption> expectedDeliveryOptionByRegion,
                                                       Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> optionGroupId2OptionGroup) {
        Assertions.assertEquals(expectedDeliveryOptionByRegion.size(), bucket.getDeliveryOptionGroupRegsCount());
        Assertions.assertEquals(expectedTariffId, bucket.getTariffId());
        for (DeliveryCalcProtos.DeliveryOptionsGroupRegion deliveryOptionsGroupRegion : bucket.getDeliveryOptionGroupRegsList()) {
            int regionId = deliveryOptionsGroupRegion.getRegion();
            long deliveryOptGroupId = deliveryOptionsGroupRegion.getDeliveryOptGroupId();
            AssertionsTestUtils.assertDeliveryOptionEquals(expectedDeliveryOptionByRegion.get(regionId), optionGroupId2OptionGroup.get(deliveryOptGroupId).getDeliveryOptions(0));
        }
    }

    private String getExpectedSerializedTariffInfoForTariffWithLocationCargoTypesTest() {
        return "<tariff min-weight=\"-1.0\" max-weight=\"50.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0" +
                " 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\" volume-weight-coefficient=\"2.0E-4\" " +
                "shop-volume-weight-coefficient=\"2.0E-4\">\n" +
                "   <programs>\n" +
                "      <program name-key=\"MARKET_DELIVERY\"/>\n" +
                "   </programs>\n" +
                "   <rule location-from-id=\"213\" min-customer-weight=\"-1.0\" max-customer-weight=\"6.0\" " +
                "min-shop-weight=\"-1.0\" max-shop-weight=\"6.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11]\" min-customer-weight=\"-1" +
                ".0\" max-customer-weight=\"6.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"6.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11, 12]\" " +
                "min-customer-weight=\"-1.0\" max-customer-weight=\"6.0\" min-shop-weight=\"-1.0\" " +
                "max-shop-weight=\"6.0\">\n" +
                "      <bucket id=\"3\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[12]\" min-customer-weight=\"-1" +
                ".0\" max-customer-weight=\"6.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"6.0\">\n" +
                "      <bucket id=\"4\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[13]\" min-customer-weight=\"-1" +
                ".0\" max-customer-weight=\"6.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"6.0\">\n" +
                "      <bucket id=\"5\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-customer-weight=\"6.0\" max-customer-weight=\"7.0\" " +
                "min-shop-weight=\"6.0\" max-shop-weight=\"7.0\">\n" +
                "      <bucket id=\"6\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[13]\" min-customer-weight=\"6.0\"" +
                " max-customer-weight=\"7.0\" min-shop-weight=\"6.0\" max-shop-weight=\"7.0\">\n" +
                "      <bucket id=\"5\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-customer-weight=\"7.0\" max-customer-weight=\"8.0\" " +
                "min-shop-weight=\"7.0\" max-shop-weight=\"8.0\">\n" +
                "      <bucket id=\"7\"/>\n" +
                "   </rule>\n" +
                "</tariff>";
    }

    private String getExpectedSerializedTariffInfoForTariffWithSeveralLocationsFromTest() {
        return "<tariff min-weight=\"-1.0\" max-weight=\"50.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\" volume-weight-coefficient=\"2.0E-4\" shop-volume-weight-coefficient=\"2.0E-4\">\n" +
                "   <cargo-type-blacklist>1</cargo-type-blacklist>\n" +
                "   <cargo-type-blacklist>2</cargo-type-blacklist>\n" +
                "   <programs>\n" +
                "      <program name-key=\"MARKET_DELIVERY\"/>\n" +
                "   </programs>\n" +
                "   <rule location-from-id=\"213\" min-customer-weight=\"-1.0\" max-customer-weight=\"5.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"8.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"39\" min-customer-weight=\"-1.0\" max-customer-weight=\"5.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"8.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-customer-weight=\"5.0\" max-customer-weight=\"8.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"8.0\">\n" +
                "      <bucket id=\"3\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"39\" min-customer-weight=\"5.0\" max-customer-weight=\"8.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"8.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "</tariff>";
    }

}
