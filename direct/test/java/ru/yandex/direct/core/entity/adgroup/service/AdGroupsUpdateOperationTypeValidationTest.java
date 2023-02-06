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
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowedTypesCampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.FeedSteps;
import ru.yandex.direct.core.validation.defects.RightsDefects;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignSourceUtils.ALL_CAMPAIGN_SOURCES;
import static ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds.ALL;
import static ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds.MEDIAPLAN;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Проверяем что валидация обрабатывает случай обновления группы обновления одного типа
 * изменениями другого типа или попытку обновления неподдерживаемого типа группы объявления
 */
@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsUpdateOperationTypeValidationTest {
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private FeedSteps feedSteps;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    ClientGeoService clientGeoService;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Autowired
    private AdGroupsUpdateOperationFactory adGroupsUpdateOperationFactory;

    private GeoTree geoTree;

    private static <T extends AdGroup> List<ModelChanges<AdGroup>> newModelChanges(Long adGroupId, Class<T> clazz) {
        return singletonList(
                new ModelChanges<>(adGroupId, clazz)
                        .process(RandomStringUtils.randomAlphanumeric(10), AdGroup.NAME)
                        .castModelUp(AdGroup.class));
    }

    @Before
    public void setUp() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    private AdGroupsUpdateOperation createUpdateOperationFor(
            List<ModelChanges<AdGroup>> modelChangesList,
            AdGroupInfo adGroupInfo) {
        return adGroupsUpdateOperationFactory.newInstance(
                Applicability.FULL,
                modelChangesList,
                AdGroupUpdateOperationParams.builder()
                        .withModerationMode(ModerationMode.DEFAULT)
                        .withValidateInterconnections(true)
                        .build(),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                adGroupInfo.getUid(),
                adGroupInfo.getClientId(),
                adGroupInfo.getShard());
    }

    @Test
    public void testUntypeAdGroupExpectSuccess() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();

        MassResult<Long> result = createUpdateOperationFor(
                newModelChanges(adGroupInfo.getAdGroupId(), AdGroup.class),
                adGroupInfo).prepareAndApply();

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void testUnwritableAdGroupTypeExpectError() {
        requestAccessibleCampaignTypes.setCustom(new AllowedTypesCampaignAccessibilityChecker(MEDIAPLAN, ALL, ALL_CAMPAIGN_SOURCES, ALL_CAMPAIGN_SOURCES));
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveDynamicTextAdGroup();

        MassResult<Long> result = createUpdateOperationFor(
                newModelChanges(adGroupInfo.getAdGroupId(), DynamicTextAdGroup.class),
                adGroupInfo)
                .prepareAndApply();

        assertThat(
                result.getValidationResult(),
                hasDefectWithDefinition(
                        validationError(path(index(0)), RightsDefects.noRights())));
    }

    @Test
    public void testDifferentAdGroupTypeExpectError() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();

        MassResult<Long> result = createUpdateOperationFor(
                newModelChanges(adGroupInfo.getAdGroupId(), DynamicTextAdGroup.class),
                adGroupInfo).prepareAndApply();

        assertThat(
                result.getValidationResult(),
                hasDefectWithDefinition(
                        validationError(path(index(0)), AdGroupDefects.inconsistentAdGroupType())));
    }

    @Test
    public void testUpdateDynamicTextByDynamicFeed() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveDynamicTextAdGroup();

        MassResult<Long> result = createUpdateOperationFor(
                newModelChanges(adGroupInfo.getAdGroupId(), DynamicFeedAdGroup.class),
                adGroupInfo).prepareAndApply();

        assertThat(
                result.getValidationResult(),
                hasDefectWithDefinition(
                        validationError(path(index(0)), AdGroupDefects.inconsistentDynamicAdGroupType())));
    }

    @Test
    public void testUpdateDynamicFeedByDynamicText() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveDynamicFeedAdGroup(
                feedSteps.createDefaultFeed());

        MassResult<Long> result = createUpdateOperationFor(
                newModelChanges(adGroupInfo.getAdGroupId(), DynamicTextAdGroup.class),
                adGroupInfo).prepareAndApply();

        assertThat(
                result.getValidationResult(),
                hasDefectWithDefinition(
                        validationError(path(index(0)), AdGroupDefects.inconsistentDynamicAdGroupType())));
    }
}
