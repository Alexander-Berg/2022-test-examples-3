package ru.yandex.market.tpl.carrier.core.config;

import java.time.Clock;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.service.monitoring.MonitoringService;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.mockito.Mockito.spy;

public class CarrierTestConfigurations {
    @Configuration
    @ComponentScan(basePackages = {
            "ru.yandex.market.tpl.core.test",
            "ru.yandex.market.tpl.common.db.test"
    })
    @Import({ClockConfig.class, TestUserHelper.class})
    public static class Domain {

    }

    @Configuration
    @ComponentScan(basePackages = {
            "ru.yandex.market.tpl.core.test",
            "ru.yandex.market.tpl.common.db.test"
    })
    @Import(ClockConfig.class)
    public static class Core {
    }

    @Configuration
    public static class ClockConfig {

        public static final LocalDateTime DEFAULT_DATE_TIME = LocalDateTime.of(1990, 1, 1, 0, 0, 0);

        @Bean
        public Clock clock() {
            TestableClock clock = new TestableClock();
            clock.setFixed(
                    DEFAULT_DATE_TIME.toInstant(DateTimeUtil.DEFAULT_ZONE_ID),
                    DateTimeUtil.DEFAULT_ZONE_ID
            );
            return spy(clock);
        }

    }

    @Configuration
    public static class DbQueue {
    }

    @Slf4j
    @Configuration
    public static class RegionServiceConfig {
        @Bean
        public RegionService regionService() {
            RegionService service = Mockito.mock(RegionService.class);
            RegionTree tree = Mockito.mock(RegionTree.class);
            Region region = Mockito.mock(Region.class);

            Mockito.doReturn(tree).when(service).get();
            Mockito.doReturn(region).when(tree).getRegion(Mockito.anyInt());
            Mockito.doReturn("Europe/Moscow").when(region)
                    .getCustomAttributeValue(Mockito.any());
            return service;
        }
    }
}
