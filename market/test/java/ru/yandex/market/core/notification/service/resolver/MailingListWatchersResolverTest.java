package ru.yandex.market.core.notification.service.resolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.core.notification.dao.ShopNotificationEspionadeConfig;
import ru.yandex.market.notification.mail.model.address.EmailAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link MailingListWatchersResolver}.
 *
 * @author avetokhin 30/08/16.
 */
public class MailingListWatchersResolverTest {

    private static final List<Long> CAMPAIGNS = Arrays.asList(1L, 2L);
    private static final String SPY_1 = "test1@yandex.ru";
    private static final String SPY_2 = "test2@yandex.ru";
    private static final Set<String> SPIES = new HashSet<>(Arrays.asList(SPY_1, SPY_2));

    /**
     * Если {@link ShopNotificationEspionadeConfig} равен null.
     */
    @Test
    public void testWithNullConfig() {
        final MailingListWatchersResolver resolver = new MailingListWatchersResolver(null);

        assertThat(resolver.resolve(null), Matchers.equalTo(Collections.emptySet()));
        assertThat(resolver.resolve(CAMPAIGNS), Matchers.equalTo(Collections.emptySet()));
    }

    /**
     * Если {@link ShopNotificationEspionadeConfig} не null.
     */
    @Test
    public void testWithNonNullConfig() {
        final ShopNotificationEspionadeConfig config = mock(ShopNotificationEspionadeConfig.class);
        when(config.getCampaignsSpies(CAMPAIGNS)).thenReturn(SPIES);

        final MailingListWatchersResolver resolver = new MailingListWatchersResolver(config);

        final Set<EmailAddress> expected = new HashSet<>(Arrays.asList(
                EmailAddress.create(SPY_1, EmailAddress.Type.BCC),
                EmailAddress.create(SPY_2, EmailAddress.Type.BCC)
        ));

        assertThat(resolver.resolve(null), Matchers.equalTo(Collections.emptySet()));
        verifyZeroInteractions(config);

        assertThat(resolver.resolve(CAMPAIGNS), Matchers.equalTo(expected));
        verify(config).getCampaignsSpies(CAMPAIGNS);
        verifyNoMoreInteractions(config);
    }

}
