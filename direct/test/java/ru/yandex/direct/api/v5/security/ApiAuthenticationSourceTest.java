package ru.yandex.direct.api.v5.security;

import java.util.Collections;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.rbac.RbacRole;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(SpringRunner.class)
public class ApiAuthenticationSourceTest {
    @Autowired
    DirectConfig cfg;
    ApiAuthenticationSource authSrc;

    @Before
    public void setUp() {
        authSrc = new ApiAuthenticationSource(
                cfg.getStringList("services_application_ids"),
                cfg.getStringList("display_url_texts_allowed_application_ids"),
                cfg.getStringList("leadform_attributes_allowed_application_ids"),
                Collections.emptyMap()
        );
    }

    @Test
    public void isServicesApplicationWorks() {
        try {
            var soft = new SoftAssertions();

            setAuth("fake_application_id");
            soft.assertThat(authSrc.isServicesApplication()).isFalse();

            setAuth("866b39c759de4008b127b4830705a81f");
            soft.assertThat(authSrc.isServicesApplication()).isTrue();

            soft.assertAll();
        } finally {
            clearAuth();
        }
    }

    @Test
    public void isDisplayUrlTextsAllowedWorks() {
        try {
            var soft = new SoftAssertions();

            setAuth("fake_application_id");
            soft.assertThat(authSrc.isDisplayUrlTextAllowed()).isFalse();

            setAuth("fe7727b44572429eae7fe3a639b1af9a");
            soft.assertThat(authSrc.isDisplayUrlTextAllowed()).isTrue();

            soft.assertAll();
        } finally {
            clearAuth();
        }
    }

    @Test
    public void isLeadformAttributesAllowedWorks() {
        try {
            var soft = new SoftAssertions();

            setAuth("fake_application_id");
            soft.assertThat(authSrc.isLeadformAttributesAllowed()).isFalse();

            setAuth("fe7727b44572429eae7fe3a639b1af9a");
            soft.assertThat(authSrc.isLeadformAttributesAllowed()).isTrue();

            soft.assertAll();
        } finally {
            clearAuth();
        }
    }

    private void clearAuth() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private void setAuth(String appId) {
        var user = mock(ApiUser.class);
        when(user.getRole()).thenReturn(RbacRole.CLIENT);
        SecurityContextHolder.getContext().setAuthentication(new DirectApiAuthentication(
                user, user, user, user,
                true, null, appId, null
        ));
    }
}
