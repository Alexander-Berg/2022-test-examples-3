package ru.yandex.market.deliverycalculator.workflow.mardopost;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
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
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.workflow.BucketIdCollection;
import ru.yandex.market.deliverycalculator.workflow.FeedSource;
import ru.yandex.market.deliverycalculator.workflow.test.AbstractTariffWorkflowTest;
import ru.yandex.market.deliverycalculator.workflow.test.AssertionsTestUtils;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;
import ru.yandex.market.deliverycalculator.workflow.util.XmlUtils;

import static ru.yandex.market.deliverycalculator.storage.StorageTestUtils.initProviderMock;
import static ru.yandex.market.deliverycalculator.test.TestUtils.extractFileContent;
import static ru.yandex.market.deliverycalculator.workflow.util.XmlUtils.serialize;

@DbUnitDataSet(before = "warehouses.csv")
class MardoPostTariffWorkflowTest extends AbstractTariffWorkflowTest {

    private static final long TARIFF_ID = 1411;

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    @Qualifier("mardoPostTariffIndexerWorkflow")
    private MardoPostTariffWorkflow indexerWorkflow;

    @Autowired
    @Qualifier("mardoPostTariffSearchEngineWorkflow")
    private MardoPostTariffWorkflow seWorkflow;


    @Test
    @DbUnitDataSet(before = "basicTest.csv")
    void basicTest() {
        Map<Long, PostPreparedTariff> preparedTariffs = prepareTariffFromFile();

        DeliveryCalcProtos.FeedOffersReq.Builder feedOffersReq = getPreparedFeedOffersReq();
        DeliveryCalcProtos.Offer offer = feedOffersReq.getOffers(0);

        Map<DeliveryCalcProtos.Offer, BucketIdCollection> offer2Buckets =
                seWorkflow.getOffer2BucketIdsMap(feedOffersReq.build(),
                        0, feedOffersReq.getOffersCount(),
                        FeedSource.UNKNOWN,
                        Collections.singleton(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM));


        Assertions.assertIterableEquals(Collections.singletonList(offer), offer2Buckets.keySet());

        BucketIdCollection bucketIds = offer2Buckets.get(offer);

        Assertions.assertAll(
                () -> Assertions.assertTrue(CollectionUtils.isEmpty(bucketIds.getCourierBucketIds())),
                () -> Assertions.assertTrue(CollectionUtils.isEmpty(bucketIds.getPickupBucketIds())),
                () -> Assertions.assertEquals(1, bucketIds.getPostBucketIds().size())
        );

        long bucketId = bucketIds.getPostBucketIds().iterator().next();

        DeliveryCalcProtos.FeedDeliveryOptionsResp bucketsResponse = preparedTariffs.get(TARIFF_ID)
                .getFeedDeliveryOptionsResp(50, 50);

        List<DeliveryCalcProtos.PostBucket> buckets =
                bucketsResponse.getDeliveryOptionsByFeed().getPostBucketsList().stream()
                        .filter(bk -> bk.getBucketId() == bucketId)
                        .collect(Collectors.toList());

        Assertions.assertEquals(1, buckets.size());

        DeliveryCalcProtos.PostBucket bucket = buckets.get(0);

        Assertions.assertAll(
                () -> Assertions.assertEquals(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM, bucket.getProgram()),
                () -> Assertions.assertEquals("RUR", bucket.getCurrency()),
                () -> Assertions.assertEquals(4, bucket.getDeliveryOptionGroupPostOutletsCount()),
                () -> Assertions.assertEquals(TARIFF_ID, bucket.getTariffId())
        );

        long optionGroupId = bucket.getDeliveryOptionGroupPostOutlets(0).getOptionGroupId();
        List<Integer> postCodes = bucket.getDeliveryOptionGroupPostOutletsList().stream()
                .map(DeliveryCalcProtos.DeliveryOptionsGroupPostOutlet::getPostCode)
                .sorted()
                .collect(Collectors.toList());

        List<Integer> regions = bucket.getDeliveryOptionGroupPostOutletsList().stream()
                .filter(DeliveryCalcProtos.DeliveryOptionsGroupPostOutlet::hasRegion)
                .map(DeliveryCalcProtos.DeliveryOptionsGroupPostOutlet::getRegion)
                .sorted()
                .collect(Collectors.toList());

        List<Long> mbiOutletsIds = bucket.getDeliveryOptionGroupPostOutletsList().stream()
                .filter(DeliveryCalcProtos.DeliveryOptionsGroupPostOutlet::hasMbiOutletId)
                .map(DeliveryCalcProtos.DeliveryOptionsGroupPostOutlet::getMbiOutletId)
                .sorted()
                .collect(Collectors.toList());

        Assertions.assertAll(
                () -> Assertions.assertIterableEquals(Arrays.asList(22081, 22082, 22083, 22084), postCodes),
                () -> Assertions.assertIterableEquals(Arrays.asList(197, 197, 197, 197), regions),
                () -> Assertions.assertIterableEquals(Arrays.asList(82L, 83L, 84L, 85L), mbiOutletsIds),
                () -> bucket.getDeliveryOptionGroupPostOutletsList().forEach(
                        postOutlet -> Assertions.assertEquals(optionGroupId, postOutlet.getOptionGroupId()))
        );

        List<DeliveryCalcProtos.DeliveryOptionsGroup> optionGroups =
                bucketsResponse.getDeliveryOptionsByFeed().getDeliveryOptionGroupsList().stream()
                        .filter(optionGroup -> optionGroup.getDeliveryOptionGroupId() == optionGroupId)
                        .collect(Collectors.toList());

        Assertions.assertEquals(1, optionGroups.size());

        DeliveryCalcProtos.DeliveryOptionsGroup optionsGroup = optionGroups.get(0);

        Assertions.assertAll(
                () -> Assertions.assertEquals(1, optionsGroup.getDeliveryOptionsCount()),
                () -> Assertions.assertEquals(31300, optionsGroup.getDeliveryOptions(0).getDeliveryCost())
        );
    }

