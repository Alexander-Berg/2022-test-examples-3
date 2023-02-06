package ru.yandex.direct.core.entity.keyword.repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.keyword.repository.internal.DbAddedPhraseType;
import ru.yandex.direct.core.entity.keyword.repository.internal.DbAddedPhrasesCache;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestKeywordCacheRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.dbschema.ppc.Tables.ADDED_PHRASES_CACHE;
import static ru.yandex.direct.utils.HashingUtils.getMd5HalfHashUtf8;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordCacheRepositoryTest {

    private static final String KEYWORD_1 = "+по город идти я";
    private static final String KEYWORD_2 = "возвращаться дом запретить мама моя мне";

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies.allFieldsExcept(newPath("addDate"));

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private KeywordCacheRepository keywordCacheRepository;

    @Autowired
    private TestKeywordCacheRepository testKeywordCacheRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    private int shard;
    private long campId1;
    private long campId2;
    private long adGroupId1;
    private long adGroupId2;
    private CampaignInfo campaignInfo1;
    private CampaignInfo campaignInfo2;
    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;

    @Before
    public void before() {
        adGroupInfo1 = adGroupSteps.createActiveTextAdGroup();
        adGroupInfo2 = adGroupSteps.createActiveTextAdGroup(adGroupInfo1.getClientInfo());

        campaignInfo1 = adGroupInfo1.getCampaignInfo();
        campaignInfo2 = adGroupInfo2.getCampaignInfo();

        shard = adGroupInfo1.getShard();
        campId1 = campaignInfo1.getCampaignId();
        campId2 = campaignInfo2.getCampaignId();
        adGroupId1 = adGroupInfo1.getAdGroupId();
        adGroupId2 = adGroupInfo2.getAdGroupId();
    }

    // минус-фразы на кампанию

    @Test
    public void addCampaignMinusKeywords_OneKeywordDoesNotExist_AddsValidRecord() {
        Map<Long, List<String>> campIdToKeywords = new HashMap<>();
        campIdToKeywords.put(campId1, singletonList(KEYWORD_1));
        keywordCacheRepository.addCampaignMinusKeywords(shard, campIdToKeywords);

        List<DbAddedPhrasesCache> actualCachedKeywords =
                keywordCacheRepository.getCachedKeywordsByCampaignIds(shard, singletonList(campId1));

        assertThat("в базе должна быть ровно 1 запись", actualCachedKeywords, hasSize(1));
        checkCachedMinusKeyword(actualCachedKeywords, campId1, 0L, KEYWORD_1);
    }

    @Test
    public void addCampaignMinusKeywords_OneKeywordExists_UpdatesChangeTime() {
        Map<Long, List<String>> campIdToKeywords = new HashMap<>();
        campIdToKeywords.put(campId1, singletonList(KEYWORD_1));

        keywordCacheRepository.addCampaignMinusKeywords(shard, campIdToKeywords);
        List<DbAddedPhrasesCache> actualCachedKeywords =
                keywordCacheRepository.getCachedKeywordsByCampaignIds(shard, singletonList(campId1));
        checkState(actualCachedKeywords.size() == 1, "в базе должна быть ровно 1 запись");

        setPreviousTimestamp(campId1, 0L);

        keywordCacheRepository.addCampaignMinusKeywords(shard, campIdToKeywords);
        actualCachedKeywords = keywordCacheRepository.getCachedKeywordsByCampaignIds(shard, singletonList(campId1));
        checkState(actualCachedKeywords.size() == 1, "в базе должна быть ровно 1 запись");

        checkCachedMinusKeyword(actualCachedKeywords, campId1, 0L, KEYWORD_1);
    }

    @Test
    public void addCampaignMinusKeywords_TwoKeywordsDoesNotExist_AddsValidRecords() {
        Map<Long, List<String>> campIdToKeywords = new HashMap<>();
        campIdToKeywords.put(campId1, asList(KEYWORD_1, KEYWORD_2));
        keywordCacheRepository.addCampaignMinusKeywords(shard, campIdToKeywords);

        List<DbAddedPhrasesCache> actualCachedKeywords =
                keywordCacheRepository.getCachedKeywordsByCampaignIds(shard, singletonList(campId1));

        assertThat("в базе должно быть ровно 2 записи", actualCachedKeywords, hasSize(2));

        checkCachedMinusKeyword(actualCachedKeywords, campId1, 0L, KEYWORD_1);
        checkCachedMinusKeyword(actualCachedKeywords, campId1, 0L, KEYWORD_2);
    }

    @Test
    public void addCampaignMinusKeywords_OneKeywordExistsAndOneDoesNot_UpdatesChangeTimeAndAddsRecord() {
        Map<Long, List<String>> campIdToKeywords = new HashMap<>();
        campIdToKeywords.put(campId1, singletonList(KEYWORD_1));
        keywordCacheRepository.addCampaignMinusKeywords(shard, campIdToKeywords);

        List<DbAddedPhrasesCache> actualCachedKeywords =
                keywordCacheRepository.getCachedKeywordsByCampaignIds(shard, singletonList(campId1));
        checkState(actualCachedKeywords.size() == 1, "в базе должна быть ровно 1 запись");

        setPreviousTimestamp(campId1, 0L);

        campIdToKeywords.put(campId1, asList(KEYWORD_1, KEYWORD_2));
        keywordCacheRepository.addCampaignMinusKeywords(shard, campIdToKeywords);

        actualCachedKeywords = keywordCacheRepository.getCachedKeywordsByCampaignIds(shard, singletonList(campId1));

        assertThat("в базе должно быть ровно 2 записи", actualCachedKeywords, hasSize(2));

        checkCachedMinusKeyword(actualCachedKeywords, campId1, 0L, KEYWORD_1);
        checkCachedMinusKeyword(actualCachedKeywords, campId1, 0L, KEYWORD_2);
    }

    @Test
    public void addCampaignMinusKeywords_TwoEqualKeywordsForDifferentCampaigns_AddsValidRecords() {
        Map<Long, List<String>> campIdToKeywords = new HashMap<>();
        campIdToKeywords.put(campId1, singletonList(KEYWORD_1));
        campIdToKeywords.put(campId2, singletonList(KEYWORD_1));
        keywordCacheRepository.addCampaignMinusKeywords(shard, campIdToKeywords);

        List<DbAddedPhrasesCache> actualCachedKeywords =
                keywordCacheRepository.getCachedKeywordsByCampaignIds(shard, asList(campId1, campId2));

        assertThat("в базе должно быть ровно 2 записи", actualCachedKeywords, hasSize(2));

        checkCachedMinusKeyword(actualCachedKeywords, campId1, 0L, KEYWORD_1);
        checkCachedMinusKeyword(actualCachedKeywords, campId2, 0L, KEYWORD_1);
    }

    // минус-фразы на группу

    @Test
    public void addAdGroupMinusKeywords_OneKeywordDoesNotExist_AddsValidRecord() {
        Map<AdGroup, List<String>> adGroupToKeywords = new HashMap<>();
        adGroupToKeywords.put(adGroupInfo1.getAdGroup(), singletonList(KEYWORD_1));
        keywordCacheRepository.addAdGroupMinusKeywords(shard, adGroupToKeywords);

        List<DbAddedPhrasesCache> actualCachedKeywords =
                testKeywordCacheRepository.getCachedKeywordsByAdGroupIds(shard, singletonList(adGroupId1));

        assertThat("в базе должна быть ровно 1 запись", actualCachedKeywords, hasSize(1));
        checkCachedMinusKeyword(actualCachedKeywords, campId1, adGroupId1, KEYWORD_1);
    }

    @Test
    public void addAdGroupMinusKeywords_OneKeywordExists_UpdatesChangeTime() {
        Map<AdGroup, List<String>> adGroupToKeywords = new HashMap<>();
        adGroupToKeywords.put(adGroupInfo1.getAdGroup(), singletonList(KEYWORD_1));

        keywordCacheRepository.addAdGroupMinusKeywords(shard, adGroupToKeywords);
        List<DbAddedPhrasesCache> actualCachedKeywords =
                testKeywordCacheRepository.getCachedKeywordsByAdGroupIds(shard, singletonList(adGroupId1));
        checkState(actualCachedKeywords.size() == 1, "в базе должна быть ровно 1 запись");

        setPreviousTimestamp(campId1, adGroupId1);

        keywordCacheRepository.addAdGroupMinusKeywords(shard, adGroupToKeywords);
        actualCachedKeywords = testKeywordCacheRepository.getCachedKeywordsByAdGroupIds(shard, singletonList(adGroupId1));
        checkState(actualCachedKeywords.size() == 1, "в базе должна быть ровно 1 запись");

        checkCachedMinusKeyword(actualCachedKeywords, campId1, adGroupId1, KEYWORD_1);
    }

    @Test
    public void addAdGroupMinusKeywords_TwoKeywordsDoesNotExist_AddsValidRecords() {
        Map<AdGroup, List<String>> adGroupToKeywords = new HashMap<>();
        adGroupToKeywords.put(adGroupInfo1.getAdGroup(), asList(KEYWORD_1, KEYWORD_2));
        keywordCacheRepository.addAdGroupMinusKeywords(shard, adGroupToKeywords);

        List<DbAddedPhrasesCache> actualCachedKeywords =
                testKeywordCacheRepository.getCachedKeywordsByAdGroupIds(shard, singletonList(adGroupId1));

        assertThat("в базе должно быть ровно 2 записи", actualCachedKeywords, hasSize(2));
        checkCachedMinusKeyword(actualCachedKeywords, campId1, adGroupId1, KEYWORD_1);
        checkCachedMinusKeyword(actualCachedKeywords, campId1, adGroupId1, KEYWORD_2);
    }

    @Test
    public void addAdGroupMinusKeywords_OneKeywordExistsAndOneDoesNot_UpdatesChangeTimeAndAddsRecord() {
        Map<AdGroup, List<String>> adGroupToKeywords = new HashMap<>();
        adGroupToKeywords.put(adGroupInfo1.getAdGroup(), singletonList(KEYWORD_1));

        keywordCacheRepository.addAdGroupMinusKeywords(shard, adGroupToKeywords);
        List<DbAddedPhrasesCache> actualCachedKeywords =
                testKeywordCacheRepository.getCachedKeywordsByAdGroupIds(shard, singletonList(adGroupId1));
        checkState(actualCachedKeywords.size() == 1, "в базе должна быть ровно 1 запись");

        setPreviousTimestamp(campId1, adGroupId1);

        adGroupToKeywords.put(adGroupInfo1.getAdGroup(), asList(KEYWORD_1, KEYWORD_2));
        keywordCacheRepository.addAdGroupMinusKeywords(shard, adGroupToKeywords);
        actualCachedKeywords = testKeywordCacheRepository.getCachedKeywordsByAdGroupIds(shard, singletonList(adGroupId1));
        checkState(actualCachedKeywords.size() == 2, "в базе должно быть ровно 2 записи");

        checkCachedMinusKeyword(actualCachedKeywords, campId1, adGroupId1, KEYWORD_1);
        checkCachedMinusKeyword(actualCachedKeywords, campId1, adGroupId1, KEYWORD_2);
    }

    @Test
    public void addAdGroupMinusKeywords_TwoEqualKeywordsForDifferentAdGroups_AddsValidRecords() {
        Map<AdGroup, List<String>> adGroupToKeywords = new HashMap<>();
        adGroupToKeywords.put(adGroupInfo1.getAdGroup(), singletonList(KEYWORD_1));
        adGroupToKeywords.put(adGroupInfo2.getAdGroup(), singletonList(KEYWORD_2));
        keywordCacheRepository.addAdGroupMinusKeywords(shard, adGroupToKeywords);

        List<DbAddedPhrasesCache> actualCachedKeywords =
                testKeywordCacheRepository.getCachedKeywordsByAdGroupIds(shard, asList(adGroupId1, adGroupId2));

        assertThat("в базе должно быть ровно 2 записи", actualCachedKeywords, hasSize(2));
        checkCachedMinusKeyword(actualCachedKeywords, campId1, adGroupId1, KEYWORD_1);
        checkCachedMinusKeyword(actualCachedKeywords, campId2, adGroupId2, KEYWORD_2);
    }

    private void checkCachedMinusKeyword(List<DbAddedPhrasesCache> actualCachedKeywords,
                                         long campaignId, long adGroupId, String minusKeyword) {
        DbAddedPhrasesCache expectedCachedKeyword =
                createExpectedMinusKeywordCache(campaignId, adGroupId, minusKeyword);
        DbAddedPhrasesCache actualCachedKeyword = StreamEx.of(actualCachedKeywords)
                .filter(c ->
                        c.getHash().equals(expectedCachedKeyword.getHash()) &&
                                c.getCampaignId().equals(campaignId) &&
                                c.getAdGroupId().equals(adGroupId))
                .findFirst()
                .orElse(null);

        assertThat("запись в базе для фразы " + minusKeyword + " не найдена",
                actualCachedKeyword, notNullValue());
        assertThat("запись в базе не соответствует ожидаемой", actualCachedKeyword,
                beanDiffer(expectedCachedKeyword).useCompareStrategy(STRATEGY));
        assertDateBounds(actualCachedKeyword.getAddDate());
    }

    private DbAddedPhrasesCache createExpectedMinusKeywordCache(long campaignId, long adGroupId1,
                                                                String minusKeyword) {
        return new DbAddedPhrasesCache()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId1)
                .withBidsId(0L)
                .withType(DbAddedPhraseType.MINUS)
                .withHash(getMd5HalfHashUtf8(minusKeyword));
    }

    private void setPreviousTimestamp(long campaignId, long adGroupId) {
        LocalDateTime prevLocalDateTime = LocalDateTime.now().minusDays(1).withNano(0);
        dslContextProvider.ppc(shard)
                .update(ADDED_PHRASES_CACHE)
                .set(ADDED_PHRASES_CACHE.ADD_DATE, prevLocalDateTime)
                .where(ADDED_PHRASES_CACHE.CID.eq(campaignId))
                .and(ADDED_PHRASES_CACHE.PID.eq(adGroupId))
                .execute();

        LocalDateTime prevLocalDateTimeToCheck = dslContextProvider.ppc(shard)
                .select(ADDED_PHRASES_CACHE.ADD_DATE)
                .from(ADDED_PHRASES_CACHE)
                .where(ADDED_PHRASES_CACHE.CID.eq(campaignId))
                .and(ADDED_PHRASES_CACHE.PID.eq(adGroupId))
                .fetchOne()
                .value1();

        checkState(prevLocalDateTimeToCheck.equals(prevLocalDateTime),
                "не удалось выставить в базе дату в прошлом времени для проведения теста");
    }

    public static void assertDateBounds(LocalDateTime checkedDateTime) {
        LocalDateTime lowerBound = LocalDateTime.now().minusMinutes(1);
        LocalDateTime upperBound = LocalDateTime.now().plusMinutes(1);
        assertThat("время должно быть выставлено в \"сейчас\"",
                checkedDateTime.isAfter(lowerBound) && checkedDateTime.isBefore(upperBound), is(true));
    }
}
