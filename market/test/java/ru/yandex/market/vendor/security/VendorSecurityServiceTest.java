package ru.yandex.market.vendor.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.User;
import ru.yandex.vendor.security.IVendorSecurityService;
import ru.yandex.vendor.security.Role;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;

public class VendorSecurityServiceTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private IVendorSecurityService vendorSecurityService;

    @Autowired
    private WireMockServer blackboxMock;

    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/security/VendorSecurityServiceTest" +
            "/testGetUsersForGlobalRoleWithHardcode/before.csv")
    void testGetUsersForGlobalRoleWithHardcode() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetUsersForGlobalRole/blackbox_response_100500.json"))));

        List<User> supercheckManagerUsers = vendorSecurityService.getUsersForGlobalRole(Role.manager_user);
        assertNotNull(supercheckManagerUsers);
        assertThat(supercheckManagerUsers, hasSize(1));
    }

    @Test
    void testGetUsersForGlobalRoleWithLocalRole() {
        List<User> supercheckManagerUsers = vendorSecurityService.getUsersForGlobalRole(Role.admin_user);
        assertNotNull(supercheckManagerUsers);
        assertThat(supercheckManagerUsers, empty());
    }

}
