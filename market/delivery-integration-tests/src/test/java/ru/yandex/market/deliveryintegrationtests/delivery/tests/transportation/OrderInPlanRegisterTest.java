package ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.RearrFactor;
import dto.requests.report.OfferItem;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("TM Test")
@Epic("TM")
@Slf4j
public class OrderInPlanRegisterTest extends AbstractTransportationTest {

@Property("delivery.marketCourier")
private long marketCourier;

    private final List<Long> outletId = Collections.singletonList(10000887541L);
    private Long transportationId;

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        OfferItem item = OfferItems.FF_172_UNFAIR_STOCK.getItem();
        params = CreateOrderParameters
                .newBuilder(regionId, item, DeliveryType.DELIVERY)
                .build();
        order = ORDER_STEPS.createOrder(params);
        Long lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LocalDate shipmentDate = ORDER_STEPS.getOrderShipmentDate(order);
        Long partnerId = LOM_ORDER_STEPS.getWaybillSegmentsWithoutTrackerId(lomOrderId).get(1).getPartnerId();
        transportationId = TM_STEPS.getTransportationIdForDay(
                item.getWarehouseId(),
                partnerId,
                shipmentDate,
                null
        );
        if (!TM_STEPS.getTransportationStatus(transportationId).equals("SCHEDULED")) {
            transportationId = TM_STEPS.getTransportationIdForDay(
                item.getWarehouseId(),
                partnerId,
                shipmentDate.plusDays(1),
                null
            );
        }
    }

    @Test
    @DisplayName("ТМ: Проверка наличия заказа в плановом реестре")
    void orderInPlanRegisterTest() {
        log.info("Starting Plan register test...");
        long registerId = TM_STEPS.getTransportationRegister(transportationId, 0);
        TM_STEPS.checkOrderInRegister(registerId, order.getId());
    }

    @AfterEach
    public void tearDown() {
        ORDER_STEPS.cancelOrderIfAllowed(order);
    }
}
