package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropship;

import java.time.LocalDateTime;
import java.util.EnumSet;

import dto.requests.checkouter.CreateOrderParameters;
import factory.OfferItems;
import step.L4SSteps;
import step.PartnerApiSteps;
import step.TMSteps;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/report.properties"})
@DisplayName("Dropship Update Shipment Date Test")
@Epic("Blue Dropship")
@Slf4j
@Tag("SlowTest")
public class DropshipUpdateShipmentDateTest extends AbstractDropshipTest {
    private static final L4SSteps L4S_STEPS = new L4SSteps();
    private static final TMSteps TM_STEPS = new TMSteps();

    @Property("reportblue.dropshipDSCourierCampaignId")
    private long dropshipDSCourierCampaignId;

    @Property("reportblue.dropshipDSCourierUID")
    private long dropshipDSCourierUID;

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
    @DisplayName("Перенос даты отгрузки дропшипом")
    void dropshipUpdateShipmentDate() {
        OrderDto lomOrder = LOM_ORDER_STEPS.getLomOrderData(order);
        lomOrderId = lomOrder.getId();
        TransportationSearchDto transportationSearchDto = TM_STEPS.searchTransportation(order.getId());
        LocalDateTime orderShipmentDateTimeBySupplierBefore = ORDER_STEPS.getOrderShipmentDateTimeBySupplier(order);

        L4S_STEPS.excludeOrderFromShipment(order.getId(), transportationSearchDto.getId());

        LOM_ORDER_STEPS.verifyChangeRequest(
            lomOrderId,
            ChangeOrderRequestType.RECALCULATE_ROUTE_DATES,
            EnumSet.of(ChangeOrderRequestStatus.SUCCESS)
        );
        LOM_ORDER_STEPS.verifyRouteChanged(lomOrderId, lomOrder.getRouteUuid());
        TM_STEPS.verifyTransportationChanged(order.getId(), transportationSearchDto.getId());
        ORDER_STEPS.verifyShipmentDateTimeBySupplierChanged(order, orderShipmentDateTimeBySupplierBefore);
    }
}
