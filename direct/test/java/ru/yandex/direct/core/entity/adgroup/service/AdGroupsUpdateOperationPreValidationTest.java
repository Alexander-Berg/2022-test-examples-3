package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.bstags.AdGroupBsTagsSettingsProvider;
import ru.yandex.direct.core.entity.adgroup.service.geotree.AdGroupGeoTreeProviderFactory;
import ru.yandex.direct.core.entity.adgroup.service.update.AdGroupUpdateServices;
import ru.yandex.direct.core.entity.adgroup.service.validation.UpdateAdGroupValidationService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.entity.minuskeywordspack.service.AddMinusKeywordsPackSubOperationFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Проверяем pre-валидацию для AdGroupUpdateOperation
 */
@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsUpdateOperationPreValidationTest {
    private static final MinusPhraseValidator.ValidationMode MINUS_PHRASE_VALIDATION_MODE =
            MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private UpdateAdGroupValidationService updateAdGroupValidationService;

    @Autowired
    private AdGroupUpdateServices adGroupUpdateServices;

    @Autowired
    private ClientGeoService clientGeoService;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private AdGroupGeoTreeProviderFactory geoTreeProviderFactory;

    @Autowired
    private AdGroupBsTagsSettingsProvider adGroupBsTagsSettingsProvider;

    @Autowired
    private AddMinusKeywordsPackSubOperationFactory addMinusKeywordsPackSubOperationFactory;

    @Autowired
    private HyperGeoRepository hyperGeoRepository;

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Autowired
    private AdGroupOperationsHelper adGroupOperationsHelper;

    private GeoTree geoTree;
    private AdGroupInfo adGroup;
    private List<ModelChanges<AdGroup>> modelChangesList;

    @Before
    public void setUp() {
        updateAdGroupValidationService = spy(updateAdGroupValidationService);

        geoTree = geoTreeFactory.getGlobalGeoTree();

        adGroup = adGroupSteps.createActiveTextAdGroup();

        modelChangesList = singletonList(
                new ModelChanges<>(adGroup.getAdGroupId(), AdGroup.class)
                        .process(RandomStringUtils.randomAlphabetic(10), AdGroup.NAME));
    }

    private AdGroupsUpdateOperation createUpdateOperation(List<ModelChanges<AdGroup>> modelChangesList) {
        return new AdGroupsUpdateOperation(
                Applicability.FULL,
                modelChangesList,
                AdGroupUpdateOperationParams.builder()
                        .withModerationMode(ModerationMode.DEFAULT)
                        .withValidateInterconnections(true)
                        .build(),
                campaignRepository,
                adGroupRepository,
                hyperGeoRepository,
                cryptaSegmentRepository,
                updateAdGroupValidationService,
                geoTree,
                geoTreeProviderFactory,
                clientGeoService,
                adGroupUpdateServices,
                adGroupOperationsHelper,
                adGroupBsTagsSettingsProvider,
                addMinusKeywordsPackSubOperationFactory,
                MINUS_PHRASE_VALIDATION_MODE,
                adGroup.getUid(),
                adGroup.getClientId(),
                adGroup.getShard());
    }

    @Test
    public void testPreValidationSuccess() {
        doReturn(ValidationResult.success(modelChangesList)).when(updateAdGroupValidationService)
                .preValidate(any(), any(), any(), any(), any(), anyInt());

        MassResult<Long> result = createUpdateOperation(modelChangesList).prepareAndApply();

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void testPreValidationFailed() {
        ValidationResult<List<ModelChanges<AdGroup>>, Defect> expectedVr = new ValidationResult<>(
                modelChangesList);
        expectedVr.getOrCreateSubValidationResult(index(0), modelChangesList.get(0))
                .addError(invalidValue());
        doReturn(expectedVr).when(updateAdGroupValidationService)
                .preValidate(any(), any(), any(), any(), any(), anyInt());
        MassResult<Long> result = createUpdateOperation(modelChangesList).prepareAndApply();

        assertThat(
                result.getValidationResult(),
                hasDefectWithDefinition(
                        validationError(
                                path(index(0)), invalidValue())));
    }
}
