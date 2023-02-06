package ru.yandex.market.logistics.nesu.enums;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.dto.enums.DaasOrderStatus;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;

public class DaasOrderStatusTest extends AbstractTest {
    private static final Set<SegmentStatus> DELIVERY_STATUSES = EnumSet.of(
        SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT,
        SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION,
        SegmentStatus.TRANSIT_CUSTOMS_ARRIVED,
        SegmentStatus.TRANSIT_CUSTOMS_CLEARED,
        SegmentStatus.TRANSIT_DELIVERY_ARRIVED,
        SegmentStatus.TRANSIT_TIME_CLARIFIED,
        SegmentStatus.TRANSIT_STORAGE_PERIOD_EXTENDED,
        SegmentStatus.TRANSIT_STORAGE_PERIOD_EXPIRED,
        SegmentStatus.TRANSIT_PICKUP,
        SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
        SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
        SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
        SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT,
        SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
        // На данный момент не используем в несу, этот статус для изменения кода передачи заказа с ПВЗ Маркета
        //SegmentStatus.TRANSIT_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED,
        // На данный момент не используем в несу, эти статусы для схемы экспресс доставки с вызовом курьера
        //SegmentStatus.TRANSIT_COURIER_SEARCH,
        //SegmentStatus.TRANSIT_COURIER_FOUND,
        //SegmentStatus.TRANSIT_COURIER_IN_TRANSIT_TO_SENDER,
        //SegmentStatus.TRANSIT_COURIER_ARRIVED_TO_SENDER,
        //SegmentStatus.TRANSIT_COURIER_RECEIVED,
        //SegmentStatus.TRANSIT_COURIER_NOT_FOUND,
        SegmentStatus.ERROR_DELIVERY_CAN_NOT_BE_COMPLETED
    );

    private static final Set<SegmentStatus> SORTING_CENTER_STATUSES = EnumSet.of(
        SegmentStatus.TRANSIT_OUT_OF_STOCK,
        //не используется в nesu
        //SegmentStatus.TRANSIT_AUTOMATICALLY_REMOVED_ITEMS,
        SegmentStatus.TRANSIT_AWAITING_CLARIFICATION,
        SegmentStatus.TRANSIT_PREPARED,
        SegmentStatus.RETURN_PREPARING_SENDER,
        SegmentStatus.RETURN_TRANSFERRED,
        SegmentStatus.RETURN_RFF_PREPARING_FULFILLMENT,
        SegmentStatus.RETURN_RFF_TRANSMITTED_FULFILLMENT,
        SegmentStatus.RETURN_RFF_ARRIVED_FULFILLMENT
        //не используется в nesu
//      SegmentStatus.RETURN_PREPARED_FOR_UTILIZE,
//      SegmentStatus.RETURN_SHIPPED_FOR_UTILIZER
    );

    private static final Set<SegmentStatus> SORTING_CENTER_RETURNING_STATUSES = EnumSet.of(
        SegmentStatus.RETURN_PREPARING_SENDER,
        SegmentStatus.RETURN_TRANSFERRED,
        SegmentStatus.RETURN_RFF_PREPARING_FULFILLMENT,
        SegmentStatus.RETURN_RFF_TRANSMITTED_FULFILLMENT,
        SegmentStatus.RETURN_RFF_ARRIVED_FULFILLMENT
    );

    private static final Set<SegmentStatus> COMMON_STATUSES = EnumSet.of(
        SegmentStatus.STARTED,
        SegmentStatus.TRACK_RECEIVED,
        SegmentStatus.PENDING,
        SegmentStatus.INFO_RECEIVED,
        SegmentStatus.ERROR,
        SegmentStatus.IN,
        SegmentStatus.OUT,
        SegmentStatus.RETURN_PREPARING,
        SegmentStatus.RETURN_ARRIVED,
        SegmentStatus.RETURNED,
        SegmentStatus.ERROR_LOST,
        SegmentStatus.ERROR_NOT_FOUND,
        SegmentStatus.CANCELLED,
        SegmentStatus.UNKNOWN
    );

