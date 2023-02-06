package ru.yandex.direct.core.entity.relevancematch.service.addoperation;

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
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchAddOperationChangeAdGroupStatusesTest extends RelevanceMatchAddOperationBaseTest {
    @Autowired
    private AdGroupRepository adGroupRepository;

    @Test
    public void prepareAndApply_AdGroupBsSyncStatusReset() {
        adGroupRepository
                .updateStatusBsSynced(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId()),
                        StatusBsSynced.YES
                );

        RelevanceMatch relevanceMatch = getValidRelevanceMatch();

        RelevanceMatchAddOperation fullAddOperation = getFullAddOperation(relevanceMatch);
        fullAddOperation.prepareAndApply();
        StatusBsSynced statusBsSynced = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusBsSynced();

        assertThat(statusBsSynced, equalTo(StatusBsSynced.NO));
    }

    @Test
    public void prepareAndApply_AdGroupStatusModerateReset() {
        RelevanceMatch relevanceMatch = getValidRelevanceMatch();

        RelevanceMatchAddOperation fullAddOperation = getFullAddOperation(relevanceMatch);
        fullAddOperation.prepareAndApply();
        StatusModerate actualAdGroupStatusModerate = adGroupRepository
                .getAdGroups(defaultUser.getShard(), Collections.singletonList(defaultAdGroup.getAdGroupId())).get(0)
                .getStatusModerate();

        assertThat(actualAdGroupStatusModerate, equalTo(StatusModerate.READY));
    }

    protected AdGroup getAdGroup() {
        return defaultTextAdGroup(activeCampaign.getCampaignId()).withStatusModerate(StatusModerate.NO);
    }
}
