package ru.yandex.market.jmf.module.ticket.test;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.catalog.items.CatalogItemsEntityInitializationProviderFactory;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entities.initialization.EntityInitializationProvider;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.comment.test.ModuleCommentTestConfiguration;
import ru.yandex.market.jmf.module.notification.ModuleNotificationTestConfiguration;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.ModuleTicketConfiguration;
import ru.yandex.market.jmf.module.ticket.OmniChannelSettingsService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ModuleTicketConfiguration.class,
        ModuleNotificationTestConfiguration.class,
        ModuleCommentTestConfiguration.class,
})
@ComponentScan({
        "ru.yandex.market.jmf.module.ticket.test.impl"
})
@PropertySource(name = "testTicketProperties", value = "classpath:jmf/module/ticket/test/module_ticket_test.properties")
public class ModuleTicketTestConfiguration extends AbstractModuleConfiguration {

    protected ModuleTicketTestConfiguration() {
        super("jmf/module/ticket/test");
    }

    @Bean
    @Primary
    public OmniChannelSettingsService mockOmniChannelSettingsService(DbService dbService) {
        return Mockito.mock(OmniChannelSettingsService.class, inv -> {
            if ("getPossibleChannels".equals(inv.getMethod().getName())
                    && inv.getArgument(0) instanceof EmployeeDistributionStatus employeeDistributionStatus) {
                if (employeeDistributionStatus.getAllTickets().isEmpty()) {
                    return dbService.<Channel>list(Query.of(Channel.FQN)
                            .withFilters(Filters.eq(Channel.ARCHIVED, false)));
                }
                return List.of();
            }

            return null;
        });
    }

    @Bean
    public EntityInitializationProvider moduleTicketTestChannelCatalogItemProvider(CatalogItemsEntityInitializationProviderFactory factory) {
        return factory.jsonCatalogItem(Channel.FQN, "classpath:test_channels.json");
    }

}
