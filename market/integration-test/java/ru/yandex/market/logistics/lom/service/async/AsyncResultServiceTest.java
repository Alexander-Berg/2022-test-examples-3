package ru.yandex.market.logistics.lom.service.async;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.filter.BusinessProcessStateFilter;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.CancelOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.CancelOrderSuccessDto;
import ru.yandex.market.logistics.lom.model.async.CreateRegistryErrorDto;
import ru.yandex.market.logistics.lom.model.async.CreateRegistrySuccessDto;
import ru.yandex.market.logistics.lom.model.async.CreateReturnRegistryErrorDto;
import ru.yandex.market.logistics.lom.model.async.CreateReturnRegistrySuccessDto;
import ru.yandex.market.logistics.lom.model.async.CreateShipmentErrorDto;
import ru.yandex.market.logistics.lom.model.async.CreateShipmentSuccessDto;
import ru.yandex.market.logistics.lom.model.async.GetOrdersDeliveryDateErrorDto;
import ru.yandex.market.logistics.lom.model.async.GetOrdersDeliveryDateSuccessDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderDeliveryDateErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderDeliveryDateSuccessDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderRecipientErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderRecipientSuccessDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderSuccessDto;
import ru.yandex.market.logistics.lom.service.process.BusinessProcessStateService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@MockBean(
    value = {
        CancelOrderAsyncResultService.class,
        CreateReturnRegistryAsyncResultService.class,
        DeliveryServiceCreateOrderAsyncResultService.class,
        DeliveryServiceCreateRegistryAsyncResultService.class,
        DeliveryServiceCreateShipmentAsyncResultService.class,
        DeliveryServiceUpdateOrderAsyncResultService.class,
        FulfillmentCreateOrderAsyncResultService.class,
        FulfillmentCreateRegistryAsyncResultService.class,
        FulfillmentCreateShipmentAsyncResultService.class,
        FulfillmentUpdateOrderAsyncResultService.class,
        UpdateOrderItemsFromChangeRequestService.class,
        ProcessDeliveryDateUpdatedByDsAsyncResultService.class,
        ProcessUpdateDeliveryDateAsyncResultService.class,
        ProcessUpdateRecipientAsyncResultService.class,
        ProcessUpdateLastMileAsyncResultService.class,
        UpdateOrderItemsInstancesSegmentAsyncResultService.class,
        ProcessUpdateTransferCodesAsyncResultService.class,
        ProcessGetCourierAsyncResultService.class,
        ProcessUpdateCourierAsyncResultService.class,
        ProcessChangeOrderToOnDemandRequestAsyncResultService.class,
    },
    answer = Answers.CALLS_REAL_METHODS
)
abstract class AsyncResultServiceTest extends AbstractContextualTest {
    static final String MESSAGE = "Ooops... Something went wrong";

