package ru.yandex.direct.core.aggregatedstatuses.repository;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatedStatusesRepositoryKeywordsUpdateTimeThresholdTest {
    private static final SelfStatus DRAFT_KEYWORD_SELF_STATUS = new SelfStatus(
            GdSelfStatusEnum.DRAFT, GdSelfStatusReason.DRAFT);
    private static final SelfStatus RANDOM_KEYWORD_SELF_STATUS = new SelfStatus(
            GdSelfStatusEnum.ARCHIVED, GdSelfStatusReason.ARCHIVED);

    private static final AggregatedStatusKeywordData DRAFT_KEYWORD_DATA = new AggregatedStatusKeywordData(
            DRAFT_KEYWORD_SELF_STATUS);
    private static final AggregatedStatusKeywordData RANDOM_KEYWORD_DATA = new AggregatedStatusKeywordData(
            RANDOM_KEYWORD_SELF_STATUS);

    private int shard;

    private Long keywordId1;
    private Long keywordId2;

    @Autowired
    private Steps steps;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Before
    public void before() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo activeTextAdGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        shard = clientInfo.getShard();

        keywordId1 = steps.keywordSteps().createKeyword(activeTextAdGroup).getId();
        keywordId2 = steps.keywordSteps().createKeyword(activeTextAdGroup).getId();
    }

    @Test
    public void initializeKeywordStatuses() {
        var firstKeywordStatuses = Map.of(keywordId1, DRAFT_KEYWORD_DATA);
        var secondKeywordStatuses = Map.of(keywordId2, DRAFT_KEYWORD_DATA);

        // Both campaigns should have initialized statuses regardless of updateBefore value
        // which only comes into play in case of onDuplicateKeyUpdate
        aggregatedStatusesRepository.updateKeywords(shard, LocalDateTime.now().plusSeconds(10), firstKeywordStatuses);
        aggregatedStatusesRepository.updateKeywords(shard, null, secondKeywordStatuses);

        checkKeywordSelfStatuses(Map.of(
                keywordId1, DRAFT_KEYWORD_SELF_STATUS,
                keywordId2, DRAFT_KEYWORD_SELF_STATUS));
    }

    @Test
    public void updateKeywordStatusesWithUpdateBefore() {
        LocalDateTime updateBefore = setDefaultKeywordStatuses();

        // Only the first campaign should update its statuses because of updateBefore condition
        var newCampaignsStatuses = Map.of(
                keywordId1, DRAFT_KEYWORD_DATA,
                keywordId2, RANDOM_KEYWORD_DATA);
        aggregatedStatusesRepository.updateKeywords(shard, updateBefore, newCampaignsStatuses);

        checkKeywordSelfStatuses(Map.of(
                keywordId1, DRAFT_KEYWORD_SELF_STATUS,
                keywordId2, DRAFT_KEYWORD_SELF_STATUS));
    }

    @Test
    public void updateKeywordStatusesWithoutUpdateBefore() {
        setDefaultKeywordStatuses();

        // Both campaigns should update their statuses because updateBefore is not set
        var newCampaignsStatuses = Map.of(
                keywordId1, DRAFT_KEYWORD_DATA,
                keywordId2, RANDOM_KEYWORD_DATA);
        aggregatedStatusesRepository.updateKeywords(shard, null, newCampaignsStatuses);

        checkKeywordSelfStatuses(Map.of(
                keywordId1, DRAFT_KEYWORD_SELF_STATUS,
                keywordId2, RANDOM_KEYWORD_SELF_STATUS));
    }

    private LocalDateTime setDefaultKeywordStatuses() {
        var keywordStatuses = Map.of(
                keywordId1, RANDOM_KEYWORD_DATA,
                keywordId2, DRAFT_KEYWORD_DATA);

        // Initialization of campaigns' statuses
        aggregatedStatusesRepository.updateKeywords(shard, null, keywordStatuses);

        // Устанавливаем значение updateBefore после обновления статуса первой и до обновления статуса второй кампании
        var updateBefore = LocalDateTime.now();
        aggregatedStatusesRepository.setKeywordStatusUpdateTime(shard, keywordId1, updateBefore.minusSeconds(1));
        aggregatedStatusesRepository.setKeywordStatusUpdateTime(shard, keywordId2, updateBefore.plusSeconds(1));

        // Checking the initialization
        checkKeywordSelfStatuses(Map.of(
                keywordId1, RANDOM_KEYWORD_SELF_STATUS,
                keywordId2, DRAFT_KEYWORD_SELF_STATUS));

        return updateBefore;
    }

    private void checkKeywordSelfStatuses(Map<Long, SelfStatus> expectedKeywordSelfStatusesByIds) {
        var keywordStatusesByIds = aggregatedStatusesRepository.getKeywordStatusesByIds(shard,
                expectedKeywordSelfStatusesByIds.keySet());
        expectedKeywordSelfStatusesByIds.forEach((keywordId, selfStatus) ->
                assertEquals(selfStatus.getStatus(), keywordStatusesByIds.get(keywordId).getStatus().orElse(null)));
    }
}
