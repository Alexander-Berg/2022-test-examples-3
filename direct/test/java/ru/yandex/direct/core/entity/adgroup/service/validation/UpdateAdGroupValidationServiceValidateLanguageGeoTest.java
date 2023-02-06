package ru.yandex.direct.core.entity.adgroup.service.validation;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.bstags.AdGroupBsTagsSettingsProvider;
import ru.yandex.direct.core.entity.adgroup.service.validation.types.AdGroupTypeSpecificValidationProvider;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.queryrec.model.Language;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.inconsistentGeoWithBannerLanguages;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(SpringRunner.class)
@CoreTest
public class UpdateAdGroupValidationServiceValidateLanguageGeoTest {
    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory;

    @Autowired
    private AdGroupValidationService commonValidationService;

    @Autowired
    private AdGroupTypeSpecificValidationProvider typeSpecificValidationProvider;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private MinusKeywordsPackRepository packRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private AdGroupLanguageGeoValidator languageGeoValidator;

    @Autowired
    private AdGroupBsTagsSettingsProvider adGroupBsTagsSettingsProvider;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ClientRepository clientRepository;

    private UpdateAdGroupValidationService validationService;

    private TextBannerInfo bannerInfo;

    @Before
    public void setUp() {
        bannerInfo = bannerSteps.createActiveTextBanner();
        languageGeoValidator = spy(languageGeoValidator);
        validationService = new UpdateAdGroupValidationService(
                campaignSubObjectAccessCheckerFactory,
                commonValidationService,
                typeSpecificValidationProvider,
                adGroupRepository,
                tagRepository,
                packRepository, languageGeoValidator, featureService, clientRepository);
    }

    @Test
    public void testSuccess() {
        when(languageGeoValidator.createConstraint(any(), any(), any(), any()))
                .thenReturn(adGroup -> null);

        ValidationResult<List<AdGroup>, Defect> vr = validate(bannerInfo);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testFail() {
        Defect expectedError = inconsistentGeoWithBannerLanguages(Language.RUSSIAN, 1L);

        when(languageGeoValidator.createConstraint(any(), any(), any(), any()))
                .thenReturn(adGroup -> expectedError);

        ValidationResult<List<AdGroup>, Defect> vr = validate(bannerInfo);

        assertThat(
                vr,
                hasDefectDefinitionWith(
                        validationError(path(index(0), field(AdGroup.GEO.name())), expectedError)));
    }

    private ValidationResult<List<AdGroup>, Defect> validate(TextBannerInfo bannerInfo) {
        var adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        var adGroupsBsTagsSettings = adGroupBsTagsSettingsProvider.getAdGroupBsTagsSettings(List.of(adGroup),
                bannerInfo.getClientId());
        var preValidationResult = ValidationResult.<List<AdGroup>, Defect>success(singletonList(adGroup));
        var appliedChanges = Map.of(0, new ModelChanges<>(adGroup.getId(), AdGroup.class).applyTo(adGroup));
        return validationService.validate(bannerInfo.getShard(), bannerInfo.getClientId(), bannerInfo.getUid(),
                preValidationResult, appliedChanges, adGroupsBsTagsSettings,
                singleton(bannerInfo.getAdGroupId()), true);
    }
}
