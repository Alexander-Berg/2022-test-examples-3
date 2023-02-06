package ru.yandex.market.crm.operatorwindow.utils;

import java.util.Optional;

import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.crm.operatorwindow.services.task.calltime.CustomerCallTimeService;
import ru.yandex.market.crm.operatorwindow.services.task.calltime.NearestCallTime;
import ru.yandex.market.jmf.timings.ServiceTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@Component
public class MockCustomerCallTimeService extends AbstractMockService<CustomerCallTimeService> {
    private final CustomerCallTimeService customerCallTimeService;

    public MockCustomerCallTimeService(CustomerCallTimeService customerCallTimeService) {
        super(customerCallTimeService);
        this.customerCallTimeService = customerCallTimeService;
    }

    public void mockGetNearestCallTime(Optional<NearestCallTime> nearestCallTime) {
        Mockito.when(customerCallTimeService.getNearestCallTime(any(Order.class), any(ServiceTime.class), anyInt()))
                .thenReturn(nearestCallTime);
    }

}
