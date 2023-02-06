package ru.yandex.market.mbo.reactui.controller;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbo.reactui.dto.roles.RoleFilter;
import ru.yandex.market.mbo.reactui.dto.roles.UserCategoryRoles;
import ru.yandex.market.mbo.security.MboRole;
import ru.yandex.market.mbo.user.CategoryRole;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.user.UserRolesManager;

@SuppressWarnings("checkstyle:MagicNumber")
public class UserRolesControllerTest {

    @InjectMocks
    private UserRolesController userRolesController;
    @Mock
    private UserManager userManager;
    @Mock
    private UserRolesManager userRolesManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFindRolesContainsOnlyRolesWithAllProjects() {
        Multimap<Long, CategoryRole> result = ArrayListMultimap.create();
        result.put(1L, new CategoryRole(MboRole.OPERATOR, 10L, 1, 2, 3, 4));
        result.put(1L, new CategoryRole(MboRole.SUPER, 10L, 1, 2));
        Mockito.when(userRolesManager.getUserCategoryRoles(Mockito.any())).thenReturn(result);

        var filter = new RoleFilter(null, null, List.of(1L));

        List<UserCategoryRoles> roles = userRolesController.findRoles(filter);
        Assertions.assertThat(roles).hasSize(1);
        UserCategoryRoles userCategoryRoles = roles.get(0);
        Collection<MboRole> mboRoles = userCategoryRoles.getRoles();
        Assertions.assertThat(mboRoles).containsExactly(MboRole.OPERATOR);
    }

}
