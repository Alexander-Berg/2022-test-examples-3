package ru.yandex.market.b2bcrm.module.pickuppoint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import ru.yandex.market.b2bcrm.module.pickuppoint.ModulePickupPointConfiguration;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.PupEventProcessor;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl.UnprocessedEventsDao;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTestConfig;
import ru.yandex.market.crm.lb.writer.LbWriter;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.module.ou.security.ModuleOuSecurityTestConfiguration;
import ru.yandex.market.jmf.script.ScriptServiceApi;
import ru.yandex.market.jmf.trigger.TriggerActionStrategy;

import static org.mockito.Mockito.mock;

@Import({
        B2bTicketTestConfig.class,
        ModuleOuSecurityTestConfiguration.class,
        ModulePickupPointConfiguration.class,
})
@Configuration
@ComponentScan(
        basePackages = "ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl",
        useDefaultFilters = false,
        includeFilters ={
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = PupEventProcessor.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = UnprocessedEventsDao.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TriggerActionStrategy.class)
        }
)
public class B2bPickupPointTestConfig {

    @Bean
    LbWriter lbkxPupEventsWriter() {
        return mock(LbWriter.class);
    }

    @Bean
    ScriptServiceApi linkScriptServiceApi() {
        return new MockLinkScriptServiceApi();
    }

    public static class MockLinkScriptServiceApi implements ScriptServiceApi {
        public String viewCard(HasGid hasGid, String label) {
            return String.format("<a href='https://HOST/entity/%s'>%s</a>", hasGid.getGid(), label);
        }
    }
}
