package ru.yandex.direct.core.entity.banner.service;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerateService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.moderateArchivedBanner;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.moderateNonDraftBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class BannerModerateServiceNegativeTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUPS_MODERATE_NEW =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate ADGROUPS_POSTMODERATE_NO =
            ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.NO;
    private static final StatusModerate CAMPAIGN_MODERATE_NEW =
            StatusModerate.NEW;

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public Function<ClientInfo, Campaign> campaignSupplier;
    @Parameterized.Parameter(2)
    public Function<Long, AdGroup> adGroupSupplier;
    @Parameterized.Parameter(3)
    public BiFunction<Long, Long, OldBanner> bannerSupplier;
    @Parameterized.Parameter(4)
    public Matcher<DefectInfo<Defect>> defect;

    private ClientInfo clientInfo;
    private long bannerId;

    @Autowired
    private Steps steps;
    @Autowired
    private BannerModerateService service;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                new Object[]{
                        "Объявление не черновик",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(CAMPAIGN_MODERATE_NEW)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                                .withStatusModerate(ADGROUPS_MODERATE_NEW)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusShow(false)
                                .withStatusModerate(OldBannerStatusModerate.YES)),
                        validationError(path(index(0)), moderateNonDraftBanner())
                },
                new Object[]{
                        "Объявление архивное",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(CAMPAIGN_MODERATE_NEW)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                                .withStatusModerate(ADGROUPS_MODERATE_NEW)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusArchived(true)
                                .withStatusModerate(OldBannerStatusModerate.YES)),
                        validationError(path(index(0)), moderateArchivedBanner())
                }
        };
    }

    /**
     * Вспомогательный метод чтоб обернуть функцию в объект
     */
    public static Function<ClientInfo, Campaign> createCampaign(Function<ClientInfo, Campaign> fun) {
        return fun;
    }

    /**
     * Вспомогательный метод чтоб обернуть функцию в объект
     */
    public static Function<Long, AdGroup> createAdGroup(Function<Long, AdGroup> fun) {
        return fun;
    }

    /**
     * Вспомогательный метод чтоб обернуть функцию в объект
     */
    public static BiFunction<Long, Long, OldBanner> createBanner(BiFunction<Long, Long, OldBanner> fun) {
        return fun;
    }

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        Campaign campaign = campaignSupplier.apply(clientInfo);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        AdGroup adGroup = adGroupSupplier.apply(campaignInfo.getCampaignId());
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        OldBanner banner = bannerSupplier.apply(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId());
        AbstractBannerInfo bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        bannerId = bannerInfo.getBanner().getId();
    }

    @Test
    public void moderateTextBanner_success() {
        MassResult<Long> result = service.moderateBanners(clientInfo.getClientId(), clientInfo.getUid(),
                singletonList(bannerId));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(defect));
    }
}
