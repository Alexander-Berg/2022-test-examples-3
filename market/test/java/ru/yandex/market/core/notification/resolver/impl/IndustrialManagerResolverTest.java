package ru.yandex.market.core.notification.resolver.impl;

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
import ru.yandex.market.core.staff.Employee;
import ru.yandex.market.core.staff.EmployeeService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link IndustrialManagerResolver}.
 *
 * @author avetokhin 01/11/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class IndustrialManagerResolverTest {

    private static final long SHOP_ID_1 = 10L;
    private static final long MANAGER_ID_1 = 1L;
    private static final String EMAIL_1 = "email1@yandex-team.ru";

    private static final long SHOP_ID_2 = 11L;
    private static final long MANAGER_ID_2 = 2L;
    private static final String EMAIL_2 = " ";

    private static final long SHOP_ID_3 = 12L;
    private static final long MANAGER_ID_3 = 2L;

    private static final long SHOP_ID_4 = 13L;

    @Mock
    private MarketManagerService marketManagerService;

    @Mock
    private EmployeeService employeeService;

    private IndustrialManagerResolver resolver;

    private static Employee employee(final String email) {
        return new Employee.Builder().setEmail(email).setPassportEmail(email).build();
    }

    @Before
    public void init() {
        // Полноценно найденный емэйл.
        when(marketManagerService.getDatasourceIndustrialManager(SHOP_ID_1)).thenReturn(MANAGER_ID_1);
        when(employeeService.getEmployee(MANAGER_ID_1)).thenReturn(employee(EMAIL_1));

        // Индустриальный менеджер с пустым емэйлом.
        when(marketManagerService.getDatasourceIndustrialManager(SHOP_ID_2)).thenReturn(MANAGER_ID_2);
        when(employeeService.getEmployee(MANAGER_ID_2)).thenReturn(employee(EMAIL_2));

        // Только идентификатор индустриального менеджера.
        when(marketManagerService.getDatasourceIndustrialManager(SHOP_ID_3)).thenReturn(MANAGER_ID_3);

        resolver = new IndustrialManagerResolver(marketManagerService, employeeService);
    }

    @Test
    public void test() {
        assertThat(resolve(SHOP_ID_1), equalTo(Collections.singleton(EMAIL_1)));
        assertThat(resolve(SHOP_ID_2), equalTo(Collections.emptySet()));
        assertThat(resolve(SHOP_ID_3), equalTo(Collections.emptySet()));
        assertThat(resolve(SHOP_ID_4), equalTo(Collections.emptySet()));
        assertThat(resolveUidable(), equalTo(Collections.emptySet()));
    }

    private Collection<String> resolveUidable() {
        return resolver.resolveAddresses(null, new UidableNotificationContext(123L));
    }

    private Collection<String> resolve(final long shopId) {
        return resolver.resolveAddresses(null, new ShopNotificationContext(shopId));
    }
}
