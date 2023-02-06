package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.data.TestRelevanceMatches;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.webAdGroupRelevanceMatch;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerCopyRelevanceMatchTest extends AdGroupControllerCopyTestBase {

    @Test
    public void relevanceMatchPriceIsCopied() {
        Long relevanceMatchId = createAdGroupWithRelevanceMatch();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withRelevanceMatches(singletonList(webAdGroupRelevanceMatch(relevanceMatchId)));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        Map<Long, RelevanceMatch> relevanceMatchCopies =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(shard, clientId,
                        singletonList(adGroupCopiesIds.get(0)));
        assumeThat("количество скопированных бесфразных таргетингов не соответствует ожидаемому",
                relevanceMatchCopies.keySet(), hasSize(1));

        RelevanceMatch expectedRelMatch = new RelevanceMatch()
                .withPrice(PRICE_SEARCH)
                .withAutobudgetPriority(PRIORITY);
        RelevanceMatch relMatchCopy = relevanceMatchCopies.values().iterator().next();
        assertThat(relMatchCopy, beanDiffer(expectedRelMatch).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void generalPriceIsSetForCopiedRMAndPriorityIsCopiedFromOldRM() {
        Long relevanceMatchId = createAdGroupWithRelevanceMatch();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withRelevanceMatches(singletonList(webAdGroupRelevanceMatch(relevanceMatchId)))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        Map<Long, RelevanceMatch> relevanceMatchCopies =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(shard, clientId,
                        singletonList(adGroupCopiesIds.get(0)));
        assumeThat("количество скопированных бесфразных таргетингов не соответствует ожидаемому",
                relevanceMatchCopies.keySet(), hasSize(1));

        RelevanceMatch expectedRelMatch = new RelevanceMatch()
                .withPrice(GENERAL_PRICE)
                .withAutobudgetPriority(PRIORITY);
        RelevanceMatch relMatchCopy = relevanceMatchCopies.values().iterator().next();
        assertThat(relMatchCopy, beanDiffer(expectedRelMatch).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void generalPriceIsSetForNewRM() {
        createAdGroup();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withRelevanceMatches(singletonList(webAdGroupRelevanceMatch(null)))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        Map<Long, RelevanceMatch> relevanceMatches =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(shard, clientId,
                        singletonList(adGroupCopiesIds.get(0)));
        assumeThat("количество скопированных бесфразных таргетингов не соответствует ожидаемому",
                relevanceMatches.keySet(), hasSize(1));

        RelevanceMatch expectedRelMatch = new RelevanceMatch()
                .withPrice(GENERAL_PRICE);
        RelevanceMatch createdRelMatch = relevanceMatches.values().iterator().next();
        assertThat(createdRelMatch, beanDiffer(expectedRelMatch).useCompareStrategy(onlyExpectedFields()));
    }

    private Long createAdGroupWithRelevanceMatch() {
        createAdGroup();
        RelevanceMatch relevanceMatch = TestRelevanceMatches.defaultRelevanceMatch()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withPrice(PRICE_SEARCH)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(PRIORITY);
        return steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(singletonList(relevanceMatch), adGroupInfo).get(0);
    }
}
