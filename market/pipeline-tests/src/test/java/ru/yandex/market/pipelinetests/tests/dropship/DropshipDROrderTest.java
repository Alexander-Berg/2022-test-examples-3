package ru.yandex.market.pipelinetests.tests.dropship;

import java.util.EnumSet;
import java.util.List;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import factory.OfferItems;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import step.PartnerApiSteps;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
@DisplayName("Blue Dropship DR order Test")
@Epic("Blue Dropship")
@Slf4j
public class DropshipDROrderTest extends AbstractDropshipTest {

    @Property("reportblue.dropshipDRCampaignId")
    private long dropshipDRCampaignId;
    @Property("reportblue.dropshipDRUID")
    private long dropshipDRUID;
    @Property("reportblue.dropshipDRPartnerId")
    private long dropshipDRPartnerId;
    @Property("delivery.marketCourier")
    private long marketCourier;

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        partnerApiSteps = new PartnerApiSteps(dropshipDRUID, dropshipDRCampaignId);

        log.info("Starting createDropshipDropoffOrderTest");

        params = CreateOrderParameters
            .newBuilder(regionId, OfferItems.DROPSHIP_DR.getItem(), DeliveryType.DELIVERY)
            .paymentType(PaymentType.POSTPAID)
            .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
            .experiment(EnumSet.of(RearrFactor.FORCE_DELIVERY_ID))
            .forceDeliveryId(marketCourier)
            .build();
        order = ORDER_STEPS.createOrder(params);

        partnerApiSteps.packOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.shipDropshipOrder(order);
        ORDER_STEPS.verifySDTracksCreated(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
        lomOrder = LOM_ORDER_STEPS.getLomOrderData(order);
    }

    @Test
    @Tag("DropoffOrderCreationTest")
    @DisplayName("Dropship Dropoff: Валидация рута и сегментов + отмена заказа")
    public void checkRouteAndSegmentsDropshipDROrderTest() {

        long deliveryServiceId = order.getDelivery().getDeliveryServiceId();
        Allure.step("Проверяем что заказ создался в ЛОМ");
        LOM_ORDER_STEPS.getOrderRoute(lomOrderId);

        Allure.step("Проверяем валидность рута и сегментов");
        LOM_ORDER_STEPS.verifyWaybillSegmentTypeByPartnerId(lomOrderId, dropshipDRPartnerId, SegmentType.FULFILLMENT);
        LOM_ORDER_STEPS.verifyWaybillSegmentTypeByPartnerId(lomOrderId, deliveryServiceId, SegmentType.COURIER);

        LOM_ORDER_STEPS.verifyOrderSchema(
            lomOrder,
            List.of(
                Pair.of(PartnerType.DROPSHIP, SegmentType.FULFILLMENT),
                Pair.of(PartnerType.DELIVERY, SegmentType.SORTING_CENTER),
                Pair.of(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER),
                Pair.of(PartnerType.DELIVERY, SegmentType.COURIER)
            )
        );

        Allure.step("Отменяем заказ");
        ORDER_STEPS.cancelOrder(order);
        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);

        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
    }
}
