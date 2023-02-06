package ru.yandex.direct.core.entity.relevancematch.service.updateoperation;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchUpdateAggregatedStatusTest extends RelevanceMatchModificationBaseTest {
    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Test
    public void suspendRelevanceMatch_setAggregatedStatusIsObsolete() {
        Long relevanceMatchId = getSavedRelevanceMatch().getId();
        createAggregatedStatuses(defaultUser.getShard(), relevanceMatchId);

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(relevanceMatchId)
                .withIsSuspended(true);
        getFullUpdateOperation(relevanceMatchChanges).prepareAndApply();

        checkIsObsolete(defaultUser.getShard(), relevanceMatchId, true);
    }

    @Test
    public void resumeRelevanceMatch_setAggregatedStatusIsObsolete() {
        Long relevanceMatchId = getSavedRelevanceMatch().getId();
        setSuspended(relevanceMatchId, true);
        createAggregatedStatuses(defaultUser.getShard(), relevanceMatchId);

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(relevanceMatchId)
                .withIsSuspended(false);
        getFullUpdateOperation(relevanceMatchChanges).prepareAndApply();

        checkIsObsolete(defaultUser.getShard(), relevanceMatchId, true);
    }

    @Test
    public void suspendAlreadySuspended_dontSetAggregatedStatusIsObsolete() {
        Long relevanceMatchId = getSavedRelevanceMatch().getId();
        setSuspended(relevanceMatchId, true);
        createAggregatedStatuses(defaultUser.getShard(), relevanceMatchId);

        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(relevanceMatchId)
                .withIsSuspended(true);
        getFullUpdateOperation(relevanceMatchChanges).prepareAndApply();

        checkIsObsolete(defaultUser.getShard(), relevanceMatchId, false);
    }

    private void createAggregatedStatuses(int shard, Long relevanceMatchId) {
        AggregatedStatusKeywordData relevanceMatchStatus = new AggregatedStatusKeywordData(
                new SelfStatus(GdSelfStatusEnum.STOP_OK, GdSelfStatusReason.RELEVANCE_MATCH_SUSPENDED_BY_USER));
        aggregatedStatusesRepository.updateKeywords(shard, null, Map.of(relevanceMatchId, relevanceMatchStatus));
    }

    private void checkIsObsolete(int shard, Long relevanceMatchId, boolean isObsolete) {
        Map<Long, Boolean> relevanceMatchStatusesIsObsolete =
                aggregatedStatusesRepository.getKeywordStatusesIsObsolete(shard, singletonList(relevanceMatchId));
        assertThat(relevanceMatchStatusesIsObsolete.get(relevanceMatchId)).isEqualTo(isObsolete);
    }
}
