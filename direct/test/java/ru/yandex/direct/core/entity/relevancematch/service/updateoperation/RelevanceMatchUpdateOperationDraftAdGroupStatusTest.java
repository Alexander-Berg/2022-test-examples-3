package ru.yandex.direct.core.entity.relevancematch.service.updateoperation;

import java.math.BigDecimal;
import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchUpdateOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchUpdateOperationDraftAdGroupStatusTest extends RelevanceMatchModificationBaseTest {
    @Autowired
    private AdGroupRepository adGroupRepository;

    @Before
    public void resetAdGroupStatusBsSynced() {
        adGroupRepository
                .updateStatusBsSynced(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId()),
                        StatusBsSynced.YES
                );
    }

    @Test
    public void prepareAndApply_AutobudgetPriorityChanged_AdGroupBsSyncStatusNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withIsSuspended(false)
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(5);
        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();

        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply_PriceChanged_AdGroupBsSyncStatusNotReset() {
        campaignsByIds.get(activeCampaign.getCampaignId()).withAutobudget(false);

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withIsSuspended(false)
                .withId(getSavedRelevanceMatch().getId())
                .withPrice(BigDecimal.TEN);
        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();

        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, CoreMatchers.equalTo(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply_SetSuspended_AdGroupBsSyncStatusNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withIsSuspended(true);
        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();

        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply__ChangeHrefParam1_AdGroupBsSyncStatusNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withHrefParam1("asdas");

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();

        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply__ChangeHrefParam2_AdGroupBsSyncStatusNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withHrefParam2("asdas");

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();

        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply_AdGroupStatusModerateNotReset() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withIsSuspended(!getSavedRelevanceMatch().getIsSuspended());

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        relevanceMatchUpdateOperation.prepareAndApply();

        StatusModerate actualAdGroupStatusModerate = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusModerate();

        assertThat(actualAdGroupStatusModerate, equalTo(StatusModerate.NEW));
    }

    protected AdGroup getAdGroup() {
        return draftTextAdgroup(activeCampaign.getCampaignId());
    }
}
