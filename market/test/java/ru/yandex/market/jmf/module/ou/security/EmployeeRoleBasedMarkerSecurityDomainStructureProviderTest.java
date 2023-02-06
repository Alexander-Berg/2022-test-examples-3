package ru.yandex.market.jmf.module.ou.security;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.jmf.module.ou.security.impl.EmployeeRole;
import ru.yandex.market.jmf.module.ou.security.impl.EmployeeRoleBasedMarkerSecurityDomainStructureProvider;
import ru.yandex.market.jmf.module.ou.security.impl.EmployeeRoleDao;
import ru.yandex.market.jmf.security.conf.marker.ProfileConf;

import static org.mockito.Mockito.when;

// Несмотря на то, что кажется, что эти тесты почти одинаковые, они работают с разными ProfileConf, поэтому
//  вряд ли стоит выделять какие-то общие части
public class EmployeeRoleBasedMarkerSecurityDomainStructureProviderTest {
    @Mock
    EmployeeRoleDao employeeRoleDao;

    private EmployeeRole employeeRoleOne;
    private EmployeeRole employeeRoleTwo;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        employeeRoleOne = new EmployeeRole("ROLE_ONE", "ROLE_ONE_TITLE");
        employeeRoleTwo = new EmployeeRole("ROLE_TWO", "ROLE_TWO_TITLE");

        when(employeeRoleDao.list()).thenReturn(List.of(employeeRoleOne, employeeRoleTwo));
    }

    @Test
    public void getDomainTest() {
        var securityDomainProvider = new EmployeeRoleBasedMarkerSecurityDomainStructureProvider(employeeRoleDao);

        var domains = Iterables.getFirst(securityDomainProvider.getAll(), null).getDomain();

        Assertions.assertEquals(1, domains.size(), "Должен быть создан единственный domain");

        var domain = domains.get(0);

        Assertions.assertNull(domain.getRoles());
        Assertions.assertNull(domain.getMarkersGroups());

        // Домен описывается для всех существующих метаклассов
        Assertions.assertNull(domain.getMetaclass());
        Assertions.assertNull(domain.getLogic());

        List<ProfileConf> profiles = domain.getProfiles().getProfile();
        var index = Maps.uniqueIndex(profiles, ProfileConf::getId);

        assertProfileConf(employeeRoleOne.getCode(), employeeRoleOne.getTitle(), index);
        assertProfileConf(employeeRoleTwo.getCode(), employeeRoleTwo.getTitle(), index);
    }

    private void assertProfileConf(String id, String expectedTitle, Map<String, ProfileConf> index) {
        ProfileConf profile = index.get(id);
        Assertions.assertNotNull(profile);
        Assertions.assertNull(profile.getRoles());
        Assertions.assertEquals(expectedTitle, profile.getTitle().getValue().iterator().next().getValue());
    }
}
