package ru.yandex.direct.core.entity.adgroup.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateTrackingParamsTest {
    private static final String TRACKING_PARAMS = "param=tracking";
    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private AdGroupRepository repository;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private AdGroupsUpdateOperationFactory adGroupsUpdateOperationFactory;

    private GeoTree geoTree;
    private CampaignInfo campaignInfo;

    @Before
    public void setUp() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        campaignInfo = campaignSteps.createDefaultCampaign();

    }

    @Test
    public void updateFromNullToValue_valueIsAdded() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withTrackingParams(null);
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(adGroup, campaignInfo);

        ModelChanges<AdGroup> change = new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class);
        change.process(TRACKING_PARAMS, AdGroup.TRACKING_PARAMS);

        AdGroupsUpdateOperation op = operation(change);
        MassResult<Long> result = op.prepareAndApply();
        assertThat(result.getErrors()).isEmpty();

        AdGroup actualAdGroup = repository.getAdGroups(adGroupInfo.getShard(),
                singletonList(adGroupInfo.getAdGroupId()))
                .get(0);
        assertThat(actualAdGroup.getTrackingParams()).isEqualTo(TRACKING_PARAMS);
    }

    @Test
    public void updateFromValueToValue_valueIsUpdated() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withTrackingParams(TRACKING_PARAMS);
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(adGroup, campaignInfo);

        ModelChanges<AdGroup> change = new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class);
        change.process(TRACKING_PARAMS + "new", AdGroup.TRACKING_PARAMS);

        AdGroupsUpdateOperation op = operation(change);
        MassResult<Long> result = op.prepareAndApply();
        assertThat(result.getErrors()).isEmpty();

        AdGroup actualAdGroup = repository.getAdGroups(adGroupInfo.getShard(),
                singletonList(adGroupInfo.getAdGroupId()))
                .get(0);
        assertThat(actualAdGroup.getTrackingParams()).isEqualTo(TRACKING_PARAMS + "new");
    }

    @Test
    public void updateFromValueToNull_valueIsUpdated() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withTrackingParams(TRACKING_PARAMS);
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(adGroup, campaignInfo);

        ModelChanges<AdGroup> change = new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class);
        change.process(null, AdGroup.TRACKING_PARAMS);

        AdGroupsUpdateOperation op = operation(change);
        MassResult<Long> result = op.prepareAndApply();
        assertThat(result.getErrors()).isEmpty();

        AdGroup actualAdGroup = repository.getAdGroups(adGroupInfo.getShard(),
                singletonList(adGroupInfo.getAdGroupId()))
                .get(0);
        assertThat(actualAdGroup.getTrackingParams()).isEqualTo(null);
    }

    @Test
    public void updateFromValueUpdatedToNullToValue_valueIsUpdated() {
        // Тестируем, что null в существующей записи group_params обновляется корректно
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withTrackingParams(TRACKING_PARAMS);
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(adGroup, campaignInfo);

        ModelChanges<AdGroup> changeBefore = new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class);
        changeBefore.process(null, AdGroup.TRACKING_PARAMS);

        AdGroupsUpdateOperation opBefore = operation(changeBefore);
        MassResult<Long> resultBefore = opBefore.prepareAndApply();
        assertThat(resultBefore.getErrors()).isEmpty();

        ModelChanges<AdGroup> change = new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class);
        change.process(TRACKING_PARAMS, AdGroup.TRACKING_PARAMS);

        AdGroupsUpdateOperation op = operation(change);
        MassResult<Long> result = op.prepareAndApply();
        assertThat(result.getErrors()).isEmpty();

        AdGroup actualAdGroup = repository.getAdGroups(adGroupInfo.getShard(),
                singletonList(adGroupInfo.getAdGroupId()))
                .get(0);
        assertThat(actualAdGroup.getTrackingParams()).isEqualTo(TRACKING_PARAMS);
    }

    private AdGroupsUpdateOperation operation(ModelChanges<AdGroup> change) {
        return adGroupsUpdateOperationFactory.newInstance(
                Applicability.FULL,
                singletonList(change),
                AdGroupUpdateOperationParams.builder()
                        .withModerationMode(ModerationMode.DEFAULT)
                        .withValidateInterconnections(true)
                        .build(),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                campaignInfo.getUid(),
                campaignInfo.getClientId(),
                campaignInfo.getShard());
    }
}
