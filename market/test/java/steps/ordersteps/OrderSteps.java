package steps.ordersteps;

import java.math.BigDecimal;

import steps.ordersteps.ordersubsteps.ItemSteps;
import steps.ordersteps.ordersubsteps.LocationSteps;
import steps.ordersteps.ordersubsteps.RecipientSteps;
import steps.ordersteps.ordersubsteps.ResourceIdSteps;
import steps.ordersteps.ordersubsteps.SenderSteps;
import steps.ordersteps.ordersubsteps.ServiceSteps;
import steps.ordersteps.ordersubsteps.WarehouseSteps;

import ru.yandex.market.delivery.entities.common.DateTime;
import ru.yandex.market.delivery.entities.common.Order;
import ru.yandex.market.delivery.entities.common.TimeInterval;
import ru.yandex.market.delivery.entities.common.constant.CargoType;
import ru.yandex.market.delivery.entities.common.constant.DeliveryType;
import ru.yandex.market.delivery.entities.common.constant.PaymentMethod;

public class OrderSteps {

    private OrderSteps() {
        throw new UnsupportedOperationException();
    }

    public static Order getOrder() {
        Order order = new Order();

        order.setLocationFrom(LocationSteps.getLocation());
        order.setLocationTo(LocationSteps.getLocation());
        order.setOrderId(ResourceIdSteps.getResourceId());
        order.setWeight(BigDecimal.ONE);
        order.setLength(10);
        order.setHeight(10);
        order.setWidth(10);
        order.setCargoType(CargoType.DOCUMENTS_AND_SECURITIES);
        order.setAssessedCost(BigDecimal.TEN);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setTariff("tariff");
        order.setDeliveryType(DeliveryType.PICKUP_POINT);
        order.setDeliveryCost(BigDecimal.valueOf(100));
        order.setDeliveryDate(new DateTime("2018-05-07T00:00:00+03:00"));
        order.setDeliveryInterval(new TimeInterval("14:30/15:30"));
        order.setTotal(BigDecimal.valueOf(200));
        order.setPickupPointCode("630090");
        order.setItems(ItemSteps.getItemList());
        order.setRecipient(RecipientSteps.getRecipient());
        order.setSender(SenderSteps.getSender());
        order.setWarehouse(WarehouseSteps.getWarehouse());
        order.setServices(ServiceSteps.getServiceList());
        order.setComment("comment");
        order.setShipmentDate(new DateTime("2017-03-21T00:00:00+03:00"));
        order.setShipmentPointCode("630090");

        return order;
    }
}
