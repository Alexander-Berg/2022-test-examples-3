package ru.yandex.travel.orders.workflows.orderitem.bus;

import lombok.RequiredArgsConstructor;

import ru.yandex.travel.bus.service.BusesService;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.services.buses.BusesServiceProvider;

@RequiredArgsConstructor
public class SingletonBusesServiceProvider implements BusesServiceProvider {
    private final BusesService busesService;
    @Override
    public BusesService getBusesServiceByOrderItem(OrderItem orderItem) {
        return busesService;
    }
}
