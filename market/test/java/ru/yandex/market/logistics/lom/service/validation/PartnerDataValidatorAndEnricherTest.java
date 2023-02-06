package ru.yandex.market.logistics.lom.service.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.LmsModelFactory;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.converter.lms.DeliveryTypeLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerExternalParamLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerSubtypeLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerTypeLmsConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.PartnerDataValidatorAndEnricher;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightClient;
import ru.yandex.market.logistics.lom.lms.model.PartnerLightModel;
import ru.yandex.market.logistics.lom.service.partner.PartnerServiceImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.PartnerType.DELIVERY;
import static ru.yandex.market.logistics.management.entity.type.PartnerType.SORTING_CENTER;

@DisplayName("Валидация и обогащение типа партнеров")
class PartnerDataValidatorAndEnricherTest extends AbstractTest {
    private static final Set<Long> PARTNER_IDS = Set.of(1L, 2L, 3L);
    private static final List<PartnerLightModel> PARTNERS = List.of(
        PartnerLightModel.build(
            LmsModelFactory.createPartnerResponse(1, SORTING_CENTER, PartnerSubtype.PARTNER_SORTING_CENTER)
        ),
        PartnerLightModel.build(
            LmsModelFactory.createPartnerResponse(2, DELIVERY, PartnerSubtype.PARTNER_CONTRACT_DELIVERY)
        ),
        PartnerLightModel.build(
            LmsModelFactory.createPartnerResponse(3, SORTING_CENTER, null)
        )
    );

    private final LmsLomLightClient lmsLomLightClient = mock(LmsLomLightClient.class);
    private final PartnerTypeLmsConverter partnerTypeLmsConverter = new PartnerTypeLmsConverter(new EnumConverter());
    private final PartnerDataValidatorAndEnricher partnerDataValidatorAndEnricher =
        new PartnerDataValidatorAndEnricher(
            new PartnerServiceImpl(
                lmsLomLightClient,
                new DeliveryTypeLmsConverter(),
                partnerTypeLmsConverter,
                new PartnerExternalParamLmsConverter()
            ),
            partnerTypeLmsConverter,
            new PartnerSubtypeLmsConverter(),
            new PartnerExternalParamLmsConverter()
        );
    private final ValidateAndEnrichContext context = new ValidateAndEnrichContext();

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsLomLightClient);
    }

    @DisplayName("Партнеры не найдены")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("partners")
    void noPartnersFound(List<Long> foundPartnerIds, List<Long> notFoundPartnerIds) {
        Order order = createOrder();
        when(lmsLomLightClient.getPartners(PARTNER_IDS)).thenReturn(
            PARTNERS.stream().filter(p -> foundPartnerIds.contains(p.getId())).collect(Collectors.toList())
        );
        ValidateAndEnrichResults results = partnerDataValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isFalse();

        softly.assertThat(results.getErrorMessage())
            .isEqualTo(String.format("Partners with ids %s not found in LMS", notFoundPartnerIds));
        softly.assertThat(context.getPartners()).isEmpty();
        softly.assertThat(context.getPartnerTypeById()).isEmpty();
        verify(lmsLomLightClient).getPartners(PARTNER_IDS);
    }

    @DisplayName("Успешное обогащение типов партнеров заказа")
    @Test
    void partnerTypeEnrichingSuccess() {
        Order order = createOrder();
        when(lmsLomLightClient.getPartners(PARTNER_IDS)).thenReturn(PARTNERS);

        ValidateAndEnrichResults results = partnerDataValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(
            results.getOrderModifier().apply(order)
                .getWaybill().stream()
                .map(WaybillSegment::getPartnerType)
        )
            .containsExactly(PartnerType.SORTING_CENTER, PartnerType.DELIVERY);

        softly.assertThat(context.getPartners()).containsAll(PARTNERS);
        softly.assertThat(context.getPartnerTypeById()).containsAllEntriesOf(Map.of(
            1L, PartnerType.SORTING_CENTER,
            2L, PartnerType.DELIVERY,
            3L, PartnerType.SORTING_CENTER
        ));
        verify(lmsLomLightClient).getPartners(PARTNER_IDS);
    }

    @DisplayName("Успешное обогащение подтипов партнеров заказа")
    @Test
    void partnerSubtypeEnrichingSuccess() {
        Order order = createOrder();
        when(lmsLomLightClient.getPartners(PARTNER_IDS)).thenReturn(PARTNERS);

        ValidateAndEnrichResults results = partnerDataValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(
            results.getOrderModifier().apply(order)
                .getWaybill().stream()
                .map(WaybillSegment::getPartnerType)
        )
            .containsExactly(PartnerType.SORTING_CENTER, PartnerType.DELIVERY);

        softly.assertThat(context.getPartners()).containsAll(PARTNERS);

        var expectedSubtypesMap = new HashMap<Long, PartnerSubtype>();
        expectedSubtypesMap.put(1L, PartnerSubtype.PARTNER_SORTING_CENTER);
        expectedSubtypesMap.put(2L, PartnerSubtype.PARTNER_CONTRACT_DELIVERY);
        expectedSubtypesMap.put(3L, null);

        softly.assertThat(context.getPartnerSubtypeById()).containsAllEntriesOf(expectedSubtypesMap);
        verify(lmsLomLightClient).getPartners(PARTNER_IDS);
    }

    @Nonnull
    private static Stream<Arguments> partners() {
        return Stream.of(
            Arguments.of(List.of(1L), List.of(2L, 3L)),
            Arguments.of(List.of(1L, 2L), List.of(3L))
        );
    }

    @Nonnull
    private Order createOrder() {
        return new Order()
            .setWaybill(List.of(new WaybillSegment().setPartnerId(1L), new WaybillSegment().setPartnerId(2L)))
            .setReturnSortingCenterId(3L);
    }
}
