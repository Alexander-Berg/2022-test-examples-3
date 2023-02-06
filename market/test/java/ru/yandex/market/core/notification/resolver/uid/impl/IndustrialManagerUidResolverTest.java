package ru.yandex.market.core.notification.resolver.uid.impl;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.marketmanager.MarketManagerService;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;
import ru.yandex.market.core.notification.context.impl.UidableNotificationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndustrialManagerUidResolverTest {

    private static final long SHOP_ID_1 = 10L;
    private static final long MANAGER_ID_1 = 1L;

    private static final long SHOP_ID_2 = 11L;

    @Mock
    private MarketManagerService marketManagerService;

    private IndustrialManagerUidResolver resolver;

    @Before
    public void init() {
        when(marketManagerService.getDatasourceIndustrialManager(SHOP_ID_1)).thenReturn(MANAGER_ID_1);
        when(marketManagerService.getDatasourceIndustrialManager(SHOP_ID_2)).thenReturn(null);

        resolver = new IndustrialManagerUidResolver(marketManagerService);
    }

    @Test
    public void test() {
        assertThat(resolve(SHOP_ID_1), equalTo(Collections.singleton(MANAGER_ID_1)));
        assertThat(resolve(SHOP_ID_2), equalTo(Collections.emptySet()));
        assertThat(resolveUidable(), equalTo(Collections.emptySet()));
    }

    private Collection<Long> resolveUidable() {
        return resolver.resolveUids(null, new UidableNotificationContext(123L));
    }

    private Collection<Long> resolve(final long shopId) {
        return resolver.resolveUids(null, new ShopNotificationContext(shopId));
    }

}
