package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.relevancematch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.RelevanceMatchSteps;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateRelevanceMatchTest extends ComplexUpdateRelevanceMatchTestBase {
    @Autowired
    RelevanceMatchSteps relevanceMatchSteps;

    @Test
    public void update_AdGroupWithRelevanceMatch_UpdateWithEmptyRelevanceMatchList_RemoveAllRelevanceMatches() {
        relevanceMatchSteps.addDefaultRelevanceMatchToAdGroup(adGroupInfo1);

        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1);
        adGroupForUpdate.withRelevanceMatches(null);
        updateAndCheckResultIsEntirelySuccessful(singletonList(adGroupForUpdate));

        List<RelevanceMatch> relevanceMatchesOfAdGroup = getRelevanceMatchesOfAdGroup(adGroupInfo1);
        assertThat(relevanceMatchesOfAdGroup, hasSize(0));
    }

    /**
     * Проверяем, что включение режима {@code autoPrices} корректно
     * прокидывается до {@link ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModifyOperation}
     */
    @Test
    public void updateWithAutoPrices_EmptyAdGroup_AddRelevanceMatch_AutoPricesAreSet() {
        assumeManualStrategyWithDifferentPlaces();

        RelevanceMatch relevanceMatch = relevanceMatchSteps.getDefaultRelevanceMatch(adGroupInfo1)
                .withPrice(null)
                .withPriceContext(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withRelevanceMatches(singletonList(relevanceMatch));
        updateWithAutoPricesAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<RelevanceMatch> relevanceMatchesOfAdGroup = getRelevanceMatchesOfAdGroup(adGroupInfo1);
        assertThat("в группе появился автотаргетинг", relevanceMatchesOfAdGroup, hasSize(1));
        relevanceMatch = relevanceMatchesOfAdGroup.get(0);
        assertThat("У автотаргетинга выставилась автоматическая ставка", relevanceMatch.getPrice(),
                is(FIXED_AUTO_PRICE));
    }

    private ArrayList<RelevanceMatch> getRelevanceMatchesOfAdGroup(AdGroupInfo adGroupInfo) {
        return new ArrayList<>(relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(shard, clientId, singleton(adGroupInfo.getAdGroupId())).values());
    }
}