    private static final List<Arguments> ERROR_PAYLOADS =
        Stream.of(
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
                    put("/orders/ds/cancel/error"),
                    new CancelOrderErrorDto("LO-1", 48L, null, false, "Error 100", 23L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
                    put("/orders/ff/cancel/error"),
                    new CancelOrderErrorDto("LO-1", 48L, null, false, "Error 100", 23L)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
                    put("/shipments/ds/createIntakeError"),
                    new CreateShipmentErrorDto(2L, 2L, 100, "Error 100", 4L, false)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_CREATE_REGISTRY_EXTERNAL,
                    put("/registries/ds/createError"),
                    new CreateRegistryErrorDto("3", 100, "Error 100", 8L, false)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
                    put("/shipments/ds/createSelfExportError"),
                    new CreateShipmentErrorDto(2L, 2L, 100, "Error 100", 4L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_SHIPMENT_CREATION,
                    put("/shipments/ff/createIntakeError"),
                    new CreateShipmentErrorDto(2L, 3L, 100, "Error 100", 5L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_CREATE_REGISTRY_EXTERNAL,
                    put("/registries/ff/createError"),
                    new CreateRegistryErrorDto("3", 100, "Error 100", 9L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_SHIPMENT_CREATION,
                    put("/shipments/ff/createSelfExportError"),
                    new CreateShipmentErrorDto(2L, 3L, 100, "Error 100", 5L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
                    put("/returnRegistries/createError"),
                    new CreateReturnRegistryErrorDto("1", List.of(), 100, "Error 100", 21L, false)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
                    put("/shipments/ds/createIntakeError"),
                    new CreateShipmentErrorDto(2L, 2L, null, null, 4L, false)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_CREATE_REGISTRY_EXTERNAL,
                    put("/registries/ds/createError"),
                    new CreateRegistryErrorDto("3", null, null, 8L, false)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
                    put("/shipments/ds/createSelfExportError"),
                    new CreateShipmentErrorDto(2L, 2L, null, null, 4L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_SHIPMENT_CREATION,
                    put("/shipments/ff/createIntakeError"),
                    new CreateShipmentErrorDto(2L, 3L, null, null, 5L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_CREATE_REGISTRY_EXTERNAL,
                    put("/registries/ff/createError"),
                    new CreateRegistryErrorDto("3", null, null, 9L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_SHIPMENT_CREATION,
                    put("/shipments/ff/createSelfExportError"),
                    new CreateShipmentErrorDto(2L, 3L, null, null, 5L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
                    put("/returnRegistries/createError"),
                    new CreateReturnRegistryErrorDto("1", List.of(), null, null, 21L, false)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_UPDATE_ORDER,
                    put("/orders/processing/ff/updateError"),
                    new UpdateOrderErrorDto("LO-1", 48L, 25L, null, null, false)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_UPDATE_ORDER,
                    put("/orders/processing/ds/updateError"),
                    new UpdateOrderErrorDto("LO-1", 48L, 24L, null, null, false)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
                    put("/orders/ds/update-items/error"),
                    new UpdateOrderErrorDto("LO-1", 48L, 26L, null, null, false)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS_INSTANCES,
                    put("/orders/ds/update-items-instances/error"),
                    new UpdateOrderErrorDto("LO-1", 48L, 30L, null, null, false)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_DELIVERY_DATE,
                    put("/orders/ds/updateDeliveryDateError"),
                    new UpdateOrderDeliveryDateErrorDto(28L, "LO-1", 1L, null, null, false)
                ),
                Triple.of(
                    QueueType.PROCESS_DELIVERY_DATE_UPDATED_BY_DS,
                    put("/orders/ds/getDeliveryDatesError"),
                    new GetOrdersDeliveryDateErrorDto(29L, List.of("LO-1"), 48L, false, null, null)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_RECIPIENT,
                    put("/orders/ds/updateRecipientError"),
                    new UpdateOrderRecipientErrorDto(31L, "LO-1", 1L, false, null, null)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_ORDER_TO_ON_DEMAND,
                    put("/orders/processing/ds/updateError"),
                    new UpdateOrderErrorDto("LO-1", 48L, 32L, null, null, false)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_LAST_MILE,
                    put("/orders/processing/ds/updateError"),
                    new UpdateOrderErrorDto("LO-1", 48L, 35L, null, null, false)
                )
            )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()))
            .collect(Collectors.toList());

    private static final List<Arguments> SUCCESS_PAYLOADS =
        Stream.of(
                Triple.of(
                    QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
                    put("/shipments/ds/createIntakeSuccess"),
                    new CreateShipmentSuccessDto(2L, "2", 2L, 4L)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_CREATE_REGISTRY_EXTERNAL,
                    put("/registries/ds/createSuccess"),
                    new CreateRegistrySuccessDto("3", "3", 8L)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
                    put("/shipments/ds/createSelfExportSuccess"),
                    new CreateShipmentSuccessDto(2L, "2", 2L, 4L)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_SHIPMENT_CREATION,
                    put("/shipments/ff/createIntakeSuccess"),
                    new CreateShipmentSuccessDto(2L, "2", 2L, 5L)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_CREATE_REGISTRY_EXTERNAL,
                    put("/registries/ff/createSuccess"),
                    new CreateRegistrySuccessDto("3", "3", 9L)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_SHIPMENT_CREATION,
                    put("/shipments/ff/createSelfExportSuccess"),
                    new CreateShipmentSuccessDto(2L, "2", 2L, 5L)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
                    put("/returnRegistries/createSuccess"),
                    new CreateReturnRegistrySuccessDto("1", List.of(), 21L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
                    put("/orders/ds/cancel/success"),
                    new CancelOrderSuccessDto("LO-1", 48L, 23L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_CANCEL,
                    put("/orders/ff/cancel/success"),
                    new CancelOrderSuccessDto("LO-1", 48L, 23L)
                ),
                Triple.of(
                    QueueType.FULFILLMENT_UPDATE_ORDER,
                    put("/orders/processing/ff/updateSuccess"),
                    new UpdateOrderSuccessDto("LO-1", 48L, 25L)
                ),
                Triple.of(
                    QueueType.DELIVERY_SERVICE_UPDATE_ORDER,
                    put("/orders/processing/ds/updateSuccess"),
                    new UpdateOrderSuccessDto("LO-1", 48L, 24L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
                    put("/orders/ds/update-items/success"),
                    new UpdateOrderSuccessDto("LO-1", 48L, 26L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS_INSTANCES,
                    put("/orders/ds/update-items-instances/success"),
                    new UpdateOrderSuccessDto("LO-1", 48L, 30L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_DELIVERY_DATE,
                    put("/orders/ds/updateDeliveryDateSuccess"),
                    new UpdateOrderDeliveryDateSuccessDto(28L, "LO-1", 1L)
                ),
                Triple.of(
                    QueueType.PROCESS_DELIVERY_DATE_UPDATED_BY_DS,
                    put("/orders/ds/getDeliveryDatesSuccess"),
                    new GetOrdersDeliveryDateSuccessDto(29L, 48L, List.of())
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_RECIPIENT,
                    put("/orders/ds/updateRecipientSuccess"),
                    new UpdateOrderRecipientSuccessDto(31L, "LO-1", 1L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_ORDER_TO_ON_DEMAND,
                    put("/orders/processing/ds/updateSuccess"),
                    new UpdateOrderSuccessDto("LO-1", 48L, 32L)
                ),
                Triple.of(
                    QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_LAST_MILE,
                    put("/orders/processing/ds/updateSuccess"),
                    new UpdateOrderSuccessDto("LO-1", 48L, 35L)
                )
            )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()))
            .collect(Collectors.toList());

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BusinessProcessStateService businessProcessStateService;

    @Nonnull
    protected AsyncResultService<?, ?> getService(QueueType queueType) {
        return applicationContext.getBeansOfType(AsyncResultService.class).values()
            .stream()
            .filter(service -> service.matchBusinessProcessState(new BusinessProcessState().setQueueType(queueType)))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No service for queue type " + queueType));
    }

    @SneakyThrows
    ResultActions performRequest(MockHttpServletRequestBuilder requestBuilder, Object payload) {
        return mockMvc.perform(
            requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        );
    }

    @Nonnull
    static Stream<Arguments> successPayloadArguments() {
        return SUCCESS_PAYLOADS.stream();
    }

    @Nonnull
    static Stream<Arguments> errorPayloadArguments() {
        return ERROR_PAYLOADS.stream();
    }

    void assertBusinessProcessStateUpdated(QueueType queueType, BusinessProcessStatus newStatus) {
        assertBusinessProcessStateUpdated(queueType, newStatus, null);
    }

    void assertBusinessProcessStateUpdated(QueueType queueType, BusinessProcessStatus newStatus, String comment) {
        List<BusinessProcessState> result = businessProcessStateService.searchSlice(
            BusinessProcessStateFilter.builder().queueTypes(Set.of(queueType)).build(),
            PageRequest.of(0, 1000)
        );
        softly.assertThat(result.stream().findFirst())
            .hasValueSatisfying(state -> softly.assertThat(state.getStatus()).isEqualTo(newStatus))
            .hasValueSatisfying(state -> softly.assertThat(state.getComment()).isEqualTo(comment));
    }
}
