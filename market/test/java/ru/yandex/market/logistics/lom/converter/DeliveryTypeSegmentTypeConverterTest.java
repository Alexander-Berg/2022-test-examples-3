package ru.yandex.market.logistics.lom.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;

@DisplayName("Конвертация типа доставки в тип сегмента и наоборот")
class DeliveryTypeSegmentTypeConverterTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(DeliveryType.class)
    @DisplayName("Всякий тип доставки конвертируется в тип сегмента")
    void deliveryTypeToSegmentType(DeliveryType deliveryType) {
        softly.assertThat(enumConverter.convert(deliveryType, SegmentType.class).name())
            .isEqualTo(deliveryType.name());
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentType.class,
        names = {"SUPPLIER", "FULFILLMENT", "SORTING_CENTER", "GO_PLATFORM", "NO_OPERATION"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Всякий DS API тип сегмента конвертируется в тип доставки")
    void segmentTypeToDeliveryType(SegmentType segmentType) {
        softly.assertThat(enumConverter.convert(segmentType, DeliveryType.class).name())
            .isEqualTo(segmentType.name());
    }
}
