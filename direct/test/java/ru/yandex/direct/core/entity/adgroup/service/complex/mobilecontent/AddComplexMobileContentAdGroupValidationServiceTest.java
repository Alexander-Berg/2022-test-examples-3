package ru.yandex.direct.core.entity.adgroup.service.complex.mobilecontent;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexMobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.CURRENCY;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierDemographics;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.convertRetargetingsToTargetInterests;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestRelevanceMatches.defaultRelevanceMatch;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AddComplexMobileContentAdGroupValidationServiceTest {

    @Autowired
    private AddComplexMobileContentAdGroupValidationService addValidationService;
    @Autowired
    private Steps steps;
    private ClientId clientId;

    @Before
    public void before() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        clientId = client.getClientId();
    }

    @Test
    public void validateAdGroups_TypeIsNotApplicable() {
        AdGroup adGroup = activeTextAdGroup();
        ComplexMobileContentAdGroup complexMobileContentAdGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(adGroup);
        List<AdGroup> adGroups = singletonList(adGroup);
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMobileContentAdGroup),
                        clientId);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.TYPE)), adGroupTypeNotSupported())));
    }

    @Test
    public void checkThatBannerValidationCalled() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner(1L, null));
        ComplexMobileContentAdGroup complexMobileContentAdGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(activeMobileAppAdGroup(1L))
                .withComplexBanners(singletonList(complexBanner));
        List<AdGroup> adGroups = singletonList(complexMobileContentAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMobileContentAdGroup),
                        clientId);
        Path errPath = path(index(0), field(ComplexMobileContentAdGroup.COMPLEX_BANNERS), index(0));
        assertThat("баннер валидируется", vr, hasDefectDefinitionWith(
                validationError(errPath, inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void validateAdGroups_AddKeywords() {
        ComplexMobileContentAdGroup complexMobileContentAdGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(activeMobileAppAdGroup(1L))
                .withKeywords(singletonList(defaultKeyword()));

        List<AdGroup> adGroups = singletonList(complexMobileContentAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMobileContentAdGroup),
                        clientId);

        assertThat("в группу можно добавить ключевые фразы", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_AddMobileBidModifiersWithoutKeywordsToBanner() {
        ComplexMobileContentAdGroup complexMobileContentAdGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(activeMobileAppAdGroup(1L))
                .withComplexBidModifier(randomComplexBidModifierDemographics());

        List<AdGroup> adGroups = singletonList(complexMobileContentAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMobileContentAdGroup),
                        clientId);

        assertThat("группа без ключевых фраз проходит валидацию с корректировками ", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_EmptyBidModifiersWithoutKeywordsToBanner() {
        ComplexMobileContentAdGroup complexMobileContentAdGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(activeMobileAppAdGroup(1L))
                .withComplexBidModifier(new ComplexBidModifier());

        List<AdGroup> adGroups = singletonList(complexMobileContentAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMobileContentAdGroup),
                        clientId);

        assertThat("группа без ключевых фраз проходит валидацию если корректировки пустые", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_AdGroupWithKeywordsAndBidModifiers_Successful() {
        ComplexMobileContentAdGroup complexMobileContentAdGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(activeMobileAppAdGroup(1L))
                .withKeywords(singletonList(defaultKeyword()))
                .withComplexBidModifier(randomComplexBidModifierDemographics());

        List<AdGroup> adGroups = singletonList(complexMobileContentAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMobileContentAdGroup),
                        clientId);

        assertThat("проходит валидацию mobile_content группа с фразами и корректировками", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void oneAdGroupWithRelevanceMatch() {
        ComplexMobileContentAdGroup adGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(activeMobileAppAdGroup(1L))
                .withRelevanceMatches(singletonList(defaultRelevanceMatch()));

        List<AdGroup> adGroups = singletonList(adGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(adGroup),
                        clientId);

        assertThat("проходит валидацию mobile_content группа с автотаргетингом", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void oneAdGroupWithRetargetings() {
        Retargeting retargeting = defaultRetargeting(null, null, 1L)
                .withPriceContext(BigDecimal.valueOf(nextLong(100, CURRENCY.getMaxPrice().longValue())));

        ComplexMobileContentAdGroup adGroup = new ComplexMobileContentAdGroup()
                .withAdGroup(activeMobileAppAdGroup(1L))
                .withTargetInterests(convertRetargetingsToTargetInterests(singletonList(retargeting), emptyList()));

        List<AdGroup> adGroups = singletonList(adGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(adGroup),
                        clientId);

        assertThat("проходит валидацию mobile_content группа с автотаргетингом", vr,
                hasNoDefectsDefinitions());
    }

}
