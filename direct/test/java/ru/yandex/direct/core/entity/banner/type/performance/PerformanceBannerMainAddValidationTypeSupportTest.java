package ru.yandex.direct.core.entity.banner.type.performance;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.banner.type.performance.defects.PerformanceBannerMainDefects.bannerInThisAdgroupAlreadyExists;
import static ru.yandex.direct.core.entity.banner.type.performance.defects.PerformanceBannerMainDefects.bannersWithoutCreativeNotEnabled;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class PerformanceBannerMainAddValidationTypeSupportTest {
    @Mock
    private BannerTypedRepository bannerTypedRepository;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private FeatureService featureService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Description("Можно добавить по одному баннеру в группу")
    public void testCreateBannersWithDifferentAdGroupIds() {
        when(shardHelper.getShardByClientId(any())).thenReturn(1);
        when(bannerTypedRepository.getBannersByGroupIds(anyInt(), anyCollection(), any())).thenReturn(emptyList());
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class))).thenReturn(true);

        List<PerformanceBannerMain> banners = asList(
                new PerformanceBannerMain().withAdGroupId(10L),
                new PerformanceBannerMain().withAdGroupId(11L)
        );

        var typeSupport = new PerformanceBannerMainAddValidationTypeSupport(bannerTypedRepository, featureService,
                shardHelper);
        var vr = typeSupport.validate(new BannersAddOperationContainerImpl(
                1, 1L, RbacRole.CLIENT, ClientId.fromLong(1L), 1L, 1L, 1L, Collections.emptySet(),
                ModerationMode.FORCE_SAVE_DRAFT, false, false, false
        ), new ValidationResult<>(banners));

        Assert.assertThat(vr, hasNoErrorsAndWarnings());
    }

    @Test
    @Description("Нельзя добавить одновременно несколько баннеров на одну группу")
    public void testCreateBannersWithSameAdGroupId() {
        when(shardHelper.getShardByClientId(any())).thenReturn(1);
        when(bannerTypedRepository.getBannersByGroupIds(anyInt(), anyCollection(), any())).thenReturn(emptyList());
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class))).thenReturn(true);

        List<PerformanceBannerMain> banners = asList(
                new PerformanceBannerMain()
                        .withAdGroupId(10L),
                new PerformanceBannerMain()
                        .withAdGroupId(10L)
        );

        var typeSupport = new PerformanceBannerMainAddValidationTypeSupport(bannerTypedRepository, featureService,
                shardHelper);
        ValidationResult<List<PerformanceBannerMain>, Defect> vr =
                typeSupport.validate(new BannersAddOperationContainerImpl(
                        1, 1L, RbacRole.CLIENT, ClientId.fromLong(1L), 1L, 1L, 1L, Collections.emptySet(),
                        ModerationMode.FORCE_SAVE_DRAFT, false, false, false
                ), new ValidationResult<>(banners));

        Assert.assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), duplicatedElement())));
        Assert.assertThat(vr, hasDefectWithDefinition(validationError(path(index(1)), duplicatedElement())));
    }

    @Test
    @Description("Нельзя добавить баннер в группу, где уже есть баннер")
    public void testAddSecondBannerToAdGroup() {
        when(shardHelper.getShardByClientId(any())).thenReturn(1);
        when(bannerTypedRepository.getBannersByGroupIds(anyInt(), anyCollection(), any()))
                .thenReturn(singletonList(new PerformanceBannerMain().withAdGroupId(10L)));
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class))).thenReturn(true);

        List<PerformanceBannerMain> banners = singletonList(
                new PerformanceBannerMain()
                        .withAdGroupId(10L)
        );

        var typeSupport = new PerformanceBannerMainAddValidationTypeSupport(bannerTypedRepository, featureService,
                shardHelper);
        ValidationResult<List<PerformanceBannerMain>, Defect> vr =
                typeSupport.validate(new BannersAddOperationContainerImpl(
                        1, 1L, RbacRole.CLIENT, ClientId.fromLong(1L), 1L, 1L, 1L, Collections.emptySet(),
                        ModerationMode.FORCE_SAVE_DRAFT, false, false, false
                ), new ValidationResult<>(banners));

        Assert.assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)),
                bannerInThisAdgroupAlreadyExists())));
    }

    @Test
    @Description("Нельзя добавить баннер, если нет фичи")
    public void testAddBannerNoFeature() {
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class))).thenReturn(false);

        List<PerformanceBannerMain> banners = singletonList(
                new PerformanceBannerMain()
                        .withAdGroupId(10L)
        );

        var typeSupport = new PerformanceBannerMainAddValidationTypeSupport(bannerTypedRepository, featureService,
                shardHelper);
        ValidationResult<List<PerformanceBannerMain>, Defect> vr =
                typeSupport.validate(new BannersAddOperationContainerImpl(
                        1, 1L, RbacRole.CLIENT, ClientId.fromLong(1L), 1L, 1L, 1L, Collections.emptySet(),
                        ModerationMode.FORCE_SAVE_DRAFT, false, false, false
                ), new ValidationResult<>(banners));

        Assert.assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)),
                bannersWithoutCreativeNotEnabled())));
    }
}
