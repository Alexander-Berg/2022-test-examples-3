package ru.yandex.direct.core.aggregatedstatuses.repository;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatedStatusesRepositoryKwOnAdGroupsForCountTest {
    private int shard;
    private AdGroupInfo adGroup1;
    private AdGroupInfo adGroup2;


    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Autowired
    private KeywordRepository keywordRepository;


    @Before
    public void createAdGroups() {
        adGroup1 = steps.adGroupSteps().createActiveTextAdGroup();
        adGroup2 = steps.adGroupSteps().createActiveTextAdGroup();

        assumeTrue("shards are equal for both adgroups",
                adGroup1.getShard().equals(adGroup2.getShard()));

        shard = adGroup1.getShard();
    }

    @Test
    public void bidsOnly() {
        List<Long> ids = createKeywords();
        populateKeywordAggregatedStatuses(ids);

        var adGroupIds = List.of(adGroup1.getAdGroupId(), adGroup2.getAdGroupId());
        Map<Long, List<AggregatedStatusKeywordData>> statuses =
                aggregatedStatusesRepository.getKeywordsStatusesOnAdGroupsForCount(shard, adGroupIds);

        assumeThat("Got both adGroup Ids", statuses.keySet(), containsInAnyOrder(adGroupIds.toArray()));
        assertEquals("Got statuses equal by count to created keywords", statuses.values().stream()
                .flatMap(l -> l.stream()).collect(Collectors.toSet()).size(), ids.size());
    }

    @Test
    public void bidsArcOnly() {
        List<Long> ids = createKeywords();
        populateKeywordAggregatedStatuses(ids);

        dslContextProvider.ppcTransaction(shard, conf -> keywordRepository.archiveKeywords(conf, ids));

        var adGroupIds = List.of(adGroup1.getAdGroupId(), adGroup2.getAdGroupId());
        Map<Long, List<AggregatedStatusKeywordData>> statuses =
                aggregatedStatusesRepository.getKeywordsStatusesOnAdGroupsForCount(shard, adGroupIds);

        assumeThat("Got both adGroup Ids", statuses.keySet(), containsInAnyOrder(adGroupIds.toArray()));
        assertEquals("Got statuses equal by count to created keywords", statuses.values().stream()
                .flatMap(l -> l.stream()).collect(Collectors.toSet()).size(), ids.size());
    }

    @Test
    public void bidsAndBidsArc() {
        List<Long> ids = createKeywords();
        populateKeywordAggregatedStatuses(ids);

        Long anyId = ids.get(0);
        dslContextProvider.ppcTransaction(shard, conf ->
                keywordRepository.copyKeywordsToArchiveNoProduction(conf, Collections.singletonList(anyId)));

        var adGroupIds = List.of(adGroup1.getAdGroupId(), adGroup2.getAdGroupId());
        Map<Long, List<AggregatedStatusKeywordData>> statuses =
                aggregatedStatusesRepository.getKeywordsStatusesOnAdGroupsForCount(shard, adGroupIds);

        assumeThat("Got both adGroup Ids", statuses.keySet(), containsInAnyOrder(adGroupIds.toArray()));
        assertEquals("Got statuses equal by count to created keywords", statuses.values().stream()
                .flatMap(l -> l.stream()).collect(Collectors.toSet()).size(), ids.size());
    }

    private List<Long> createKeywords() {
        long id1 = steps.keywordSteps().createKeyword(adGroup1).getId();
        long id2 = steps.keywordSteps().createKeyword(adGroup1).getId();
        long id3 = steps.keywordSteps().createKeyword(adGroup2).getId();
        long id4 = steps.keywordSteps().createKeyword(adGroup2).getId();
        return List.of(id1, id2, id3, id4);
    }

    @Test
    public void emptyAdGroupIdsList() {
        Map<Long, List<AggregatedStatusKeywordData>> statuses =
                aggregatedStatusesRepository.getKeywordsStatusesOnAdGroupsForCount(shard, Collections.emptyList());
        assertTrue("Got no statuses for empty adgroup ids list", statuses.isEmpty());
    }

    @Test
    public void nullAdGroupIdsList() {
        Map<Long, List<AggregatedStatusKeywordData>> statuses =
                aggregatedStatusesRepository.getKeywordsStatusesOnAdGroupsForCount(shard, null);
        assertTrue("Got no statuses for empty adgroup ids list", statuses.isEmpty());
    }


    private void populateKeywordAggregatedStatuses(List<Long> keywordIds) {
        Map<Long, AggregatedStatusKeywordData> keywordStatuses = new HashMap<>();
        for (Long keywordId : keywordIds) {
            keywordStatuses.put(keywordId, new AggregatedStatusKeywordData((GdSelfStatusEnum) null));
        }
        aggregatedStatusesRepository.updateKeywords(shard, null, keywordStatuses);
    }
}
