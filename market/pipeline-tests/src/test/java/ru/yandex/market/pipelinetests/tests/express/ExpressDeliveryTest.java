package ru.yandex.market.pipelinetests.tests.express;

import java.time.Duration;
import java.util.EnumSet;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import dto.responses.lgw.LgwTaskItem;
import dto.responses.lgw.message.get_courier.GetCourierResponse;
import dto.responses.lgw.message.get_courier.PersonsItem;
import dto.responses.lgw.message.update_courier.UpdateCourierResponse;
import factory.OfferItems;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.PartnerApiSteps;
import toolkit.Pair;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

import static dto.responses.lgw.LgwTaskFlow.DS_GET_COURIER_SUCCESS;
import static dto.responses.lgw.LgwTaskFlow.FF_UPDATE_COURIER;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
@DisplayName("Blue Express delivery Test")
@Epic("Blue Express")
@Slf4j
public class ExpressDeliveryTest extends AbstractExpressDeliveryTest {

    @Property("reportblue.dropshipExpressCampaignId")
    private long dropshipExpressCampaignId;
    @Property("reportblue.dropshipExpressUID")
    private long dropshipExpressUID;

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        partnerApiSteps = new PartnerApiSteps(dropshipExpressUID, dropshipExpressCampaignId);
    }

    //Создаем заказ
    //Выбираем дату доставки и ее интервалы потом чекаутим заказ
    private void createExpressOrder() {
        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.DROPSHIP_EXPRESS.getItem(), DeliveryType.DELIVERY)
            .paymentType(PaymentType.PREPAID)
            .paymentMethod(PaymentMethod.YANDEX)
            .experiment(EnumSet.of(RearrFactor.EXPRESS))
            .build();

        Delivery delivery = ORDER_STEPS.cartWithRetry(params);
        Assertions.assertNotNull(delivery, "Пустой список опций доставки");

        RawDeliveryInterval rawDeliveryInterval = delivery.getRawDeliveryIntervals()
            .getForJson()
            .stream()
            .flatMap(raw -> raw.getIntervals().stream())
            .filter(interval -> Duration.between(interval.getFromTime(), interval.getToTime()).toHours() < 3)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Отсутствуют часовые слоты для экспресса"));

        params.setDeliveryInterval(Pair.of(rawDeliveryInterval.getFromTime(), rawDeliveryInterval.getToTime()));

        order = ORDER_STEPS.checkout(params, delivery).get(0);

        ORDER_STEPS.payOrder(order);
        partnerApiSteps.packOrder(order);

        ORDER_STEPS.verifySDTracksCreated(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        lastMileWaybillSegment = LOM_ORDER_STEPS.getWaybillSegmentForPartner(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId()
        );

    }

    @Test
    @Tag("ExpressOrderCreationTest")
    @DisplayName("DropshipExpress: Создание и успешная доставка")
    @Description("Создаем заказ. Отправляем в трекер 10-31-32-34-35-48-49 чп. " +
        "Проверяем, что после каждого чп статус сегмента меняется")
    public void createAndDeliveryDropshipExpressOrderTest() {
        createExpressOrder();

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        //ждем 1 чп и статуса INFO_RECEIVED
        DELIVERY_TRACKER_STEPS.instantRequest(order.getId());
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.INFO_RECEIVED
        );

        //10
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_AT_START
        );
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.IN

        );

        //120, вызов курьера
        WaybillSegmentDto dshWaybillSegment = LOM_ORDER_STEPS.verifyWaybillSegmentTypeByPartnerId(
            lomOrderId,
            OfferItems.DROPSHIP_EXPRESS.getItem().getWarehouseId(),
            SegmentType.FULFILLMENT
        );
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            dshWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED
        );
        //нужно перевыставить процесс т.к. изначально LOM создает его с задержкой на выполнение
        LOM_ORDER_STEPS.retryBusinessProcess(lomOrderId, "CALL_COURIER");
        LOM_ORDER_STEPS.verifySyncBusinessProcessSuccess(lomOrderId, "CALL_COURIER");

        //31
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_SEARCH
        );
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_SEARCH
        );

        //32
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_FOUND
        );
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_FOUND
        );

        //34
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_ARRIVED_TO_SENDER
        );
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_ARRIVED_TO_SENDER
        );

        //35
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_COURIER_RECEIVED
        );
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_RECEIVED
        );

        //48
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT
        );

        //49
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentHasStatusInHistory(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT
        );

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY);
    }

    private void send120StatusToDropship(Long lomOrderId) {
        WaybillSegmentDto dshWaybillSegment = LOM_ORDER_STEPS.verifyWaybillSegmentTypeByPartnerId(
            lomOrderId,
            OfferItems.DROPSHIP_EXPRESS.getItem().getWarehouseId(),
            SegmentType.FULFILLMENT
        );
        LOM_ORDER_STEPS.verifyOrderSegmentStatus(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.INFO_RECEIVED
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            dshWaybillSegment.getTrackerId(),
            OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED
        );
        LOM_ORDER_STEPS.retryBusinessProcess(lomOrderId, "CALL_COURIER");
    }

    /**
     * Тест выключен до лучших времен, когда договоримся с CARGODEV о нормальном тестинге. Пока не удаляем с целью
     * сохранения кода, чтобы потом не искать в истории.
     */
    @Test
    @Disabled("От такси не приходит поле electronicAcceptanceCertificate https://st.yandex-team.ru/CARGODEV-5520")
    @TmsLink("logistic-88")
    @DisplayName("Экспресс: Передача контактов курьера от такси")
    @Description("После 34 чп запрашиваем контакты курьера у такси (getCourier), "
        + "получаем их и передаем в дропшип (ff-update-courier)")
    public void courierContactExpressTest() {
        createExpressOrder();
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        send120StatusToDropship(lomOrderId);
        LOM_ORDER_STEPS.verifySegmentStatusCount(
            lomOrderId,
            order.getDelivery().getDeliveryServiceId(),
            SegmentStatus.TRANSIT_COURIER_FOUND,
            1
        );

        //Получаем данные о курьере в get-courier-success (что получили от СД)
        LgwTaskItem getCourierTask = LGW_STEPS.getReadyTaskFromListWithEntityIdAndRequestFlow(
            String.valueOf(order.getId()),
            DS_GET_COURIER_SUCCESS
        );
        GetCourierResponse getCourierResponse = LGW_STEPS.getTask(getCourierTask.getId(), DS_GET_COURIER_SUCCESS);

        Assertions.assertFalse(
            getCourierResponse.getCourier().getPersons().isEmpty(),
            "Пустой список persons в с таске DS_GET_COURIER_SUCCESS"
        );
        PersonsItem courierFromDS = getCourierResponse.getCourier().getPersons().get(0);
        String carFromDS = getCourierResponse.getCourier().getCar().getNumber();
        String codeFromDS = getCourierResponse.getElectronicAcceptanceCertificate().getCode();

        //Получаем данные о курьере в ff-update-courier (что передаем на склад)
        LgwTaskItem ffUpdateCourierTask =
            LGW_STEPS.getReadyTaskFromListWithEntityIdAndRequestFlow(String.valueOf(order.getId()), FF_UPDATE_COURIER);
        UpdateCourierResponse updateCourierResponse = LGW_STEPS.getTask(ffUpdateCourierTask.getId(), FF_UPDATE_COURIER);

        Assertions.assertFalse(
            updateCourierResponse.getOutboundCourier().getPersons().isEmpty(),
            "Пустой список persons в с таске FF_UPDATE_COURIER"
        );
        dto.responses.lgw.message.update_courier.PersonsItem courierToFf =
            updateCourierResponse.getOutboundCourier().getPersons().get(0);
        String carForFf = updateCourierResponse.getOutboundCourier().getCar().getNumber();
        String codeForFf = updateCourierResponse.getCodes().toString();

        Assertions.assertEquals(courierFromDS.getName(), courierToFf.getName(), "Имя курьера не совпадает");
        Assertions.assertEquals(
            courierFromDS.getPatronymic(),
            courierToFf.getPatronymic(),
            "Отчество курьера не совпадает"
        );
        Assertions.assertEquals(courierFromDS.getSurname(), courierToFf.getSurname(), "Фамилия курьера не совпадает");
        Assertions.assertEquals(carFromDS, carForFf, "Номер машины не совпадает");
        Assertions.assertEquals(codeFromDS, codeForFf, "Код курьера не совпадает");

    }
}
