package ru.yandex.market.logistics.nesu.jobs.processor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.Validator;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deliverycalculator.indexerclient.DeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.OfferWeightDimensionsDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.YaDeliverySenderSettingsDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.YaDeliveryTariffTypeDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryModifierDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.PercentValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto.OperationEnum;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.DeliveryCalculatorIndexerProperties;
import ru.yandex.market.logistics.nesu.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.nesu.jobs.model.SenderModifiersUploadPayload;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.utils.ShopAvailableDeliveriesUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.CommonsConstants.MSK_TIME_ZONE;

@ParametersAreNonnullByDefault
@DisplayName("Загрузка модификаторов в КД")
@DatabaseSetup("/jobs/processor/modifier_upload/before/base_setup.xml")
class ModifierUploadProcessorTest extends AbstractContextualTest {
    private static final int DEFAULT_DIMENSION = 10;
    private static final OfferWeightDimensionsDto DEFAULT_AVERAGE_ORDER_MEASURES = OfferWeightDimensionsDto.builder()
        .withLength(DEFAULT_DIMENSION)
        .withWidth(DEFAULT_DIMENSION)
        .withHeight(DEFAULT_DIMENSION)
        .withWeight(1.0)
        .build();
    private static final ZonedDateTime UPDATED_DATE = ZonedDateTime.of(2019, 7, 27, 12, 30, 0, 0, MSK_TIME_ZONE);

    @Autowired
    private DeliveryCalculatorIndexerProperties properties;

    @Autowired
    private DeliveryCalculatorIndexerClient indexerClient;

    @Autowired
    private ModifierUploadProcessor processor;

