package ru.yandex.market.notification.service.provider.complex;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.NotificationData;
import ru.yandex.market.notification.service.NotificationFacade;
import ru.yandex.market.notification.service.provider.NotificationDataProvider;
import ru.yandex.market.notification.service.registry.NotificationDataProviderRegistry;
import ru.yandex.market.notification.simple.service.provider.complex.ComplexNotificationDataProvider;
import ru.yandex.market.notification.simple.service.registry.NotificationDataProviderRegistryImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ComplexNotificationDataProvider}.
 *
 * @author Vladislav Bauer
 */
public class ComplexNotificationDataProviderTest {

    @Test
    public void testProvideWithoutProviders() {
        checkIt(0);
    }

    @Test
    public void testProvideSingleProvider() {
        checkIt(1);
    }

    @Test
    public void testProvideFewProvider() {
        checkIt(2);
    }


    private void checkIt(int count) {
        List<NotificationData> extData = IntStream.range(0, count)
            .mapToObj(i -> mock(NotificationData.class))
            .collect(Collectors.toList());

        List<NotificationDataProvider> providers = IntStream.range(0, count)
            .mapToObj(i -> (NotificationDataProvider) context -> extData.get(i))
            .collect(Collectors.toList());

        Collection<NotificationData> data = provideData(providers);

        assertThat(data, hasSize(count));

        int i = 0;
        for (NotificationData item : data) {
            assertThat(item, equalTo(extData.get(i)));
            i++;
        }
    }

    private Collection<NotificationData> provideData(Collection<NotificationDataProvider> providers) {
        NotificationDataProviderRegistry registry = new NotificationDataProviderRegistryImpl(providers);
        NotificationFacade facade = mock(NotificationFacade.class);

        when(facade.getExtraDataProviderRegistry()).thenReturn(registry);

        ComplexNotificationDataProvider provider = new ComplexNotificationDataProvider(facade);
        NotificationContext context = mock(NotificationContext.class);

        return provider.provide(context);
    }

}
