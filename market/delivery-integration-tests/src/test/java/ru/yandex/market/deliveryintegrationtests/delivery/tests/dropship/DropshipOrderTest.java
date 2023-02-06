package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropship;

import dto.requests.checkouter.CreateOrderParameters;
import dto.responses.lgw.LgwTaskFlow;
import dto.responses.lgw.message.create_order.FFCreateOrderSuccess;
import factory.OfferItems;
import io.qameta.allure.TmsLink;
import step.PartnerApiSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
@DisplayName("Blue Dropship order Test")
@Epic("Blue Dropship")
@Slf4j
public class DropshipOrderTest extends AbstractDropshipTest {

    @Property("reportblue.dropshipDSCourierCampaignId")
    private long dropshipDSCourierCampaignId;

    @Property("reportblue.dropshipDSCourierUID")
    private long dropshipDSCourierUID;

    private static final long DROPSHIP_PARTNER_ID = 48423L;

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        partnerApiSteps = new PartnerApiSteps(dropshipDSCourierUID, dropshipDSCourierCampaignId);

        params = CreateOrderParameters
                .newBuilder(regionId, OfferItems.DROPSHIP_SD_COURIER.getItem(), DeliveryType.DELIVERY)
                .build();
        order = ORDER_STEPS.createOrder(params);
    }

    @Test
    @TmsLink("logistic-78")
    @DisplayName("Dropship: Создание заказа типа Курьер")
    public void createDropshipOrderTest() {
        log.info("Starting createDropshipOrderTest");

        partnerApiSteps.packOrder(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        ORDER_STEPS.shipDropshipOrder(order);
        ORDER_STEPS.verifySDTracksCreated(order);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.PROCESSING);
    }

    @Test
    @TmsLink("logistic-79")
    @DisplayName("Dropship: Создание заказа с использование идентификатора заказа из системы партнера")
    public void createDropshipWithPartnerExternalOrderId() {
        Long checkouterOrderId = order.getId();

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        Long successCreateFFOrderTaskId = LGW_STEPS.getReadyTaskFromListWithEntityIdAndRequestFlow(
                String.valueOf(checkouterOrderId),
                LgwTaskFlow.FF_CREATE_ORDER_SUCCESS
            )
            .getId();
        FFCreateOrderSuccess createFFOrderResponse = LGW_STEPS.getTask(successCreateFFOrderTaskId)
            .getItem()
            .getValues()
            .getMessage()
            .getText(LgwTaskFlow.FF_CREATE_ORDER_SUCCESS);
        Long partnerExternalOrderId = Long.parseLong(createFFOrderResponse.getTrackId());

        Long externalIdFromDropshipSegment = Long.parseLong(
            LOM_ORDER_STEPS.getWaybillSegmentForPartner(lomOrderId, DROPSHIP_PARTNER_ID).getExternalId()
        );

        Assertions.assertNotEquals(
            checkouterOrderId,
            externalIdFromDropshipSegment,
            "Чекаутерный идентификатор заказа эквивалентен полученному от партнера"
        );
        Assertions.assertEquals(
            externalIdFromDropshipSegment,
            partnerExternalOrderId,
            String.format(
                "ExternalId на сегменте дропшипа (%s) не эквивалентен exteranlId то партнера (%s)",
                externalIdFromDropshipSegment,
                partnerExternalOrderId
            )
        );
    }

    @Test
    @TmsLink("logistic-80")
    @Tag("SmokeTest")
    @DisplayName("Dropship: Создание и отмена курьерского заказа в службе")
    public void cancelDropshipOrderTest() {
        log.info("Starting cancelDropshipOrderTest");

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        partnerApiSteps.packOrder(order);

        ORDER_STEPS.shipDropshipOrder(order);
        ORDER_STEPS.verifySDTracksCreated(order);

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
    }

    @Test
    @TmsLink("logistic-81")
    @DisplayName("Dropship: Отмена заказа до создания в службе")
    public void cancelBeforeCreatedInDsDropshipOrderTest() {
        log.info("Starting cancelBeforeCreatedInDsDropshipOrderTest");

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);

        partnerApiSteps.packOrder(order);

        ORDER_STEPS.shipDropshipOrder(order);

        ORDER_STEPS.cancelOrder(order);

        ORDER_STEPS.verifyForOrderStatus(order, ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED);
        LOM_ORDER_STEPS.verifyOrderStatus(lomOrderId, OrderStatus.CANCELLED);
    }
}
