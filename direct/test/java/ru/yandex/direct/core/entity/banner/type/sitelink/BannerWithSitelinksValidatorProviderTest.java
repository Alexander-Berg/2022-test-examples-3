package ru.yandex.direct.core.entity.banner.type.sitelink;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithSitelinks;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.Helpers;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.sitelinksSetNotFound;
import static ru.yandex.direct.core.testing.data.TestCampaigns.contextAverageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.dafaultAverageCpaPayForConversionStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelinkWithTurbolandingId;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithSitelinksValidatorProviderTest {

    @Autowired
    public BannerWithSitelinksValidatorProvider provider;

    @Autowired
    public Helpers bannerHelpers;

    @Autowired
    public Steps steps;

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private SitelinkSetInfo sitelinkSetInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
    }

    @Test
    public void validBannerWithSitelinks() {
        BannerWithSitelinks banner = clientBannerWithSitelinks();
        ValidationResult<List<BannerWithSitelinks>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void nonExistingSitelinkSet() {
        BannerWithSitelinks banner = clientBannerWithSitelinks()
                .withSitelinksSetId(Long.MAX_VALUE);
        ValidationResult<List<BannerWithSitelinks>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(BannerWithSitelinks.SITELINKS_SET_ID)),
                sitelinksSetNotFound()))));
    }

    @Test
    public void textBannerWithPayForConversionStrategyAndTurboSitelink() {
        SitelinkSetInfo sitelinkSetInfo = createTurboSitelinkSet();
        var campaign = createAverageCpaPayForConversionCampaign();
        TextAdGroup adGroup = createTextAdGroup(campaign);

        BannerWithSitelinks banner = clientTextBanner()
                .withCampaignId(campaign.getId())
                .withAdGroupId(adGroup.getId())
                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());
        ValidationResult<List<BannerWithSitelinks>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void textBannerWithNonPayForConversionStrategyAndNonTurboSitelink() {
        SitelinkSetInfo sitelinkSetInfo = createNonTurboSitelinkSet();
        var campaign = createNonAverageCpaPayForConversionCampaign();
        TextAdGroup adGroup = createTextAdGroup(campaign);

        BannerWithSitelinks banner = clientTextBanner()
                .withCampaignId(campaign.getId())
                .withAdGroupId(adGroup.getId())
                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());
        ValidationResult<List<BannerWithSitelinks>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void textBannerWithPayForConversionStrategyAndNonTurboSitelinkAndFeatureEnabled() {
        SitelinkSetInfo sitelinkSetInfo = createNonTurboSitelinkSet();
        var campaign = createAverageCpaPayForConversionCampaign();
        TextAdGroup adGroup = createTextAdGroup(campaign);

        BannerWithSitelinks banner = clientTextBanner()
                .withCampaignId(campaign.getId())
                .withAdGroupId(adGroup.getId())
                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());
        ValidationResult<List<BannerWithSitelinks>, Defect> result = validate(banner);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    private BannerWithSitelinks clientBannerWithSitelinks() {
        return new DynamicBanner()
                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());
    }

    private TextBanner clientTextBanner() {
        return new TextBanner()
                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());
    }

    private SitelinkSetInfo createTurboSitelinkSet() {
        return steps.sitelinkSetSteps().createSitelinkSet(defaultSitelinkSet()
                .withSitelinks(List.of(defaultSitelinkWithTurbolandingId())), clientInfo);
    }

    private SitelinkSetInfo createNonTurboSitelinkSet() {
        return steps.sitelinkSetSteps().createSitelinkSet(defaultSitelinkSet()
                .withSitelinks(List.of(defaultSitelink())), clientInfo);
    }

    private TextCampaignInfo createAverageCpaPayForConversionCampaign() {
        return steps.textCampaignSteps().createCampaign(clientInfo,
                defaultTextCampaignWithSystemFields(clientInfo)
                        .withStrategy(dafaultAverageCpaPayForConversionStrategy()));
    }

    private TextCampaignInfo createNonAverageCpaPayForConversionCampaign() {
        return steps.textCampaignSteps().createCampaign(clientInfo,
                defaultTextCampaignWithSystemFields(clientInfo)
                        .withStrategy(contextAverageClickStrategy()));
    }

    private TextAdGroup createTextAdGroup(TextCampaignInfo campaign) {
        TextAdGroup adGroup = activeTextAdGroup(campaign.getId());
        steps.adGroupSteps().createAdGroupRaw(adGroup, clientInfo);
        return adGroup;
    }

    private ValidationResult<List<BannerWithSitelinks>, Defect> validate(BannerWithSitelinks banner) {
        return validate(List.of(banner));
    }

    private ValidationResult<List<BannerWithSitelinks>, Defect> validate(List<BannerWithSitelinks> banners) {
        BannersAddOperationContainer validationContainer = bannerHelpers.createValidationContainer(clientInfo, banners);
        return ListValidationBuilder.<BannerWithSitelinks, Defect>of(banners)
                .checkEachBy(provider.bannerWithSitelinksValidator(validationContainer))
                .getResult();
    }
}
