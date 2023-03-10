package ru.yandex.direct.core.entity.adgroup.service.complex.mcbanner;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexMcBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierDemographics;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AddComplexMcBannerAdGroupValidationServiceTest {

    @Autowired
    private AddComplexMcBannerAdGroupValidationService addValidationService;
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
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(adGroup);
        List<AdGroup> adGroups = singletonList(adGroup);
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMcBannerAdGroup),
                        clientId);
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.TYPE)), adGroupTypeNotSupported())));
    }

    @Test
    public void checkThatBannerValidationCalled() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner(1L, null));
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(1L))
                .withComplexBanners(singletonList(complexBanner));
        List<AdGroup> adGroups = singletonList(complexMcBannerAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMcBannerAdGroup),
                        clientId);
        Path errPath = path(index(0), field(ComplexMcBannerAdGroup.COMPLEX_BANNERS), index(0));
        assertThat("???????????? ????????????????????????", vr, hasDefectDefinitionWith(
                validationError(errPath, inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void validateAdGroups_AddKeywords() {
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(1L))
                .withKeywords(singletonList(defaultKeyword()));

        List<AdGroup> adGroups = singletonList(complexMcBannerAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMcBannerAdGroup),
                        clientId);

        assertThat("?? ???????????? ?????????? ???????????????? ???????????????? ??????????", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_AddMobileBidModifiersWithoutKeywordsToMcBannerBanner() {
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(1L))
                .withComplexBidModifier(randomComplexBidModifierDemographics());

        List<AdGroup> adGroups = singletonList(complexMcBannerAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMcBannerAdGroup),
                        clientId);

        assertThat("???????????? ?????? ???????????????? ???????? ???????????????? ?????????????????? ?? ?????????????????????????????? ", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_EmptyBidModifiersWithoutKeywordsToMcBannerBanner() {
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(1L))
                .withComplexBidModifier(new ComplexBidModifier());

        List<AdGroup> adGroups = singletonList(complexMcBannerAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMcBannerAdGroup),
                        clientId);

        assertThat("???????????? ?????? ???????????????? ???????? ???????????????? ?????????????????? ???????? ?????????????????????????? ????????????", vr,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroups_McBannerAdGroupWithKeywordsAndBidModifiers_Successful() {
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(1L))
                .withKeywords(singletonList(defaultKeyword()))
                .withComplexBidModifier(randomComplexBidModifierDemographics());

        List<AdGroup> adGroups = singletonList(complexMcBannerAdGroup.getAdGroup());
        ValidationResult<List<AdGroup>, Defect> vr = addValidationService
                .validateAdGroups(ValidationResult.success(adGroups), singletonList(complexMcBannerAdGroup),
                        clientId);

        assertThat("???????????????? ?????????????????? mc_banner ???????????? ?? ?????????????? ?? ??????????????????????????????", vr,
                hasNoDefectsDefinitions());
    }
}
