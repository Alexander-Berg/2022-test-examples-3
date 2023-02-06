package ru.yandex.market.mboc.app.web;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.common.users.User;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.users.UserRoles;
import ru.yandex.market.mboc.idm.IdmInfoResult;
import ru.yandex.market.mboc.idm.IdmStatusResult;
import ru.yandex.market.mboc.idm.IdmUser;
import ru.yandex.market.mboc.idm.IdmUsersRoles;

/**
 * @author yuramalinov
 * @created 10.08.18
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "checkstyle:MagicNumber"})
public class IdmControllerTest extends BaseMbocAppTest {
    private IdmController controller;

    @Autowired
    private UserRepository userRepository;

    @Before
    public void setup() {
        controller = new IdmController(userRepository, new ObjectMapper());
    }

    @Test
    public void testAddRole() {
        controller.addRole("pupkin", "{\"group\": \"VIEWER\"}");

        Assertions.assertThat(userRepository.findByLogin("pupkin").get().getRoles()).containsExactly(UserRoles.VIEWER);
    }

    @Test
    public void testRemoveRole() {
        userRepository.insert(new User("pupkin").addRole(UserRoles.VIEWER).addRole(UserRoles.MANAGE_ASSORTMENT));
        controller.removeRole("pupkin", "{\"group\": \"VIEWER\"}");

        Assertions.assertThat(userRepository.findByLogin("pupkin").get().getRoles())
            .containsExactly(UserRoles.MANAGE_ASSORTMENT);
    }

    @Test
    public void testRemoveUnknownRole() {
        IdmStatusResult result = controller.removeRole("pupkin", "{\"group\": \"VIEWER\"}");
        Assertions.assertThat(result.getCode()).isEqualTo(1);
        Assertions.assertThat(result.getError()).isNotEmpty();
    }

    @Test
    public void testInfo() {
        IdmInfoResult info = controller.getInfo();

        // Not so strict check, so that it doesn't fall on new roles
        Assertions.assertThat(info.getRoles().getValues()).containsKey("VIEWER");
    }

    @Test
    public void testListRoles() {
        userRepository.insert(new User("no-roles"));
        userRepository.insert(new User("pupkin").addRole(UserRoles.VIEWER));
        userRepository.insert(new User("foo").addRole(UserRoles.VIEWER).addRole(UserRoles.MANAGE_ASSORTMENT));

        IdmUsersRoles roles = controller.getAllRoles();
        Assertions.assertThat(roles.getUsers()).hasSize(2);
        Assertions.assertThat(roles.getUsers()).extracting(IdmUser::getLogin)
            .containsExactly("foo", "pupkin");
        Assertions.assertThat(roles.getUsers().get(1).getRoles())
            .containsExactly(
                Collections.singletonMap("group", UserRoles.VIEWER)
            );
        Assertions.assertThat(roles.getUsers().get(0).getRoles())
            .containsExactly(
                Collections.singletonMap("group", UserRoles.MANAGE_ASSORTMENT),
                Collections.singletonMap("group", UserRoles.VIEWER)
            );

    }
}
