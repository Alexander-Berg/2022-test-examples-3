package ru.yandex.direct.core.entity.relevancematch.service.deleteoperation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchDeleteOperation;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchDeleteOperationTest extends RelevanceMatchModificationBaseTest {

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_ResultIsFullySuccessful() {

        RelevanceMatchDeleteOperation relevanceMatchUpdateOperation =
                getFullDeleteOperation(singletonList(getSavedRelevanceMatch().getId()));
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneInValidItem_ResultHasElementError() {
        getFullDeleteOperation(singletonList(getSavedRelevanceMatch().getId())).prepareAndApply();
        RelevanceMatchDeleteOperation relevanceMatchUpdateOperation =
                getFullDeleteOperation(singletonList(getSavedRelevanceMatch().getId()));
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();
        assertThat(massResult, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_DeleteCorrectly() {
        RelevanceMatchDeleteOperation relevanceMatchUpdateOperation =
                getFullDeleteOperation(singletonList(getSavedRelevanceMatch().getId()));
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();

        RelevanceMatch actualRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        singletonList(massResult.get(0).getResult()))
                .get(massResult.get(0).getResult());

        assertThat(actualRelevanceMatch, nullValue());
    }

    @Test
    public void prepareAndApply_DeleteRelevanceMatch_AdGroupStatusBsSyncedNo() {
        adGroupRepository.updateStatusBsSynced(defaultAdGroup.getShard(), singletonList(defaultAdGroup.getAdGroupId()),
                StatusBsSynced.YES);

        RelevanceMatchDeleteOperation relevanceMatchUpdateOperation =
                getFullDeleteOperation(singletonList(getSavedRelevanceMatch().getId()));
        relevanceMatchUpdateOperation.prepareAndApply();

        AdGroup adGroup =
                adGroupRepository.getAdGroups(defaultAdGroup.getShard(), singletonList(defaultAdGroup.getAdGroupId()))
                        .get(0);

        assertThat(adGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }
}
