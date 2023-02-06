package ru.yandex.market.deliverycalculator.workflow.daas;

import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.model.DeliveryServicePriceSchemaType;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryService;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationRepository;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DeliveryOptionData;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DeliverySearchRequest;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType.COURIER;

class DaasCourierTariffWorkflowTest extends FunctionalTest {

    @Autowired
    @Qualifier("daasCourierTariffSearchEngineWorkflow")
    private DaasCourierTariffWorkflow tested;

    @Autowired
    private GenerationRepository generationRepository;

    /**
     * Тест для {@link DaasCourierTariffWorkflow#searchDeliveryOptions(long, DeliverySearchRequest)}.
     * Случай: опции, подходящие по критериям поиска найдены
     */
    @Test
    @DbUnitDataSet(before = "daasCourierDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions_optionsFound() {
        tested.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        tested.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        Set<DeliveryOptionData> optionData = tested.searchDeliveryOptions(2, createSearchRequest());

        assertNotNull(optionData);
        assertEquals(createExpectedCourierOptions(), optionData);
    }

    /**
     * Тест для {@link DaasCourierTariffWorkflow#searchDeliveryOptions(long, DeliverySearchRequest)}.
     * Случай: опции для нескольких разных локаций
     */
    @Test
    @DbUnitDataSet(before = "daasCourierDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptionsForFewDestinations() {
        tested.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        tested.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setLocationsTo(Set.of(197, 200));

        Set<DeliveryOptionData> optionData = tested.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);

        Set<DeliveryOptionData> expectedData = createExpectedCourierOptions();
        expectedData.add(createDeliveryOption(8000L));
        assertEquals(expectedData, optionData);
    }

    /**
     * Тест для {@link DaasCourierTariffWorkflow#searchDeliveryOptions(long, DeliverySearchRequest)}.
     * Случай: опции, подходящие по критериям поиска не найдены
     */
    @Test
    @DbUnitDataSet(before = "daasCourierDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions_noOptionsFound() {
        tested.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        tested.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        DeliverySearchRequest searchRequest = createSearchRequest();
        searchRequest.setLocationsTo(Set.of(215));

        Set<DeliveryOptionData> optionData = tested.searchDeliveryOptions(2, searchRequest);

        assertNotNull(optionData);
        assertEquals(0, optionData.size());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("tooLargeWeightOrSizeTestProvider")
    @DisplayName("ВГХ превышают допустимый максимум тарифа, опции не найдены")
    @DbUnitDataSet(before = "daasCourierDeliveryOptions.before.csv")
    @Transactional
    void testSearchDeliveryOptions_tooLargeWightOrSize(
            String displayName,
            Consumer<DeliverySearchRequest> requestModifier
    ) {
        tested.loadFromGeneration(generationRepository.findById(1L).orElseThrow());
        tested.loadFromGeneration(generationRepository.findById(2L).orElseThrow());

        DeliverySearchRequest searchRequest = createSearchRequest();
        requestModifier.accept(searchRequest);

        Set<DeliveryOptionData> optionData = tested.searchDeliveryOptions(2, searchRequest);

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

    private Set<DeliveryOptionData> createExpectedCourierOptions() {
        return Sets.newHashSet(createDeliveryOption(5000L));
    }

    @Nonnull
    private DeliveryOptionData createDeliveryOption(long cost) {
        return DeliveryOptionData.builder()
                .withDeliveryServiceId(34L)
                .withTariffId(1234L)
                .withVolumeWeightCoefficient(0.0)
                .withMinDays(1)
                .withMaxDays(3)
                .withCost(cost)
                .withTariffType(COURIER)
                .withServices(Sets.newHashSet(
                        DeliveryService.builder()
                                .withCode("DELIVERY")
                                .withPriceCalculationRule(DeliveryServicePriceSchemaType.FIX)
                                .withPriceCalculationParameter(cost)
                                .withMinPrice(0)
                                .withMaxPrice(Long.MAX_VALUE)
                                .withEnabledByDefault(true)
                                .build()
                ))
                .build();
    }

}
