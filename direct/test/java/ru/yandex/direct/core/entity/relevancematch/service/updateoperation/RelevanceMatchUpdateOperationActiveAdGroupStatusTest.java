package ru.yandex.direct.core.entity.relevancematch.service.updateoperation;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchUpdateOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchUpdateOperationActiveAdGroupStatusTest extends RelevanceMatchModificationBaseTest {
    @Autowired
    private AdGroupRepository adGroupRepository;

    @Test
    public void prepareAndApply_GroupAlreadyModerated_AdGroupStatusModerateNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withIsSuspended(true)
                .withAutobudgetPriority(5);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();
        StatusModerate actualAdGroupStatusModerate = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusModerate();

        assertThat(actualAdGroupStatusModerate, equalTo(StatusModerate.YES));
    }

    @Test
    public void prepareAndApply_GroupNotModerated_AdGroupStatusModerateReset() {
        AppliedChanges<AdGroup> changes = new ModelChanges<>(defaultAdGroup.getAdGroup().getId(), AdGroup.class)
                .process(StatusModerate.SENDING, AdGroup.STATUS_MODERATE).applyTo(defaultAdGroup.getAdGroup());
        adGroupRepository.updateAdGroups(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                Collections.singleton(changes));

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withIsSuspended(true)
                .withAutobudgetPriority(5);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();
        StatusModerate actualAdGroupStatusModerate = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusModerate();

        assertThat(actualAdGroupStatusModerate, equalTo(StatusModerate.READY));
    }

    @Test
    public void prepareAndApply_Suspend_AdGroupStatusBsSyncedNo() {
        adGroupRepository
                .updateStatusBsSynced(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId()),
                        StatusBsSynced.YES
                );

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withIsSuspended(true);

        getFullUpdateOperation(relevanceMatchChanges).prepareAndApply();

        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.NO));
    }

    @Test
    public void prepareAndApply_ChangeRelevanceMatchCategories_AdGroupStatusBsSyncedNo() {
        adGroupRepository
                .updateStatusBsSynced(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId()),
                        StatusBsSynced.YES
                );

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));

        getFullUpdateOperation(relevanceMatchChanges).prepareAndApply();

        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.NO));
    }

    protected AdGroup getAdGroup() {
        return defaultTextAdGroup(activeCampaign.getCampaignId());
    }
}
