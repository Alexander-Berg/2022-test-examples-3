package ru.yandex.travel.orders.factories;

import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.travel.orders.repository.mock.MockSuburbanOrderItemRepository;
import ru.yandex.travel.orders.services.DeduplicationService;
import ru.yandex.travel.orders.services.suburban.environment.SuburbanOrderItemEnvProvider;
import ru.yandex.travel.orders.workflows.orderitem.suburban.SuburbanProperties;
import ru.yandex.travel.suburban.partners.aeroexpress.AeroexpressClient;
import ru.yandex.travel.suburban.partners.movista.MovistaClient;
import ru.yandex.travel.train.partners.im.ImClient;

import static org.mockito.Mockito.mock;


public class SuburbanOrderItemEnvProviderFactory {
    public static SuburbanOrderItemEnvProvider createEnvProvider() {
        return createEnvProvider(mock(SuburbanProperties.class));
    }

    public static SuburbanOrderItemEnvProvider createEnvProvider(SuburbanProperties props) {
        return new SuburbanOrderItemEnvProvider(
                mock(Environment.class),
                mock(MovistaClient.class),
                mock(ImClient.class),
                mock(AeroexpressClient.class),
                mock(MockSuburbanOrderItemRepository.class),
                mock(PlatformTransactionManager.class),
                mock(DeduplicationService.class),
                props
        );
    }
}
