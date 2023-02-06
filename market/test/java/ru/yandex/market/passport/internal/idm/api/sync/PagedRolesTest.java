package ru.yandex.market.passport.internal.idm.api.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.passport.utils.ResourceUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 * @since 10/27/2018
 */
public class PagedRolesTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testPagedRoles() throws JsonProcessingException {
        PagedRoles pagedRoles = new PagedRoles();
        pagedRoles.setCode(0);

        pagedRoles.setNextUrl("next-url");

        PagedRole pagedRole1 = new PagedRole();
        pagedRole1.setPath("/group/clothes/role/operator");
        pagedRole1.setLogin("user1");
        pagedRole1.setFields(ImmutableMap.of("key", "value"));

        PagedRole pagedRole2 = new PagedRole();
        pagedRole2.setPath("/group/clothes/role/admin");
        pagedRole2.setGroup(1);
        pagedRole2.setFields(ImmutableMap.of("key", "value2"));

        pagedRoles.setUsers(ImmutableList.of(pagedRole1, pagedRole2));

        String serialized = mapper.writer().writeValueAsString(pagedRoles);
        String expected = ResourceUtils.getJsonResourceAsString("idm/paged-roles.json");
        assertEquals(expected, serialized);
    }
}
