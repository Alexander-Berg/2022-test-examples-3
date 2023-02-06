package ru.yandex.direct.core.entity.banner.type.internal;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestNewInternalBanners.clientInternalBanner;
import static ru.yandex.direct.core.testing.data.TestNewInternalBanners.moderatedInternalBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithInternalAdditionalInfoAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    private AdGroupRepository adGroupRepository;

    private ClientInfo clientInfo;

    @Before
    public void initTestData() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
    }

    @Test
    public void validInternalBannerAdd() {
        var campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        var banner = clientInternalBanner(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);
        InternalBanner actualBanner = getBanner(id, InternalBanner.class);

        List<AdGroup> adGroups =
                adGroupRepository.getAdGroups(adGroupInfo.getShard(), Set.of(adGroupInfo.getAdGroupId()));
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getTemplateId())
                    .isEqualTo(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1);
            softly.assertThat(actualBanner.getTemplateVariables()).isEqualTo(
                    singletonList(new TemplateVariable().withTemplateResourceId(
                            TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED)
                            .withInternalValue("bbb")));
            //проверяем статусы модерации
            softly.assertThat(actualBanner.getStatusModerate())
                    .isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actualBanner.getStatusPostModerate())
                    .isEqualTo(BannerStatusPostModerate.YES);

            softly.assertThat(adGroups)
                    .hasSize(1)
                    .extracting(AdGroup::getStatusBsSynced)
                    .containsExactly(StatusBsSynced.NO);
        });
    }

    @Test
    public void validModeratedInternalBanner_WithStatusShowNo_AndStatusShowAfterModerationYes() {
        var campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        var banner = moderatedInternalBanner(adGroupInfo.getAdGroupId())
                .withStatusShow(false);

        Long id = prepareAndApplyValid(banner);
        InternalBanner actualBanner = getBanner(id, InternalBanner.class);

        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getStatusShow())
                    .isEqualTo(false);

            softly.assertThat(actualBanner.getModerationInfo())
                    .isNotNull()
                    .extracting(InternalModerationInfo::getStatusShowAfterModeration)
                    .isEqualTo(banner.getModerationInfo().getStatusShowAfterModeration());
        });
    }

}
