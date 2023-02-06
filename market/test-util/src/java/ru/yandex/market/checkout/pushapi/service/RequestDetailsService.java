package ru.yandex.market.checkout.pushapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

@Component
public class RequestDetailsService {
    
    private GeoService geoService;

    @Autowired
    public void setGeoService(GeoService geoService) {
        this.geoService = geoService;
    }

    public ExternalCart createExternalCart(Cart cart) {
        final Delivery delivery = cart.getDelivery();
        
        final DeliveryWithRegion deliveryWithRegion = new DeliveryWithRegion();
        deliveryWithRegion.setAddress(delivery.getAddress());
        deliveryWithRegion.setRegion(geoService.getRegion(delivery.getRegionId()));
        
        final ExternalCart externalCart = new ExternalCart();
        externalCart.setItems(cart.getItems());
        externalCart.setCurrency(cart.getCurrency());
        externalCart.setDeliveryWithRegion(deliveryWithRegion);
        return externalCart;
    }

    public ShopOrder createShopOrder(Order order) {
        order.hidePersonalInfoFromShop();

        final Delivery delivery = order.getDelivery();
        final DeliveryWithRegion deliveryWithRegion = new DeliveryWithRegion();
        final DeliveryDates deliveryDates = delivery.getDeliveryDates();
        deliveryWithRegion.setId(delivery.getId());
        deliveryWithRegion.setType(delivery.getType());
        deliveryWithRegion.setServiceName(delivery.getServiceName());
        deliveryWithRegion.setPrice(delivery.getPrice());
        deliveryWithRegion.setDeliveryDates(new DeliveryDates(deliveryDates.getFromDate(), deliveryDates.getToDate()));
        if(delivery.getRegionId() != null) {
            deliveryWithRegion.setRegion(geoService.getRegion(delivery.getRegionId()));
        }
        deliveryWithRegion.setAddress(delivery.getAddress());
        deliveryWithRegion.setOutletId(delivery.getOutletId());

        final ShopOrder shopOrder = new ShopOrder();
        shopOrder.setId(order.getId());
        shopOrder.setCurrency(order.getCurrency());
        shopOrder.setItems(order.getItems());
        shopOrder.setPaymentType(order.getPaymentType());
        shopOrder.setPaymentMethod(order.getPaymentMethod());
        shopOrder.setFake(order.isFake());
        shopOrder.setDeliveryWithRegion(deliveryWithRegion);
        shopOrder.setBuyer(order.getBuyer());
        shopOrder.setNotes(order.getNotes());
        shopOrder.setAcceptMethod(order.getAcceptMethod());
        return shopOrder;
    }

    public ShopOrder createShopOrderStatus(Order order) {
        final ShopOrder shopOrder = createShopOrder(order);
        shopOrder.setStatus(order.getStatus());
        shopOrder.setSubstatus(order.getSubstatus());
        shopOrder.setCreationDate(order.getCreationDate());
        shopOrder.setItemsTotal(order.getItemsTotal());
        shopOrder.setTotal(order.getTotal());

        return shopOrder;
    }

    public void fixCartResponse(CartResponse cartResponse) {
        if(cartResponse.getDeliveryOptions() != null) {
            for (Delivery delivery : cartResponse.getDeliveryOptions()) {
                final DeliveryDates deliveryDates = delivery.getDeliveryDates();
                if (deliveryDates.getToDate() == null) {
                    deliveryDates.setToDate(deliveryDates.getFromDate());
                }
            }
        }
    }

    
}
