package ru.yandex.direct.core.entity.keyword.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_PHRASEID_HISTORY;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordRepositoryUpdateTest {

    private static final CompareStrategy UPDATE_COMPARE_STRATEGY =
            allFieldsExcept(newPath("isSuspended"), newPath("needCheckPlaceModified"), newPath("showsForecast"),
                    newPath("phraseBsId"));
    private static final CompareStrategy UPDATE_BIDS_HREF_PARAMS_COMPARE_STRATEGY =
            onlyFields(newPath("hrefParam1"), newPath("hrefParam2"));
    private static final CompareStrategy UPDATE_BIDS_PHRASE_HISTORY_COMPARE_STRATEGY =
            onlyFields(newPath("phraseIdHistory"), newPath("modificationTime"));

    @Autowired
    private Steps steps;

    @Autowired
    private TestKeywordRepository testKeywordRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private KeywordRepository repoUnderTest;

    private int shard;
    private Keyword keywordFirst;
    private Keyword keywordSecond;
    private ClientId clientId;

    @Before
    public void before() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        shard = adGroupInfo.getShard();
        keywordFirst = steps.keywordSteps().createKeyword(adGroupInfo).getKeyword();
        keywordSecond = steps.keywordSteps().createKeyword(adGroupInfo).getKeyword();
        clientId = adGroupInfo.getClientId();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateKeywords_BidsFieldsUpdated() {
        AppliedChanges<Keyword> appliedChanges1 =
                changeKeyword(keywordFirst, "new first phrase", "new first norm phrase", 3, 10.12, 11.12, 2,
                        Place.PREMIUM);
        AppliedChanges<Keyword> appliedChanges2 =
                changeKeyword(keywordSecond, "new second phrase", "new second norm phrase", 3, 9.12, 15.12, 3,
                        Place.GUARANTEE1);

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, asList(appliedChanges1, appliedChanges2));
        });

        List<Keyword> actualKeywords =
                repoUnderTest.getKeywordsByIds(shard, clientId, asList(keywordFirst.getId(), keywordSecond.getId()));
        assertThat(actualKeywords,
                Matchers.contains(beanDiffer(keywordFirst).useCompareStrategy(UPDATE_COMPARE_STRATEGY),
                        beanDiffer(keywordSecond).useCompareStrategy(UPDATE_COMPARE_STRATEGY)));
    }

    @Test
    public void updateKeywords_BidsHrefParamsAdded() {
        AppliedChanges<Keyword> appliedChanges1 =
                changeKeywordHrefParams(keywordFirst, "new first param1", "new first param2");
        AppliedChanges<Keyword> appliedChanges2 =
                changeKeywordHrefParams(keywordSecond, "new second param1", "new second param2");

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, asList(appliedChanges1, appliedChanges2));
        });

        checkBidsHrefParams();
    }

    @Test
    public void updateKeywords_BidsHrefParamsFieldsUpdated() {
        keywordFirst.withHrefParam1("first param1").withHrefParam2("first param2");
        keywordSecond.withHrefParam1("second param1").withHrefParam2("second param2");
        testKeywordRepository.addKeywordToBidsHrefParamsTable(shard, keywordFirst);
        testKeywordRepository.addKeywordToBidsHrefParamsTable(shard, keywordSecond);

        AppliedChanges<Keyword> appliedChanges1 =
                changeKeywordHrefParams(keywordFirst, "new first param1", "new first param2");
        AppliedChanges<Keyword> appliedChanges2 =
                changeKeywordHrefParams(keywordSecond, "new second param1", "new second param2");

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, asList(appliedChanges1, appliedChanges2));
        });

        checkBidsHrefParams();
    }

    @Test
    public void updateKeywords_BidsHrefParamsDeleted() {
        keywordFirst.withHrefParam1("first param1").withHrefParam2("first param2");
        keywordSecond.withHrefParam1("second param1").withHrefParam2("second param2");
        testKeywordRepository.addKeywordToBidsHrefParamsTable(shard, keywordFirst);
        testKeywordRepository.addKeywordToBidsHrefParamsTable(shard, keywordSecond);

        AppliedChanges<Keyword> appliedChanges1 =
                changeKeywordHrefParams(keywordFirst, null, null);
        AppliedChanges<Keyword> appliedChanges2 =
                changeKeywordHrefParams(keywordSecond, null, null);

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, asList(appliedChanges1, appliedChanges2));
        });

        checkBidsHrefParams();
    }

    @Test
    public void updateKeywords_BidsPhraseHistoryAdded() {
        AppliedChanges<Keyword> appliedChanges1 = new ModelChanges<>(keywordFirst.getId(), Keyword.class)
                .process(History.parse("O1"), Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordFirst);
        AppliedChanges<Keyword> appliedChanges2 = new ModelChanges<>(keywordSecond.getId(), Keyword.class)
                .process(History.parse("O2"), Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordSecond);

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, asList(appliedChanges1, appliedChanges2));
        });

        checkBidsPhraseIdHistory();
    }

    @Test
    public void updateKeywords_BidsPhraseHistoryUpdated() {
        keywordFirst.withPhraseIdHistory(History.parse("O11"));
        keywordSecond.withPhraseIdHistory(History.parse("O21"));
        testKeywordRepository.addKeywordToBidsPhraseHistoryTable(shard, keywordFirst);
        testKeywordRepository.addKeywordToBidsPhraseHistoryTable(shard, keywordSecond);

        AppliedChanges<Keyword> appliedChanges1 = new ModelChanges<>(keywordFirst.getId(), Keyword.class)
                .process(History.parse("O12"), Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordFirst);
        AppliedChanges<Keyword> appliedChanges2 = new ModelChanges<>(keywordSecond.getId(), Keyword.class)
                .process(History.parse("O22"), Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordSecond);

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, asList(appliedChanges1, appliedChanges2));
        });

        checkBidsPhraseIdHistory();
    }

    @Test
    public void updateKeywords_UpdatePhraseHistory_UpdateTimeChanged() {
        keywordFirst.withPhraseIdHistory(History.parse("O11"));

        LocalDateTime initialUpdateTime = LocalDateTime.now().minus(3, ChronoUnit.HOURS).withNano(0);
        testKeywordRepository.addKeywordToBidsPhraseHistoryTable(shard, keywordFirst, initialUpdateTime);
        AppliedChanges<Keyword> appliedChanges = new ModelChanges<>(keywordFirst.getId(), Keyword.class)
                .process(History.parse("O12"), Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordFirst);

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, singletonList(appliedChanges));
        });

        LocalDateTime actualUpdateTime = getPhraseIdHistoryUpdateTime(keywordFirst.getCampaignId(),
                singletonList(keywordFirst.getId())).get(0);
        assertThat("update_time должен был ÷змениться", actualUpdateTime, greaterThan(initialUpdateTime));
    }

    @Test
    public void updateKeywords_BidsPhraseHistorysDeleted() {
        keywordFirst.withPhraseIdHistory(History.parse("O1"));
        keywordSecond.withPhraseIdHistory(History.parse("O2"));
        testKeywordRepository.addKeywordToBidsPhraseHistoryTable(shard, keywordFirst);
        testKeywordRepository.addKeywordToBidsPhraseHistoryTable(shard, keywordSecond);

        AppliedChanges<Keyword> appliedChanges1 = new ModelChanges<>(keywordFirst.getId(), Keyword.class)
                .process(null, Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordFirst);
        AppliedChanges<Keyword> appliedChanges2 = new ModelChanges<>(keywordSecond.getId(), Keyword.class)
                .process(null, Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordSecond);

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, asList(appliedChanges1, appliedChanges2));
        });

        checkBidsPhraseIdHistory();
    }

    @Test
    public void updateKeywords_BidsPhraseHistory_NotUpdated() {
        String phraseIdHistory =
                "O17208788;G2947883827;P1668811195,1668811195;:4871622287;4814709126:4871870749;im:4871622288";
        keywordFirst.withPhraseIdHistory(History.parse(phraseIdHistory));
        LocalDateTime expectedUpdateTime = LocalDateTime.now().minus(3, ChronoUnit.HOURS).withNano(0);
        testKeywordRepository.addKeywordToBidsPhraseHistoryTable(shard, keywordFirst, expectedUpdateTime);

        AppliedChanges<Keyword> appliedChanges = new ModelChanges<>(keywordFirst.getId(), Keyword.class)
                .process(History.parse(phraseIdHistory), Keyword.PHRASE_ID_HISTORY)
                .applyTo(keywordFirst);

        dslContextProvider.ppc(shard).transaction(ctx -> {
            repoUnderTest.update(ctx, singletonList(appliedChanges));
        });
        LocalDateTime actualUpdateTime =
                getPhraseIdHistoryUpdateTime(keywordFirst.getCampaignId(), singletonList(keywordFirst.getId())).get(0);
        assertThat("update_time не должен был измениться", actualUpdateTime, is(expectedUpdateTime));
    }

    private List<LocalDateTime> getPhraseIdHistoryUpdateTime(long campaignId, List<Long> keywordIds) {
        return dslContextProvider.ppc(shard)
                .select(BIDS_PHRASEID_HISTORY.UPDATE_TIME)
                .from(BIDS_PHRASEID_HISTORY)
                .where(BIDS_PHRASEID_HISTORY.CID.eq(campaignId))
                .and(BIDS_PHRASEID_HISTORY.ID.in(keywordIds))
                .fetch(BIDS_PHRASEID_HISTORY.UPDATE_TIME);
    }

    @SuppressWarnings("unchecked")
    private void checkBidsHrefParams() {
        List<Keyword> actualKeywords =
                repoUnderTest.getKeywordsByIds(shard, clientId, asList(keywordFirst.getId(), keywordSecond.getId()));
        assertThat(actualKeywords,
                Matchers.contains(beanDiffer(keywordFirst).useCompareStrategy(UPDATE_BIDS_HREF_PARAMS_COMPARE_STRATEGY),
                        beanDiffer(keywordSecond).useCompareStrategy(UPDATE_BIDS_HREF_PARAMS_COMPARE_STRATEGY)));
    }

    @SuppressWarnings("unchecked")
    private void checkBidsPhraseIdHistory() {
        List<Keyword> actualKeywords =
                repoUnderTest.getKeywordsByIds(shard, clientId, asList(keywordFirst.getId(), keywordSecond.getId()));
        assertThat(actualKeywords,
                Matchers.contains(
                        beanDiffer(keywordFirst).useCompareStrategy(UPDATE_BIDS_PHRASE_HISTORY_COMPARE_STRATEGY),
                        beanDiffer(keywordSecond).useCompareStrategy(UPDATE_BIDS_PHRASE_HISTORY_COMPARE_STRATEGY)));
    }


    private AppliedChanges<Keyword> changeKeyword(Keyword keyword, String phrase, String normPhrase, Integer wordCount,
                                                  Double price, Double priceContext, Integer autoBudgetPriority, Place place) {
        return new ModelChanges<>(keyword.getId(), Keyword.class)
                .process(phrase, Keyword.PHRASE)
                .process(normPhrase, Keyword.NORM_PHRASE)
                .process(wordCount, Keyword.WORDS_COUNT)
                .process(BigDecimal.valueOf(price), Keyword.PRICE)
                .process(BigDecimal.valueOf(priceContext), Keyword.PRICE_CONTEXT)
                .process(autoBudgetPriority, Keyword.AUTOBUDGET_PRIORITY)
                .process(place, Keyword.PLACE)
                .process(StatusBsSynced.SENDING, Keyword.STATUS_BS_SYNCED)
                .process(StatusModerate.YES, Keyword.STATUS_MODERATE)
                .process(now().plusSeconds(10).truncatedTo(ChronoUnit.SECONDS), Keyword.MODIFICATION_TIME)
                .applyTo(keyword);
    }

    private AppliedChanges<Keyword> changeKeywordHrefParams(Keyword keyword, String hrefParam1, String hrefParam2) {
        return new ModelChanges<>(keyword.getId(), Keyword.class)
                .process(hrefParam1, Keyword.HREF_PARAM1)
                .process(hrefParam1, Keyword.HREF_PARAM2)
                .process(now().plusSeconds(10).truncatedTo(ChronoUnit.SECONDS), Keyword.MODIFICATION_TIME)
                .applyTo(keyword);
    }


}