    private static final Set<SegmentStatus> COMMON_RETURNING_STATUSES = EnumSet.of(
        SegmentStatus.RETURN_PREPARING,
        SegmentStatus.RETURN_ARRIVED,
        SegmentStatus.RETURNED
    );

    private static final Set<SegmentStatus> OWN_DELIVERY_STATUSES = EnumSet.of(
        SegmentStatus.INFO_RECEIVED
    );

    @Nonnull
    private static Stream<Arguments> successConverting() {
        return Stream.of(
            Arguments.of(
                OrderStatus.PROCESSING,
                DELIVERY_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                SORTING_CENTER_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.SORTING_CENTER))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                COMMON_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                OWN_DELIVERY_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.OWN_DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                COMMON_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.SORTING_CENTER))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.RETURNING,
                SORTING_CENTER_RETURNING_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.SORTING_CENTER))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.RETURNING,
                COMMON_RETURNING_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.SORTING_CENTER))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.RETURNING,
                COMMON_RETURNING_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.RETURNED,
                List.of(createWaybillSegment(SegmentStatus.RETURNED, PartnerType.SORTING_CENTER))
            ),
            Arguments.of(
                OrderStatus.DELIVERED,
                List.of(createWaybillSegment(SegmentStatus.OUT, PartnerType.DELIVERY))
            ),
            Arguments.of(
                OrderStatus.LOST,
                List.of(createWaybillSegment(SegmentStatus.ERROR_LOST, PartnerType.DELIVERY))
            ),
            Arguments.of(
                OrderStatus.DRAFT,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.VALIDATING,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.VALIDATION_ERROR,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.ENQUEUED,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.PROCESSING_ERROR,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.FINISHED,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.CANCELLED,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.DELIVERED,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.RETURNED,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.LOST,
                Collections.singletonList(null)
            )
        );
    }

    @ParameterizedTest
    @MethodSource("successConverting")
    @DisplayName("Успешная конвертация статусов LOM")
    void success(OrderStatus lomOrderStatus, List<WaybillSegmentDto> waybill) {
        waybill.forEach(segment -> softly.assertThatCode(() -> DaasOrderStatus.createBasedOn(lomOrderStatus, segment))
            .doesNotThrowAnyException());
    }

    @Nonnull
    private static Stream<Arguments> failConverting() {
        return Stream.of(
            Arguments.of(
                OrderStatus.PROCESSING,
                DELIVERY_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.SORTING_CENTER))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                SORTING_CENTER_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                SORTING_CENTER_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.OWN_DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                DELIVERY_STATUSES.stream()
                    .map(status -> createWaybillSegment(status, PartnerType.OWN_DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                Sets.difference(COMMON_STATUSES, OWN_DELIVERY_STATUSES)
                    .stream()
                    .map(status -> createWaybillSegment(status, PartnerType.OWN_DELIVERY))
                    .collect(Collectors.toList())
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                Collections.singletonList(null)
            ),
            Arguments.of(
                OrderStatus.PROCESSING,
                List.of(createWaybillSegment(null, PartnerType.DELIVERY))
            )
        );
    }

    @ParameterizedTest
    @MethodSource("failConverting")
    @DisplayName("Неуспешная конвертация статусов LOM")
    void fail(OrderStatus lomOrderStatus, List<WaybillSegmentDto> waybill) {
        waybill.forEach(
            segment -> softly.assertThatThrownBy(
                () -> DaasOrderStatus.createBasedOn(lomOrderStatus, segment)
            )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create DaasOrderStatus from processing status")
        );
    }

    @Nonnull
    private static WaybillSegmentDto createWaybillSegment(@Nullable SegmentStatus status, PartnerType partnerType) {
        return WaybillSegmentDto.builder()
            .partnerType(partnerType)
            .segmentStatus(status)
            .waybillSegmentStatusHistory(
                List.of(
                    WaybillSegmentStatusHistoryDto.builder()
                        .status(status)
                        .date(
                            LocalDate.of(2019, 10, 1)
                                .atStartOfDay(CommonsConstants.MSK_TIME_ZONE)
                                .toInstant()
                        ).build()
                )
            ).build();
    }
}
