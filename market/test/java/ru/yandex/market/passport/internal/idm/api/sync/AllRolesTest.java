package ru.yandex.market.passport.internal.idm.api.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.market.passport.utils.ResourceUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 * @since 10/27/2018
 */
public class AllRolesTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testAllRoles() throws JsonProcessingException {
        AllRoles allRoles = new AllRoles();
        allRoles.setCode(0);

        UserRoles userRoles = new UserRoles();
        userRoles.setLogin("user1");

        Role role1 = new Role();
        role1.addRole("group", "clothes");
        role1.addRole("role", "admin");

        Role role2 = new Role();
        role2.addRole("group", "electronic");
        role2.addRole("role", "operator");
        role2.addField("field1", "user");

        userRoles.setRoles(ImmutableList.of(role1, role2));
        allRoles.setUsers(ImmutableList.of(userRoles));

        GroupRoles groupRoles = new GroupRoles();
        groupRoles.setGroup(1);

        Role role3 = new Role();
        role3.addRole("group", "electronic");
        role3.addRole("role", "admin");
        role3.addField("field1", "group");

        groupRoles.setRoles(ImmutableList.of(role3));
        allRoles.setGroups(ImmutableList.of(groupRoles));

        String serialized = mapper.writer().writeValueAsString(allRoles);
        String expected = ResourceUtils.getJsonResourceAsString("idm/all-roles.json");
        assertEquals(expected, serialized);
    }
}
