package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.inconsistentGeoWithBannerLanguages;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.queryrec.model.Language.KAZAKH;
import static ru.yandex.direct.queryrec.model.Language.UKRAINIAN;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsUpdateOperationLanguageGeoValidationTest {
    private static final String UKRAINE_TITLE = "українська мова";
    private static final String UKRAINE_TITLE_EXT = "мова";
    private static final String UKRAINE_BODY = "купити машину";

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private AdGroupsUpdateOperationFactory adGroupsUpdateOperationFactory;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    private TextBannerInfo bannerInfo;

    @Before
    public void setUp() {
        bannerInfo = createBannerWithUkrainianText(Language.NO);
    }

    private AdGroupsUpdateOperation createUpdateOperation(List<ModelChanges<AdGroup>> modelChangesList,
                                                          boolean validateInterconnections) {
        AdGroupUpdateOperationParams operationParams = AdGroupUpdateOperationParams.builder()
                .withModerationMode(ModerationMode.DEFAULT)
                .withValidateInterconnections(validateInterconnections)
                .build();
        return adGroupsUpdateOperationFactory.newInstance(
                Applicability.FULL,
                modelChangesList,
                operationParams,
                geoTreeFactory.getGlobalGeoTree(),
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                bannerInfo.getUid(),
                bannerInfo.getClientId(),
                bannerInfo.getShard());
    }

    @Test
    public void validate_UkraineBannerInUkrainianRegion_GeoIsValid() {
        Long zaporozhyeRegionId = 960L;

        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(
                bannerInfo.getAdGroupInfo().getAdGroupId(), AdGroup.class)
                .process(singletonList(zaporozhyeRegionId), AdGroup.GEO);

        MassResult<Long> result = createUpdateOperation(singletonList(modelChanges), true)
                .prepareAndApply();

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void validate_UkraineBannerInRussianRegion_GeoInvalid() {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(
                bannerInfo.getAdGroupInfo().getAdGroupId(), TextAdGroup.class)
                .process(singletonList(RUSSIA_REGION_ID), AdGroup.GEO)
                .castModelUp(AdGroup.class);

        MassResult<Long> result = createUpdateOperation(singletonList(modelChanges), true)
                .prepareAndApply();

        assertThat(
                result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(
                                path(index(0), field(AdGroup.GEO.name())),
                                inconsistentGeoWithBannerLanguages(UKRAINIAN, bannerInfo.getBannerId()))));
    }

    @Test
    public void validate_UkraineBannerInRussianRegionAndValidationTurnedOff_NoErrors() {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(
                bannerInfo.getAdGroupInfo().getAdGroupId(), TextAdGroup.class)
                .process(singletonList(RUSSIA_REGION_ID), AdGroup.GEO)
                .castModelUp(AdGroup.class);

        MassResult<Long> result = createUpdateOperation(singletonList(modelChanges), false)
                .prepareAndApply();

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void validate_BannerWithKazakhLanguageAndUkrText_RegionUkraine_GeoInvalid() {
        bannerInfo = createBannerWithUkrainianText(Language.KK);

        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(
                bannerInfo.getAdGroupInfo().getAdGroupId(), TextAdGroup.class)
                .process(singletonList(UKRAINE_REGION_ID), AdGroup.GEO)
                .castModelUp(AdGroup.class);

        MassResult<Long> result = createUpdateOperation(singletonList(modelChanges), true)
                .prepareAndApply();

        assertThat(
                result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(
                                path(index(0), field(AdGroup.GEO.name())),
                                inconsistentGeoWithBannerLanguages(KAZAKH, bannerInfo.getBannerId()))));
    }

    @Test
    public void validate_BannerWithRussianLanguageAndUkrText_RegionRussia_GeoIsValid() {
        bannerInfo = createBannerWithUkrainianText(Language.RU_);

        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(
                bannerInfo.getAdGroupInfo().getAdGroupId(), TextAdGroup.class)
                .process(singletonList(RUSSIA_REGION_ID), AdGroup.GEO)
                .castModelUp(AdGroup.class);

        MassResult<Long> result = createUpdateOperation(singletonList(modelChanges), true)
                .prepareAndApply();

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    private TextBannerInfo createBannerWithUkrainianText(Language language) {
        return bannerSteps.createBanner(
                activeTextBanner()
                        .withTitle(UKRAINE_TITLE)
                        .withTitleExtension(UKRAINE_TITLE_EXT)
                        .withLanguage(language)
                        .withBody(UKRAINE_BODY));
    }
}
