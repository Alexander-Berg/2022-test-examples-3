package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupRelevanceMatch;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.webAdGroupRelevanceMatch;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateRelMatchTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_AdGroupWithAddedRelevanceMatch_RelevanceMatchIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);
        WebAdGroupRelevanceMatch requestRelevanceMatch = webAdGroupRelevanceMatch(0L);
        requestAdGroup.withRelevanceMatches(singletonList(requestRelevanceMatch));

        updateAndCheckResult(singletonList(requestAdGroup));

        List<RelevanceMatch> relevanceMatches = findRelevanceMatches(adGroupId);
        assertThat("должен быть добавлен один бесфразный таргетинг",
                relevanceMatches, hasSize(1));
    }

    @Test
    public void update_AdGroupWithUpdatedRelevanceMatch_RelevanceMatchIsUpdated() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        Long relevanceMatchId = steps.relevanceMatchSteps()
                .addDefaultRelevanceMatchToAdGroup(adGroupInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);
        WebAdGroupRelevanceMatch requestRelevanceMatch = webAdGroupRelevanceMatch(relevanceMatchId);
        requestAdGroup.withRelevanceMatches(singletonList(requestRelevanceMatch));

        updateAndCheckResult(singletonList(requestAdGroup));

        List<RelevanceMatch> relevanceMatches = findRelevanceMatches(adGroupId);
        assertThat("должен быть один бесфразный таргетинг",
                relevanceMatches, hasSize(1));
        assertThat("данные обновленного бесфразного таргетинга отличаются от ожидаемых",
                relevanceMatches.get(0).getId(), equalTo(relevanceMatchId));
    }

    @Test
    public void update_AdGroupWithDeletedRelevanceMatch_RelevanceMatchIsDeleted() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.relevanceMatchSteps().addDefaultRelevanceMatchToAdGroup(adGroupInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);

        updateAndCheckResult(singletonList(requestAdGroup));

        List<RelevanceMatch> relevanceMatches = findRelevanceMatches(adGroupId);
        assertThat("бесфразный таргетинг должен быть удален", relevanceMatches, emptyIterable());
    }
}
