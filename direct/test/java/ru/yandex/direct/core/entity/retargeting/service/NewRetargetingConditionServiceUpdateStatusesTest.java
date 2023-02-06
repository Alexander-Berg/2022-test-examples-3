package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRules;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingConditionServiceUpdateStatusesTest {

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetargetingSteps retargetingSteps;

    @Autowired
    private RetargetingConditionOperationFactory serviceUnderTest;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private TranslationService translationService;

    private int shard;
    private long uid;
    private ClientId clientId;
    private AdGroup adGroup1;
    private AdGroup adGroup2;
    private RetargetingInfo retargetingInfo1;
    private RetargetingInfo retargetingInfo2;

    @Before
    public void before() {
        // создаем две активные группы, в каждой по ретаргетингу
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign();
        retargetingInfo1 = retargetingSteps.createDefaultRetargetingInActiveTextAdGroup(campaignInfo);
        retargetingInfo2 = retargetingSteps.createDefaultRetargetingInActiveTextAdGroup(campaignInfo);
        adGroup1 = retargetingInfo1.getAdGroupInfo().getAdGroup();
        adGroup2 = retargetingInfo2.getAdGroupInfo().getAdGroup();
        shard = campaignInfo.getShard();
        uid = campaignInfo.getUid();
        clientId = campaignInfo.getClientId();
    }

    @Test
    public void updateRetargetingConditions_UpdateRules_DropsAdGroupStatusBsSynced() {
        checkState(adGroup1.getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        List<Rule> newRules = bigRules();
        metrikaHelperStub.addGoalsFromRules(uid, newRules);
        ModelChanges<RetargetingCondition> modelChanges =
                getFieldChange(retargetingInfo1.getRetConditionId(), newRules, RetargetingCondition.RULES);

        MassResult<Long> result = serviceUnderTest.updateRetargetingConditions(clientId, singletonList(modelChanges));
        assumeThat(result, isFullySuccessful());

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(retargetingInfo1.getAdGroupId())).get(0);
        assertThat("после обновления правил условия ретаргетинга статус statusBsSynced группы должен сбрасываться",
                adGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void updateRetargetingConditions_OneOfItemsIsInvalid_DropsOnlyChangedAdGroupStatusBsSynced() {
        checkState(adGroup1.getStatusBsSynced() == StatusBsSynced.YES &&
                        adGroup2.getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        List<Rule> newRules1 = bigRules();
        List<Rule> newRules2 = bigRules();
        newRules2.get(0).setGoals(null);
        metrikaHelperStub.addGoalsFromRules(uid, newRules1);

        ModelChanges<RetargetingCondition> modelChanges1 =
                getFieldChange(retargetingInfo1.getRetConditionId(), newRules1, RetargetingCondition.RULES);
        ModelChanges<RetargetingCondition> modelChanges2 =
                getFieldChange(retargetingInfo2.getRetConditionId(), newRules2, RetargetingCondition.RULES);

        MassResult<Long> result =
                serviceUnderTest.updateRetargetingConditions(clientId, asList(modelChanges1, modelChanges2));
        assumeThat(result, isSuccessful(true, false));

        List<AdGroup> adGroups = adGroupRepository
                .getAdGroups(shard, asList(retargetingInfo1.getAdGroupId(), retargetingInfo2.getAdGroupId()));
        List<StatusBsSynced> adGroupsStatusBsSynced = mapList(adGroups, AdGroup::getStatusBsSynced);
        assertThat("статус statusBsSynced должен быть сброшен только у затронутых групп",
                adGroupsStatusBsSynced, contains(StatusBsSynced.NO, StatusBsSynced.YES));
    }

    @Test
    public void updateRetargetingConditions_UpdateName_DoesNotDropAdGroupStatusBsSynced() {
        checkState(adGroup1.getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        ModelChanges<RetargetingCondition> modelChanges = getFieldChange(retargetingInfo1.getRetConditionId(),
                "updated " + randomAlphanumeric(5), RetargetingCondition.NAME);

        MassResult<Long> result = serviceUnderTest.updateRetargetingConditions(clientId, singletonList(modelChanges));
        assumeThat(result, isSuccessful(true));

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(retargetingInfo1.getAdGroupId())).get(0);
        assertThat("после обновления имени условия ретаргетинга статус statusBsSynced группы не должен сбрасываться",
                adGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    private <T> ModelChanges<RetargetingCondition> getFieldChange(long id,
                                                                  T value, ModelProperty<? super RetargetingConditionBase, T> property) {
        ModelChanges<RetargetingCondition> modelChanges = new ModelChanges<>(id, RetargetingCondition.class);
        modelChanges.process(value, property);
        return modelChanges;
    }
}
