package ru.yandex.direct.core.entity.adgroup.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynSmartAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.PerformanceBannerSteps;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

public class AdGroupsUpdateOperationTestBase {

    protected static final CompareStrategy STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("lastChange"), newPath("minusKeywordsId"));
    protected static final String NEW_NAME = "такоевотновоеимечко " + randomAlphanumeric(5);
    protected static final List<Long> NEW_GEO = singletonList(7L);
    protected static final List<Long> NEW_PROJECT_PARAM_CONDITIONS = singletonList(121L);
    protected static final List<String> NEW_MINUS_KEYWORDS = singletonList("деревянный карлик " + randomNumeric(5));
    protected static final String NEW_FIELD_TO_USE_AS_NAME = "new_name_field";

    @Autowired
    protected AdGroupSteps adGroupSteps;

    @Autowired
    protected BannerSteps bannerSteps;

    @Autowired
    protected PlacementSteps placementSteps;

    @Autowired
    protected Steps steps;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected OldBannerRepository bannerRepository;

    @Autowired
    protected BannerRepository newBannerRepository;

    @Autowired
    protected PerformanceBannerSteps performanceBannerSteps;

    @Autowired
    protected TextBannerSteps textBannerSteps;

    @Autowired
    protected DslContextProvider dslContextProvider;

    @Autowired
    protected GeoTreeFactory geoTreeFactory;

    @Autowired
    protected CampaignModifyRepository campaignModifyRepository;

    @Autowired
    protected AdGroupsUpdateOperationFactory adGroupsUpdateOperationFactory;

    protected ClientInfo clientInfo;
    protected Campaign campaign;
    protected AdGroup adGroup1;
    protected AdGroup adGroup2;
    protected GeoTree geoTree;
    protected long operatorUid;
    protected ClientId clientId;
    protected int shard;

    @Before
    public void before() {
        LocalDateTime longTimeAgo = LocalDateTime.now().minusDays(2).withNano(0);

        campaign = TestCampaigns.activeTextCampaign(null, null)
                .withAutobudgetForecastDate(longTimeAgo);
        adGroup1 = activeTextAdGroup(null)
                .withLastChange(longTimeAgo);
        adGroup2 = activeTextAdGroup(null)
                .withLastChange(longTimeAgo);

        AdGroupInfo adGroupInfo1 = adGroupSteps.createAdGroup(adGroup1, new CampaignInfo().withCampaign(campaign));
        adGroupSteps.createAdGroup(adGroup2, adGroupInfo1.getCampaignInfo());

        geoTree = geoTreeFactory.getGlobalGeoTree();

        initClientData(adGroupInfo1.getClientInfo());
    }

    protected void initClientData(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        this.operatorUid = clientInfo.getUid();
        this.clientId = clientInfo.getClientId();
        this.shard = clientInfo.getShard();
    }

    protected <T extends AdGroup> Function<AdGroupInfo, ModelChanges<AdGroup>> modelChangesWith(
            Consumer<ModelChanges<T>> changer, Class<T> clazz) {
        return adGroup -> {
            ModelChanges<T> modelChanges =
                    new ModelChanges<>(adGroup.getAdGroupId(), clazz);
            changer.accept(modelChanges);
            checkState(modelChanges.isAnyPropChanged());
            return modelChanges.castModelUp(AdGroup.class);
        };
    }


    protected void updateAndAssumeResultIsSuccessful(Applicability applicability, ModelChanges<AdGroup> modelChanges) {
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(applicability, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assumeThat(result, isSuccessful(true));
    }

    protected AdGroupsUpdateOperation createUpdateOperation(Applicability applicability,
                                                            List<ModelChanges<AdGroup>> modelChangesList) {
        return createUpdateOperation(applicability, modelChangesList, operatorUid, clientId, shard);
    }

    protected AdGroupsUpdateOperation createUpdateOperation(Applicability applicability,
                                                            List<ModelChanges<AdGroup>> modelChangesList, AdGroupInfo adGroupInfo) {
        return createUpdateOperation(applicability, modelChangesList, adGroupInfo.getUid(),
                adGroupInfo.getClientId(), adGroupInfo.getShard());
    }

    protected AdGroupsUpdateOperation createUpdateOperation(Applicability applicability,
                                                            List<ModelChanges<AdGroup>> modelChangesList, long operatorUid,
                                                            ClientId clientId, int shard) {
        return createUpdateOperation(applicability, modelChangesList, ModerationMode.DEFAULT,
                operatorUid, clientId, shard);
    }

    protected AdGroupsUpdateOperation createUpdateOperation(Applicability applicability,
                                                            List<ModelChanges<AdGroup>> modelChangesList, ModerationMode moderationMode,
                                                            long operatorUid, ClientId clientId, int shard) {
        return adGroupsUpdateOperationFactory.newInstance(
                applicability,
                modelChangesList,
                AdGroupUpdateOperationParams.builder()
                        .withModerationMode(moderationMode)
                        .withValidateInterconnections(true)
                        .build(),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                operatorUid,
                clientId,
                shard);
    }

    protected ModelChanges<AdGroup> modelChangesWithValidName(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(NEW_NAME, AdGroup.NAME);
        return modelChanges;
    }

    protected ModelChanges<AdGroup> modelChangesWithValidName(AdGroup adGroup, Class modelType) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), modelType);
        modelChanges.process(NEW_NAME, AdGroup.NAME);
        return modelChanges;
    }

    protected ModelChanges<AdGroup> modelChangesWithGeo(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(NEW_GEO, AdGroup.GEO);
        return modelChanges;
    }

    protected ModelChanges<AdGroup> modelChangesWithProjectParamConditions(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(NEW_PROJECT_PARAM_CONDITIONS, AdGroup.PROJECT_PARAM_CONDITIONS);
        return modelChanges;
    }

    protected ModelChanges<AdGroup> modelChangesWithValidMinusKeywords(Long adGroupId) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroupId, AdGroup.class);
        modelChanges.process(NEW_MINUS_KEYWORDS, AdGroup.MINUS_KEYWORDS);
        return modelChanges;
    }

    protected ModelChanges<AdGroup> modelChangesWithValidMinusKeywords(Long adGroupId, Class modelType) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroupId, modelType);
        modelChanges.process(NEW_MINUS_KEYWORDS, AdGroup.MINUS_KEYWORDS);
        return modelChanges;
    }

    protected ModelChanges<AdGroup> modelChangesWithInvalidMinusKeywords(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(singletonList("[]"), AdGroup.MINUS_KEYWORDS);
        return modelChanges;
    }

    protected ModelChanges<DynSmartAdGroup> modelChangesWithFieldToUseAsName(Long adGroupId) {
        return new ModelChanges<>(adGroupId, DynSmartAdGroup.class)
                .process(NEW_FIELD_TO_USE_AS_NAME, DynSmartAdGroup.FIELD_TO_USE_AS_NAME);
    }
}
