package ru.yandex.direct.grid.processing.service.attributes;


import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.abac.Attribute;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class AttributeResolverServiceTest {

    @Mock
    private DirectWebAuthenticationSource directWebAuthenticationSource;
    @Mock
    private FeatureService featureService;

    private AttributeResolverService attributeResolverService;

    @Before
    public void before() {
        doReturn(new DirectAuthentication(new User().withRole(RbacRole.SUPER), new User()))
                .when(directWebAuthenticationSource).getAuthentication();
        attributeResolverService =
                new AttributeResolverService(directWebAuthenticationSource, featureService);
    }

    @Test
    public void resolve_byFeatureEnabled() {
        doReturn(Set.of(FeatureName.GRID.getName()))
                .when(featureService).getEnabledForUid((Long) any());

        assertThat(attributeResolverService.resolve(new Attribute[]{Attribute.OPERATOR_HAS_GRID_FEATURE}))
                .isTrue();
    }

    @Test
    public void resolve_byFeatureDisabled() {
        doReturn(emptySet())
                .when(featureService).getEnabledForUid((Long) any());

        assertThat(attributeResolverService.resolve(new Attribute[]{Attribute.OPERATOR_HAS_GRID_FEATURE}))
                .isFalse();
    }

    @Test
    public void resolve_byRoleMatch() {
        doReturn(new DirectAuthentication(new User().withRole(RbacRole.SUPER), new User()))
                .when(directWebAuthenticationSource).getAuthentication();

        assertThat(attributeResolverService.resolve(new Attribute[]{Attribute.CAN_EDIT_CAMPAIGN_CONTENT_LANGUAGE_BLOCK}))
                .isTrue();
    }

    @Test
    public void resolve_byRoleDoNotMatch() {
        doReturn(new DirectAuthentication(new User().withRole(RbacRole.SUPERREADER), new User()))
                .when(directWebAuthenticationSource).getAuthentication();

        assertThat(attributeResolverService.resolve(new Attribute[]{Attribute.CAN_EDIT_CAMPAIGN_CONTENT_LANGUAGE_BLOCK}))
                .isFalse();
    }

    @Test
    public void resolve_twoAttributeNoneMatch() {
        doReturn(new DirectAuthentication(new User().withRole(RbacRole.SUPERREADER), new User()))
                .when(directWebAuthenticationSource).getAuthentication();
        doReturn(emptySet())
                .when(featureService).getEnabledForUid((Long) any());

        assertThat(attributeResolverService.resolve(new Attribute[]{Attribute.OPERATOR_HAS_GRID_FEATURE,
                Attribute.CAN_EDIT_CAMPAIGN_CONTENT_LANGUAGE_BLOCK}))
                .isFalse();
    }

    @Test
    public void resolve_twoAttributeOneMatch() {
        doReturn(new DirectAuthentication(new User().withRole(RbacRole.SUPERREADER), new User()))
                .when(directWebAuthenticationSource).getAuthentication();
        doReturn(Set.of(FeatureName.GRID.getName()))
                .when(featureService).getEnabledForUid((Long) any());

        assertThat(attributeResolverService.resolve(new Attribute[]{Attribute.OPERATOR_HAS_GRID_FEATURE,
                Attribute.CAN_EDIT_CAMPAIGN_CONTENT_LANGUAGE_BLOCK}))
                .isTrue();
    }

}
