package ru.yandex.market.passport.internal.idm.api.info;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.passport.internal.idm.api.common.LocalizedString;
import ru.yandex.market.passport.utils.ResourceUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 * @since 10/27/2018
 */
public class RolesInfoTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSimpleRoles() throws JsonProcessingException {
        RolesInfo rolesInfo = new RolesInfo();
        rolesInfo.setSlug("role");
        rolesInfo.setName(new LocalizedString("Роль"));
        rolesInfo.setValues(ImmutableMap.of(
            "operator", new SimpleRoleInfo("Оператор"),
            "admin", new SimpleRoleInfo("Администратор")
        ));
        String serialized = mapper.writer().writeValueAsString(rolesInfo);
        String expected = ResourceUtils.getJsonResourceAsString("idm/roles-info-simple.json");
        assertEquals(expected, serialized);
    }

    @Test
    public void testHierarchicalRoles() throws JsonProcessingException {
        RolesInfo groupInfo = new RolesInfo();
        groupInfo.setSlug("dept");
        groupInfo.setName(new LocalizedString(
            "ru", "Дерартамент",
            "en", "Department")
        );

        RoleInfo groupClothes = new RoleInfo();
        groupClothes.setName(new LocalizedString(
            "ru", "Одежда",
            "en", "Clothes")
        );

        RoleInfo groupElectronic = new RoleInfo();
        groupElectronic.setName(new LocalizedString(
            "ru", "Электроника",
            "en", "Electronic")
        );

        groupInfo.setValues(ImmutableMap.of(
            "clothes", groupClothes,
            "electrolic", groupElectronic
        ));

        RolesInfo rolesInfo = new RolesInfo();
        rolesInfo.setSlug("role");
        rolesInfo.setName(new LocalizedString(
            "ru", "Роль",
            "en", "Role")
        );

        RoleInfo roleAdmin = new RoleInfo();
        roleAdmin.setName(new LocalizedString(
            "ru", "Админитратор",
            "en", "Administrator")
        );

        RoleInfo roleOperator = new RoleInfo();
        roleOperator.setName(new LocalizedString(
            "ru", "Оператор",
            "en", "Operator")
        );

        rolesInfo.setValues(ImmutableMap.of(
            "admin", roleAdmin,
            "operator", roleOperator
        ));

        groupClothes.setRoles(rolesInfo);
        groupElectronic.setRoles(rolesInfo);

        String serialized = mapper.writer().writeValueAsString(groupInfo);
        String expected = ResourceUtils.getJsonResourceAsString("idm/roles-info-hierarchical.json");
        assertEquals(expected, serialized);
    }
}
