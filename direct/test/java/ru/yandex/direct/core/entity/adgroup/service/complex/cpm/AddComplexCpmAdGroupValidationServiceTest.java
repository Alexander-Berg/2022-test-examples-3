package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobile;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobileAndDemographics;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmOutdoorAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.emptyAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.bidModifiersNotAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.eitherKeywordsOrRetargetingsAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.keywordsNotAllowed;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AddComplexCpmAdGroupValidationServiceTest {

    @Autowired
    private AddComplexCpmAdGroupValidationService addValidationService;

    @Autowired
    private ClientSteps clientSteps;

    private ClientId clientId;

    @Before
    public void setUp() {
        ClientInfo defaultClient = clientSteps.createDefaultClient();
        clientId = defaultClient.getClientId();
    }

    @Test
    public void validateAdGroups_TypeIsNotApplicable() {
        AdGroup adGroup = activeTextAdGroup();
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(adGroup);
        List<AdGroup> adGroups = singletonList(adGroup);
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.TYPE)), adGroupTypeNotSupported())));
    }

    @Test
    public void checkThatBannerValidationCalled() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withBanners(singletonList(fullTextBanner()));
        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);
        Path errPath = path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0));
        assertThat("баннер валидируется", vr, hasDefectDefinitionWith(
                validationError(errPath, inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void validateAdGroups_AddKeywordsAndRetargetings() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withKeywords(singletonList(keywordForCpmBanner()))
                .withTargetInterests(singletonList(defaultTargetInterest()));

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat("в группу нельзя добавить и ключевые фразы, и ретаргетинги", vr, hasDefectDefinitionWith(
                validationError(path(index(0)), eitherKeywordsOrRetargetingsAllowed())));
    }

    @Test
    public void validateAdGroups_AddKeywordsToCpmVideo() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(activeCpmVideoAdGroup(1L))
                .withKeywords(singletonList(keywordForCpmBanner()));

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

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
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat("в CPM_VIDEO группу нельзя добавить ключевые фразы", vr, hasDefectDefinitionWith(
                validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void validateAdGroups_AddMobileBidModifiersWithoutKeywordsToCpmBanner() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withTargetInterests(singletonList(defaultTargetInterest()))
                .withComplexBidModifier(randomComplexBidModifierMobile());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat("группа без ключевых фраз проходит валидацию с корректировками на устройство", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_AddBidModifiersWithoutKeywordsToCpmBanner() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withTargetInterests(singletonList(defaultTargetInterest()))
                .withComplexBidModifier(randomComplexBidModifierMobileAndDemographics());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat("в группу без ключевых фраз нельзя добавить корректировки", vr, hasDefectDefinitionWith(
                validationError(path(index(0)), bidModifiersNotAllowed())));
    }

    @Test
    public void validateAdGroups_EmptyBidModifiersWithoutKeywordsToCpmBanner() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withTargetInterests(singletonList(defaultTargetInterest()))
                .withComplexBidModifier(new ComplexBidModifier());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat("группа без ключевых фраз проходит валидацию если корректировки пустые", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_AddBidModifiersWithoutKeywordsToCpmVideo() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(activeCpmVideoAdGroup(1L))
                .withTargetInterests(singletonList(defaultTargetInterest()))
                .withComplexBidModifier(randomComplexBidModifierMobile());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat("в группу cpm_video можно добавлять корректировки", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_CpmBannerAdGroupWithKeywordsAndBidModifiers_Successful() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(1L)
                .withKeywords(singletonList(keywordForCpmBanner()))
                .withComplexBidModifier(randomComplexBidModifierMobile());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat("проходит валидацию cpm_banner группа с фразами и корректировками", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_FullCpmOutdoor_Successful() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmOutdoorAdGroup(1L, null, null, null);

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_CpmOutdoorWithKeywords_Error() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmOutdoorAdGroup(1L, null, null, null)
                .withKeywords(singletonList(keywordForCpmBanner()));

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void validateAdGroups_CpmOutdoorWithBidModifiers_Error() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmOutdoorAdGroup(1L, null, null, null)
                .withComplexBidModifier(randomComplexBidModifierMobile());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), bidModifiersNotAllowed())));
    }

    @Test
    public void validateAdGroups_FullCpmYndxFrontpage_Successful() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmYndxFrontpageAdGroup(1L, null);

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_CpmYndxFrontpageWithKeywords_Error() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmYndxFrontpageAdGroup(1L, null)
                .withKeywords(singletonList(keywordForCpmBanner()));

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void validateAdGroups_CpmYndxFrontpageWithBidModifiers_Error() {
        ComplexCpmAdGroup complexCpmAdGroup = cpmYndxFrontpageAdGroup(1L, null)
                .withComplexBidModifier(randomComplexBidModifierMobile());

        List<AdGroup> adGroups = singletonList(complexCpmAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexCpmAdGroup),
                        clientId);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), bidModifiersNotAllowed())));
    }
}
