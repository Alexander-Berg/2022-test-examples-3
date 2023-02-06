package ru.yandex.direct.oneshot.oneshots.delete_keywords_from_bids_base;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestBidsRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BidsBaseBidType;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.oneshot.oneshots.delete_keywords_from_bids_base.repository.OneshotBidsBaseRepository;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE;

@OneshotTest
@RunWith(Parameterized.class)
public class DeleteKeywordsFromBidsBaseOneshotTest {

    private static final long CHUNK_SIZE_BID_IDS_FOR_TEST = 1L;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private OneshotBidsBaseRepository oneshotBidsBaseRepository;
    @Autowired
    private TestBidsRepository testBidsRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private ShardHelper shardHelper;

    private DeleteKeywordsFromBidsBaseOneshot oneshot;
    private int shard;
    private AdGroupInfo adGroupInfo;

    @Parameterized.Parameter
    public String testDescription;
    @Parameterized.Parameter(1)
    public Set<Long> keywordBidIds;
    @Parameterized.Parameter(2)
    public Set<Long> relevanceMatchBidIds;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{
                        "В BIDS_BASE только одна фраза -> таблица пустая",
                        Set.of(1L),
                        Collections.emptySet()
                },
                new Object[]{
                        "Для удаления фраз в BIDS_BASE нужно сделать несколько итераций -> таблица пустая",
                        Set.of(1L, CHUNK_SIZE_BID_IDS_FOR_TEST * 2L, CHUNK_SIZE_BID_IDS_FOR_TEST * 10L),
                        Collections.emptySet()
                },
                new Object[]{
                        "В BIDS_BASE только бесфразный таргетинг -> в таблице остался бесфразный таргетинг",
                        Collections.emptySet(),
                        Set.of(1L)
                },
                new Object[]{
                        "В BIDS_BASE есть бесфразный таргетинг и фраза " +
                                "-> в таблице остался только бесфразный таргетинг",
                        Set.of(1L),
                        Set.of(2L)
                },
                new Object[]{
                        "В BIDS_BASE есть бесфразный таргетинг и фразы, при этом для удаления фраз нужно сделать " +
                                "несколько итераций -> в таблице остался только бесфразный таргетинг",
                        Set.of(1L, CHUNK_SIZE_BID_IDS_FOR_TEST * 3L, CHUNK_SIZE_BID_IDS_FOR_TEST * 5L),
                        Set.of(2L, CHUNK_SIZE_BID_IDS_FOR_TEST * 4L, CHUNK_SIZE_BID_IDS_FOR_TEST * 10L)
                },
                new Object[]{
                        "Таблица BIDS_BASE пустая -> oneshot отработал без падений",
                        Collections.emptySet(),
                        Collections.emptySet()
                }
        );
    }

    @Before
    public void before() {
        oneshot = new DeleteKeywordsFromBidsBaseOneshot(
                oneshotBidsBaseRepository, ppcPropertiesSupport, CHUNK_SIZE_BID_IDS_FOR_TEST, shardHelper);
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        shard = adGroupInfo.getShard();
    }

    @After
    public void after() {
        relevanceMatchBidIds.forEach(bidId -> testBidsRepository.deleteBid(shard, bidId));
    }

    @Test
    public void test() {
        addRelevanceMatchToBidsBase(relevanceMatchBidIds);
        addKeywordsToBidsBase(keywordBidIds);

        executeOneshot();

        List<Long> bidBaseIds = getBidsBaseIds();

        assertThat(bidBaseIds)
                .as("данные в bids_base")
                .containsOnlyElementsOf(relevanceMatchBidIds);
    }

    /**
     * Эмулация выполнения oneshot'a
     */
    private void executeOneshot() {
        State state = null;
        int maxIterationsCount = 20;
        while (maxIterationsCount-- > 0) {
            state = oneshot.execute(null, state, shard);
            if (state == null) {
                return;
            }
        }
    }

    public void addRelevanceMatchToBidsBase(Set<Long> bidIds) {
        bidIds.forEach(bidId -> addToBidsBase(bidId, BidsBaseBidType.relevance_match));
    }

    public void addKeywordsToBidsBase(Set<Long> bidIds) {
        bidIds.forEach(bidId -> addToBidsBase(bidId, BidsBaseBidType.keyword));

    }

    private void addToBidsBase(long bidId, BidsBaseBidType bidsBaseBidType) {
        dslContextProvider.ppc(shard)
                .insertInto(BIDS_BASE)
                .set(BIDS_BASE.BID_ID, bidId)
                .set(BIDS_BASE.BID_TYPE, bidsBaseBidType)
                .set(BIDS_BASE.CID, adGroupInfo.getCampaignId())
                .set(BIDS_BASE.PID, adGroupInfo.getAdGroupId())
                .set(BIDS_BASE.PRICE, BigDecimal.TEN)
                .set(BIDS_BASE.PRICE_CONTEXT, BigDecimal.TEN)
                .set(BIDS_BASE.OPTS, "")
                .execute();
    }

    public List<Long> getBidsBaseIds() {
        return dslContextProvider.ppc(shard)
                .select(BIDS_BASE.BID_ID)
                .from(BIDS_BASE)
                .where(BIDS_BASE.PID.eq(adGroupInfo.getAdGroupId()))
                .fetch(BIDS_BASE.BID_ID);
    }
}
