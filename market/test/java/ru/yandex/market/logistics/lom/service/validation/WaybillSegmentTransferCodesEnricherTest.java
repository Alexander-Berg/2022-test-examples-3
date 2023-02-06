package ru.yandex.market.logistics.lom.service.validation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.PartnerSettings;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentTransferCodesEnricher;
import ru.yandex.market.logistics.lom.service.waybill.TransferCodesServiceImpl;

import static ru.yandex.market.logistics.lom.entity.enums.SegmentType.LAST_MILE_SEGMENT_TYPES;

@DisplayName("Обогащение сегментов кодами подтверждения передачи заказа")
public class WaybillSegmentTransferCodesEnricherTest extends AbstractTest {
    private static final PartnerSettings SETTINGS_IN = PartnerSettings.builder()
        .inboundVerificationCodeRequired(true)
        .build();

    private static final PartnerSettings SETTINGS_OUT = PartnerSettings.builder()
        .outboundVerificationCodeRequired(true)
        .build();

    private static final PartnerSettings SETTINGS_IN_OUT = PartnerSettings.builder()
        .inboundVerificationCodeRequired(true)
        .outboundVerificationCodeRequired(true)
        .build();

    private final WaybillSegmentTransferCodesEnricher enricher =
        new WaybillSegmentTransferCodesEnricher(
            new TransferCodesServiceImpl()
        );