    @Test
    void tariffWithSeveralLocationsFromTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "tariff_4841.xml", getClass());
        long id = 4841;
        YaDeliveryTariffUpdatedInfo tariff = WorkflowTestUtils.createMardoPostTariff(id, 0.0, "RUR",
                ImmutableSet.of(1, 2), Collections.emptySet());

        yaDeliveryTariffDbService.save(tariff);

        List<Long> tariffIds = indexerWorkflow.getNotExportedTariffIds();
        Assertions.assertEquals(1, tariffIds.size());
        Assertions.assertEquals(tariff.getId(), (long) tariffIds.get(0));

        PostPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(tariff.getId());

        Assertions.assertEquals(7, preparedTariff.getMetaTariff().getRules().size());
        Assertions.assertEquals(getExpectedSerializedTariffInfoForTariffWithSeveralLocationsFromTest(),
                XmlUtils.serialize(MardoPostTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()));
        DeliveryCalcProtos.FeedDeliveryOptionsResp feedDeliveryOptionsResp = preparedTariff.getFeedDeliveryOptionsResp(1, 1);
        Assertions.assertEquals(2, feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionGroupsCount());
        Assertions.assertEquals(2, feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getPostBucketsCount());
        Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> optionGroupId2OptionGroup = feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getDeliveryOptionGroupsList().stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsGroup::getDeliveryOptionGroupId, Function.identity()));
        Map<Long, DeliveryCalcProtos.PostBucket> postBucketId2Bucket = feedDeliveryOptionsResp.getDeliveryOptionsByFeed().getPostBucketsList().stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.PostBucket::getBucketId, Function.identity()));
        checkBucketDeliveryOptionsCorrectness(postBucketId2Bucket.get(1L),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(17700, 3, 6), optionGroupId2OptionGroup);
        checkBucketDeliveryOptionsCorrectness(postBucketId2Bucket.get(2L),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(30500, 6, 6), optionGroupId2OptionGroup);
    }

    @Test
    @DisplayName("Почтовый тариф 'MARKET_DELIVERY' из тарификатора (forCustomer = true)")
    @DbUnitDataSet(before = "/tariffs/post/100063/forCustomer/market_delivery_drom_tarifficator.csv")
    @DbUnitDataSet(before = "/pickuppoints/pickuppoints139.csv")
    void tariffFromTarifficatorForCustomer() {
        initProviderMock(tariffInfoProvider, filename -> "/tariffs/post/100063/forCustomer/tariff.xml", getClass());

        Long notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);
        PostPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        var deliveryOptionsByFeed = getDeliveryOptionsByFeed(preparedTariff);
        var optionGroupId2OptionGroup = groupOptionGroupById(deliveryOptionsByFeed);
        var postBucketId2Bucket = groupPostBucketsById(deliveryOptionsByFeed);

        softly.assertThat(preparedTariff.getMetaTariff().getRules()).hasSize(3);
        softly.assertThat(deliveryOptionsByFeed.getPostBucketsCount()).isEqualTo(3);
        softly.assertThat(deliveryOptionsByFeed.getDeliveryOptionGroupsCount()).isEqualTo(4);
        softlyAssertXmlEquals(
                serialize(MardoPostTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()),
                extractFileContent("tariffs/post/100063/forCustomer/tariff_meta.xml")
        );
        softlyAssertProtobufAsJson(Map.of(
                postBucketId2Bucket.get(1L), "tariffs/post/100063/forCustomer/bucket_1.json",
                postBucketId2Bucket.get(2L), "tariffs/post/100063/forCustomer/bucket_2.json",
                postBucketId2Bucket.get(3L), "tariffs/post/100063/forCustomer/bucket_3.json"
        ));
        softlyAssertProtobufAsJson(Map.of(
                optionGroupId2OptionGroup.get(1L), "tariffs/post/100063/forCustomer/delivery_option_group_1.json",
                optionGroupId2OptionGroup.get(2L), "tariffs/post/100063/forCustomer/delivery_option_group_2.json",
                optionGroupId2OptionGroup.get(3L), "tariffs/post/100063/forCustomer/delivery_option_group_3.json",
                optionGroupId2OptionGroup.get(4L), "tariffs/post/100063/forCustomer/delivery_option_group_4.json"
        ));
    }

    @Test
    @DisplayName("Почтовый тариф 'MARKET_DELIVERY' из тарификатора (forCustomer = false)")
    @DbUnitDataSet(before = "/tariffs/post/100063/forShop/market_delivery_drom_tarifficator.csv")
    @DbUnitDataSet(before = "/pickuppoints/pickuppoints139.csv")
    void tariffFromTarifficatorForShops() {
        initProviderMock(tariffInfoProvider, filename -> "/tariffs/post/100063/forShop/tariff.xml", getClass());

        Long notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);
        PostPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        var deliveryOptionsByFeed = getDeliveryOptionsByFeed(preparedTariff);
        var optionGroupId2OptionGroup = groupOptionGroupById(deliveryOptionsByFeed);
        var postBucketId2Bucket = groupPostBucketsById(deliveryOptionsByFeed);

        softly.assertThat(preparedTariff.getMetaTariff().getRules()).hasSize(3);
        softly.assertThat(deliveryOptionsByFeed.getPostBucketsCount()).isEqualTo(3);
        softly.assertThat(deliveryOptionsByFeed.getDeliveryOptionGroupsCount()).isEqualTo(4);
        softlyAssertXmlEquals(
            serialize(MardoPostTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()),
            extractFileContent("tariffs/post/100063/forShop/tariff_meta.xml")
        );
        softlyAssertProtobufAsJson(Map.of(
            postBucketId2Bucket.get(1L), "tariffs/post/100063/forShop/bucket_1.json",
            postBucketId2Bucket.get(2L), "tariffs/post/100063/forShop/bucket_2.json",
            postBucketId2Bucket.get(3L), "tariffs/post/100063/forShop/bucket_3.json"
        ));
        softlyAssertProtobufAsJson(Map.of(
            optionGroupId2OptionGroup.get(1L), "tariffs/post/100063/forShop/delivery_option_group_1.json",
            optionGroupId2OptionGroup.get(2L), "tariffs/post/100063/forShop/delivery_option_group_2.json",
            optionGroupId2OptionGroup.get(3L), "tariffs/post/100063/forShop/delivery_option_group_3.json",
            optionGroupId2OptionGroup.get(4L), "tariffs/post/100063/forShop/delivery_option_group_4.json"
        ));
    }

    /**
     * Тест проверяет нарезку бакетов по карго-типам
     * <p>
     * bucket_id -> [post_code]
     * 1 -> [3]
     * 2 -> [1]
     * 3 -> [2]
     * <p>
     * max_weights -> [post_code] ->[outlets]
     * 6 -> [1,2,3] -> [1]
     * 7 -> [2,3]   -> [2]
     * 8 -> [3]     -> [3]
     * <p>
     * cargo types blacklist -> [post_code]
     * 11 -> [1]
     * 12 -> [2]
     */
    @Test
    @DbUnitDataSet(before = "mardoPostTariffWorkflowTest.csv")
    void testCargoTypes() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "tariff_4207.xml", getClass());
        Long notExportedTariff = indexerWorkflow.getNotExportedTariffIds().get(0);

        PostPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(notExportedTariff);

        Assertions.assertEquals(getExpectedSerializedTariffInfoForCargoTypeTest(),
                XmlUtils.serialize(MardoPostTariffWorkflow.JAXB_CONTEXT, preparedTariff.getMetaTariff()));

        DeliveryCalcProtos.FeedDeliveryOptionsResp feedDeliveryOptionsResp = preparedTariff.getFeedDeliveryOptionsResp(1, 1);
        DeliveryCalcProtos.DeliveryOptions deliveryOptions = feedDeliveryOptionsResp.getDeliveryOptionsByFeed();
        List<DeliveryCalcProtos.PostBucket> postBuckets = deliveryOptions.getPostBucketsList();

        Assert.assertEquals(3, postBuckets.get(0).getDeliveryOptionGroupPostOutlets(0).getPostCode());
        Assert.assertEquals(3, postBuckets.get(0).getDeliveryOptionGroupPostOutlets(0).getRegion());

        Assert.assertEquals(1, postBuckets.get(1).getDeliveryOptionGroupPostOutlets(0).getPostCode());
        Assert.assertEquals(1, postBuckets.get(1).getDeliveryOptionGroupPostOutlets(0).getRegion());

        Assert.assertEquals(2, postBuckets.get(2).getDeliveryOptionGroupPostOutlets(0).getPostCode());
        Assert.assertEquals(2, postBuckets.get(2).getDeliveryOptionGroupPostOutlets(0).getRegion());

    }

    private String getExpectedSerializedTariffInfoForTariffWithSeveralLocationsFromTest() {
        return "<tariff min-weight=\"-1.0\" max-weight=\"10.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\" volume-weight-coefficient=\"0.0\">\n" +
                "   <cargo-type-blacklist>1</cargo-type-blacklist>\n" +
                "   <cargo-type-blacklist>2</cargo-type-blacklist>\n" +
                "   <programs>\n" +
                "      <program name-key=\"MARKET_DELIVERY\"/>\n" +
                "   </programs>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"-1.0\" max-weight=\"5.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"39\" min-weight=\"-1.0\" max-weight=\"5.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\">\n" +
                "      <bucket id=\"2\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"-1.0\" max-weight=\"5.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"120.0\" max-dim-sum=\"567.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"-1.0\" max-weight=\"5.0\" min-dimension=\"-1.0 -1.0 60.0\" max-dimension=\"60.0 60.0 349.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"567.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"-1.0\" max-weight=\"5.0\" min-dimension=\"-1.0 60.0 -1.0\" max-dimension=\"60.0 119.0 349.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"567.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"-1.0\" max-weight=\"5.0\" min-dimension=\"60.0 -1.0 -1.0\" max-dimension=\"99.0 119.0 349.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"567.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"5.0\" max-weight=\"10.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"99.0 119.0 349.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"567.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "</tariff>";
    }

    private String getExpectedSerializedTariffInfoForCargoTypeTest() {
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
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[12]\" min-weight=\"6.0\" max-weight=\"7.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"3\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"7.0\" max-weight=\"8.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"10.0 10.0 10.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"10.0\">\n" +
                "      <bucket id=\"1\"/>\n" +
                "   </rule>\n" +
                "</tariff>";
    }

    private void checkBucketDeliveryOptionsCorrectness(DeliveryCalcProtos.PostBucket bucket, DeliveryCalcProtos.DeliveryOption expectedDeliveryOption,
                                                       Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> optionGroupId2OptionGroup) {
        Assertions.assertEquals(1, bucket.getDeliveryOptionGroupPostOutletsCount());
        DeliveryCalcProtos.DeliveryOptionsGroupPostOutlet deliveryOptionsGroupOutlet = bucket.getDeliveryOptionGroupPostOutlets(0);
        DeliveryCalcProtos.DeliveryOptionsGroup deliveryOptionsGroup = optionGroupId2OptionGroup.get(deliveryOptionsGroupOutlet.getOptionGroupId());
        DeliveryCalcProtos.DeliveryOption deliveryOption = deliveryOptionsGroup.getDeliveryOptions(0);
        AssertionsTestUtils.assertDeliveryOptionEquals(expectedDeliveryOption, deliveryOption);
    }

    @Test
    void testReturnTariffBucketIfSummaryOrderWeightInTariffLimits() {
        final double availableOrderMaxWeightKg = 31; // Максимальный вес для заказа у тарифа 1411 - 31 кг

        prepareTariffFromFile();

        DeliveryCalcProtos.FeedOffersReq.Builder feedOffersReq = getPreparedFeedOffersReq();
        feedOffersReq.setMaxSumWeight(availableOrderMaxWeightKg);

        Map<DeliveryCalcProtos.Offer, BucketIdCollection> offer2Buckets =
                seWorkflow.getOffer2BucketIdsMap(feedOffersReq.build(),
                        0, feedOffersReq.getOffersCount(),
                        FeedSource.UNKNOWN,
                        Collections.singleton(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM));

        Assertions.assertIterableEquals(Collections.singletonList(feedOffersReq.getOffers(0)), offer2Buckets.keySet());
    }

    @Test
    void testDontReturnTariffBucketIfSummaryOrderWeightOutOfTariffLimits() {
        final double notAvailableOrderMaxWeightKg = 31.1; // Максимальный вес для заказа у тарифа 1411 - 31 кг

        prepareTariffFromFile();

        DeliveryCalcProtos.FeedOffersReq.Builder feedOffersReq = getPreparedFeedOffersReq();
        feedOffersReq.setMaxSumWeight(notAvailableOrderMaxWeightKg);

        Map<DeliveryCalcProtos.Offer, BucketIdCollection> offer2Buckets =
                seWorkflow.getOffer2BucketIdsMap(feedOffersReq.build(),
                        0, feedOffersReq.getOffersCount(),
                        FeedSource.UNKNOWN,
                        Collections.singleton(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM));

        Assertions.assertIterableEquals(Collections.emptyList(), offer2Buckets.keySet());
    }

    @NotNull
    private DeliveryCalcProtos.FeedOffersReq.Builder getPreparedFeedOffersReq() {
        DeliveryCalcProtos.Offer.Builder offer = DeliveryCalcProtos.Offer.newBuilder();
        offer.setOfferId("Offer123");
        offer.addProgramType(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM);
        offer.setLength(9);
        offer.setHeight(30);
        offer.setWidth(40);
        offer.setWeight(5);
        DeliveryCalcProtos.FeedOffersReq.Builder feedOffersReq = DeliveryCalcProtos.FeedOffersReq.newBuilder();
        feedOffersReq.setFeedId(-1);
        feedOffersReq.setGenerationId(100);
        feedOffersReq.addOffers(offer);
        feedOffersReq.setWarehouseId(7L);
        return feedOffersReq;
    }

    private Map<Long, PostPreparedTariff> prepareTariffFromFile() {
        initProviderMock(tariffInfoProvider, filename -> "tariff_1411.xml", getClass());
        YaDeliveryTariffUpdatedInfo tariff = WorkflowTestUtils.createMardoPostTariff(TARIFF_ID, 0.0, "RUR",
                ImmutableSet.of(1, 2), Collections.emptySet());
        yaDeliveryTariffDbService.save(tariff);

        List<Long> tariffIds = indexerWorkflow.getNotExportedTariffIds();

        Assertions.assertAll(
                () -> Assertions.assertEquals(1, tariffIds.size()),
                () -> Assertions.assertEquals(tariffIds.get(0).longValue(), tariff.getId())
        );

        Map<Long, PostPreparedTariff> preparedTariffs = tariffIds.stream()
                .collect(Collectors.toMap(Function.identity(), indexerWorkflow::prepareTariff));

        Generation generation = new Generation(50, 50);

        preparedTariffs.forEach((tariffId, preparedTariff) ->
                indexerWorkflow.addToGeneration(generation, tariffId, preparedTariff, "bucketsUrl"));

        seWorkflow.loadFromGeneration(generation);

        List<String> bucketUrls = seWorkflow.getBucketsResponseUrls(-1, FeedSource.UNKNOWN,
                Collections.singleton(DeliveryTariffProgramType.MARKET_DELIVERY.getSEProgramName()), 70);
        Assertions.assertIterableEquals(Collections.singletonList("bucketsUrl"), bucketUrls);
        return preparedTariffs;
    }
}
