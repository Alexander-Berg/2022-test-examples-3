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
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentYandexGoTagEnricher;

@DisplayName("Обогащение сегментов тэгом YANDEX_GO")
@ParametersAreNonnullByDefault
class WaybillSegmentYandexGoTagEnricherTest extends AbstractTest {
    private final WaybillSegmentYandexGoTagEnricher enricher = new WaybillSegmentYandexGoTagEnricher();

    @Test
    @DisplayName("Успешное обогащение сегментов")
    void enrichSuccess() {
        Order order = Order.builder()
            .waybill(List.of(
                getWaybillSegment(PartnerType.YANDEX_GO_SHOP, SegmentType.NO_OPERATION, 0),
                getWaybillSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 1),
                getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 2)
            ))
            .platformClient(PlatformClient.YANDEX_GO)
            .build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> resultWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(resultWaybill.get(0).hasTag(WaybillSegmentTag.YANDEX_GO)).isTrue();
        softly.assertThat(resultWaybill.get(1).hasTag(WaybillSegmentTag.YANDEX_GO)).isTrue();
        softly.assertThat(resultWaybill.get(2).hasTag(WaybillSegmentTag.YANDEX_GO)).isTrue();
    }

    @Test
    @DisplayName("Не добавился тэг сегмента, так как заказ не от платформы Yandex Go")
    void enrichFailed() {
        Order order = Order.builder()
            .waybill(List.of(
                getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                getWaybillSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 1),
                getWaybillSegment(PartnerType.DELIVERY, SegmentType.PICKUP, 2)
            ))
            .platformClient(PlatformClient.YANDEX_DELIVERY)
            .build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        List<WaybillSegment> resultWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(resultWaybill.get(0).hasTag(WaybillSegmentTag.YANDEX_GO)).isFalse();
        softly.assertThat(resultWaybill.get(1).hasTag(WaybillSegmentTag.YANDEX_GO)).isFalse();
        softly.assertThat(resultWaybill.get(2).hasTag(WaybillSegmentTag.YANDEX_GO)).isFalse();
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
