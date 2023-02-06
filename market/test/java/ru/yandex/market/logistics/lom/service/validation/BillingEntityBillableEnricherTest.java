package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.billing.BillingEntity;
import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.BillingEntityBillableEnricher;

@DisplayName("Обогащение признаком billable")
@ParametersAreNonnullByDefault
class BillingEntityBillableEnricherTest extends AbstractTest {
    private final FeatureProperties featureProperties = new FeatureProperties();
    private final BillingEntityBillableEnricher enricher = new BillingEntityBillableEnricher(featureProperties);

    @DisplayName("Успешное обогащение сегментов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void enrichSuccess(String name, List<WaybillSegment> segments, Long contractId, boolean result) {
        featureProperties.setBalanceContractIdsWithDisabledBilling(List.of(contractId));
        Order order = Order.builder()
            .waybill(segments)
            .billingEntity(
                new BillingEntity()
                    .setBalanceContractId(2L)
                    .setBalancePersonId(123L)
                    .setBillable(true)
            )
            .build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        Order resultOrder = results.getOrderModifier().apply(order);

        softly.assertThat(resultOrder.getBillingEntity().getBillable()).isEqualTo(result);
    }

    @Nonnull
    private static Stream<Arguments> enrichSuccess() {
        return Stream.of(
            Arguments.of(
                "Не биллить, есть сегмент с сабтипом",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 1),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.MOVEMENT, 2)
                        .setPartnerSubtype(PartnerSubtype.COURIER_PLATFORM_FOR_SHOP),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 3)
                ),
                1L,
                false
            ),
            Arguments.of(
                "Не биллить, contract_id в списке исключенных",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 1),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.MOVEMENT, 2),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 3)
                ),
                2L,
                false
            ),
            Arguments.of(
                "Не биллить, есть сегмент с сабтипом и contract_id в списке исключенных",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 1),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.MOVEMENT, 2)
                        .setPartnerSubtype(PartnerSubtype.COURIER_PLATFORM_FOR_SHOP),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 3)
                ),
                2L,
                false
            ),
            Arguments.of(
                "Биллить",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 1),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.MOVEMENT, 2),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 3)
                ),
                1L,
                true
            )
        );
    }

    @Nonnull
    private static WaybillSegment getWaybillSegment(PartnerType partnerType, SegmentType segmentType, int index) {
        return new WaybillSegment()
            .setPartnerType(partnerType)
            .setSegmentType(segmentType)
            .setWaybillSegmentIndex(index)
            .setPartnerId(1L);
    }
}
