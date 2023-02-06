package ru.yandex.direct.core.entity.banner.type.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithInternalAdditionalInfoUpdatePositiveTest
        extends BannerNewBannerInfoUpdateOperationTestBase {

    private static final String NEW_TEMPLATE_VALUE = "new val";

    @Before
    public void initTestData() {
        var campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign();
        var adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        bannerInfo = steps.internalBannerSteps().createInternalBanner(new NewInternalBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withCampaignInfo(campaignInfo));
    }

    @Test
    public void changeTemplateVariableForInternalBanner() {
        Long bannerId = bannerInfo.getBannerId();

        var expectedTemplateVariables = singletonList(
                new TemplateVariable()
                        .withTemplateResourceId(TEMPLATE_1_RESOURCE_1_REQUIRED)
                        .withInternalValue(NEW_TEMPLATE_VALUE));
        var modelChanges = new ModelChanges<>(bannerId, InternalBanner.class)
                .process(expectedTemplateVariables, InternalBanner.TEMPLATE_VARIABLES);

        Long id = prepareAndApplyValid(modelChanges);
        InternalBanner actualBanner = getBanner(id, InternalBanner.class);
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getTemplateVariables()).isEqualTo(expectedTemplateVariables);
            //проверяем статусы модерации
            softly.assertThat(actualBanner.getStatusModerate())
                    .isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actualBanner.getStatusPostModerate())
                    .isEqualTo(BannerStatusPostModerate.YES);
        });
    }

    @Test
    public void changeStatusShowForInternalBanner() {
        InternalBanner banner = bannerInfo.getBanner();
        boolean newStatusShow = !banner.getStatusShow();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), InternalBanner.class)
                .process(newStatusShow, InternalBanner.STATUS_SHOW);

        Long id = prepareAndApplyValid(modelChanges);
        InternalBanner actualBanner = getBanner(id, InternalBanner.class);

        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getStatusShow())
                    .isEqualTo(newStatusShow);

            // проверяем, что сбросили StatusBsSynced, а до апдейта он был равен YES
            softly.assertThat(banner.getStatusBsSynced())
                    .isEqualTo(StatusBsSynced.YES);
            softly.assertThat(actualBanner.getStatusBsSynced())
                    .isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void resetStoppedByUrlMonitoring_ForInternalBanner() {
        InternalBanner actualBanner = prepareBanner(false, true);

        assertThat(actualBanner.getStatusShow())
                .isEqualTo(false);
        assertThat(actualBanner.getIsStoppedByUrlMonitoring())
                .isEqualTo(true);

        var newTemplateVariables = singletonList(
                new TemplateVariable()
                        .withTemplateResourceId(TEMPLATE_1_RESOURCE_1_REQUIRED)
                        .withInternalValue(NEW_TEMPLATE_VALUE));
        actualBanner = updateBanner(new ModelChanges<>(bannerInfo.getBannerId(), InternalBanner.class)
                .process(newTemplateVariables, InternalBanner.TEMPLATE_VARIABLES));

        assertThat(actualBanner.getTemplateVariables())
                .isEqualTo(newTemplateVariables);
        assertThat(actualBanner.getIsStoppedByUrlMonitoring())
                .isEqualTo(true);

        actualBanner = updateBanner(new ModelChanges<>(bannerInfo.getBannerId(), InternalBanner.class)
                .process(true, InternalBanner.STATUS_SHOW));

        assertThat(actualBanner.getStatusShow())
                .isEqualTo(true);
        assertThat(actualBanner.getIsStoppedByUrlMonitoring())
                .isEqualTo(false);
    }

    private InternalBanner prepareBanner(boolean statusShow, boolean isStoppedByUrlMonitoring) {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), InternalBanner.class)
                .process(statusShow, InternalBanner.STATUS_SHOW)
                .process(isStoppedByUrlMonitoring, InternalBanner.IS_STOPPED_BY_URL_MONITORING);
        Long id = prepareAndApplyValid(modelChanges);
        return getBanner(id, InternalBanner.class);
    }

    private InternalBanner updateBanner(ModelChanges<InternalBanner> modelChanges) {
        Long id = prepareAndApplyValid(modelChanges);
        return getBanner(id, InternalBanner.class);
    }

}