    @Test
    @DisplayName("Смежные сегменты с совпадающими парами INBOUND_REQUIRED и OUTBOUND_REQUIRED")
    void enrichSuccessWithConsecutive() {
        WaybillSegment delivery0 = getWaybillSegment(1, PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0, SETTINGS_OUT);
        WaybillSegment delivery1 = getWaybillSegment(2, PartnerType.DELIVERY, SegmentType.MOVEMENT, 1, SETTINGS_IN_OUT);
        WaybillSegment delivery2 = getWaybillSegment(3, PartnerType.DELIVERY, SegmentType.PICKUP, 2, SETTINGS_IN);
        Order order = buildOrder(List.of(delivery0, delivery1, delivery2));

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> enrichedWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(enrichedWaybill.get(0).getTransferCodes().getOutbound()).isNotNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes().getInbound()).isNotNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes().getOutbound()).isNotNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes().getInbound()).isNotNull();
        softly.assertThat(order.getRecipientVerificationCode()).isNull();

        softly.assertThat(enrichedWaybill.get(0).getTransferCodes().getOutbound())
            .usingRecursiveComparison()
            .isEqualTo(enrichedWaybill.get(1).getTransferCodes().getInbound());
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes().getOutbound())
            .usingRecursiveComparison()
            .isEqualTo(enrichedWaybill.get(2).getTransferCodes().getInbound());
    }

    @Test
    @DisplayName("Смежные сегменты без совпадающих пар INBOUND_REQUIRED и OUTBOUND_REQUIRED")
    void enrichSuccessWithoutConsecutive() {
        WaybillSegment delivery0 =
            getWaybillSegment(1, PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0, SETTINGS_IN_OUT);
        WaybillSegment delivery1 = getWaybillSegment(2, PartnerType.DELIVERY, SegmentType.MOVEMENT, 1, null);
        WaybillSegment delivery2 = getWaybillSegment(3, PartnerType.DELIVERY, SegmentType.PICKUP, 2, SETTINGS_IN);
        Order order = buildOrder(List.of(delivery0, delivery1, delivery2));

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> enrichedWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(enrichedWaybill.get(0).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes()).isNull();
        softly.assertThat(order.getRecipientVerificationCode()).isNull();
    }

    @Test
    @DisplayName("Последний сегмент с OUTBOUND_REQUIRED")
    void enrichSuccessLastOutbound() {
        WaybillSegment delivery0 = getWaybillSegment(1, PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0, SETTINGS_OUT);
        WaybillSegment delivery1 = getWaybillSegment(2, PartnerType.DELIVERY, SegmentType.MOVEMENT, 1, null);
        WaybillSegment delivery2 = getWaybillSegment(3, PartnerType.DELIVERY, SegmentType.PICKUP, 2, SETTINGS_OUT);
        Order order = buildOrder(List.of(delivery0, delivery1, delivery2));

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> enrichedWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(enrichedWaybill.get(0).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes()).isNotNull();
        softly.assertThat(order.getRecipientVerificationCode()).isNotNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes().getOutbound().getVerification())
            .isEqualTo(order.getRecipientVerificationCode());
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Заказ с тэгом B2B_CUSTOMER")
    void enrichSuccessB2BOrder(SegmentType lastSegmentType) {
        WaybillSegment delivery0 = getWaybillSegment(1, PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0, null);
        WaybillSegment delivery1 = getWaybillSegment(2, PartnerType.DELIVERY, SegmentType.MOVEMENT, 1, null);
        WaybillSegment delivery2 = getWaybillSegment(3, PartnerType.DELIVERY, lastSegmentType, 2, null);
        Order order = buildOrder(List.of(delivery0, delivery1, delivery2));
        order.addOrderTag(OrderTag.B2B_CUSTOMER);

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> enrichedWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(enrichedWaybill.get(0).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes()).isNotNull();
        softly.assertThat(order.getRecipientVerificationCode()).isNotNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes().getOutbound().getVerification())
            .isEqualTo(order.getRecipientVerificationCode());
    }

    @Nonnull
    private static Stream<Arguments> enrichSuccessB2BOrder() {
        return LAST_MILE_SEGMENT_TYPES.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Заказ с тэгом B2B_CUSTOMER, не подходящий тип сегмента")
    void enrichSuccessB2BOrderNotLastMile(SegmentType lastSegmentType) {
        WaybillSegment delivery0 = getWaybillSegment(1, PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0, null);
        WaybillSegment delivery1 = getWaybillSegment(2, PartnerType.DELIVERY, SegmentType.MOVEMENT, 1, null);
        WaybillSegment delivery2 = getWaybillSegment(3, PartnerType.DELIVERY, lastSegmentType, 2, null);
        Order order = buildOrder(List.of(delivery0, delivery1, delivery2));
        order.addOrderTag(OrderTag.B2B_CUSTOMER);

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> enrichedWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(enrichedWaybill.get(0).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes()).isNull();
    }

    @Test
    @DisplayName("Код для выдачи невыкупа в C2C заказе")
    void enrichSuccessC2COrder() {
        WaybillSegment delivery0 = getWaybillSegment(1, PartnerType.YANDEX_GO_SHOP, SegmentType.NO_OPERATION, 0, null);
        WaybillSegment delivery1 = getWaybillSegment(2, PartnerType.DELIVERY, SegmentType.SORTING_CENTER, 1, null);
        WaybillSegment delivery2 = getWaybillSegment(3, PartnerType.DELIVERY, SegmentType.PICKUP, 2, SETTINGS_OUT);
        Order order = buildOrder(List.of(delivery0, delivery1, delivery2));
        order.addOrderTag(OrderTag.C2C);

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> enrichedWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(order.getRecipientVerificationCode()).isNotNull();
        softly.assertThat(enrichedWaybill.get(0).getTransferCodes()).isNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes()).isNotNull();
        softly.assertThat(enrichedWaybill.get(1).getTransferCodes().getReturnOutbound().getVerification())
            .isEqualTo(order.getRecipientVerificationCode());
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes()).isNotNull();
        softly.assertThat(enrichedWaybill.get(2).getTransferCodes().getOutbound().getVerification())
            .isEqualTo(order.getRecipientVerificationCode());
    }

    @Nonnull
    private static Stream<Arguments> enrichSuccessB2BOrderNotLastMile() {
        return Arrays.stream(SegmentType.values())
            .filter(type -> !LAST_MILE_SEGMENT_TYPES.contains(type))
            .map(Arguments::of);
    }

    @Nonnull
    private Order buildOrder(List<WaybillSegment> waybillSegments) {
        Order order = Order.builder()
            .waybill(waybillSegments)
            .returnSortingCenterId(1111L)
            .build();
        waybillSegments.forEach(ws -> ws.setOrder(order));
        return order;
    }

    @Nonnull
    private static WaybillSegment getWaybillSegment(
        long id,
        PartnerType partnerType,
        SegmentType segmentType,
        int index,
        PartnerSettings settings
    ) {
        return new WaybillSegment()
            .setId(id)
            .setPartnerType(partnerType)
            .setSegmentType(segmentType)
            .setWaybillSegmentIndex(index)
            .setPartnerSettings(settings)
            .setPartnerId(1L);
    }
}
