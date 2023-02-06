package ru.yandex.market.logistics.lom.jobs.processor.strategy.recalculatedroute;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.converter.DeliveryIntervalConverter;
import ru.yandex.market.logistics.lom.dto.changerequest.RecalculateOrderDeliveryDatePayload;
import ru.yandex.market.logistics.lom.entity.ChangeOrderRequest;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.dto.RecalculatedOrderDatesPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderDeliveryDateRequestDto;
import ru.yandex.market.logistics.lom.service.order.ChangeOrderRequestService;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class BaseRddProcessingStrategyTest extends AbstractTest {
    @Mock
    protected ChangeOrderRequestService changeOrderRequestService;

    @Mock
    protected FeatureProperties featureProperties;

    protected final DeliveryIntervalConverter deliveryIntervalConverter = new DeliveryIntervalConverter();

    @Nonnull
    protected RecalculateOrderDeliveryDatePayload mockRecalculateOrderDeliveryDatePayload(
        Boolean notifyUser,
        ServiceCodeName serviceCodeName,
        ChangeOrderRequest changeOrderRequest,
        Boolean isRddDay
    ) {
        RecalculateOrderDeliveryDatePayload payload = new RecalculateOrderDeliveryDatePayload(
            0,
            serviceCodeName,
            null,
            null,
            notifyUser,
            isRddDay
        );
        when(
            changeOrderRequestService.getPayload(
                eq(changeOrderRequest),
                eq(RecalculateOrderDeliveryDatePayload.class),
                eq(ChangeOrderRequestStatus.CREATED)
            )
        ).thenReturn(payload);
        return payload;
    }

    protected void mockOrderDeliveryDateCor() {
        ChangeOrderRequest changeOrderRequest = new ChangeOrderRequest();
        changeOrderRequest.setRequestType(ChangeOrderRequestType.DELIVERY_DATE);
        changeOrderRequest.setCreated(Instant.EPOCH);
        when(changeOrderRequestService.searchChangeOrderRequests(any(), any()))
            .thenReturn(List.of(changeOrderRequest));
        UpdateOrderDeliveryDateRequestDto payload = UpdateOrderDeliveryDateRequestDto.builder()
            .dateMin(LocalDate.of(2022, 1, 25))
            .dateMax(LocalDate.of(2022, 1, 26))
            .build();
        when(
            changeOrderRequestService.getPayload(
                eq(changeOrderRequest),
                eq(UpdateOrderDeliveryDateRequestDto.class),
                eq(ChangeOrderRequestStatus.INFO_RECEIVED)
            )
        ).thenReturn(payload);
    }

    protected void mockOrderRecalculateRddCorWithoutUserNotification() {
        ChangeOrderRequest changeOrderRequest = new ChangeOrderRequest();
        changeOrderRequest.setRequestType(ChangeOrderRequestType.RECALCULATE_ROUTE_DATES);
        changeOrderRequest.setCreated(Instant.EPOCH);
        when(changeOrderRequestService.searchChangeOrderRequests(any(), any()))
            .thenReturn(List.of(changeOrderRequest));
        RecalculateOrderDeliveryDatePayload createPayload = new RecalculateOrderDeliveryDatePayload(
            0,
            ServiceCodeName.SHIPMENT,
            SegmentStatus.OUT,
            null,
            false,
            null
        );
        when(
            changeOrderRequestService.getPayload(
                eq(changeOrderRequest),
                eq(RecalculateOrderDeliveryDatePayload.class),
                eq(ChangeOrderRequestStatus.CREATED)
            )
        ).thenReturn(createPayload);

        RecalculatedOrderDatesPayloadDto infoReceivedPayload = new TestDatesPayloadDtoBuilder()
            .setOldDeliveryDateMin(LocalDate.of(2022, 1, 25))
            .setOldDeliveryDateMax(LocalDate.of(2022, 1, 26))
            .build();
        when(
            changeOrderRequestService.getPayload(
                eq(changeOrderRequest),
                eq(RecalculatedOrderDatesPayloadDto.class),
                eq(ChangeOrderRequestStatus.INFO_RECEIVED)
            )
        ).thenReturn(infoReceivedPayload);
    }

    @Setter
    @Accessors(chain = true)
    protected static class TestDatesPayloadDtoBuilder {
        private LocalDate oldDeliveryDateMin = LocalDate.of(2022, 1, 20);
        private LocalDate oldDeliveryDateMax = LocalDate.of(2022, 1, 21);
        private LocalTime oldStartTime = LocalTime.of(9, 0);
        private LocalTime oldEndTime = LocalTime.of(18, 0);
        private LocalDate deliveryDateMin = LocalDate.of(2022, 1, 21);
        private LocalDate deliveryDateMax = LocalDate.of(2022, 1, 22);
        private LocalTime startTime = LocalTime.of(9, 0);
        private LocalTime endTime = LocalTime.of(18, 0);

        RecalculatedOrderDatesPayloadDto build() {
            return new RecalculatedOrderDatesPayloadDto(
                oldDeliveryDateMin,
                oldDeliveryDateMax,
                oldStartTime,
                oldEndTime,
                deliveryDateMin,
                deliveryDateMax,
                startTime,
                endTime,
                null,
                null
            );
        }
    }
}
