package ru.yandex.market.deliveryintegrationtests.delivery.tests.taxiintegration.pvzondemand;

import java.util.EnumSet;
import java.util.List;

import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.OrderComment;
import dto.requests.checkouter.RearrFactor;
import dto.responses.lgw.LgwTaskItem;
import dto.responses.lgw.message.create_order.CreateOrderResponse;
import dto.responses.lgw.message.transfer_code.TransferCodeResponse;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;

import static dto.responses.lgw.LgwTaskFlow.DS_CREATE_ORDER;
import static dto.responses.lgw.LgwTaskFlow.DS_CREATE_ORDER_SUCCESS;
import static dto.responses.lgw.LgwTaskFlow.DS_UPDATE_TRANSFER_CODES;
import static dto.responses.lgw.LgwTaskFlow.DS_UPDATE_TRANSFER_CODES_SUCCESS;
import static ru.yandex.market.logistics.lom.model.enums.SegmentStatus.TRANSIT_PICKUP;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Ondemand pvz order test")
@Epic("Ondemand pvz order test")
@Slf4j
public class PvzCodeForCourierTest extends AbstractPvzOnDemandTest {

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.FF_172_UNFAIR_STOCK.getItem(), DeliveryType.DELIVERY)
                .paymentType(PaymentType.PREPAID)
                .paymentMethod(PaymentMethod.YANDEX)
                .address(Address.PVZ_ON_DEMAND)
                .experiment(EnumSet.of(RearrFactor.LAVKA, RearrFactor.COMBINATORONDEMAND))
                .forceDeliveryId(ondemandDS)
                .comment(OrderComment.FIND_COURIER_FASTER)
                .build();
        order = ORDER_STEPS.createOrder(params);
        ORDER_STEPS.payOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        ORDER_STEPS.verifySDTracksCreated(order);
    }

    @Test
    @DisplayName("Проверяем, что коды ушли в службы при создании заказа. Обновление кодов")
    public void createAndChangeCodeTest() {
        log.debug("Start createAndChangeCodeTest");

        //Собираем таски создания заказа
        List<LgwTaskItem> tasksOrderCreate = LGW_STEPS.getReadyTasksFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            DS_CREATE_ORDER,
            2
        );

        //Достаем тела запросов тасок создания заказа
        Pair<CreateOrderResponse, CreateOrderResponse> messagesCreateOrder =
                LGW_STEPS.getMessagesByOrderIdAndRequestFlow(tasksOrderCreate, DS_CREATE_ORDER);
        CreateOrderResponse messageCreateOrderToLastMile = messagesCreateOrder.getLeft();
        CreateOrderResponse messageCreateOrderToMiddleMile = messagesCreateOrder.getRight();


        //Проверем, что таски создания в правильные сд
        Assertions.assertTrue(
                order.getDelivery().getDeliveryServiceId().equals(messageCreateOrderToLastMile.getPartner().getId())
                        || order.getDelivery().getDeliveryServiceId().equals(messageCreateOrderToMiddleMile.getPartner().getId()),
                "Таска создания заказа ЛГВ не в службу последней мили");
        Assertions.assertTrue(pickupPartner.equals(messageCreateOrderToLastMile.getPartner().getId())
                        || pickupPartner.equals(messageCreateOrderToMiddleMile.getPartner().getId()),
                "Таска создания заказа ЛГВ не в службу средней мили");

        //Тут проверяем, что таски с кодами выставились успешно
        List<LgwTaskItem> listCreateOrderSuccess = LGW_STEPS.getReadyTasksFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            DS_CREATE_ORDER_SUCCESS,
            2
        );
        Integer parentIdCreateOrderLastMile = listCreateOrderSuccess.get(0).getValues().getParentId();
        Integer parentIdCreateOrderMiddleMile = listCreateOrderSuccess.get(1).getValues().getParentId();

        Integer createOrderTaskToLastMileId = tasksOrderCreate.get(0).getValues().getTaskId();
        Integer createOrderTaskToMiddleMileId = tasksOrderCreate.get(1).getValues().getTaskId();

        Assertions.assertEquals(createOrderTaskToLastMileId, parentIdCreateOrderLastMile,
                "Success-task выставился не к таске создание заказа в последней миле");
        Assertions.assertEquals(createOrderTaskToMiddleMileId, parentIdCreateOrderMiddleMile,
                "Success-task выставился не к таске создание заказа в средней миле");

        //Достаем коды после создания заказа
        Integer codeCreatedToLastMile = messageCreateOrderToLastMile.getRestrictedData().getTransferCodes()
                .getInbound().getVerification();
        Integer codeCreatedToMiddleMile = messageCreateOrderToMiddleMile.getRestrictedData().getTransferCodes()
                .getOutbound().getVerification();

        //Проверяем коды
        Assertions.assertNotNull(codeCreatedToLastMile, "Не передаем код курьера в последнюю милю (такси)");
        Assertions.assertNotNull(codeCreatedToMiddleMile, "Не передаем код курьера в среднюю милю (ПВЗ)");
        Assertions.assertEquals(codeCreatedToLastMile, codeCreatedToMiddleMile,
                "Не совпадают коды в последней и средней миле после создания");


        //Шлем 45, 92 чп по треку ПВЗ
        WaybillSegmentDto middleSDWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, pickupPartner);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT);
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(middleSDWaybillSegment.getTrackerId(), OrderDeliveryCheckpointStatus.DELIVERY_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED);
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(lomOrderId, order.getDelivery().getDeliveryServiceId(), TRANSIT_PICKUP);

        //Собираем таски на обновление кода
        List<LgwTaskItem> tasksUpdateCodes = LGW_STEPS.getReadyTasksFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            DS_UPDATE_TRANSFER_CODES,
            2
        );

        //Достаем тела запросов тасок на обновление кода
        Pair<TransferCodeResponse, TransferCodeResponse> messagesUpdateCode =
                LGW_STEPS.getMessagesByOrderIdAndRequestFlow(tasksUpdateCodes, DS_UPDATE_TRANSFER_CODES);

        TransferCodeResponse messageUpdateCodeToLastMile, messageUpdateCodeToMiddleMile;
        if (messagesUpdateCode.getLeft().getCodes().getInbound() == null) {
            messageUpdateCodeToMiddleMile = messagesUpdateCode.getLeft();
            messageUpdateCodeToLastMile = messagesUpdateCode.getRight();
        } else {
            messageUpdateCodeToMiddleMile = messagesUpdateCode.getRight();
            messageUpdateCodeToLastMile = messagesUpdateCode.getLeft();
        }

        //Проверяем, что коды обновились в правильных сд

        Assertions.assertEquals(order.getDelivery().getDeliveryServiceId(), messageUpdateCodeToLastMile.getPartner().getId(),
                "Таска обновления кодов ЛГВ не в службу последней мили");
        Assertions.assertEquals(pickupPartner, messageUpdateCodeToMiddleMile.getPartner().getId(),
                "Таска обновления кодов ЛГВ не в службу средней мили");


        List<LgwTaskItem> tasksUpdateCodeSuccess =
            LGW_STEPS.getReadyTasksFromListWithEntityIdAndRequestFlow(
                String.valueOf(order.getId()),
                DS_UPDATE_TRANSFER_CODES_SUCCESS,
                2
            );
        Integer parentIdUpdateCodeLastMile = tasksUpdateCodeSuccess.get(0).getValues().getParentId();
        Integer parentIdUpdateCodeMiddleMile = tasksUpdateCodeSuccess.get(1).getValues().getParentId();

        Integer idUpdateCodeTaskLastMile = tasksUpdateCodes.get(0).getValues().getTaskId();
        Integer idUpdateCodeTaskMiddleMile = tasksUpdateCodes.get(1).getValues().getTaskId();

        Assertions.assertEquals(idUpdateCodeTaskLastMile, parentIdUpdateCodeLastMile,
                "Success-task выставился не к таске обновления кода в последней миле");
        Assertions.assertEquals(idUpdateCodeTaskMiddleMile, parentIdUpdateCodeMiddleMile,
                "Success-task выставился не к таске обновления кода в средней миле");

        //Получаем коды после обновления
        Integer codeUpdatedToLastMile = messageUpdateCodeToLastMile.getCodes().getInbound().getVerification();
        Integer codeUpdatedToMiddleMile = messageUpdateCodeToMiddleMile.getCodes().getOutbound().getVerification();

        //Проверяем коды
        Assertions.assertNotEquals(codeCreatedToLastMile, codeUpdatedToLastMile,
                "Код не обновился в последней миле (такси)");
        Assertions.assertNotEquals(codeCreatedToMiddleMile, codeUpdatedToMiddleMile,
                "Код не обновился в средней миле (ПВЗ)");
        Assertions.assertEquals(codeUpdatedToLastMile, codeUpdatedToMiddleMile,
                "Не совпадают коды в последней и средней миле после обновления");
    }
}

