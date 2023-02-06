package ru.yandex.direct.core.entity.adgroup.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.repository.typesupport.InternalAdGroupSupport.DEFAULT_LEVEL;
import static ru.yandex.direct.operation.Applicability.FULL;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationInternalTest extends AdGroupsUpdateOperationTestBase {
    private static final Long INITIAL_LEVEL = 0L;
    private static final Long NEW_LEVEL = 2L;
    private static final Long NEW_INVALID_LEVEL = -1L;
    private static final Integer INITIAL_RF = null;
    private static final Integer NEW_RF = 1;
    private static final Integer NEW_INVALID_RF = 0;
    private static final Integer INITIAL_RF_RESET = null;
    private static final Integer NEW_RF_RESET = 1;
    private static final Integer NEW_INVALID_RF_RESET = 0;

    @Test
    public void prepareAndApply_ChangeMinusKeywords_UnsuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultInternalAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithMinusKeywords(adGroupInfo.getAdGroup());

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));

        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat(realAdGroup.getMinusKeywords(), is(emptyList()));
    }

    @Test
    public void prepareAndApply_ChangeLevel_UnsuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultInternalAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithLevel(adGroupInfo.getAdGroup(), NEW_INVALID_LEVEL);

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));

        InternalAdGroup realAdGroup =
                (InternalAdGroup) adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId()))
                        .get(0);
        Long expectedLevel = ((InternalAdGroup) adGroupInfo.getAdGroup()).getLevel();
        assertThat(realAdGroup.getLevel(), equalTo(expectedLevel));
    }

    @Test
    public void prepareAndApply_ChangeLevel_SuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultInternalAdGroup(INITIAL_LEVEL);
        ModelChanges<AdGroup> modelChanges = modelChangesWithLevel(adGroupInfo.getAdGroup(), NEW_LEVEL);

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        InternalAdGroup realAdGroup =
                (InternalAdGroup) adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId()))
                        .get(0);
        assertThat(realAdGroup.getLevel(), is(NEW_LEVEL));

        modelChanges = modelChangesWithLevel(adGroupInfo.getAdGroup(), null);

        updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        realAdGroup = (InternalAdGroup) adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId()))
                .get(0);
        assertThat(realAdGroup.getLevel(), equalTo(DEFAULT_LEVEL));
    }

    @Test
    public void prepareAndApply_ChangeRf_UnsuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultInternalAdGroup(null, INITIAL_RF, INITIAL_RF_RESET);
        ModelChanges<AdGroup> modelChanges = modelChangesWithRf(adGroupInfo.getAdGroup(), NEW_INVALID_RF,
                NEW_INVALID_RF_RESET);

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));

        InternalAdGroup realAdGroup =
                (InternalAdGroup) adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId()))
                        .get(0);
        assertThat(realAdGroup.getRf(), equalTo(INITIAL_RF));
        assertThat(realAdGroup.getRfReset(), equalTo(INITIAL_RF_RESET));
    }

    @Test
    public void prepareAndApply_ChangeRfNull_UnsuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultInternalAdGroup(null, INITIAL_RF, INITIAL_RF_RESET);
        ModelChanges<AdGroup> modelChanges = modelChangesWithRf(adGroupInfo.getAdGroup(), null, NEW_RF_RESET);

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));

        InternalAdGroup realAdGroup =
                (InternalAdGroup) adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat(realAdGroup.getRf(), equalTo(INITIAL_RF));
        assertThat(realAdGroup.getRfReset(), equalTo(INITIAL_RF_RESET));
    }

    @Test
    public void prepareAndApply_ChangeRf_SuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultInternalAdGroup(null, INITIAL_RF, INITIAL_RF_RESET);
        ModelChanges<AdGroup> modelChanges = modelChangesWithRf(adGroupInfo.getAdGroup(), NEW_RF, NEW_RF_RESET);

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);

        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        InternalAdGroup realAdGroup =
                (InternalAdGroup) adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId()))
                        .get(0);
        assertThat(realAdGroup.getRf(), equalTo(NEW_RF));
        assertThat(realAdGroup.getRfReset(), equalTo(NEW_RF_RESET));
    }

    private ModelChanges<AdGroup> modelChangesWithMinusKeywords(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(singletonList("banana!"), AdGroup.MINUS_KEYWORDS);
        return modelChanges;
    }

    private ModelChanges<AdGroup> modelChangesWithLevel(AdGroup adGroup, Long level) {
        ModelChanges<InternalAdGroup> internalModelChanges = new ModelChanges<>(adGroup.getId(), InternalAdGroup.class);
        internalModelChanges.process(level, InternalAdGroup.LEVEL).castModelUp(AdGroup.class);
        return internalModelChanges.castModelUp(AdGroup.class);
    }

    private ModelChanges<AdGroup> modelChangesWithRf(AdGroup adGroup, Integer rf, Integer rfReset) {
        ModelChanges<InternalAdGroup> internalModelChanges = new ModelChanges<>(adGroup.getId(), InternalAdGroup.class);
        internalModelChanges
                .process(rf, InternalAdGroup.RF)
                .process(rfReset, InternalAdGroup.RF_RESET)
                .castModelUp(AdGroup.class);
        return internalModelChanges.castModelUp(AdGroup.class);
    }
}
