package ru.yandex.direct.internaltools.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.exception.InternalToolAccessDeniedException;
import ru.yandex.direct.internaltools.core.exception.InternalToolNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.feature.FeatureName.AS_SOON_AS_POSSIBLE;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.DEVELOPER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.MANAGER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.PLACER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.SUPER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole.SUPERREADER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolCategory.API;
import static ru.yandex.direct.internaltools.core.enums.InternalToolCategory.OTHER;
import static ru.yandex.direct.internaltools.core.enums.InternalToolCategory.YA_AGENCY;


public class InternalToolsRegistryTest {
    private static final String LABEL_MANAGER = "manager";
    private static final String LABEL_SUPER = "super";
    private static final String LABEL_SUPERREADER = "super_reader";
    private static final String UNKNOWN_LABEL = "unknown_label";
    private static final String LABEL_SUPER_WITH_FEATURE = "super_with_feature";

    @Mock
    private InternalToolProxy toolForManager;

    @Mock
    private InternalToolProxy toolForSuper;

    @Mock
    private InternalToolProxy toolForSuperWithFeature;

    @Mock
    private InternalToolProxy toolForSuperReader;

    @Mock
    private FeatureService featureService;

    private InternalToolsRegistry registry;
    private ClientId operatorClientId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(EnumSet.of(SUPER))
                .when(toolForSuper).getAllowedRoles();
        doReturn(API)
                .when(toolForSuper).getCategory();

        doReturn(EnumSet.of(SUPER))
                .when(toolForSuperWithFeature).getAllowedRoles();
        doReturn(API)
                .when(toolForSuperWithFeature).getCategory();
        doReturn(AS_SOON_AS_POSSIBLE)
                .when(toolForSuperWithFeature).getRequiredFeature();

        doReturn(EnumSet.of(SUPER, SUPERREADER, DEVELOPER))
                .when(toolForSuperReader).getAllowedRoles();
        doReturn(OTHER)
                .when(toolForSuperReader).getCategory();

        doReturn(EnumSet.of(SUPER, MANAGER))
                .when(toolForManager).getAllowedRoles();
        doReturn(YA_AGENCY)
                .when(toolForManager).getCategory();

        operatorClientId = ClientId.fromLong(1L);

        registry = new InternalToolsRegistry(ImmutableMap.<String, InternalToolProxy<?>>builder()
                .put(LABEL_MANAGER, toolForManager)
                .put(LABEL_SUPER, toolForSuper)
                .put(LABEL_SUPERREADER, toolForSuperReader)
                .put(LABEL_SUPER_WITH_FEATURE, toolForSuperWithFeature)
                .build(), featureService);
    }

    @Test
    public void testIsToolAccessibleBy() {
        assertThat(registry.isToolAccessibleBy(toolForSuper, Collections.singletonList(SUPERREADER), operatorClientId))
                .isFalse();
        assertThat(registry.isToolAccessibleBy(toolForSuperReader, Collections.singletonList(DEVELOPER), operatorClientId))
                .isTrue();
        assertThat(registry.isToolAccessibleBy(toolForManager, Collections.singletonList(PLACER), operatorClientId))
                .isFalse();
    }

    @Test
    public void isToolAccessibleBy_NotAccessibleByAccessListAndFeatureIsOn_ToolNotAccessible() {
        doReturn(true).when(featureService).isEnabledForClientId(operatorClientId, AS_SOON_AS_POSSIBLE);
        assertThat(registry.isToolAccessibleBy(toolForSuperWithFeature, Collections.singletonList(SUPERREADER), operatorClientId))
                .isFalse();
    }

    @Test
    public void isToolAccessibleBy_NotAccessibleByAccessListAndFeatureIsOff_ToolNotAccessible() {
        doReturn(false).when(featureService).isEnabledForClientId(operatorClientId, AS_SOON_AS_POSSIBLE);
        assertThat(registry.isToolAccessibleBy(toolForSuperWithFeature, Collections.singletonList(SUPERREADER), operatorClientId))
                .isFalse();
    }

    @Test
    public void isToolAccessibleBy_AccessibleByAccessListAndFeatureIsOn_ToolIsAccessible() {
        doReturn(true).when(featureService).isEnabledForClientId(operatorClientId, AS_SOON_AS_POSSIBLE);
        assertThat(registry.isToolAccessibleBy(toolForSuperWithFeature, Collections.singletonList(SUPER), operatorClientId))
                .isTrue();
    }

    @Test
    public void isToolAccessibleBy_AccessibleByAccessListAndFeatureIsOff_ToolNotAccessible() {
        doReturn(false).when(featureService).isEnabledForClientId(operatorClientId, AS_SOON_AS_POSSIBLE);
        assertThat(registry.isToolAccessibleBy(toolForSuperWithFeature, Collections.singletonList(SUPER), operatorClientId))
                .isFalse();
    }

    @Test(expected = InternalToolNotFoundException.class)
    public void testToolNotFoundWhenNotInMap() {
        registry.getInternalToolProxy(UNKNOWN_LABEL, Collections.singleton(SUPER), operatorClientId);
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void testNoEnoughRightsForTool() {
        registry.getInternalToolProxy(LABEL_SUPER, Collections.singleton(SUPERREADER), operatorClientId);
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void testNoRights() {
        registry.getInternalToolProxy(LABEL_SUPER, Collections.singleton(SUPERREADER), operatorClientId);
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void getInternalToolProxy_NoRightByFeature() {
        doReturn(false).when(featureService).isEnabledForClientId(operatorClientId, AS_SOON_AS_POSSIBLE);
        registry.getInternalToolProxy(LABEL_SUPER_WITH_FEATURE, Collections.singleton(SUPER), operatorClientId);
    }

    @Test
    public void testGetToolProxy() {
        InternalToolProxy proxy = registry.getInternalToolProxy(LABEL_SUPER, Collections.singleton(SUPER),
                operatorClientId);
        assertThat(proxy)
                .isEqualTo(toolForSuper);
    }

    @Test
    public void testGetInternalToolsByCategoryNoRights() {
        Map<InternalToolCategory, List<InternalToolProxy>>
                categories = registry.getInternalToolsByCategory(Collections.emptySet(), operatorClientId);
        assertThat(categories)
                .isEmpty();
    }

    @Test
    public void testGetInternalToolsByCategoryAll() {
        Map<InternalToolCategory, List<InternalToolProxy>>
                categories = registry.getInternalToolsByCategory(Collections.singleton(SUPER), operatorClientId);

        assertThat(categories)
                .containsOnlyKeys(API, OTHER, YA_AGENCY);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(categories.get(API)).containsExactly(toolForSuper);
        soft.assertThat(categories.get(OTHER)).containsExactly(toolForSuperReader);
        soft.assertThat(categories.get(YA_AGENCY)).containsExactly(toolForManager);
        soft.assertAll();
    }

    @Test
    public void testGetInternalToolsByCategoryOne() {
        Map<InternalToolCategory, List<InternalToolProxy>>
                categories = registry.getInternalToolsByCategory(Collections.singleton(SUPERREADER), operatorClientId);

        assertThat(categories)
                .containsOnlyKeys(OTHER);
        assertThat(categories.get(OTHER))
                .containsExactly(toolForSuperReader);
    }
}
