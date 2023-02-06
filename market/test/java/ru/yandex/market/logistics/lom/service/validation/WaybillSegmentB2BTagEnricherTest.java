package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentB2BTagEnricher;

@DisplayName("Обогащение сегментов тэгом B2B_CUSTOMER")
@ParametersAreNonnullByDefault
class WaybillSegmentB2BTagEnricherTest extends AbstractTest {
    private final WaybillSegmentB2BTagEnricher enricher = new WaybillSegmentB2BTagEnricher();

    @Test
    @DisplayName("Успешное обогащение сегментов")
    void enrichSuccess() {
        Order order = Order.builder().waybill(List.of(
            getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
            getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 1),
            getWaybillSegment(PartnerType.DELIVERY, SegmentType.PICKUP, 2)
        )).build();
        order.addOrderTag(OrderTag.B2B_CUSTOMER);

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> resultWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(resultWaybill.get(0).hasTag(WaybillSegmentTag.B2B_CUSTOMER)).isFalse();
        softly.assertThat(resultWaybill.get(1).hasTag(WaybillSegmentTag.B2B_CUSTOMER)).isTrue();
        softly.assertThat(resultWaybill.get(2).hasTag(WaybillSegmentTag.B2B_CUSTOMER)).isTrue();
    }

    @Test
    @DisplayName("Не добавился тэг сегмента, так как нет тэга заказ")
    void enrichFailed() {
        Order order = Order.builder().waybill(List.of(
            getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
            getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 1),
            getWaybillSegment(PartnerType.DELIVERY, SegmentType.PICKUP, 2)
        )).build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> resultWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(resultWaybill.get(0).hasTag(WaybillSegmentTag.B2B_CUSTOMER)).isFalse();
        softly.assertThat(resultWaybill.get(1).hasTag(WaybillSegmentTag.B2B_CUSTOMER)).isFalse();
        softly.assertThat(resultWaybill.get(2).hasTag(WaybillSegmentTag.B2B_CUSTOMER)).isFalse();
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
