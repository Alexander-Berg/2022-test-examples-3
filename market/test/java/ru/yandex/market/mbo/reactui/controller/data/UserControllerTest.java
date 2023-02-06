package ru.yandex.market.mbo.reactui.controller.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbo.catalogue.category.CategoryManagersManager;
import ru.yandex.market.mbo.reactui.dto.roles.User;
import ru.yandex.market.mbo.security.MboRole;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.MboUserWithRoles;
import ru.yandex.market.mbo.user.UserManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
public class UserControllerTest {

    @InjectMocks
    private UserController userController;
    @Mock
    private UserManager userManager;
    @Mock
    private CategoryManagersManager categoryManagersManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Map<Long, MboUserWithRoles> userMap = Stream.of(
            user(1L, "Auto_А_DAVID_BLANE", MboRole.OPERATOR),
            user(2L, "trololo"),
            user(3L, "AutoW_6_M_VASYA", MboRole.ADMIN, MboRole.CHIEF),
            user(4L, "AutoW_7_ELECTRIC_BARBARELLA", MboRole.OPERATOR),
            user(5L, "Auto_kl2_M_TRUMP", MboRole.ADMIN)
        ).collect(Collectors.toMap(MboUser::getUid, u -> u));

        when(userManager.getAllUsersWithGlobalRoles()).thenReturn(
            userMap.values().stream().filter(u -> u.getRoles().size() > 0).collect(Collectors.toList())
        );

        Multimap<Long, Long> subordinates = ArrayListMultimap.create();
        subordinates.putAll(1L, Arrays.asList(4L, 5L));
        subordinates.putAll(3L, Arrays.asList(2L, 4L, 5L));
        subordinates.put(4L, 5L);

        when(userManager.getSubordinatesMap()).thenReturn(subordinates.asMap());

        when(categoryManagersManager.getManagersForAllCategories()).thenReturn(
            Arrays.asList(
                new CategoryManagersManager.CategoryManagers(10L, 101L, userMap.get(1L), null),
                new CategoryManagersManager.CategoryManagers(20L, 102L, userMap.get(2L), null),
                new CategoryManagersManager.CategoryManagers(30L, 103L, userMap.get(3L), null),
                new CategoryManagersManager.CategoryManagers(40L, 104L, userMap.get(5L), null),
                new CategoryManagersManager.CategoryManagers(50L, 105L, userMap.get(5L), null)
            )
        );
    }

    @Test
    public void testGetUsers() {
        assertThat(userController.getUsers()).containsExactlyInAnyOrder(
            new User("", 1L, "Auto_А_DAVID_BLANE", "", "")
                .setGlobalRoles(Collections.singleton(MboRole.OPERATOR))
                .setSubordinates(Arrays.asList(4L, 5L)),
            new User("", 3L, "AutoW_6_M_VASYA", "", "")
                .setGlobalRoles(set(MboRole.ADMIN, MboRole.CHIEF))
                .setManagerCategories(Collections.singletonList(30L))
                .setSubordinates(Arrays.asList(2L, 4L, 5L)),
            new User("", 4L, "AutoW_7_ELECTRIC_BARBARELLA", "", "")
                .setGlobalRoles(Collections.singleton(MboRole.OPERATOR))
                .setSubordinates(Collections.singletonList(5L)),
            new User("", 5L, "Auto_kl2_M_TRUMP", "", "")
                .setGlobalRoles(Collections.singleton(MboRole.ADMIN))
                .setManagerCategories(Arrays.asList(40L, 50L))
                .setSubordinates(Collections.emptyList())
        );
    }

    private Set<MboRole> set(MboRole... roles) {
        return Arrays.stream((roles))
            .collect(Collectors.toSet());
    }

    private MboUserWithRoles user(long uid, String name, MboRole... roles) {
        MboUserWithRoles user = new MboUserWithRoles("login" + uid, uid, name, uid + "@ya.ru",
            "staff_login" + uid);
        for (MboRole role : roles) {
            user.addRole(role.getId(), role.name());
        }
        return user;
    }

}
