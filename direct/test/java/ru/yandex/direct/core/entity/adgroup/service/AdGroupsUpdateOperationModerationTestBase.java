package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Before;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusmoderate;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

public class AdGroupsUpdateOperationModerationTestBase {

    private static final List<Long> INITIAL_GEO = singletonList(225L);
    private static final List<Long> NEW_GEO = singletonList(159L);

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private AdGroupsUpdateOperationFactory operationFactory;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private AdGroupRepository adGroupRepository;

    private AdGroupInfo adGroupInfo;

    @Parameterized.Parameter
    public ModerationMode moderationMode;

    @Parameterized.Parameter(1)
    public CampaignsStatusmoderate initialCampaignStatusModerate;

    @Parameterized.Parameter(2)
    public StatusModerate initialStatusModerate;

    @Parameterized.Parameter(3)
    public StatusPostModerate initialStatusPostModerate;

    @Parameterized.Parameter(4)
    public boolean changeGeo;

    @Parameterized.Parameter(5)
    public CampaignsStatusmoderate expectedCampaignStatusModerate;

    @Parameterized.Parameter(6)
    public StatusModerate expectedStatusModerate;

    @Parameterized.Parameter(7)
    public StatusPostModerate expectedStatusPostModerate;

    @Before
    public void before() throws Exception {
        new TestContextManager(this.getClass()).prepareTestInstance(this);

        AdGroup adGroup = TestGroups.activeTextAdGroup(null)
                .withGeo(INITIAL_GEO)
                .withStatusModerate(initialStatusModerate)
                .withStatusPostModerate(initialStatusPostModerate);
        adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup);

        dslContextProvider.ppc(adGroupInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, initialCampaignStatusModerate)
                .where(CAMPAIGNS.CID.eq(adGroupInfo.getCampaignId()))
                .execute();

        MassResult<Long> result = createOperation().prepareAndApply();
        assumeThat(result, isFullySuccessful());
    }

    public void adGroupModerationStatusesAreUpdatedWell() {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(adGroupInfo.getShard(),
                singleton(adGroupInfo.getAdGroupId()));

        AdGroup expectedAdGroup = new AdGroup()
                .withStatusModerate(expectedStatusModerate)
                .withStatusPostModerate(expectedStatusPostModerate);

        assertThat(adGroups.get(0), beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields()));
    }

    public void campaignModerationStatusIsUpdatedWell() {
        CampaignsStatusmoderate campaignsStatusmoderate =
                dslContextProvider.ppc(adGroupInfo.getShard())
                        .select(CAMPAIGNS.STATUS_MODERATE)
                        .from(CAMPAIGNS)
                        .where(CAMPAIGNS.CID.eq(adGroupInfo.getCampaignId()))
                        .fetchOne(CAMPAIGNS.STATUS_MODERATE);
        assertThat(campaignsStatusmoderate, is(expectedCampaignStatusModerate));
    }

    private AdGroupsUpdateOperation createOperation() {
        AdGroupUpdateOperationParams operationParams = AdGroupUpdateOperationParams.builder()
                .withModerationMode(moderationMode)
                .withValidateInterconnections(true)
                .build();
        return operationFactory.newInstance(Applicability.FULL, createModelChanges(), operationParams,
                geoTreeFactory.getApiGeoTree(), MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                adGroupInfo.getUid(), adGroupInfo.getClientId(), adGroupInfo.getShard());
    }

    private List<ModelChanges<AdGroup>> createModelChanges() {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class);
        modelChanges.process(changeGeo ? NEW_GEO : INITIAL_GEO, AdGroup.GEO);
        return singletonList(modelChanges);
    }
}
