package ru.yandex.market.ocrm.module.loyalty.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.jmf.entity.query.AttributeContainsAnyFilter;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.module.ou.security.Employee;
import ru.yandex.market.jmf.module.ou.security.EmployeeRole;
import ru.yandex.market.jmf.security.SecurityDataService;
import ru.yandex.market.jmf.security.action.SecurityService;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyPromo;
import ru.yandex.market.ocrm.module.loyalty.Permissions;
import ru.yandex.market.ocrm.module.loyalty.impl.LoyaltyPromoQueryInterceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class LoyaltyPromoQueryInterceptorTest {
    @Mock
    private SecurityService securityService;

    @Mock
    private SecurityDataService securityDataService;

    @Mock
    private Metaclass metaclass;

    @Mock
    private Query query;

    @Mock
    private Employee employee;

    @Mock
    private EmployeeRole employeeRoleOne;

    @Mock
    private EmployeeRole employeeRoleTwo;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(employeeRoleOne.getCode()).thenReturn("RoleOne");
        when(employeeRoleTwo.getCode()).thenReturn("RoleTwo");

        when(employee.getRoles()).thenReturn(Set.of(employeeRoleOne, employeeRoleTwo));

        when(securityDataService.getCurrentNonRobotEmployee())
                .thenReturn(employee);
    }

    @Test
    public void getFiltersTest() {
        when(securityService.hasPermission(any(Fqn.class), any()))
                .thenReturn(false);

        var queryInterceptor = new LoyaltyPromoQueryInterceptor(securityService, securityDataService);
        Collection<Filter> filter = queryInterceptor.getFilters(metaclass, new ArrayList<>(query.getFilters()));

        Assertions.assertEquals(1, filter.size());
        var firstFilter = filter.toArray()[0];

        Assertions.assertTrue(firstFilter instanceof AttributeContainsAnyFilter);
        var firstFilterEq = (AttributeContainsAnyFilter) firstFilter;

        Assertions.assertEquals(LoyaltyPromo.AVAILABLE_FOR_ROLES, firstFilterEq.getAttribute());

        Assertions.assertEquals(2, firstFilterEq.getValues().size());

        Assertions.assertTrue(List.of(employeeRoleOne, employeeRoleTwo)
                .containsAll(firstFilterEq.getValues()));
    }

    @Test
    public void getFiltersHasFullAccessTest() {
        when(securityService.hasPermission(any(Fqn.class), eq(Permissions.LOYALTY_FULL_ACCESS)))
                .thenReturn(true);

        var queryInterceptor = new LoyaltyPromoQueryInterceptor(securityService, securityDataService);
        var filter = queryInterceptor.getFilters(metaclass, new ArrayList<>(query.getFilters()));

        Assertions.assertEquals(0, filter.size());
    }
}
