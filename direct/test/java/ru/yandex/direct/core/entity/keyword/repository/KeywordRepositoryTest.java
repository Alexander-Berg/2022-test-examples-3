package ru.yandex.direct.core.entity.keyword.repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.keyword.container.CampaignIdAndKeywordIdPair;
import ru.yandex.direct.core.entity.keyword.container.KeywordDeleteInfo;
import ru.yandex.direct.core.entity.keyword.container.PhraseIdHistoryInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.KeywordModeration;
import ru.yandex.direct.core.entity.keyword.model.KeywordText;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.entity.statistics.container.ChangedPhraseIdInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImageFormat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_MANUAL_PRICES;
import static ru.yandex.direct.dbschema.ppc.tables.BidsBase.BIDS_BASE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordRepositoryTest {

    static final CompareStrategy KW_COMPARE_STRATEGY = DefaultCompareStrategies
            .onlyFields(newPath("id"), newPath("campaignId"), newPath("adGroupId"), newPath("phrase"));

    @Autowired
    private Steps steps;

    @Autowired
    private TestKeywordRepository testKeywordRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private KeywordRepository repo;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
    }

    @Test
    public void getKeywordsByAdGroupId_emptyKeywords() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();

        List<Keyword> res = repo.getKeywordsByAdGroupId(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        assertThat("должна быть пустая коллекция по id группы", res, empty());
    }

    @Test
    public void getKeywordsByAdGroupId_OneKeyword() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();

        List<Keyword> res = repo.getKeywordsByAdGroupId(keywordInfo.getShard(), keywordInfo.getAdGroupId());
        assertThat("должно быть 1 ключевое слово по группе", res, hasSize(1));
    }

    @Test
    public void getKeywordsByAdgroupId_SeveralKeywords() {
        final int keywordsInAdGroupNum = 3;
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        for (int i = 0; i < keywordsInAdGroupNum; i++) {
            steps.keywordSteps().createKeyword(adGroupInfo);
        }

        List<Keyword> res = repo.getKeywordsByAdGroupId(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        assertThat("количество слов по группе должно совпадать с заданным", res, hasSize(keywordsInAdGroupNum));

        Set<Long> adGroupIds = res.stream().map(Keyword::getAdGroupId).collect(toSet());
        assertThat("ключевые слова должны принадлежать запрашиваемой группе", adGroupIds,
                containsInAnyOrder(adGroupInfo.getAdGroupId()));
    }

    @Test
    public void getKeywordByCampaignId_OneKeyword() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();

        List<Keyword> res = repo.getKeywordsByCampaignId(keywordInfo.getShard(), keywordInfo.getCampaignId());
        assertThat("должно быть 1 ключевое слово по кампании", res, hasSize(1));
    }

    @Test
    public void getKeywordsByCampaignId_emptyKeywords() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();

        List<Keyword> res = repo.getKeywordsByCampaignId(campaignInfo.getShard(), campaignInfo.getCampaignId());
        assertThat("должна быть пустая коллекция по id кампании", res, empty());
    }

    @Test
    public void getKeywordsByCampaignId_SeveralKeywords() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        final int adGroupsNum = 2;
        final int keywordsInAdGroupNum = 3;
        for (int k = 0; k < adGroupsNum; k++) {
            AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
            for (int i = 0; i < keywordsInAdGroupNum; i++) {
                steps.keywordSteps().createKeyword(adGroupInfo);
            }
        }
        final int expectedValue = adGroupsNum * keywordsInAdGroupNum;

        List<Keyword> res = repo.getKeywordsByCampaignId(campaignInfo.getShard(), campaignInfo.getCampaignId());
        assertThat("количество слов по кампании должно совпадать с заданным", res, hasSize(expectedValue));

        Set<Long> campaignIds = res.stream().map(Keyword::getCampaignId).collect(toSet());
        assertThat("ключевые слова должны принадлежать запрашиваемой кампании", campaignIds,
                containsInAnyOrder(campaignInfo.getCampaignId()));
    }

    @Test
    public void getKeywordsByCampaignIds_noFilterByClientId_ReturnsKeywordsForEachRequestedCampaign() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        long firstClientCampaignId = firstClientCampaignInfo.getCampaignId();
        // Вторая кампания создаётся под другим ClientId
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        long secondClientCampaignId = secondClientCampaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        KeywordInfo firstClientKeywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        KeywordInfo secondClientKeywordInfo = steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allCampaignIds = asList(firstClientCampaignId, secondClientCampaignId);
        Map<Long, List<Keyword>> result = repo.getKeywordsByCampaignIds(
                firstClientCampaignInfo.getShard(), null, allCampaignIds);

        HashSet<Long> expected = new HashSet<>(allCampaignIds);
        assertThat("в результирующей мапе должны присутствовать ключевые фразы для всех запрашиваемых кампаний",
                result.keySet(), equalTo(expected));

        assertThat(
                "ключевая фраза для первой кампании не соответствует ожидаемой",
                result.get(firstClientCampaignId).get(0),
                beanDiffer(createExpectedKeyword(firstClientKeywordInfo)).useCompareStrategy(KW_COMPARE_STRATEGY));
        assertThat(
                "ключевая фраза для второй кампании не соответствует ожидаемой",
                result.get(secondClientCampaignId).get(0),
                beanDiffer(createExpectedKeyword(secondClientKeywordInfo)).useCompareStrategy(KW_COMPARE_STRATEGY));
    }

    @Test
    public void getKeywordsByCampaignIds_filterByClientId_ReturnOnlyStatusNo() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();

        Campaign secondClientCampaign = newTextCampaign(firstClientCampaignInfo.getClientId(), null);

        secondClientCampaign.setStatusEmpty(true);

        // Вторая кампания для того же ClientId имеет statusEmpty == Yes
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withCampaign(secondClientCampaign)
                .withClientInfo(firstClientCampaignInfo.getClientInfo()));

        AdGroupInfo firstAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        steps.keywordSteps().createKeyword(firstAdGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allCampaignIds = asList(firstClientCampaignInfo.getCampaignId(), secondAdGroupInfo.getCampaignId());
        Map<Long, List<Keyword>> result = repo.getKeywordsByCampaignIds(
                firstClientCampaignInfo.getShard(), firstClientCampaignInfo.getClientId(), allCampaignIds);

        HashSet<Long> expected = new HashSet<>(singletonList(firstClientCampaignInfo.getCampaignId()));

        assertThat(
                "в результирующей мапе должны присутствовать ключевые фразы только для кампании со statusEmpty == No",
                result.keySet(), equalTo(expected));
    }

    @Test
    public void getKeywordsByCampaignIds_filterByClientId_ReturnsKeywordsOnlyForRequestedClient() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        // Вторая кампания создаётся под другим ClientId
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();

        AdGroupInfo firstAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        steps.keywordSteps().createKeyword(firstAdGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allCampaignIds = asList(firstClientCampaignInfo.getCampaignId(), secondAdGroupInfo.getCampaignId());
        Map<Long, List<Keyword>> result = repo.getKeywordsByCampaignIds(
                firstClientCampaignInfo.getShard(), firstClientCampaignInfo.getClientId(), allCampaignIds);

        HashSet<Long> expected = new HashSet<>(singletonList(firstClientCampaignInfo.getCampaignId()));
        assertThat("в результирующей мапе должны присутствовать ключевые фразы для всех запрашиваемых кампаний",
                result.keySet(), equalTo(expected));
    }

    @Test
    public void getKeywordsByAdGroupIds_noFilterByClientId_ReturnsKeywordsForEachRequestedAdGroup() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        // Вторая кампания создаётся под другим ClientId
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();

        AdGroupInfo firstAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        long firstClientAdGroupId = firstAdGroupInfo.getAdGroupId();
        KeywordInfo firstClientKeywordInfo = steps.keywordSteps().createKeyword(firstAdGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        long secondClientAdGroupId = secondAdGroupInfo.getAdGroupId();
        KeywordInfo secondClientKeywordInfo = steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allAdGroupsIds = asList(firstClientAdGroupId, secondClientAdGroupId);
        Map<Long, List<Keyword>> result = repo.getKeywordsByAdGroupIds(
                firstAdGroupInfo.getShard(), null, allAdGroupsIds);

        HashSet<Long> expected = new HashSet<>(allAdGroupsIds);
        assertThat("в результирующей мапе должны присутствовать ключевые фразы для всех запрашиваемых групп",
                result.keySet(), equalTo(expected));

        assertThat(
                "ключевая фраза для первой кампании не соответствует ожидаемой",
                result.get(firstClientAdGroupId).get(0),
                beanDiffer(createExpectedKeyword(firstClientKeywordInfo)).useCompareStrategy(KW_COMPARE_STRATEGY));
        assertThat(
                "ключевая фраза для второй кампании не соответствует ожидаемой",
                result.get(secondClientAdGroupId).get(0),
                beanDiffer(createExpectedKeyword(secondClientKeywordInfo)).useCompareStrategy(KW_COMPARE_STRATEGY));
    }

    @Test
    public void getKeywordsByAdgroupIds_filterByClientId_ReturnsKeywordsOnlyForRequestedClient() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        // Вторая кампания создаётся под другим ClientId
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();

        AdGroupInfo firstAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        steps.keywordSteps().createKeyword(firstAdGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allAdGroupsIds = asList(firstAdGroupInfo.getAdGroupId(), secondAdGroupInfo.getAdGroupId());
        Map<Long, List<Keyword>> result = repo.getKeywordsByAdGroupIds(
                firstAdGroupInfo.getShard(), firstAdGroupInfo.getClientId(), allAdGroupsIds);

        HashSet<Long> expected = new HashSet<>(singletonList(firstAdGroupInfo.getAdGroupId()));
        assertThat("в результирующей мапе должны присутствовать ключевые фразы для всех запрашиваемых групп",
                result.keySet(), equalTo(expected));
    }

    @Test
    public void getKeywordTextsByCampaignIds_noFilterByClientId_ReturnsKeywordsForEachRequestedCampaign() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        long firstClientCampaignId = firstClientCampaignInfo.getCampaignId();
        // Вторая кампания создаётся под другим ClientId
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        long secondClientCampaignId = secondClientCampaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        KeywordInfo firstClientKeywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        KeywordInfo secondClientKeywordInfo = steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allCampaignIds = asList(firstClientCampaignId, secondClientCampaignId);
        Map<Long, List<KeywordText>> result = repo.getKeywordTextsByCampaignIds(
                firstClientCampaignInfo.getShard(), null, allCampaignIds);

        HashSet<Long> expected = new HashSet<>(allCampaignIds);
        assertThat("в результирующей мапе должны присутствовать ключевые фразы для всех запрашиваемых групп",
                result.keySet(), equalTo(expected));

        assertThat(
                "ключевая фраза для первой кампании не соответствует ожидаемой",
                result.get(firstClientCampaignId).get(0),
                beanDiffer(createExpectedKeyword(firstClientKeywordInfo)));
        assertThat(
                "ключевая фраза для второй кампании не соответствует ожидаемой",
                result.get(secondClientCampaignId).get(0),
                beanDiffer(createExpectedKeyword(secondClientKeywordInfo)));
    }

    @Test
    public void getKeywordTextsByAdGroupIds_noFilterByClientId_ReturnsKeywordsForEachRequestedAdGroup() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        // Вторая кампания создаётся под другим ClientId
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();

        AdGroupInfo firstAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        long firstClientAdGroupId = firstAdGroupInfo.getAdGroupId();
        KeywordInfo firstClientKeywordInfo = steps.keywordSteps().createKeyword(firstAdGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        long secondClientAdGroupId = secondAdGroupInfo.getAdGroupId();
        KeywordInfo secondClientKeywordInfo = steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allAdGroupsIds = asList(firstClientAdGroupId, secondClientAdGroupId);
        Map<Long, List<KeywordText>> result = repo.getKeywordTextsByAdGroupIds(
                firstAdGroupInfo.getShard(), null, allAdGroupsIds);

        HashSet<Long> expected = new HashSet<>(allAdGroupsIds);
        assertThat("в результирующей мапе должны присутствовать ключевые фразы для всех запрашиваемых групп",
                result.keySet(), equalTo(expected));

        assertThat(
                "ключевая фраза для первой кампании не соответствует ожидаемой",
                result.get(firstClientAdGroupId).get(0),
                beanDiffer(createExpectedKeyword(firstClientKeywordInfo)));
        assertThat(
                "ключевая фраза для второй кампании не соответствует ожидаемой",
                result.get(secondClientAdGroupId).get(0),
                beanDiffer(createExpectedKeyword(secondClientKeywordInfo)));
    }

    @Test
    public void getKeywordModerationsByAdGroupIds_noFilterByClientId_ReturnsKeywordsForEachRequestedAdGroup() {
        // Все создаваемые клиенты располагаются в одном shard'е
        CampaignInfo firstClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();
        // Вторая кампания создаётся под другим ClientId
        CampaignInfo secondClientCampaignInfo = steps.campaignSteps().createDefaultCampaign();

        AdGroupInfo firstAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(firstClientCampaignInfo);
        long firstClientAdGroupId = firstAdGroupInfo.getAdGroupId();
        KeywordInfo firstClientKeywordInfo = steps.keywordSteps().createKeyword(firstAdGroupInfo);
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientCampaignInfo);
        long secondClientAdGroupId = secondAdGroupInfo.getAdGroupId();
        KeywordInfo secondClientKeywordInfo = steps.keywordSteps().createKeyword(secondAdGroupInfo);


        List<Long> allAdGroupsIds = asList(firstClientAdGroupId, secondClientAdGroupId);
        Map<Long, List<KeywordModeration>> result = repo.getKeywordModerationsByAdGroupIds(
                firstAdGroupInfo.getShard(), null, allAdGroupsIds);

        HashSet<Long> expected = new HashSet<>(allAdGroupsIds);
        assertThat("в результирующей мапе должны присутствовать ключевые фразы для всех запрашиваемых групп",
                result.keySet(), equalTo(expected));

        assertThat(
                "ключевая фраза для первой кампании не соответствует ожидаемой",
                result.get(firstClientAdGroupId).get(0),
                beanDiffer(createExpectedKeywordModeration(firstClientKeywordInfo)));
        assertThat(
                "ключевая фраза для второй кампании не соответствует ожидаемой",
                result.get(secondClientAdGroupId).get(0),
                beanDiffer(createExpectedKeywordModeration(secondClientKeywordInfo)));
    }

    @Test
    public void deleteKeyword_OneKeyword() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();
        dslContextProvider.ppc(keywordInfo.getShard()).transaction(ctx -> {
            repo.deleteKeywords(ctx, singletonList(
                    new CampaignIdAndKeywordIdPair(keywordInfo.getCampaignId(), keywordInfo.getId())));
        });

        List<Keyword> keywords =
                repo.getKeywordsByIds(keywordInfo.getShard(), keywordInfo.getAdGroupInfo().getClientId(),
                        singletonList(keywordInfo.getId()));
        assertThat("Ключевая фраза должна быть удалена", keywords, empty());
    }

    @Test
    public void deleteKeyword_TwoKeywords_AllDeleted() {
        KeywordInfo keywordInfo1 = steps.keywordSteps().createDefaultKeyword();
        KeywordInfo keywordInfo2 = steps.keywordSteps().createDefaultKeyword();
        dslContextProvider.ppc(keywordInfo1.getShard()).transaction(ctx -> {
            CampaignIdAndKeywordIdPair key1 = new CampaignIdAndKeywordIdPair(keywordInfo1.getCampaignId(), keywordInfo1.getId());
            CampaignIdAndKeywordIdPair key2 = new CampaignIdAndKeywordIdPair(keywordInfo2.getCampaignId(), keywordInfo2.getId());
            repo.deleteKeywords(ctx, asList(key1, key2));
        });

        List<Keyword> keywords =
                repo.getKeywordsByIds(keywordInfo1.getShard(), keywordInfo1.getAdGroupInfo().getClientId(),
                        asList(keywordInfo1.getId(), keywordInfo2.getId()));
        assertThat("Ключевые фразы должны быть удалены", keywords, empty());
    }

    @Test
    public void deleteKeyword_TwoKeywords_OneDeleted() {
        KeywordInfo keywordInfo1 = steps.keywordSteps().createDefaultKeyword();
        KeywordInfo keywordInfo2 = steps.keywordSteps().createDefaultKeyword();
        dslContextProvider.ppc(keywordInfo1.getShard()).transaction(ctx -> {
            CampaignIdAndKeywordIdPair key2 = new CampaignIdAndKeywordIdPair(keywordInfo2.getCampaignId(), keywordInfo2.getId());
            repo.deleteKeywords(ctx, singletonList(key2));
        });

        List<Keyword> keywords =
                repo.getKeywordsByIds(keywordInfo1.getShard(), keywordInfo1.getAdGroupInfo().getClientId(),
                        asList(keywordInfo1.getId(), keywordInfo2.getId()));
        List<Long> actualIds = mapList(keywords, Keyword::getId);
        assertThat("Ключевые фразы должны быть удалены", actualIds, contains(keywordInfo1.getId()));
    }

    @Test
    public void getKeywordsByIds_ReturnCorrect() {
        KeywordInfo keywordInfo1 = steps.keywordSteps().createDefaultKeyword();
        KeywordInfo keywordInfo2 = steps.keywordSteps().createDefaultKeyword();

        List<Keyword> keywords =
                repo.getKeywordsByIds(keywordInfo1.getShard(), asList(keywordInfo1.getId(), keywordInfo2.getId()));
        List<Long> actualIds = mapList(keywords, Keyword::getId);
        assertThat(actualIds, contains(keywordInfo1.getId(), keywordInfo2.getId()));
    }

    @Test
    public void getKeywordsByIds_EmptyList_ReturnEmptyList() {
        KeywordInfo keywordInfo1 = steps.keywordSteps().createDefaultKeyword();
        KeywordInfo keywordInfo2 = steps.keywordSteps().createDefaultKeyword();

        List<Keyword> keywords =
                repo.getKeywordsByIds(keywordInfo1.getShard(), emptyList());
        assertThat(keywords, hasSize(0));
    }

    @Test
    public void deleteKeyword_FromBidsHrefParamsTable() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();
        testKeywordRepository.addKeywordToBidsHrefParamsTable(keywordInfo.getShard(), keywordInfo.getKeyword());
        Set<Long> ids = testKeywordRepository
                .getKeywordIdFromBidsHrefParamsTable(keywordInfo.getShard(), singletonList(keywordInfo.getId()));
        assumeThat("Должна быть запись в таблице", ids, contains(keywordInfo.getId()));

        dslContextProvider.ppc(keywordInfo.getShard()).transaction(ctx -> {
            repo.deleteKeywords(ctx, singletonList(
                    new CampaignIdAndKeywordIdPair(keywordInfo.getCampaignId(), keywordInfo.getId())));
        });

        Set<Long> actualIds = testKeywordRepository
                .getKeywordIdFromBidsHrefParamsTable(keywordInfo.getShard(), singletonList(keywordInfo.getId()));
        assertThat("Ключевая фраза должна быть удалена", actualIds, empty());
    }

    @Test
    public void deleteKeyword_FromBidsManualPricesTable() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();
        dslContextProvider.ppc(keywordInfo.getShard())
                .insertInto(BIDS_MANUAL_PRICES)
                .set(BIDS_MANUAL_PRICES.ID, keywordInfo.getId())
                .set(BIDS_MANUAL_PRICES.CID, keywordInfo.getCampaignId())
                .execute();
        Set<Long> ids = testKeywordRepository
                .getKeywordIdFromBidsManualPricesTable(keywordInfo.getShard(), singletonList(keywordInfo.getId()));
        assumeThat("Должна быть запись в таблице", ids, contains(keywordInfo.getId()));

        dslContextProvider.ppc(keywordInfo.getShard()).transaction(ctx -> {
            repo.deleteKeywords(ctx, singletonList(new CampaignIdAndKeywordIdPair(keywordInfo.getCampaignId(), keywordInfo.getId())));
        });

        Set<Long> actualIds = testKeywordRepository
                .getKeywordIdFromBidsManualPricesTable(keywordInfo.getShard(), singletonList(keywordInfo.getId()));
        assertThat("Ключевая фраза должна быть удалена", actualIds, empty());
    }

    @Test
    public void deleteKeyword_FromBidsPhraseIdHistoryTable() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();
        testKeywordRepository.addKeywordToBidsPhraseHistoryTable(keywordInfo.getShard(), keywordInfo.getKeyword());
        Set<Long> ids = testKeywordRepository
                .getKeywordIdFromBidsPhraseIdHistoryTable(keywordInfo.getShard(), singletonList(keywordInfo.getId()));
        assumeThat("Должна быть запись в таблице", ids, contains(keywordInfo.getId()));

        dslContextProvider.ppc(keywordInfo.getShard()).transaction(ctx -> {
            repo.deleteKeywords(ctx, singletonList(
                    new CampaignIdAndKeywordIdPair(keywordInfo.getCampaignId(), keywordInfo.getId())));
        });

        Set<Long> actualIds = testKeywordRepository
                .getKeywordIdFromBidsPhraseIdHistoryTable(keywordInfo.getShard(), singletonList(keywordInfo.getId()));
        assertThat("Ключевая фраза должна быть удалена", actualIds, empty());
    }

    @Test
    public void addKeyword_ReturnsPositiveId() {
        Keyword keyword = getKeywordForAdd();
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        List<Long> ids = repo.addKeywords(conf, singletonList(keyword));
        assertThat("метод вернул один положительный id", ids, contains(greaterThan(0L)));
    }

    @Test
    public void addTwoKeywords_ReturnsPositiveIds() {
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        List<Long> ids = repo.addKeywords(conf, asList(getKeywordForAdd(), getKeywordForAdd()));
        assertThat("метод вернул два положительных id", ids, contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void addKeyword_FullSaveCorrectly() {
        Keyword keyword = getKeywordForAddWithoutDefaults();
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        List<Long> ids = repo.addKeywords(conf, singletonList(keyword));
        Keyword keywordFromDB = repo.getKeywordsByIds(adGroupInfo.getShard(), adGroupInfo.getClientId(), ids).get(0);
        assertThat("ключевая фраза корректно сохранена и извлечена из базы", keywordFromDB, beanDiffer(keyword));
    }

    @Test
    public void addKeyword_ZeroPrices_SaveCorrectly() {
        Keyword keyword =
                getKeywordForAddWithoutDefaults().withPrice(BigDecimal.ZERO).withPriceContext(BigDecimal.ZERO);
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        List<Long> ids = repo.addKeywords(conf, singletonList(keyword));
        Keyword keywordFromDB = repo.getKeywordsByIds(adGroupInfo.getShard(), adGroupInfo.getClientId(), ids).get(0);
        assertThat("ключевая фраза корректно сохранена и извлечена из базы", keywordFromDB,
                beanDiffer(keyword.withPrice(null).withPriceContext(null)));
    }

    @Test
    public void addKeyword_NotGetKeywordsFromBidsBase() {
        Keyword keyword = getKeywordForAddWithoutDefaults();
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        List<Long> ids = repo.addKeywords(conf, singletonList(keyword));

        List<Bid> bidsFromDB = bidRepository.getRelevanceMatchByIds(adGroupInfo.getShard(), ids);
        MatcherAssert.assertThat("по добавленному keyword ничего не получаем из bids_base", bidsFromDB, empty());
    }

    @Test
    public void addKeyword_NullPhraseIdHistory_SaveCorrectly() {
        Keyword keyword = getKeywordForAddWithoutDefaults().withPhraseIdHistory(null);
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        List<Long> ids = repo.addKeywords(conf, singletonList(keyword));
        Keyword keywordFromDB = repo.getKeywordsByIds(adGroupInfo.getShard(), adGroupInfo.getClientId(), ids).get(0);
        assertThat("не должна была добавиться phraseIdHistory", keywordFromDB, beanDiffer(keyword));
    }

    @Test
    public void deduplicateKeywords_OneGroup_NoDuplicateKeywords() {
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        repo.addKeywords(conf, asList(
                getKeywordForAddWithoutDefaults().withPhrase("фраза1"),
                getKeywordForAddWithoutDefaults().withPhrase("фраза2")));
        List<Keyword> deletedKeywords = repo.deduplicateKeywords(conf, singletonList(adGroupInfo.getAdGroupId()));
        assertThat("повторяющиеся фразы не найдены", deletedKeywords, hasSize(0));
        List<Keyword> keywords = repo.getKeywordsByAdGroupId(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        assertThat("фразы не удалены", keywords, hasSize(2));
    }

    @Test
    public void getKeyword_FromBidsArcByCorrectCidNoPids() {
        getKeywordFromBidsArcTestBase(
                () -> repo.getArchivedKeywordsByAdGroupIds(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                        List.of(adGroupInfo.getCampaignId()), List.of()),
                true
        );
    }

    @Test
    public void getKeyword_FromBidsArcByCorrectCidAndPid() {
        getKeywordFromBidsArcTestBase(
                () -> repo.getArchivedKeywordsByAdGroupIds(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                        List.of(adGroupInfo.getCampaignId()), List.of(adGroupInfo.getAdGroupId())),
                true
        );
    }

    @Test
    public void getKeyword_FromBidsArcByIncorrectCidNoPids() {
        getKeywordFromBidsArcTestBase(
                () -> repo.getArchivedKeywordsByAdGroupIds(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                        List.of(adGroupInfo.getCampaignId() + 1), List.of()),
                false
        );
    }

    @Test
    public void getKeyword_FromBidsArcCorrectCidIncorrectPid() {
        getKeywordFromBidsArcTestBase(
                () -> repo.getArchivedKeywordsByAdGroupIds(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                        List.of(adGroupInfo.getCampaignId()), List.of(adGroupInfo.getAdGroupId() + 1)),
                false
        );
    }

    private void getKeywordFromBidsArcTestBase(Supplier<Map<Long, List<Keyword>>> getArcKwsByAdgroupsFromDbSupplier,
                                               boolean resultExpected) {
        Keyword keyword = steps.keywordSteps().createKeyword(adGroupInfo).getKeyword();
        List<Keyword> keywordFromDbList = repo.getKeywordsByIds(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                List.of(keyword.getId()));
        assumeThat(keywordFromDbList, hasSize(1));
        Keyword keywordFromDb = keywordFromDbList.stream().findFirst().orElse(null);
        assumeThat(keywordFromDb, notNullValue());

        repo.archiveKeywords(dslContextProvider.ppc(adGroupInfo.getShard()).configuration(),
                List.of(keyword.getId()));
        assumeThat(repo.getKeywordsByIds(adGroupInfo.getShard(), List.of(keyword.getId())), hasSize(0));

        Map<Long, List<Keyword>> archivedKws = getArcKwsByAdgroupsFromDbSupplier.get();
        if (resultExpected) {
            assertThat("вернулась одна архивная фраза", archivedKws.keySet(), hasSize(1));
            assertThat("вернулась одна фраза для нужной группы", archivedKws.keySet(),
                    contains(is(adGroupInfo.getAdGroupId())));

            Keyword keywordArchivedFromDb = archivedKws.get(adGroupInfo.getAdGroupId())
                    .stream()
                    .filter(t -> t.getId().equals(keyword.getId()))
                    .findFirst()
                    .orElse(null);
            assertThat("вернулась нужная архивная фраза", keywordArchivedFromDb, notNullValue());
            assertThat("архивная фраза совпала с исходной", keywordArchivedFromDb, beanDiffer(keywordFromDb));
        } else {
            List<Keyword> archivedKwsList = nvl(archivedKws.get(adGroupInfo.getAdGroupId()), List.of());
            Keyword keywordArchivedFromDb = archivedKwsList
                    .stream()
                    .filter(t -> t.getId().equals(keyword.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat("не вернулась нужная архивная фраза", keywordArchivedFromDb, nullValue());
        }
    }

    @Test
    public void keywordFromBidsAfterUnarcTest() {
        Keyword keyword = steps.keywordSteps().createKeyword(adGroupInfo).getKeyword();
        List<Long> kwIds = List.of(keyword.getId());
        List<Long> cids = List.of(adGroupInfo.getCampaignId());
        int shard = adGroupInfo.getShard();
        List<Keyword> keywordFromDbList = repo.getKeywordsByIds(shard, kwIds);
        assumeThat(keywordFromDbList, hasSize(1));
        Keyword keywordFromDb = keywordFromDbList.stream().findFirst().orElse(null);
        assumeThat(keywordFromDb, notNullValue());

        Configuration conf = dslContextProvider.ppc(shard).configuration();
        repo.archiveKeywords(conf, kwIds);
        assumeThat(repo.getKeywordsByIds(shard, kwIds), hasSize(0));

        Map<Long, List<Keyword>> archivedKws = repo.getArchivedKeywordsByAdGroupIds(
                shard, adGroupInfo.getClientId(), cids, List.of(adGroupInfo.getAdGroupId()));
        assumeThat(archivedKws.keySet(), hasSize(1));

        repo.unarchiveKeywords(conf, cids, emptyList(), BigDecimal.ZERO);

        List<Keyword> keywordUnarcFromDbList = repo.getKeywordsByIds(shard, kwIds);
        assertThat("вернулась одна разархивированная фраза", keywordUnarcFromDbList, hasSize(1));
        Keyword keywordUnarcFromDb = keywordFromDbList.stream().findFirst().orElse(null);
        assertThat("разархивированная фраза совпала с исходной", keywordUnarcFromDb, beanDiffer(keywordFromDb));
    }

    @Test
    public void addKeyword_NotAddToBidsBase() {
        Keyword keyword = getKeywordForAdd();
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        repo.addKeywords(conf, singletonList(keyword));

        List<Long> bidsBase = getBidIdByCampaignId(adGroupInfo.getShard(), adGroupInfo.getCampaignId());
        assertThat("в bids_base ничего не добавилось", bidsBase, empty());
    }

    @Test
    public void deduplicateKeywords_VariousGroups_DuplicateKeywords() {
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        Keyword keyword = getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза");
        repo.addKeywords(conf, singletonList(keyword));
        Long newAdGroupId = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(adGroupInfo.getCampaignId())).getAdGroupId();
        repo.addKeywords(conf, singletonList(keyword.withAdGroupId(newAdGroupId)));

        List<Long> adGroupIds = asList(adGroupInfo.getAdGroupId(), newAdGroupId);
        List<Keyword> deletedKeywords = repo.deduplicateKeywords(conf, adGroupIds);
        assertThat("повторяющиеся фразы не найдены", deletedKeywords, hasSize(0));
        Set<Long> keywordIds = repo.getKeywordsByAdGroupIds(
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupIds).keySet();
        assertThat("фразы не удалены", keywordIds, hasSize(2));
    }

    @Test
    public void deduplicateKeywords_OneGroupKeepPhraseWithMinIdAndPhraseIdExist() {
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        repo.addKeywords(conf, asList(
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза").withPhraseBsId(BigInteger.ZERO),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза").withPhraseBsId(BigInteger.ZERO)));
        List<Keyword> deletedKeywords = repo.deduplicateKeywords(conf, singletonList(adGroupInfo.getAdGroupId()));
        assertThat("найдены все повторяющиеся фразы", deletedKeywords, hasSize(2));

        List<Keyword> keywords = repo.getKeywordsByAdGroupId(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        assertThat("повторяющиеся фразы удалены", keywords, hasSize(1));
        assertThat("в группе осталась фраза со статистикой", keywords.get(0).getPhraseBsId(), equalTo(BigInteger.ONE));
    }

    @Test
    public void deduplicateKeywords_OneGroup_DuplicateKeywords() {
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        repo.addKeywords(conf, asList(
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза2"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза2")));
        Set<Long> deletedKeywordIds =
                listToSet(repo.deduplicateKeywords(conf, singletonList(adGroupInfo.getAdGroupId())), Keyword::getId);
        assertThat("найдены все повторяющиеся фразы", deletedKeywordIds, hasSize(3));

        List<Keyword> keywords = repo.getKeywordsByAdGroupId(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
        assertThat("повторяющиеся фразы удалены",
                mapList(keywords, Keyword::getId), Matchers.not(contains(deletedKeywordIds)));
    }

    @Test
    public void deduplicateKeywords_TwoGroup_DuplicateKeywords() {
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        Keyword keyword = getKeywordForAddWithoutDefaults();
        //фразы для первой группы
        repo.addKeywords(conf, asList(
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза2"),
                getKeywordForAddWithoutDefaults().withPhrase("повторяющаяся фраза2")));

        //фразы для второй группы
        Long newAdGroupId = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(adGroupInfo.getCampaignId())).getAdGroupId();
        repo.addKeywords(conf, asList(
                getKeywordForAddWithoutDefaults().withAdGroupId(newAdGroupId).withPhrase("повторяющаяся фраза3"),
                getKeywordForAddWithoutDefaults().withAdGroupId(newAdGroupId).withPhrase("повторяющаяся фраза3"),
                getKeywordForAddWithoutDefaults().withAdGroupId(newAdGroupId).withPhrase("повторяющаяся фраза3"),
                getKeywordForAddWithoutDefaults().withAdGroupId(newAdGroupId).withPhrase("повторяющаяся фраза4"),
                getKeywordForAddWithoutDefaults().withAdGroupId(newAdGroupId).withPhrase("повторяющаяся фраза4")));
        List<Long> adGroupIds = asList(adGroupInfo.getAdGroupId(), newAdGroupId);
        Set<Long> deletedKeywordIds = listToSet(repo.deduplicateKeywords(conf, adGroupIds), Keyword::getId);
        assertThat("найдены все повторяющиеся фразы", deletedKeywordIds, hasSize(6));
        Set<Long> keywordIds = repo.getKeywordsByAdGroupIds(
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupIds).keySet();
        assertThat("повторяющиеся фразы удалены", keywordIds, Matchers.not(contains(deletedKeywordIds)));
    }

    @Test
    public void getKeywordDeleteInfo_WorksCorrectly() {
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                .withName("notDefaultName")
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW));
        steps.bannerSteps().createBanner(activeTextBanner().withStatusArchived(true), adGroupInfo2);
        testCampaignRepository.archiveCampaign(adGroupInfo2.getShard(), adGroupInfo2.getCampaignId());
        KeywordInfo defaultKeyword = steps.keywordSteps().createKeyword(adGroupInfo2);
        KeywordDeleteInfo expected = new KeywordDeleteInfo()
                .withOwnerUid(adGroupInfo2.getCampaignInfo().getCampaign().getUid())
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withAdGroupName(adGroupInfo2.getAdGroup().getName())
                .withAdGroupDraft(true)
                .withCampaignId(adGroupInfo2.getCampaignId())
                .withCampaignArchived(true);

        Map<Long, KeywordDeleteInfo> actual =
                repo.getKeywordDeleteInfo(adGroupInfo2.getShard(), singletonList(defaultKeyword.getId()));
        assumeThat(actual.size(), is(1));
        assumeThat(actual.get(defaultKeyword.getId()), notNullValue());
        assertThat(actual.get(defaultKeyword.getId()), beanDiffer(expected));
    }

    @Test
    public void getInfoForPhraseIdHistory_FullInfo() {
        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null), singletonList(activeTextBanner(null, null).withBsBannerId(2L)),
                singletonMap(0, defaultBannerImage(null, null).withBsBannerId(3L)),
                defaultKeyword().withPhraseBsId(BigInteger.ONE));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_WithoutBanners() {
        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null), emptyList(), emptyMap(),
                defaultKeyword().withPhraseBsId(BigInteger.ONE));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_WithoutImageBanners() {
        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null), singletonList(activeTextBanner(null, null).withBsBannerId(2L)),
                emptyMap(), defaultKeyword().withPhraseBsId(BigInteger.ONE));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_SeveralBannersWithImageBanners() {
        List<OldTextBanner> banners = asList(activeTextBanner(null, null).withBsBannerId(2L),
                activeTextBanner(null, null).withBsBannerId(3L));

        Map<Integer, OldBannerImage> bannerImagesMap = new HashMap<>();
        bannerImagesMap.put(0, defaultBannerImage(null, null).withBsBannerId(4L));
        bannerImagesMap.put(1, defaultBannerImage(null, null).withBsBannerId(10L));

        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null), banners, bannerImagesMap,
                defaultKeyword().withPhraseBsId(BigInteger.ONE));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_OneBannerWithoutImageAndOneWith() {
        List<OldTextBanner> banners = asList(activeTextBanner(null, null).withBsBannerId(2L),
                activeTextBanner(null, null).withBsBannerId(3L));
        Map<Integer, OldBannerImage> bannerImagesMap = singletonMap(1, defaultBannerImage(null, null).withBsBannerId(7L));

        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null), banners, bannerImagesMap,
                defaultKeyword().withPhraseBsId(BigInteger.ONE));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_WithoutOrderIdWithBannerAndBannerImage() {
        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null).withOrderId(null),
                singletonList(activeTextBanner(null, null).withBsBannerId(2L)),
                singletonMap(0, defaultBannerImage(null, null).withBsBannerId(3L)),
                defaultKeyword().withPhraseBsId(BigInteger.ONE));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_BannersAndImagesWithoutBannerBsId() {
        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null),
                singletonList(activeTextBanner(null, null)),
                singletonMap(0, defaultBannerImage(null, null)),
                defaultKeyword().withPhraseBsId(BigInteger.ONE));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_WithoutPhraseId() {
        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null), singletonList(activeTextBanner(null, null).withBsBannerId(2L)),
                singletonMap(0, defaultBannerImage(null, null).withBsBannerId(3L)),
                defaultKeyword());

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void getInfoForPhraseIdHistory_WithOldHistory() {
        PhraseIdHistoryTestInfo testInfo = new PhraseIdHistoryTestInfo(adGroupInfo.getClientInfo(),
                activeTextCampaign(null, null), singletonList(activeTextBanner(null, null).withBsBannerId(2L)),
                singletonMap(0, defaultBannerImage(null, null).withBsBannerId(3L)),
                defaultKeyword().withPhraseBsId(BigInteger.ONE)
                        .withPhraseIdHistory(History.parse("O123;P67;G7;63:134;im397:9")));

        getAndCheckPhraseIdHistoryInfo(testInfo);
    }

    @Test
    public void updatePhraseId_fromZeroPhraseIdTest() {
        var newPhraseID = new BigInteger("10750614502062865404");
        KeywordInfo keyword = steps.keywordSteps().createKeyword(defaultKeyword().withPhraseBsId(BigInteger.ZERO));

        var change = new ChangedPhraseIdInfo(keyword.getId(), null, BigInteger.ZERO, newPhraseID);

        repo.updatePhraseId(keyword.getShard(), List.of(change));

        var result = repo.getKeywordsByIds(keyword.getShard(), keyword.getAdGroupInfo().getClientId(),
                List.of(keyword.getId()));

        var sa = new SoftAssertions();

        sa.assertThat(result)
                .first()
                .extracting(Keyword::getPhraseBsId)
                .as("PhraseID changed after update")
                .isEqualTo(newPhraseID);
        sa.assertThat(result)
                .first()
                .extracting(Keyword::getModificationTime)
                .as("mod_time wasn't changed")
                .isEqualTo(keyword.getKeyword().getModificationTime());

        sa.assertAll();
    }

    @Test
    public void updatePhraseId_OldPhraseIdDoesntMatterTest() {
        BigInteger oldPhraseId = BigInteger.valueOf(RandomNumberUtils.nextPositiveInteger());
        BigInteger newPhraseId = new BigInteger("10750614502062865404");
        Keyword keyword = defaultKeyword().withPhraseBsId(oldPhraseId);
        KeywordInfo kwInfo = steps.keywordSteps().createKeyword(keyword);

        var change = new ChangedPhraseIdInfo(kwInfo.getId(), null, oldPhraseId.add(BigInteger.ONE), newPhraseId);

        repo.updatePhraseId(kwInfo.getShard(), List.of(change));

        var result = repo.getKeywordsByIds(kwInfo.getShard(), kwInfo.getAdGroupInfo().getClientId(),
                List.of(kwInfo.getId()));

        Assertions.assertThat(result)
                .first()
                .extracting(Keyword::getPhraseBsId)
                .isEqualTo(newPhraseId);
    }

    @Test
    public void updatePhraseId_bulkTest() {
        BigInteger newPhraseId1 = BigInteger.valueOf(RandomNumberUtils.nextPositiveLong());
        BigInteger newPhraseId2 = BigInteger.valueOf(RandomNumberUtils.nextPositiveLong());

        KeywordInfo kwInfo1 = steps.keywordSteps().createDefaultKeyword();
        KeywordInfo kwInfo2 = steps.keywordSteps().createKeyword(
                defaultKeyword().withPhraseBsId(BigInteger.valueOf(324324)));

        var changes = List.of(
                new ChangedPhraseIdInfo(kwInfo1.getId(), null, kwInfo1.getKeyword().getPhraseBsId(), newPhraseId1),
                new ChangedPhraseIdInfo(kwInfo2.getId(), null, kwInfo2.getKeyword().getPhraseBsId(), newPhraseId2));

        repo.updatePhraseId(kwInfo1.getShard(), changes);

        var sa = new SoftAssertions();

        var result1 = repo.getKeywordsByIds(kwInfo1.getShard(), kwInfo1.getAdGroupInfo().getClientId(),
                List.of(kwInfo1.getId()));
        sa.assertThat(result1)
                .first()
                .extracting(Keyword::getPhraseBsId)
                .isEqualTo(newPhraseId1);

        var result2 = repo.getKeywordsByIds(kwInfo2.getShard(), kwInfo2.getAdGroupInfo().getClientId(),
                List.of(kwInfo2.getId()));
        sa.assertThat(result2)
                .first()
                .extracting(Keyword::getPhraseBsId)
                .isEqualTo(newPhraseId2);

        sa.assertAll();
    }

    private void getAndCheckPhraseIdHistoryInfo(PhraseIdHistoryTestInfo testInfo) {
        PhraseIdHistoryInfo expectedPhraseIdHistoryInfo = getExpectedPhraseIdHistoryInfo(testInfo);

        Map<Long, PhraseIdHistoryInfo> infoForPhraseIdHistory =
                repo.getInfoForPhraseIdHistory(testInfo.campaignInfo.getShard(), testInfo.campaignInfo.getClientId(),
                        singletonList(testInfo.keywordInfo.getId()));
        assertThat("для ключевой фразы не вернулась информация для истории", infoForPhraseIdHistory.keySet(),
                contains(is(testInfo.keywordInfo.getId())));
        PhraseIdHistoryInfo actualInfo = infoForPhraseIdHistory.get(testInfo.keywordInfo.getId());
        assertThat("информация для истории не соответствует ожидаемой", actualInfo,
                beanDiffer(expectedPhraseIdHistoryInfo));
    }

    private PhraseIdHistoryInfo getExpectedPhraseIdHistoryInfo(PhraseIdHistoryTestInfo testInfo) {
        Map<Long, Long> adGroupBanners = testInfo.textBanners.stream().collect(
                toMap(AbstractBannerInfo::getBannerId, textBanner -> nvl(textBanner.getBanner().getBsBannerId(), 0L)));
        Map<Long, Long> adGroupBannerImages = testInfo.bannerImages.stream().collect(
                toMap(b -> b.getBannerImage().getBannerId(), b -> nvl(b.getBannerImage().getBsBannerId(), 0L)));
        return new PhraseIdHistoryInfo()
                .withPhraseId(nvl(testInfo.keywordInfo.getKeyword().getPhraseBsId(), BigInteger.ZERO))
                .withOldHistory(testInfo.keywordInfo.getKeyword().getPhraseIdHistory())
                .withOrderId(nvl(testInfo.campaignInfo.getCampaign().getOrderId(), 0L))
                .withAdGroupId(testInfo.adGroupInfo.getAdGroupId())
                .withAdGroupBanners(adGroupBanners)
                .withAdGroupImageBanners(adGroupBannerImages);
    }

    private class PhraseIdHistoryTestInfo {
        CampaignInfo campaignInfo;
        AdGroupInfo adGroupInfo;
        List<TextBannerInfo> textBanners;
        List<BannerImageInfo<TextBannerInfo>> bannerImages = new ArrayList<>();
        KeywordInfo keywordInfo;

        PhraseIdHistoryTestInfo(ClientInfo clientInfo, Campaign campaign, List<OldTextBanner> banners,
                                Map<Integer, OldBannerImage> bannerImagesMap, Keyword keyword) {
            campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
            adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
            textBanners = banners.stream()
                    .map(banner -> steps.bannerSteps().createBanner(banner, adGroupInfo))
                    .collect(Collectors.toList());
            bannerImagesMap.forEach((textBannerIndex, inputBannerImage) -> {
                TextBannerInfo textBanner = textBanners.get(textBannerIndex);
                bannerImages.add(steps.bannerSteps()
                        .createBannerImage(textBanner, defaultBannerImageFormat(null), inputBannerImage));
            });
            keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo, keyword);
        }
    }

    private Keyword getKeywordForAdd() {
        return defaultKeyword()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPhraseBsId(BigInteger.ZERO);
    }

    private Keyword getKeywordForAddWithoutDefaults() {
        return getKeywordForAdd()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withWordsCount(1)
                .withPrice(new BigDecimal("123.00"))
                .withPriceContext(new BigDecimal("100.00"))
                .withPlace(Place.PREMIUM)
                .withPhraseBsId(BigInteger.ONE)
                .withStatusModerate(StatusModerate.NO)
                .withNeedCheckPlaceModified(false)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withAutobudgetPriority(5)
                .withShowsForecast(7L)
                .withIsSuspended(true)
                .withPhraseIdHistory(History.parse("O1;G2;P3;362:9523;im362:9523"))
                .withHrefParam1("hrefParam1")
                .withHrefParam2("hrefParam2")
                .withModificationTime(LocalDateTime.now().withNano(0));
    }

    private Keyword createExpectedKeyword(KeywordInfo keywordInfo) {
        return new Keyword()
                .withId(keywordInfo.getId())
                .withAdGroupId(keywordInfo.getAdGroupId())
                .withCampaignId(keywordInfo.getCampaignId())
                .withIsAutotargeting(false)
                .withPhrase(keywordInfo.getKeyword().getPhrase());
    }

    private Keyword createExpectedKeywordModeration(KeywordInfo keywordInfo) {
        return createExpectedKeyword(keywordInfo)
                .withNormPhrase(keywordInfo.getKeyword().getNormPhrase())
                .withShowsForecast(keywordInfo.getKeyword().getShowsForecast());
    }

    private List<Long> getBidIdByCampaignId(int shard, Long campaignId) {
        return dslContextProvider.ppc(shard)
                .select(BIDS_BASE.BID_ID, BIDS_BASE.CID)
                .from(BIDS_BASE)
                .where(BIDS_BASE.CID.eq(campaignId))
                .fetch(BIDS_BASE.CID);
    }
}
