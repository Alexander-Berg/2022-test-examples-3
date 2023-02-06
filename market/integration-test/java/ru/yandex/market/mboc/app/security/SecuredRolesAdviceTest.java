package ru.yandex.market.mboc.app.security;

import javax.servlet.http.Cookie;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.app.BaseWebIntegrationTestClass;
import ru.yandex.market.mboc.app.util.JsonPathUtils;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.users.User;
import ru.yandex.market.mboc.common.users.UserRoles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author yuramalinov
 * @created 25.09.18
 */
public class SecuredRolesAdviceTest extends BaseWebIntegrationTestClass {
    private static final Cookie DEV_COOKIE = new Cookie("dev-check", "ignore");

    @Autowired
    private SupplierRepository supplierRepository;

    @Override
    public void setupUser() {
        // No setup for this test, it's about security so it'll use it's one
    }

    @Test
    public void testSupplierControllerRequiresUser() throws Exception {
        mvc.perform(get("/api/supplier").cookie(DEV_COOKIE))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testSupplierControllerRequiresRoles() throws Exception {
        userRepository.insert(new User("test"));

        mvc.perform(
                get("/api/supplier?filter=ALL")
                    .cookie(DEV_COOKIE)
                    .header("Authorization", "test"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.result")
                .value(JsonPathUtils.jsonArray(UserRoles.VIEWER, UserRoles.MANAGE_ASSORTMENT,
                    UserRoles.MANAGE_CONTENT_ASSORTMENT,
                    UserRoles.MANAGE_PRIORITY)));
    }

    @Test
    public void testSupplierControllerWorksIfEnoughRoles() throws Exception {
        supplierRepository.insert(new Supplier(1, "Test"));
        userRepository.insert(new User("test").addRole(UserRoles.VIEWER));

        mvc.perform(
                get("/api/supplier?filter=ALL")
                    .cookie(DEV_COOKIE)
                    .header("Authorization", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Test"));
    }

    @Test
    public void testMethodsRequireCustomPermissions() throws Exception {
        supplierRepository.insert(new Supplier(1, "Test"));
        userRepository.insert(new User("test").addRole(UserRoles.VIEWER));

        mvc.perform(
            post("/api/supplier/updateVisible?supplierId=1&visible=false")
                .cookie(DEV_COOKIE)
                .header("Authorization", "test"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.result")
                .value(JsonPathUtils.jsonArray(UserRoles.MANAGE_ASSORTMENT)));
    }

    @Test
    public void testMethodsRequireCustomPermissionsUpdateVisible() throws Exception {
        supplierRepository.insert(new Supplier(1, "Test").setVisible(true));
        userRepository.insert(new User("test").addRole(UserRoles.MANAGE_ASSORTMENT));

        mvc.perform(
            post("/api/supplier/updateVisible?supplierId=1&visible=false")
                .cookie(DEV_COOKIE)
                .header("Authorization", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible").value(false));

        Assertions.assertThat(supplierRepository.findById(1).getVisible()).isFalse();
    }

    @Test
    public void testInheritance() throws Exception {
        userRepository.insert(new User("test"));

        mvc.perform(
            get("/calendaring/do-something")
                .cookie(DEV_COOKIE)
                .header("Authorization", "test"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.result")
                .value(JsonPathUtils.jsonArray(
                    UserRoles.SHOP_REQUESTS_CALENDARING,
                    UserRoles.SHOP_REQUESTS_CALENDARING_172,
                    UserRoles.SHOP_REQUESTS_CALENDARING_171,
                    UserRoles.SHOP_REQUESTS_CALENDARING_147,
                    UserRoles.SHOP_REQUESTS_CALENDARING_145
                    )
                )
            );
    }

}
