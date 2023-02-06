package ru.yandex.market.loyalty.admin.security;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.loyalty.admin.controller.DocumentationController;
import ru.yandex.market.loyalty.admin.controller.PingController;
import ru.yandex.market.loyalty.admin.controller.ProfilingController;
import ru.yandex.market.loyalty.admin.controller.SupportDetailsController;
import ru.yandex.market.loyalty.core.model.security.AdminRole;
import ru.yandex.market.loyalty.core.utils.CommonTestUtils;
import ru.yandex.market.loyalty.core.utils.CoreCollectionUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CoreCollectionUtils.minus;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

public class SecuredHandlesTest {
    @NotNull
    private static Set<String> allUrlsInAdminRole() {
        return Arrays.stream(AdminRole.Urls.class.getDeclaredFields())
                .filter(field -> field.getType().equals(String.class))
                .map(makeExceptionsUnchecked(f -> (String) f.get(null)))
                .collect(Collectors.toSet());
    }

    @Test
    public void checkAllHandlesHasSecurityRestrictions() {
        CommonTestUtils.checkAllWebMethodsSecured(
                "ru.yandex.market.loyalty.admin",
                ImmutableSet.of(
                        PingController.class, ProfilingController.class, DocumentationController.class,
                        SupportDetailsController.class
                )
        );
    }

    @Test
    public void checkNoDuplicatesInRolesAllowed() {
        List<String> roles = CommonTestUtils.allRolesAllowed("ru.yandex.market.loyalty.admin")
                .collect(Collectors.toList());
        assertThat(CoreCollectionUtils.findDuplicates(roles), is(empty()));
    }

    @Test
    public void checkAllRolesFromAdminRoleUrls() {
        Set<String> usedRoles = CommonTestUtils.allRolesAllowed("ru.yandex.market.loyalty.admin")
                .collect(Collectors.toSet());
        Set<String> allUrls = allUrlsInAdminRole();
        assertThat(minus(usedRoles, allUrls), is(empty()));
    }

    @Test
    public void allUrlsUsedInAdminRoles() {
        Set<String> allUrls = allUrlsInAdminRole();

        Set<String> usedUrls = Arrays.stream(AdminRole.values())
                .flatMap(adminRole -> adminRole.getAllowedUrls().stream())
                .collect(Collectors.toSet());

        assertThat(minus(allUrls, usedUrls), is(empty()));
        assertThat(minus(usedUrls, allUrls), is(empty()));
    }
}
