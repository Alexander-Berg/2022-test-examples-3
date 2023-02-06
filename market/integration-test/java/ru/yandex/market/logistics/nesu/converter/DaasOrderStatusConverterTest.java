package ru.yandex.market.logistics.nesu.converter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto.WaybillSegmentDtoBuilder;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.api.converter.OrderConverter;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;

public class DaasOrderStatusConverterTest extends AbstractContextualTest {

    @Autowired
    private OrderConverter orderConverter;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("successConverting")
    @DisplayName("Успешная конвертация статусов LOM")
    void success(String caseName, OrderStatus lomOrderStatus, List<WaybillSegmentDto> waybill) {
        softly.assertThatCode(() -> orderConverter.convertStatus(lomOrderStatus, waybill)).doesNotThrowAnyException();
    }

    @Nonnull
    private static Stream<Arguments> successConverting() {
        return Stream.of(
            Arguments.of(
                "Есть статус только у одного сегмента",
                OrderStatus.PROCESSING,
                List.of(
                    createWaybillSegment(null, PartnerType.DELIVERY).waybillSegmentStatusHistory(null).build(),
                    createWaybillSegment(SegmentStatus.IN, PartnerType.DELIVERY).build()
                )
            ),
            Arguments.of(
                "Заказ в статусе DRAFT, нет статуса у сегмента",
                OrderStatus.DRAFT,
                List.of(createWaybillSegment(null, PartnerType.DELIVERY).waybillSegmentStatusHistory(null).build())
            ),
            Arguments.of(
                "Заказ в статусе DRAFT, нет вейбилла",
                OrderStatus.DRAFT,
                null
            )
        );
    }

    @Nonnull
    private static WaybillSegmentDtoBuilder createWaybillSegment(
        @Nullable SegmentStatus status,
        PartnerType partnerType
    ) {
        return WaybillSegmentDto.builder()
            .partnerType(partnerType)
            .segmentStatus(status)
            .waybillSegmentStatusHistory(List.of(
                WaybillSegmentStatusHistoryDto.builder()
                    .status(status)
                    .date(
                        LocalDate.of(2019, 10, 1)
                            .atStartOfDay(CommonsConstants.MSK_TIME_ZONE)
                            .toInstant()
                    )
                    .build()
            ));
    }
}
