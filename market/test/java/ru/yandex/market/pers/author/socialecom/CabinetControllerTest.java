package ru.yandex.market.pers.author.socialecom;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.model.AgitationUserType;
import ru.yandex.market.pers.author.mock.PersAuthorSaasMocks;
import ru.yandex.market.pers.author.mock.mvc.socialecom.CabinetMvcMocks;
import ru.yandex.market.pers.author.socialecom.dto.RoleInfo;
import ru.yandex.market.pers.author.socialecom.model.UserType;
import ru.yandex.market.pers.author.socialecom.service.UserRoleService;
import ru.yandex.market.pers.service.common.util.ConfigurationCache;

public class CabinetControllerTest extends PersAuthorTest {

    private static final String UID = "12345";
    private static final Long BUSINESS_ID = 123L;
    private static final Long BRAND_ID = 198L;

    @Autowired
    private CabinetMvcMocks mvc;

    @Autowired
    private PersAuthorSaasMocks authorSaasMocks;

    @Autowired
    UserRoleService userRoleService;

    @Autowired
    ConfigurationCache configurationCache;

    @Test
    public void testGetUserRolesByUID() {
        AgitationUser user = new AgitationUser(AgitationUserType.UID, UID);
        authorSaasMocks.mockUserRoles(user, List.of(
            Map.of("partner_id", String.valueOf(BUSINESS_ID), "partner_type", UserType.BUSINESS.getName()),
            Map.of("partner_id", String.valueOf(BRAND_ID), "partner_type", UserType.BRAND.getName()),
            Map.of("partner_id", String.valueOf(877L), "partner_type", "ILLEGAL_TYPE!")

        ));

        List<RoleInfo> result = mvc.getUserRoleInfoByUID(UID);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(BUSINESS_ID, result.get(0).getPartnerId());
        Assertions.assertEquals(BRAND_ID, result.get(1).getPartnerId());
    }

    @Test
    public void testNoRoles() {
        List<RoleInfo> result = mvc.getUserRoleInfoByUID(UID);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckBrandRoleWithSuperUser() {
        boolean result = mvc.checkUserRoleByBusiness(UID, BRAND_ID);
        Assertions.assertFalse(result);
        userRoleService.setSuperUser(UID);
        configurationCache.invalidateCache();
        result = mvc.checkUserRoleByBrand(UID, BRAND_ID);
        Assertions.assertTrue(result);
    }

    @Test
    public void testCheckBusinessRoleWithSuperUser() {
        boolean result = mvc.checkUserRoleByBusiness(UID, BUSINESS_ID);
        Assertions.assertFalse(result);
        userRoleService.setSuperUser(UID);
        configurationCache.invalidateCache();
        result = mvc.checkUserRoleByBusiness(UID, BUSINESS_ID);
        Assertions.assertTrue(result);
    }

    @Test
    public void testCheckBrandRole() {
        AgitationUser user = new AgitationUser(AgitationUserType.UID, UID);
        authorSaasMocks.mockUserRoles(user, List.of(
            Map.of("partner_id", String.valueOf(BUSINESS_ID), "partner_type", UserType.BUSINESS.getName()),
            Map.of("partner_id", String.valueOf(BRAND_ID), "partner_type", UserType.BRAND.getName()),
            Map.of("partner_id", String.valueOf(877L), "partner_type", "ILLEGAL_TYPE!")

        ));
        boolean result = mvc.checkUserRoleByBrand(UID, BRAND_ID);
        Assertions.assertTrue(result);
        result = mvc.checkUserRoleByBrand(UID, BRAND_ID + 1);
        Assertions.assertFalse(result);
    }

    @Test
    public void testCheckBusinessRole() {
        AgitationUser user = new AgitationUser(AgitationUserType.UID, UID);
        authorSaasMocks.mockUserRoles(user, List.of(
            Map.of("partner_id", String.valueOf(BUSINESS_ID), "partner_type", UserType.BUSINESS.getName()),
            Map.of("partner_id", String.valueOf(BRAND_ID), "partner_type", UserType.BRAND.getName()),
            Map.of("partner_id", String.valueOf(877L), "partner_type", "ILLEGAL_TYPE!")

        ));
        boolean result = mvc.checkUserRoleByBusiness(UID, BUSINESS_ID);
        Assertions.assertTrue(result);
        result = mvc.checkUserRoleByBusiness(UID, BUSINESS_ID + 1);
        Assertions.assertFalse(result);

    }
}
