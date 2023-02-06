package ru.yandex.market.deliverycalculator.workflow.daas;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.common.CurrencyUtils;
import ru.yandex.market.deliverycalculator.model.DeliveryServicePriceSchemaType;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffUpdatedInfo;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasOutletMetaRule;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.DaasOutletMetaTariff;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryService;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.workflow.FeedSource;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DaasOutletPreparedTariff;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DeliveryOptionData;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DeliverySearchRequest;
import ru.yandex.market.deliverycalculator.workflow.test.AssertionsTestUtils;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType.POST;

class DaasPostTariffWorkflowTest extends FunctionalTest {

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    @Qualifier("daasPostTariffIndexerWorkflow")
    private DaasPostTariffWorkflow indexerWorkflow;

    @Autowired
    @Qualifier("daasPostTariffSearchEngineWorkflow")
    private DaasPostTariffWorkflow searchEngineWorkflow;

    @Test
    @DbUnitDataSet(before = "basicTest.before.csv")
    void basicTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "tariff_post_1234.xml", getClass());
        final int pickpointId = 1;
        final int regionFrom = 213;
        final int regionTo = 197;
        YaDeliveryTariffUpdatedInfo tariff = WorkflowTestUtils.createRandomDaasPostTariff(1234, 35);

        yaDeliveryTariffDbService.save(tariff);

        List<Long> tariffIds = indexerWorkflow.getNotExportedTariffIds();
        Assertions.assertEquals(1, tariffIds.size());
        Assertions.assertEquals(tariff.getId(), (long) tariffIds.get(0));

        DaasOutletPreparedTariff preparedTariff = indexerWorkflow.prepareTariff(tariff.getId());

        Generation generation = new Generation(1, 1);
        DeliveryCalcProtos.FeedDeliveryOptionsResp feedDeliveryOptionsResp = preparedTariff.getFeedDeliveryOptionsResp(generation.getId(), generation.getExternalGenerationId());
        DeliveryCalcProtos.DeliveryOptions deliveryOptions = feedDeliveryOptionsResp.getDeliveryOptionsByFeed();
        List<DeliveryCalcProtos.DeliveryOptionsGroup> deliveryOptionsGroups = deliveryOptions.getDeliveryOptionGroupsList();
        List<DeliveryCalcProtos.PickupOptionsBucket> postBuckets = deliveryOptions.getPostBucketsV2List();
        Assertions.assertEquals(1, deliveryOptionsGroups.size());
        Assertions.assertEquals(1, postBuckets.size());
        Assertions.assertEquals(0, deliveryOptions.getPickupBucketsCount());
        Assertions.assertEquals(0, deliveryOptions.getDeliveryOptionBucketsCount());

        DaasOutletMetaTariff metaTariff = (DaasOutletMetaTariff) preparedTariff.getMetaTariff();
        Assertions.assertEquals(tariff.getCarrierId(), metaTariff.getCarrierId());
        Assertions.assertEquals(1, metaTariff.getRules().size());
        DaasOutletMetaRule rule = metaTariff.getRules().get(0);

        Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> optionGroupId2OptionGroup = deliveryOptionsGroups.stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsGroup::getDeliveryOptionGroupId, Function.identity()));
        Map<Long, DeliveryCalcProtos.PickupOptionsBucket> postBucketId2Bucket = postBuckets.stream()
                .collect(Collectors.toMap(DeliveryCalcProtos.PickupOptionsBucket::getBucketId, Function.identity()));
        checkBucketCorrectness(rule.getBucketId(), tariff.getCarrierId(), pickpointId, regionTo,
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(30000, 2, 3, 13),
                optionGroupId2OptionGroup, postBucketId2Bucket, tariff.getId());

        String testBucketsUrl = "testBucketsUrl";
        indexerWorkflow.addToGeneration(generation, tariff.getId(), preparedTariff, testBucketsUrl);
        generation.getDaasPostGenerations().stream().flatMap(gen -> gen.getBuckets().stream()).forEach(bucket -> bucket.setId(bucket.getExternalId()));
        searchEngineWorkflow.loadFromGeneration(generation);

        FeedSource feedSource = new FeedSource();
        feedSource.setRegionId(regionFrom);
        Assertions.assertEquals(Collections.emptyList(), searchEngineWorkflow.getBucketsResponseUrls(-1,
                feedSource, Collections.singletonList(DeliveryTariffProgramType.DAAS.getSEProgramName()), generation.getId()));
    }

    private void checkBucketCorrectness(long bucketId, int carrierId, long pickpointId, int regionId,
                                        DeliveryCalcProtos.DeliveryOption expectedDeliveryOption,
                                        Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> optionGroupId2OptionGroup,
                                        Map<Long, DeliveryCalcProtos.PickupOptionsBucket> postBucketId2Bucket,
                                        long expectedTariffId) {
        DeliveryCalcProtos.PickupOptionsBucket pickupBucket = postBucketId2Bucket.get(bucketId);
        assertNotNull(pickupBucket);
        Assertions.assertEquals(DeliveryCalcProtos.ProgramType.DAAS, pickupBucket.getProgram());
        Assertions.assertEquals(CurrencyUtils.DEFAULT_CURRENCY, pickupBucket.getCurrency());
        Assertions.assertEquals(expectedTariffId, pickupBucket.getTariffId());
        Assertions.assertEquals(Collections.singletonList(carrierId), pickupBucket.getCarrierIdsList());

        assertEquals(1, pickupBucket.getPickupDeliveryRegionsCount());

        DeliveryCalcProtos.PickupDeliveryRegion region = pickupBucket.getPickupDeliveryRegions(0);
        assertEquals(regionId, region.getRegionId());

        //assert outlets
        assertEquals(1, region.getOutletGroupsCount());
        assertEquals(1, region.getOutletGroups(0).getOutletIdCount());
        assertEquals(pickpointId, region.getOutletGroups(0).getOutletId(0));

        //assert option groups
        DeliveryCalcProtos.DeliveryOptionsGroup deliveryOptionsGroup = optionGroupId2OptionGroup.get(region.getOptionGroupId());
        assertNotNull(deliveryOptionsGroup);
        assertEquals(1, deliveryOptionsGroup.getDeliveryOptionsCount());
        assertEquals(expectedDeliveryOption, deliveryOptionsGroup.getDeliveryOptions(0));
    }

    /**
     * Тест для {@link DaasPickupTariffWorkflow#searchDeliveryOptions(long, DeliverySearchRequest)}.
     * Случай: опции, подходящие по критериям поиска найдены
     */
    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_optionsFound() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, createSearchRequest());

        assertNotNull(optionData);
        assertEquals(createExpectedPickupOptions(), optionData);
    }

    /**
     * Тест для {@link DaasPickupTariffWorkflow#searchDeliveryOptions(long, DeliverySearchRequest)}.
     * Случай: опции, подходящие по критериям поиска не найдены
     */
    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_noOptionsFound_byLocationTo() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        final DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setLocationsTo(Set.of(215));

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_noOptionsFound_byLocationFrom() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        final DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setLocationFrom(215);

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_noOptionsFound_byWeight() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        final DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setWeight(BigDecimal.valueOf(1000));

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_noOptionsFound_byLength() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        final DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setLength(1000);

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_noOptionsFound_byWidth() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        final DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setWidth(1000);

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_noOptionsFound_byHeight() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        final DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setHeight(1000);

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @Test
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions2_noOptionsFound_byDeliveryServiceId() {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        final DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setDeliveryServiceIds(Sets.newHashSet(35L));

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }


    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("tooLargeWeightOrSizeTestProvider")
    @DisplayName("ВГХ превышают допустимый максимум тарифа, опции не найдены")
    @DbUnitDataSet(before = "daasPostDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions_tooLargeWightOrSize(
            String displayName,
            Consumer<DeliverySearchRequest> requestModifier
    ) {
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        searchEngineWorkflow.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        DeliverySearchRequest searchRequest = createSearchRequest();
        requestModifier.accept(searchRequest);

        Set<DeliveryOptionData> optionData = searchEngineWorkflow.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @Nonnull
    private static Stream<Arguments> tooLargeWeightOrSizeTestProvider() {
        return Stream.<Pair<String, Consumer<DeliverySearchRequest>>>of(
                Pair.of("Слишком большой вес", request -> request.setWeight(new BigDecimal("29"))),
                Pair.of("Слишком большая длина", request -> request.setLength(121)),
                Pair.of("Слишком большая ширина", request -> request.setWidth(121)),
                Pair.of("Слишком большая высота", request -> request.setHeight(121))
        ).map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    private DeliverySearchRequest createSearchRequest() {
        DeliverySearchRequest searchRequest = new DeliverySearchRequest();
        searchRequest.setLocationFrom(213);
        searchRequest.setLocationsTo(Set.of(197));
        searchRequest.setWeight(BigDecimal.valueOf(5));
        searchRequest.setLength(10);
        searchRequest.setWidth(20);
        searchRequest.setHeight(30);
        searchRequest.setDeliveryServiceIds(Sets.newHashSet(34L));
        return searchRequest;
    }

    private Set<DeliveryOptionData> createExpectedPickupOptions() {
        return Sets.newHashSet(DeliveryOptionData.builder()
                .withDeliveryServiceId(34L)
                .withTariffId(1234L)
                .withVolumeWeightCoefficient(0.0)
                .withMinDays(1)
                .withMaxDays(3)
                .withCost(5000L)
                .withTariffType(POST)
                .withPickupPoints(Sets.newHashSet(1L, 2L, 3L, 4L))
                .withServices(createExpectedServices())
                .build());
    }

    private Set<DeliveryService> createExpectedServices() {
        return Sets.newHashSet(DeliveryService.builder()
                        .withCode("Very Important 4")
                        .withPriceCalculationRule(DeliveryServicePriceSchemaType.PERCENT_CASH)
                        .withPriceCalculationParameter(1.2)
                        .withMinPrice(5)
                        .withMaxPrice(6)
                        .withEnabledByDefault(true)
                        .build(),
                DeliveryService.builder()
                        .withCode("DELIVERY")
                        .withPriceCalculationRule(DeliveryServicePriceSchemaType.FIX)
                        .withPriceCalculationParameter(5000.0)
                        .withMinPrice(0)
                        .withMaxPrice(Long.MAX_VALUE)
                        .withEnabledByDefault(true)
                        .build()
        );
    }
}
