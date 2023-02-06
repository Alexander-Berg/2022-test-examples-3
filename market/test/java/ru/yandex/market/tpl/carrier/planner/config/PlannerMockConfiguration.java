package ru.yandex.market.tpl.carrier.planner.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.tpl.carrier.core.domain.run.RunMessageRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.carrier.core.service.region.CarrierRegionService;
import ru.yandex.market.tpl.carrier.planner.service.manual.ManualTimezonesTool;

@MockBean(classes = {
        LMSClient.class
})
@Configuration
public class PlannerMockConfiguration {

    @Bean(name = "timezonesToolMock")
    public ManualTimezonesTool timezonesToolMock(
            @Autowired OrderWarehouseRepository orderWarehouseRepository,
            @Autowired RoutePointRepository routePointRepository,
            @Autowired RunMessageRepository runMessageRepository,
            @Autowired CarrierRegionService regionService
    ) {
        return new ManualTimezonesTool(
                orderWarehouseRepository,
                routePointRepository,
                runMessageRepository,
                regionService
        );
    }

}