    @Autowired
    private Validator validator;

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(indexerClient, lmsClient);
    }

    @Test
    @DisplayName("Пустой список модификаторов и пустые настройки СД сендера")
    @DatabaseSetup(
        value = "/jobs/processor/modifier_upload/before/no_delivery_settings.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    void emptyModifiers() {
        processModifierUploadTest(request -> request.withCarrierIdToRegionIds(Map.of()));
    }

    @Test
    @DisplayName("Нет модификаторов, но есть активные СД")
    @DatabaseSetup(
        value = "/jobs/processor/modifier_upload/before/multiregional_setup.xml",
        type = DatabaseOperation.INSERT
    )
    void noModifiersAndActiveDeliveries() {
        mockShopAvailableDeliveries();
        processModifierUploadTest(
            request -> request.withCarrierIdToRegionIds(Map.of(
                1L, Set.of(1, 213),
                2L, Set.of(1, 213, 11117),
                3L, Set.of(11117)
            ))
        );
        verifyShopAvailableDeliveries();
    }

    @Test
    @DisplayName("Минимально заполненный модификатор")
    @DatabaseSetup("/jobs/processor/modifier_upload/before/minimal_modifier.xml")
    void minimalModifier() {
        mockShopAvailableDeliveries();
        processModifierUploadTest(request -> request.withModifiers(Set.of(defaultModifierBuilder(1).build())));
        verifyShopAvailableDeliveries();
    }

    @Test
    @DatabaseSetup("/jobs/processor/modifier_upload/before/one_modifier.xml")
    @DisplayName("Виртуальный партнер корректно конвертится в настоящих")
    void virtualServiceConvertsToRealServices() {
        mockShopAvailableDeliveries(53916L);

        ConditionDto condition = new ConditionDto.Builder()
            .withCost(
                new PercentValueLimiterDto.Builder()
                    .withMinValue(BigDecimal.valueOf(100))
                    .withMaxValue(BigDecimal.valueOf(1000))
                    .withPercent(BigDecimal.valueOf(100))
                    .build()
            )
            .withWeight(valueLimiter(0, 10))
            .withChargeableWeight(valueLimiter(0, 10))
            .withDimension(valueLimiter(0, 10))
            .withCarrierIds(Set.of(12345L, 12346L))
            .withDeliveryDestinations(Set.of(23222, 3321))
            .withDeliveryTypes(Set.of(YaDeliveryTariffTypeDTO.COURIER))
            .build();

        SearchPartnerFilter findBySubtypeFilter = SearchPartnerFilter.builder()
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setPlatformClientStatuses(EnumSet.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .setStatuses(Set.of(PartnerStatus.ACTIVE))
            .setPartnerSubTypeIds(Set.of(2L))
            .build();

        when(lmsClient.searchPartners(findBySubtypeFilter)).thenReturn(
            List.of(partnerWithSubtype(12345L), partnerWithSubtype(12346L))
        );

        processModifierUploadTest(
            request -> request.withCarrierIdToRegionIds(
                Map.of(12345L, Set.of(1, 213), 12346L, Set.of(1, 213))
            )
                .withModifiers(Set.of(
                    defaultModifierBuilder(2)
                        .withAction(
                            defaultActionBuilder()
                                .withCostModificationRule(modificationRule(OperationEnum.FIX_VALUE, "500", null, null))
                                .withTimeModificationRule(modificationRule(OperationEnum.SUBSTRACT, "2", 1, null))
                                .build()
                        )
                        .withCondition(condition)
                        .build()
                ))
        );

        verifyShopAvailableDeliveries(53916L);
        verify(lmsClient).searchPartners(findBySubtypeFilter);
    }

    @Test
    @DisplayName("Заполненные модификаторы")
    @DatabaseSetup("/jobs/processor/modifier_upload/before/modifier.xml")
    void severalModifiers() {
        mockShopAvailableDeliveries();

        ConditionDto condition = new ConditionDto.Builder()
            .withCost(
                new PercentValueLimiterDto.Builder()
                    .withMinValue(BigDecimal.valueOf(100))
                    .withMaxValue(BigDecimal.valueOf(1000))
                    .withPercent(BigDecimal.valueOf(100))
                    .build()
            )
            .withWeight(valueLimiter(0, 10))
            .withChargeableWeight(valueLimiter(0, 10))
            .withDimension(valueLimiter(0, 10))
            .withCarrierIds(Set.of(1L, 2L))
            .withDeliveryDestinations(Set.of(23222, 3321))
            .withDeliveryTypes(Set.of(YaDeliveryTariffTypeDTO.COURIER))
            .build();

        processModifierUploadTest(
            request -> request.withModifiers(Set.of(
                defaultModifierBuilder(2)
                    .withAction(
                        defaultActionBuilder()
                            .withCostModificationRule(modificationRule(OperationEnum.FIX_VALUE, "500", null, null))
                            .withTimeModificationRule(modificationRule(OperationEnum.SUBSTRACT, "2", 1, null))
                            .build()
                    )
                    .withCondition(condition)
                    .build(),
                defaultModifierBuilder(3)
                    .withAction(
                        defaultActionBuilder()
                            .withCostModificationRule(modificationRule(OperationEnum.MULTIPLY, "1.25", 0, 1000))
                            .withTimeModificationRule(modificationRule(OperationEnum.ADD, "3", null, 14))
                            .build()
                    )
                    .withCondition(condition)
                    .build(),
                defaultModifierBuilder(4)
                    .withAction(
                        defaultActionBuilder()
                            .withCostModificationRule(modificationRule(OperationEnum.DIVIDE, "1.25", 0, 1000))
                            .withTimeModificationRule(modificationRule(OperationEnum.ADD, "3", null, 14))
                            .build()
                    )
                    .withCondition(condition)
                    .build()
            ))
        );

        verifyShopAvailableDeliveries();
    }

    @Test
    @DisplayName("Весогабариты среднего заказа в КД")
    @DatabaseSetup("/jobs/processor/modifier_upload/before/avg_measures.xml")
    void avgMeasures() {
        mockShopAvailableDeliveries();
        processModifierUploadTest(
            request -> request.withAverageOfferWeightDimensions(
                OfferWeightDimensionsDto.builder()
                    .withLength(13)
                    .withWidth(7)
                    .withHeight(42)
                    .withWeight(1.14)
                    .build()
            )
        );
        verifyShopAvailableDeliveries();
    }

    /**
     * Флоу, по которому проходят тесты:
     * <ol>
     *     <li>Запускается процессор, которому передали выгрузку модификаторов по конкретному сендеру;</li>
     *     <li>Он производит какие-то операции поиска и формирования запроса к индексатору;</li>
     *     <li>Мы проверяем, какой запрос был произведен.</li>
     * </ol>
     *
     * @param requestModifier ожидаемый запрос к индексатору
     */
    private void processModifierUploadTest(Consumer<YaDeliverySenderSettingsDto.Builder> requestModifier) {
        process();

        YaDeliverySenderSettingsDto.Builder expectedRequestBuilder = defaultSettingsBuilder();
        requestModifier.accept(expectedRequestBuilder);
        YaDeliverySenderSettingsDto expectedRequest = expectedRequestBuilder.build();

        ArgumentCaptor<YaDeliverySenderSettingsDto> captor = ArgumentCaptor.forClass(YaDeliverySenderSettingsDto.class);
        verify(indexerClient).updateShopSettings(captor.capture());

        YaDeliverySenderSettingsDto uploadRequest = captor.getValue();
        softly.assertThat(validator.validate(uploadRequest)).isEmpty();
        softly.assertThat(uploadRequest)
            .usingRecursiveComparison()
            .isEqualTo(expectedRequest);
    }

    @Test
    @DisplayName("Обработка задач выключается по настройке")
    void processorCanBeDisabled() {
        properties.setEnabled(false);
        process();
        properties.setEnabled(true);
    }

    @Test
    @DisplayName("Сендер не существует")
    void senderNotExists() {
        softly.assertThatThrownBy(() -> process(2))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SENDER] with ids [2]");
    }

    @Test
    @DisplayName("Сендер существует, но уже не активный")
    @DatabaseSetup(
        value = "/jobs/processor/modifier_upload/before/not_active_sender.xml",
        type = DatabaseOperation.UPDATE
    )
    void deletedSender() {
        softly.assertThatThrownBy(this::process)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SENDER] with ids [1]");
    }

    @Test
    @DisplayName("Индексатор отдает ошибку")
    void indexerError() {
        mockShopAvailableDeliveries();
        RuntimeException thrownException = new RuntimeException("Some error");
        doThrow(thrownException).when(indexerClient).updateShopSettings(any(YaDeliverySenderSettingsDto.class));

        softly.assertThatThrownBy(this::process)
            .isSameAs(thrownException);

        verify(indexerClient).updateShopSettings(any(YaDeliverySenderSettingsDto.class));
        verifyShopAvailableDeliveries();
    }

    private void process(long senderId) {
        processor.processPayload(new SenderModifiersUploadPayload(REQUEST_ID, senderId));
    }

    private void process() {
        process(1);
    }

    @Nonnull
    private PartnerResponse partnerWithSubtype(long partnerId) {
        return PartnerResponse.newBuilder()
            .id(partnerId)
            .partnerType(PartnerType.DELIVERY)
            .subtype(PartnerSubtypeResponse.newBuilder().id(2L).build())
            .build();
    }

    @Nonnull
    private YaDeliverySenderSettingsDto.Builder defaultSettingsBuilder() {
        return new YaDeliverySenderSettingsDto.Builder()
            .withSenderId(1L)
            .withCarrierIdToRegionIds(Map.of(1L, Set.of(1, 213)))
            .withAverageOfferWeightDimensions(DEFAULT_AVERAGE_ORDER_MEASURES)
            .withModifiers(Set.of());
    }

    @Nonnull
    private DeliveryModifierDto.Builder defaultModifierBuilder(long modifierId) {
        return new DeliveryModifierDto.Builder()
            .withId(modifierId)
            .withTimestamp(UPDATED_DATE.toInstant().toEpochMilli())
            .withAction(new ActionDto.Builder().withIsCarrierTurnedOn(false).build());
    }

    @Nonnull
    private ActionDto.Builder defaultActionBuilder() {
        return new ActionDto.Builder()
            .withIsCarrierTurnedOn(false)
            .withPaidByCustomerServices(Set.of(ActionDto.DeliveryServiceCode.CASH_SERVICE));
    }

    @Nonnull
    private ValueModificationRuleDto modificationRule(
        OperationEnum operation,
        String parameter,
        @Nullable Integer limitMin,
        @Nullable Integer limitMax
    ) {
        return new ValueModificationRuleDto.Builder()
            .withOperation(operation)
            .withParameter(new BigDecimal(parameter))
            .withResultLimit(valueLimiter(limitMin, limitMax))
            .build();
    }

    @Nullable
    private ValueLimiterDto valueLimiter(@Nullable Integer limitMin, @Nullable Integer limitMax) {
        return limitMin == null && limitMax == null
            ? null
            : new ValueLimiterDto(
                Optional.ofNullable(limitMin).map(BigDecimal::valueOf).orElse(null),
                Optional.ofNullable(limitMax).map(BigDecimal::valueOf).orElse(null)
            );
    }

    @Nonnull
    private static SearchPartnerFilter.Builder createValidPartnerFilterBuilder() {
        return SearchPartnerFilter.builder()
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setStatuses(EnumSet.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .setPlatformClientStatuses(Set.of(PartnerStatus.ACTIVE));
    }

    private void mockShopAvailableDeliveries() {
        mockShopAvailableDeliveries(1L);
    }

    private void mockShopAvailableDeliveries(long deliveryServiceId) {
        ShopAvailableDeliveriesUtils.mockShopAvailableDeliveries(
            lmsClient,
            201L,
            List.of(
                LmsFactory.createPartner(10L, PartnerType.SORTING_CENTER),
                LmsFactory.createPartner(deliveryServiceId, PartnerType.DELIVERY)
            ),
            Set.of(10L),
            Map.of(10L, Set.of(deliveryServiceId))
        );
    }

    private void verifyShopAvailableDeliveries() {
        verifyShopAvailableDeliveries(1L);
    }

    private void verifyShopAvailableDeliveries(long deliveryServiceId) {
        verify(lmsClient).searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(10L))
                .fromPartnerTypes(Set.of(PartnerType.SORTING_CENTER))
                .enabled(true)
                .build()
        );
        verify(lmsClient).searchPartners(LmsFactory.createPartnerFilter(Set.of(deliveryServiceId, 10L), null));
    }
}
