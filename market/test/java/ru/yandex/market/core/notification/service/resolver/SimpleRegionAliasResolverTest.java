package ru.yandex.market.core.notification.service.resolver;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.notification.context.NotificationContext;
import ru.yandex.market.core.notification.resolver.impl.SimpleRegionAliasResolver;

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link SimpleRegionAliasResolver}.
 *
 * @author Vladislav Bauer
 */
public class SimpleRegionAliasResolverTest {

    private static final String DEFAULT_EMAIL = "default@yandex-teamr.ru";
    private static final String ALIAS = "TestAlias";


    @Test
    public void testDefaultEmail() {
        final SimpleRegionAliasResolver resolver = createResolver();
        final NotificationContext context = createContext();
        final Collection<String> emails = resolver.resolveAddresses(ALIAS, context);

        assertThat(isEqualCollection(emails, Collections.singleton(DEFAULT_EMAIL)), equalTo(true));
    }


    private NotificationContext createContext() {
        return Mockito.mock(NotificationContext.class);
    }

    private SimpleRegionAliasResolver createResolver() {
        final SimpleRegionAliasResolver resolver = new SimpleRegionAliasResolver();
        resolver.setDefaultEmail(DEFAULT_EMAIL);
        resolver.setRegionToEmail(Collections.emptyMap());
        return resolver;
    }

}
