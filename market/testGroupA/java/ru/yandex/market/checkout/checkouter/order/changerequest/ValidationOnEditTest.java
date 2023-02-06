package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.exception.AmbiguousParcelStateException;
import ru.yandex.market.checkout.checkouter.order.changerequest.exception.CancellationRequestCreatedException;
import ru.yandex.market.checkout.checkouter.order.changerequest.exception.ChangeRequestNotAppliedException;
import ru.yandex.market.checkout.checkouter.order.changerequest.recipient.RecipientEditRequest;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.checkout.checkouter.util.DateUtils;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.ParcelPatchRequestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.RecipientProvider;

import static java.util.Collections.singletonList;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class ValidationOnEditTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;

    private Order order;

    @BeforeEach
    public void init() {
        setFixedTime(getClock().instant());
        order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();
    }

    @Test
    public void checkFinalOrderStatus() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

            DeliveryOption chosenOption = new DeliveryOption();
            chosenOption.setFromDate(LocalDate.now());
            chosenOption.setToDate(LocalDate.now());
            OrderEditRequest orderEditRequest = new OrderEditRequest();
            orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                    .prerequest(DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                            .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                            .build())
                    .deliveryOption(chosenOption)
                    .reason(HistoryEventReason.SHIPPING_DELAYED)
                    .build());

            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }

    @Test
    public void checkCancellationRequest() {
        Assertions.assertThrows(CancellationRequestCreatedException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, "NOTES");
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, new ClientInfo(ClientRole.USER, BuyerProvider.UID));

            DeliveryOption chosenOption = new DeliveryOption();
            chosenOption.setFromDate(LocalDate.now());
            chosenOption.setToDate(LocalDate.now());
            OrderEditRequest orderEditRequest = new OrderEditRequest();
            orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                    .prerequest(DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                            .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                            .build())
                    .deliveryOption(chosenOption)
                    .reason(HistoryEventReason.SHIPPING_DELAYED)
                    .build());

            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }

    @Test
    public void checkBlockingChangeRequest() {
        Assertions.assertThrows(ChangeRequestNotAppliedException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
            recipientEditRequest.setPerson(RecipientProvider.getDefaultRecipient().getPerson());
            recipientEditRequest.setPhone(RecipientProvider.getDefaultRecipient().getPhone());

            OrderEditRequest orderEditRequest = new OrderEditRequest();
            orderEditRequest.setRecipientEditRequest(recipientEditRequest);

            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
            // второй раз не должно сработать
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }

    @Test
    public void checkParcelStatus() {
        Assertions.assertThrows(AmbiguousParcelStateException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            DeliveryOption chosenOption = new DeliveryOption();
            chosenOption.setFromDate(LocalDate.now());
            chosenOption.setToDate(LocalDate.now());
            OrderEditRequest orderEditRequest = new OrderEditRequest();
            orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                    .prerequest(DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                            .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                            .build())
                    .deliveryOption(chosenOption)
                    .deliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                    .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                    .build());

            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }

    @Test
    public void checkAllActiveChangeRequestsForDeliveryServiceProblemReason() {
        Assertions.assertThrows(ChangeRequestNotAppliedException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            // переводим статус parcel-а в ERROR
            ParcelPatchRequest parcelPatchRequest =
                    ParcelPatchRequestProvider.getStatusUpdateRequest(ParcelStatus.ERROR);
            client.updateParcel(
                    order.getId(),
                    order.getDelivery().getParcels().iterator().next().getId(),
                    parcelPatchRequest,
                    ClientRole.SYSTEM,
                    null);

            // создаём запрос на изменения данных доставки заказа
            RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
            recipientEditRequest.setPerson(RecipientProvider.getDefaultRecipient().getPerson());
            recipientEditRequest.setPhone(RecipientProvider.getDefaultRecipient().getPhone());
            OrderEditRequest orderEditRequest = new OrderEditRequest();
            orderEditRequest.setRecipientEditRequest(recipientEditRequest);
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);

            // пытаемся изменить службу доставки
            DeliveryOption chosenOption = new DeliveryOption();
            chosenOption.setFromDate(LocalDate.now());
            chosenOption.setToDate(LocalDate.now());
            orderEditRequest = new OrderEditRequest();
            orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                    .prerequest(DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                            .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                            .build())
                    .deliveryOption(chosenOption)
                    .deliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                    .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                    .build());

            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }

    @Test
    public void checkOrderStatusAfterProcessing() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

            // переводим статус parcel-а в ERROR
            ParcelPatchRequest parcelPatchRequest =
                    ParcelPatchRequestProvider.getStatusUpdateRequest(ParcelStatus.ERROR);
            client.updateParcel(
                    order.getId(),
                    order.getDelivery().getParcels().iterator().next().getId(),
                    parcelPatchRequest,
                    ClientRole.SYSTEM,
                    null);

            // пытаемся изменить службу доставки
            DeliveryOption chosenOption = new DeliveryOption();
            chosenOption.setFromDate(LocalDate.now());
            chosenOption.setToDate(LocalDate.now());
            OrderEditRequest orderEditRequest = new OrderEditRequest();
            orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                    .prerequest(DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                            .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                            .build())
                    .deliveryOption(chosenOption)
                    .deliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                    .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                    .build());

            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }

    @Test
    @DisplayName("Кидаем ошибку, если запрос на уточнение времени пришел без временного интервала")
    public void validateTimeIntervalOnDeliveryTimeIntervalClarifiedReason() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

            OrderEditRequest orderEditRequest = new OrderEditRequest();
            DeliveryDates dates = order.getDelivery().getDeliveryDates();
            orderEditRequest.setDeliveryEditRequest(
                    DeliveryEditRequest.newDeliveryEditRequest()
                            .fromDate(DateUtils.dateToLocalDate(dates.getFromDate(), TestableClock.systemDefaultZone()))
                            .toDate(DateUtils.dateToLocalDate(dates.getToDate(), TestableClock.systemDefaultZone()))
                            .reason(HistoryEventReason.DELIVERY_TIME_INTERVAL_CLARIFIED)
                            .build()
            );
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }

    @Test
    @DisplayName("Кидаем ошибку, если запрос на уточнение времени пришел с измененной датой")
    public void validateDatesOnDeliveryTimeIntervalClarifiedReason() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

            OrderEditRequest orderEditRequest = new OrderEditRequest();
            DeliveryDates dates = order.getDelivery().getDeliveryDates();
            LocalDate dateFrom = DateUtils.dateToLocalDate(dates.getFromDate(), TestableClock.systemDefaultZone());
            LocalDate dateTo = DateUtils.dateToLocalDate(dates.getToDate(), TestableClock.systemDefaultZone());
            orderEditRequest.setDeliveryEditRequest(
                    DeliveryEditRequest.newDeliveryEditRequest()
                            .fromDate(dateFrom.plusDays(1))
                            .toDate(dateTo.plusDays(1))
                            .timeInterval(new TimeInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)))
                            .reason(HistoryEventReason.DELIVERY_TIME_INTERVAL_CLARIFIED)
                            .build()
            );
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
        });
    }
}
