package ru.yandex.market.passport.internal.idm.api.info;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.passport.internal.idm.api.common.LocalizedString;
import ru.yandex.market.passport.utils.ResourceUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 * @since 10/27/2018
 */
public class RoleInfoTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testRoleInfo() throws JsonProcessingException {

        RoleInfo groupClothes = new RoleInfo();
        groupClothes.setName(new LocalizedString(
            "ru", "Одежда",
            "en", "Clothes")
        );
        groupClothes.setHelp(new LocalizedString("Помощь по одежде"));
        groupClothes.setFirewallDeclaration("clothes-firewall");
        groupClothes.setSet("clothes-set");
        groupClothes.setUniqueId("clothes-unique-id");
        groupClothes.setVisibility(false);

        RoleResponsibility responsibility = new RoleResponsibility("user1", true);
        groupClothes.setResponsibilities(ImmutableList.of(responsibility));

        RolesInfo rolesInfo = new RolesInfo();
        rolesInfo.setSlug("role");
        rolesInfo.setName(new LocalizedString(
            "ru", "Роль",
            "en", "Role")
        );

        SimpleRoleInfo roleAdmin = new SimpleRoleInfo("Админитратор");
        SimpleRoleInfo roleOperator = new SimpleRoleInfo("Оператор");

        rolesInfo.setValues(ImmutableMap.of(
            "admin", roleAdmin,
            "operator", roleOperator
        ));

        groupClothes.setRoles(rolesInfo);

        String serialized = mapper.writer().writeValueAsString(groupClothes);
        String expected = ResourceUtils.getJsonResourceAsString("idm/role-info.json");
        assertEquals(expected, serialized);
    }
}
