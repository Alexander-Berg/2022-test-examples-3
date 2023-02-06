package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobile;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobileAndDemographics;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmOutdoorAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.emptyAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupDoesNotContainThisKeyword;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.bidModifiersNotAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.eitherKeywordsOrRetargetingsAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.keywordsNotAllowed;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdateComplexCpmAdGroupValidationServiceTest {


    @Autowired
    private UpdateComplexCpmAdGroupValidationService updateValidationService;
    @Autowired
    private Steps steps;

    private ClientId clientId;
    private Long campaignId;
    private ComplexCpmAdGroup complexCpmAdGroup;
    private ValidationResult<List<AdGroup>, Defect> adGroupResult;


    @Before
    public void before() {
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        campaignId = adGroup.getCampaignId();
        clientId = adGroup.getClientId();

        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(adGroup.getClientInfo());
        Long creativeId = creativeInfo.getCreativeId();

        var bannerInfo = steps.cpmBannerSteps().createCpmBanner(
                new NewCpmBannerInfo()
                        .withAdGroupInfo(adGroup)
                        .withBanner(fullCpmBanner(creativeId)));

        complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(adGroup.getAdGroup())
                .withBanners(singletonList(bannerInfo.getBanner()));
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        adGroupResult = ValidationResult.success(adGroups);
    }

    @Test
    public void validationSuccessfull() {
        complexCpmAdGroup.getAdGroup().setName("updated name");
        complexCpmAdGroup.withKeywords(singletonList(defaultKeyword()));

        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(adGroupResult, singletonList(complexCpmAdGroup), clientId);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void checkBannersValidationCalled() {
        List<BannerWithSystemFields> banners = IntStream.range(0, MAX_BANNERS_IN_ADGROUP).boxed()
                .map(i -> fullCpmBanner(campaignId, null, null))
                .collect(Collectors.toList());
        banners.addAll(complexCpmAdGroup.getBanners());
        complexCpmAdGroup.withBanners(banners);

        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(adGroupResult, singletonList(complexCpmAdGroup), clientId);

        assertThat("баннеры валидируются", vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS)),
                        maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
    }

    @Test
    public void checkKeywordsValidationCalled() {
        KeywordInfo defaultKeyword = steps.keywordSteps().createDefaultKeyword();
        complexCpmAdGroup.withKeywords(singletonList(defaultKeyword.getKeyword()));

        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(adGroupResult, singletonList(complexCpmAdGroup), clientId);

        assertThat("ключевые фразы валидируются", vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(ComplexCpmAdGroup.KEYWORDS), index(0)),
                        adGroupDoesNotContainThisKeyword())));
    }

    @Test
    public void validateAdGroups_AddKeywordsAndRetargetings() {
        complexCpmAdGroup
                .withKeywords(singletonList(keywordForCpmBanner()))
                .withTargetInterests(singletonList(defaultTargetInterest()));

        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(adGroupResult, singletonList(complexCpmAdGroup), clientId);

        assertThat("в группу нельзя добавить и ключевые фразы, и ретаргетинги", vr,
                hasDefectDefinitionWith(validationError(path(index(0)), eitherKeywordsOrRetargetingsAllowed())));
    }

    @Test
    public void validateAdGroups_AddKeywordsToCpmVideo() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(activeCpmVideoAdGroup(1L))
                .withKeywords(singletonList(keywordForCpmBanner()));

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat("в CPM_VIDEO группу нельзя добавить ключевые фразы", vr, hasDefectDefinitionWith(
                validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void validateAdGroups_AddKeywordsAndRetargetingsToCpmVideo() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(activeCpmVideoAdGroup(1L))
                .withKeywords(singletonList(keywordForCpmBanner()))
                .withTargetInterests(singletonList(defaultTargetInterest()));

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat("в CPM_VIDEO группу нельзя добавить ключевые фразы", vr, hasDefectDefinitionWith(
                validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void validateAdGroups_AddBidModifiersWithoutKeywordsToCpmBanner() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withTargetInterests(singletonList(defaultTargetInterest()))
                .withComplexBidModifier(randomComplexBidModifierMobileAndDemographics());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat("в группу без ключевых фраз нельзя добавить корректировки", vr, hasDefectDefinitionWith(
                validationError(path(index(0)), bidModifiersNotAllowed())));
    }

    @Test
    public void validateAdGroups_AddBidModifiersWithoutKeywordsToCpmVideo() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(activeCpmVideoAdGroup(1L))
                .withTargetInterests(singletonList(defaultTargetInterest()))
                .withComplexBidModifier(randomComplexBidModifierMobile());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat("в группу cpm_video без ключевых фраз можно добавлять корректировки", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_CpmBannerAdGroupWithKeywordsAndBidModifiers_Successfull() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withKeywords(singletonList(keywordForCpmBanner()))
                .withComplexBidModifier(randomComplexBidModifierMobile());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat("проходит валидацию cpm_banner группа с фразами и корректировками", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_FullCpmOutdoorAdGroup_Successful() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmOutdoorAdGroup(null, null, null, null);
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_CpmOutdoorAdGroupWithKewords_ValidationError() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmOutdoorAdGroup(null, null, null, null)
                .withKeywords(singletonList(keywordForCpmBanner()));
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void validateAdGroups_CpmOutdoorAdGroupWithBidModifiers_ValidationError() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmOutdoorAdGroup(null, null, null, null)
                .withComplexBidModifier(randomComplexBidModifierMobile());
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), bidModifiersNotAllowed())));
    }

    @Test
    public void validateAdGroups_FullCpmYndxFrontpageAdGroup_Successful() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmYndxFrontpageAdGroup(null, null);
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_CpmYndxFrontpageAdGroupWithKewords_ValidationError() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmYndxFrontpageAdGroup(null, null)
                .withKeywords(singletonList(keywordForCpmBanner()));
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void validateAdGroups_CpmYndxFrontpageAdGroupWithBidModifiers_ValidationError() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmYndxFrontpageAdGroup(null, null)
                .withComplexBidModifier(randomComplexBidModifierMobile());
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup), clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), bidModifiersNotAllowed())));
    }
}
