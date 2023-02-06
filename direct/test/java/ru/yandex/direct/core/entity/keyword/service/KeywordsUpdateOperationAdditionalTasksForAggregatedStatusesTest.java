package ru.yandex.direct.core.entity.keyword.service;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAdditionalTasksForAggregatedStatusesTest extends KeywordsUpdateOperationBaseTest {

    @Test
    public void suspendKeyword_setAggregatedStatusIsObsolete() {
        createOneActiveAdGroup();
        KeywordInfo keywordInfo = createKeyword(adGroupInfo1, PHRASE_1);
        createAggregatedStatuses(keywordInfo);

        suspendResumeKeyword(keywordInfo.getId(), true);
        checkIsObsolete(keywordInfo, true);
    }

    @Test
    public void resumeKeyword_setAggregatedStatusIsObsolete() {
        createOneActiveAdGroup();
        Keyword keyword = getDefaultActiveKeyword(PHRASE_1).withIsSuspended(true);
        KeywordInfo keywordInfo = keywordSteps.createKeyword(adGroupInfo1, keyword);
        createAggregatedStatuses(keywordInfo);

        suspendResumeKeyword(keywordInfo.getId(), false);
        checkIsObsolete(keywordInfo, true);
    }

    @Test
    public void suspendAlreadySuspended_dontSetAggregatedStatusIsObsolete() {
        createOneActiveAdGroup();
        Keyword keyword = getDefaultActiveKeyword(PHRASE_1).withIsSuspended(true);
        KeywordInfo keywordInfo = keywordSteps.createKeyword(adGroupInfo1, keyword);
        createAggregatedStatuses(keywordInfo);

        suspendResumeKeyword(keywordInfo.getId(), true);
        checkIsObsolete(keywordInfo, false);
    }

    private void suspendResumeKeyword(Long keywordId, boolean isSuspended) {
        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordId, Keyword.class)
                .process(isSuspended, Keyword.IS_SUSPENDED);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordId, PHRASE_1, isSuspended)));
    }

    private void createAggregatedStatuses(KeywordInfo keywordInfo) {
        int shard = keywordInfo.getShard();
        AggregatedStatusKeywordData keywordStatus = new AggregatedStatusKeywordData(
                new SelfStatus(GdSelfStatusEnum.DRAFT, GdSelfStatusReason.DRAFT));
        aggregatedStatusesRepository.updateKeywords(shard, null, Map.of(keywordInfo.getId(), keywordStatus));
    }

    private void checkIsObsolete(KeywordInfo keywordInfo, boolean isObsolete) {
        int shard = keywordInfo.getShard();
        Long keywordId = keywordInfo.getId();
        Map<Long, Boolean> keywordStatusesIsObsolete =
                aggregatedStatusesRepository.getKeywordStatusesIsObsolete(shard, singletonList(keywordId));
        assertThat(keywordStatusesIsObsolete.get(keywordId)).isEqualTo(isObsolete);
    }
}
