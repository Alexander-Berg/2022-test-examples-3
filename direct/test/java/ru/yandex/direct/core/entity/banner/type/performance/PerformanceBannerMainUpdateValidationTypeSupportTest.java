package ru.yandex.direct.core.entity.banner.type.performance;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.banner.type.performance.defects.PerformanceBannerMainDefects.bannersWithoutCreativeNotEnabled;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class PerformanceBannerMainUpdateValidationTypeSupportTest {
    @Mock
    private FeatureService featureService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Description("Нельзя обновить баннер, если нет фичи")
    public void testUpdateBannerNoFeature() {
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class))).thenReturn(false);

        List<PerformanceBannerMain> banners = singletonList(
                new PerformanceBannerMain()
                        .withAdGroupId(10L)
        );

        var typeSupport = new BannerPerformanceMainUpdateValidationTypeSupport(featureService);
        ValidationResult<List<PerformanceBannerMain>, Defect> vr =
                typeSupport.validate(new BannersUpdateOperationContainerImpl(
                        1, 1L, RbacRole.CLIENT, ClientId.fromLong(1L), 1L, 1L, 1L, Collections.emptySet(),
                        ModerationMode.DEFAULT, false, false, false
                ), new ValidationResult<>(banners));

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), bannersWithoutCreativeNotEnabled())));
    }
}
