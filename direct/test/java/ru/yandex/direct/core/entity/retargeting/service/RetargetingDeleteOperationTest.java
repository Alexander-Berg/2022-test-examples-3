package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.duplicatedRetargetingId;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingDeleteOperationTest {

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private DeleteRetargetingValidationService deleteRetargetingValidationService;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private RetargetingDeleteService retargetingDeleteService;

    @Autowired
    private Steps steps;

    private RetargetingInfo retargetingInfo;
    private long operatonUid;
    private ClientId clientId;
    private long clientUid;
    private int shard;

    @Before
    public void before() {
        retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();
        operatonUid = retargetingInfo.getUid();
        clientId = retargetingInfo.getClientId();
        clientUid = rbacService.getChiefByClientId(clientId);
        shard = retargetingInfo.getShard();

        assumeThat(retargetingInfo.getAdGroupInfo().getAdGroup().getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply_DeleteId_SuccesfullDelete() {
        MassResult<Long> result = createDeleteOpereation(singletonList(retargetingInfo.getRetargetingId()))
                .prepareAndApply();
        assumeThat(result, isSuccessful());

        List<Retargeting> retargetings = retargetingRepository
                .getRetargetingsByIds(shard, singletonList(retargetingInfo.getRetargetingId()), maxLimited());
        assertThat(retargetings, hasSize(0));
    }

    @Test
    public void prepareAndApply_DeleteId_ResetAdGroupStatusBsSynced() {
        MassResult<Long> result = createDeleteOpereation(singletonList(retargetingInfo.getRetargetingId()))
                .prepareAndApply();
        assumeThat(result, isSuccessful());

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(retargetingInfo.getAdGroupId())).get(0);
        assertThat(adGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void prepareAndApply_EmptyAdGroupNotModerated_CheckModerationStatuses() {
        long adGroupId = retargetingInfo.getAdGroupId();
        AdGroup adGroupBefore = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        AppliedChanges<AdGroup> appliedChanges = ModelChanges
                .build(adGroupId, AdGroup.class, AdGroup.STATUS_POST_MODERATE, StatusPostModerate.NO)
                .applyTo(adGroupBefore);
        adGroupRepository.updateAdGroups(shard, clientId, singleton(appliedChanges));
        MassResult<Long> result = createDeleteOpereation(singletonList(retargetingInfo.getRetargetingId()))
                .prepareAndApply();
        assumeThat(result, isSuccessful());

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assertThat(adGroup.getStatusPostModerate(), is(StatusPostModerate.REJECTED));
        assertThat(adGroup.getStatusModerate(), is(StatusModerate.READY));
    }

    @Test
    public void prepareAndApply_EmptyAdGroupModerated_CheckModerationStatuses() {
        long adGroupId = retargetingInfo.getAdGroupId();
        AdGroup adGroupBefore = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assumeThat(adGroupBefore.getStatusPostModerate(), is(StatusPostModerate.YES));
        MassResult<Long> result = createDeleteOpereation(singletonList(retargetingInfo.getRetargetingId()))
                .prepareAndApply();
        assumeThat(result, isSuccessful());

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assertThat(adGroup.getStatusPostModerate(), is(StatusPostModerate.YES));
        assertThat(adGroup.getStatusModerate(), is(adGroupBefore.getStatusModerate()));
    }

    @Test
    public void prepareAndApply_InvalidId_BaseValidationIsActive() {
        MassResult<Long> result = createDeleteOpereation(singletonList(-1L)).prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path(index(0)), validId())));
    }

    @Test
    public void prepareAndApply_DuplicateId_DeleteValidationIsActive() {
        long id = retargetingInfo.getRetargetingId();
        MassResult<Long> result = createDeleteOpereation(asList(id, id)).prepareAndApply();

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), duplicatedRetargetingId())));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), duplicatedRetargetingId())));
    }


    private RetargetingDeleteOperation createDeleteOpereation(List<Long> ids) {
        return new RetargetingDeleteOperation(Applicability.FULL, ids,
                retargetingRepository, retargetingDeleteService,
                deleteRetargetingValidationService,
                operatonUid, clientId, clientUid, shard);
    }
}
