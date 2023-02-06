package ru.yandex.market.pipelinetests.tests.dropship;

import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.PartnerApiSteps;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
@DisplayName("Blue Dropship SC order Test")
@Epic("Blue Dropship")
@Slf4j
public class DropshipSCOrderTest extends AbstractDropshipTest {

    @Property("reportblue.dropshipSCCampaignId")
    private long dropshipSCCampaignId;

    @Property("reportblue.dropshipSCUID")
    private long dropshipSCUID;

    @BeforeEach
    public void setUp() {
        partnerApiSteps = new PartnerApiSteps(dropshipSCUID, dropshipSCCampaignId);
    }

    private void createOrder(DeliveryType deliveryType) {
        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.DROPSHIP_SC.getItem(), deliveryType)
            .build();
        order = ORDER_STEPS.createOrder(params);
    }

    @Tag("DropshipOrderCreationTest")
    @ParameterizedTest(name = "Dropship через СЦ: Создание {0} заказа")
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP"})
    @TmsLinks({@TmsLink(value = "logistic-1"), @TmsLink(value = "logistic-2")})
    // DELIVERY – https://testpalm.yandex-team.ru/testcase/logistic-1
    // PICKUP – https://testpalm.yandex-team.ru/testcase/logistic-2
    public void createDropshipSCOrderTest(DeliveryType deliveryType) {
        log.info("Starting createDropshipSCOrderTest");

        createOrder(deliveryType);

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        partnerApiSteps.packOrder(order);

        ORDER_STEPS.shipDropshipOrder(order);
        ORDER_STEPS.verifySDTracksCreated(order);

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        // Проставляем 110 чекпоинт СЦ. Когда событие дойдет до MDB,
        // то MDB должен будет перевести статус заказа в чекаутере в SHIPPED.
        LOM_ORDER_STEPS.getWaybillSegments(lomOrderId).stream()
            .filter(s -> s.getSegmentType() == SegmentType.SORTING_CENTER)
            .map(WaybillSegmentDto::getTrackerId)
            .forEach(trackerId -> DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
                trackerId,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START
            ));

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);

        // Проверяем, что заказ в чекаутере статусе PROCESSING.SHIPPED.
        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING);
        ORDER_STEPS.verifyForOrderSubStatus(order, OrderSubstatus.SHIPPED);

        // Т.к. в ПИ или по API еще будет оставаться возможность перевести заказ в статус SHIPPED,
        // то хотим убедиться, что чекаутер не упадет на повторном переводе из MDB, из ПИ, по API.
        // Внутри этот метод проверяет, что http-запрос в чекаутер завершился успешно.
        ORDER_STEPS.changeOrderStatusAndSubStatus(
            order.getId(),
            order.getShopId(),
            ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING,
            OrderSubstatus.SHIPPED
        );

        // Имитация запроса по API.
        ORDER_STEPS.changeOrderStatusAndSubStatusByShop(
            order,
            ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING,
            OrderSubstatus.SHIPPED
        );

        // Имитация запроса из ПИ.
        ORDER_STEPS.changeOrderStatusAndSubStatusByShopUser(
            order,
            ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING,
            OrderSubstatus.SHIPPED
        );

        // Отмена заказа.
        ORDER_STEPS.cancelOrder(order);
        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
    }
}
