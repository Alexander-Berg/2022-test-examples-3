package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.auction.container.bs.Position;
import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.data.TestKeywords;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.mock.KeywordShowsForecastServiceMockUtils;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.bsauction.BsTrafaretClient.PLACE_TRAFARET_MAPPING;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_HREF_PARAMS;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_MANUAL_PRICES;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_PHRASEID_HISTORY;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@QueryWithoutIndex("Тесты")
public abstract class KeywordsBaseTest {

    protected static final Long DEFAULT_PRICE = 160L;

    protected static final String PHRASE_1 = "конь -слон";
    protected static final String PHRASE_2 = "розовый слон";
    protected static final String PHRASE_3 = "рик и морти";

    protected static final String NEW_PHRASE_1 = "хоспади да какой еще конь";

    //    protected static String PREINVALID_PHRASE_1 = "синтаксически невалидная \"фраза\"";
    protected static final String PREINVALID_PHRASE_1 = "синтаксически невалидная %$&";
    protected static final String INVALID_PHRASE_1 = "невалидная по смыслу -невалидное -смысл";

    protected static final Long UNEXISTENT_PHRASE_ID = (long) Integer.MAX_VALUE;

    protected static final Long DEFAULT_SHOWS_FORECAST = 63424L;

    @Autowired
    protected KeywordRepository keywordRepository;

    @Autowired
    protected DslContextProvider ppcDslContextProvider;

    @Autowired
    protected TestKeywordRepository testKeywordRepository;

    @Autowired
    protected Steps steps;

    @Autowired
    protected AdGroupSteps adGroupSteps;

    @Autowired
    protected BannerSteps bannerSteps;

    @Autowired
    protected KeywordSteps keywordSteps;

    @Autowired
    protected KeywordNormalizer keywordNormalizer;

    @Autowired
    protected KeywordBsAuctionService keywordBsAuctionService;

    @Autowired
    private KeywordShowsForecastServiceMockUtils keywordShowsForecastServiceMockUtils;
    KeywordShowsForecastService keywordShowsForecastService;

    @Autowired
    private DslContextProvider dslContextProvider;

    protected ClientInfo clientInfo;
    protected ClientInfo operatorClientInfo;

    protected AdGroupInfo adGroupInfo1;
    protected AdGroupInfo adGroupInfo2;

    @Before
    public void before() {
        setUpBsAuctionServiceDefault();
        keywordShowsForecastService =
                keywordShowsForecastServiceMockUtils.mockWithDefaultForecast(DEFAULT_SHOWS_FORECAST);
        clientInfo = steps.clientSteps().createDefaultClient();
        operatorClientInfo = clientInfo;
    }

    protected void createOneActiveAdGroup() {
        adGroupInfo1 = adGroupSteps.createActiveTextAdGroup(clientInfo);
    }

    protected void createOneActiveAdGroup(ClientInfo managerClientInfo) {
        adGroupInfo1 = adGroupSteps.createActiveTextAdGroup(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withManagerUid(managerClientInfo.getUid())));
    }

    protected void createOneActiveAdGroupAutoStrategy() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaignAutoStrategy(clientInfo);
        adGroupInfo1 = adGroupSteps.createActiveTextAdGroup(campaignInfo);
    }

    protected void createOneActiveAdGroupCpmBannerWithManualStrategy() {
        adGroupInfo1 = adGroupSteps.createActiveCpmBannerAdGroupWithManualStrategy(clientInfo, CriterionType.KEYWORD);
    }

    protected void createOneActiveAdGroup(ClientLimits clientLimits) {
        createOneActiveAdGroup();
        clientLimits.withClientId(clientInfo.getClientId());
        clientInfo.withClientLimits(clientLimits);
        steps.clientSteps().updateClientLimits(clientInfo);
    }

    protected void createTwoActiveAdGroups() {
        adGroupInfo1 = adGroupSteps.createAdGroup(activeTextAdGroup(), new CampaignInfo().withClientInfo(clientInfo));
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getCampaignInfo());
    }

    protected void createTwoActiveAdGroups(ClientInfo managerClientInfo) {
        adGroupInfo1 = adGroupSteps.createAdGroup(activeTextAdGroup(), new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withManagerUid(managerClientInfo.getUid())));
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getCampaignInfo());
    }

    protected KeywordInfo createKeyword(AdGroupInfo adGroupInfo) {
        String phrase = RandomStringUtils.randomAlphabetic(10);
        return createKeyword(adGroupInfo, phrase);
    }

    protected KeywordInfo createValidKeyword1(AdGroupInfo adGroupInfo) {
        return createKeyword(adGroupInfo, PHRASE_1);
    }

    protected KeywordInfo createValidKeyword2(AdGroupInfo adGroupInfo) {
        return createKeyword(adGroupInfo, PHRASE_2);
    }

    protected KeywordInfo createDraftKeyword(AdGroupInfo adGroupInfo, String phrase) {
        Keyword keyword = getDefaultDraftKeyword(phrase);
        return keywordSteps.createKeyword(adGroupInfo, keyword);
    }

    protected KeywordInfo createKeyword(AdGroupInfo adGroupInfo, String phrase) {
        Keyword keyword = getDefaultActiveKeyword(phrase);
        return keywordSteps.createKeyword(adGroupInfo, keyword);
    }

    protected KeywordInfo createKeyword(AdGroupInfo adGroupInfo, String phrase,
                                        BigDecimal searchPrice, BigDecimal contextPrice) {
        Keyword keyword = getDefaultActiveKeyword(phrase)
                .withPrice(searchPrice)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(null);
        return keywordSteps.createKeyword(adGroupInfo, keyword);
    }

    protected KeywordInfo createFullKeywordAndCheckRecordsPresentInAllTables(AdGroupInfo adGroupInfo, String phrase) {
        KeywordInfo keywordInfo = createFullKeyword(adGroupInfo, phrase);
        checkState(isRecordPresentsInAllTables(keywordInfo));
        return keywordInfo;
    }

    protected KeywordInfo createFullKeyword(AdGroupInfo adGroupInfo, String phrase) {
        KeywordWithMinuses parsedKeyword = KeywordParser.parseWithMinuses(phrase);
        KeywordWithMinuses parsedNormalKeyword = keywordNormalizer.normalizeKeywordWithMinuses(parsedKeyword);

        History history = new History(1L, 2L, BigInteger.TEN, ImmutableMap.of(2L, 1L), ImmutableMap.of(3L, 2L));

        Keyword keyword = defaultKeyword()
                .withPhrase(parsedKeyword.toString())
                .withNormPhrase(parsedNormalKeyword.toString())
                .withWordsCount(parsedNormalKeyword.getKeyword().getAllKeywords().size())
                .withHrefParam1("href-param-1")
                .withHrefParam2("href-param-2")
                .withPhraseIdHistory(history);

        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo, keyword);

        ppcDslContextProvider.ppc(keywordInfo.getShard())
                .insertInto(BIDS_MANUAL_PRICES)
                .set(BIDS_MANUAL_PRICES.ID, keywordInfo.getId())
                .set(BIDS_MANUAL_PRICES.CID, keywordInfo.getCampaignId())
                .set(BIDS_MANUAL_PRICES.PRICE, BigDecimal.valueOf(123L))
                .set(BIDS_MANUAL_PRICES.PRICE_CONTEXT, BigDecimal.valueOf(345L))
                .execute();

        return keywordInfo;
    }

    public boolean isRecordPresentsInAllTables(KeywordInfo keywordInfo) {
        return isBidsBaseRecordNotPresent(keywordInfo) &&
                isBidsRecordPresent(keywordInfo) &&
                isBidsHrefParamsRecordPresent(keywordInfo) &&
                isBidsPhraseIdHistoryRecordPresent(keywordInfo) &&
                isBidsManualPricesRecordPresent(keywordInfo);
    }

    protected boolean isBidsRecordPresent(KeywordInfo keywordInfo) {
        return dslContextProvider.ppc(keywordInfo.getShard())
                .selectCount()
                .from(BIDS)
                .where(BIDS.ID.eq(keywordInfo.getId()))
                .fetch()
                .get(0)
                .getValue(0, Integer.class) > 0;
    }

    protected boolean isBidsBaseRecordNotPresent(KeywordInfo keywordInfo) {
        return dslContextProvider.ppc(keywordInfo.getShard())
                .selectCount()
                .from(BIDS_BASE)
                .where(BIDS_BASE.BID_ID.eq(keywordInfo.getId()))
                .fetch()
                .get(0)
                .getValue(0, Integer.class).equals(0);
    }

    protected boolean isBidsHrefParamsRecordPresent(KeywordInfo keywordInfo) {
        return dslContextProvider.ppc(keywordInfo.getShard())
                .selectCount()
                .from(BIDS_HREF_PARAMS)
                .where(BIDS_HREF_PARAMS.ID.eq(keywordInfo.getId()))
                .fetch()
                .get(0)
                .getValue(0, Integer.class) > 0;
    }

    protected boolean isBidsPhraseIdHistoryRecordPresent(KeywordInfo keywordInfo) {
        return dslContextProvider.ppc(keywordInfo.getShard())
                .selectCount()
                .from(BIDS_PHRASEID_HISTORY)
                .where(BIDS_PHRASEID_HISTORY.ID.eq(keywordInfo.getId()))
                .fetch()
                .get(0)
                .getValue(0, Integer.class) > 0;
    }

    protected boolean isBidsManualPricesRecordPresent(KeywordInfo keywordInfo) {
        return dslContextProvider.ppc(keywordInfo.getShard())
                .selectCount()
                .from(BIDS_MANUAL_PRICES)
                .where(BIDS_MANUAL_PRICES.ID.eq(keywordInfo.getId()))
                .fetch()
                .get(0)
                .getValue(0, Integer.class) > 0;
    }

    protected Keyword getDefaultDraftKeyword(String phrase) {
        KeywordWithMinuses parsedKeyword = KeywordParser.parseWithMinuses(phrase);
        KeywordWithMinuses parsedNormalKeyword = keywordNormalizer.normalizeKeywordWithMinuses(parsedKeyword);
        return TestKeywords.defaultKeyword()
                .withPhrase(parsedKeyword.toString())
                .withNormPhrase(parsedNormalKeyword.getKeyword().toString())
                .withPrice(BigDecimal.ONE)
                .withPriceContext(BigDecimal.ONE)
                .withAutobudgetPriority(3)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withWordsCount(1)
                .withIsSuspended(false);
    }

    protected Keyword getDefaultActiveKeyword(String phrase) {
        return getDefaultDraftKeyword(phrase)
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withPhraseBsId(BigInteger.ONE)
                .withPhraseIdHistory(History.parse("O1"));
    }

    protected void createKeywords(AdGroupInfo adGroupInfo, int number) {
        for (int i = 0; i < number; i++) {
            createKeyword(adGroupInfo);
        }
    }

    protected Keyword getKeyword(Long id) {
        return keywordRepository
                .getKeywordsByIds(clientInfo.getShard(), clientInfo.getClientId(), singletonList(id)).get(0);
    }

    protected int getClientKeywordsNumber() {
        return ppcDslContextProvider.ppc(clientInfo.getShard())
                .selectCount()
                .from(BIDS).join(CAMPAIGNS).on(CAMPAIGNS.CID.eq(BIDS.CID))
                .where(CAMPAIGNS.CLIENT_ID.eq(clientInfo.getClientId().asLong()))
                .fetchOne()
                .value1();
    }

    protected void rewindKeywordModificationTime(Keyword keyword) {
        ModelChanges<Keyword> modelChanges = ModelChanges.build(keyword,
                Keyword.MODIFICATION_TIME, LocalDateTime.now().minusHours(1));
        AppliedChanges<Keyword> appliedChanges = modelChanges.applyTo(keyword);
        keywordRepository.update(clientInfo.getShard(), singletonList(appliedChanges));
    }

    protected void setUpBsAuctionServiceDefault() {
        keywordBsAuctionService = spy(keywordBsAuctionService);

        Answer<List<KeywordTrafaretData>> answerTrafaretAuction = invocationOnMock -> {
            //noinspection unchecked
            List<Keyword> keywords = (List<Keyword>) invocationOnMock.getArguments()[1];

            return StreamEx.of(keywords)
                    .map(this::defaultBsTrafaretAuctionData)
                    .toList();
        };

        doAnswer(answerTrafaretAuction).when(keywordBsAuctionService)
                .getTrafaretAuction(any(ClientId.class), anyList(), any());
    }

    protected KeywordTrafaretData defaultBsTrafaretAuctionData(Keyword keyword) {
        Position position1 = new Position(Money.valueOf(50d, CurrencyCode.RUB), Money.valueOf(55d, CurrencyCode.RUB));
        Position position2 = new Position(Money.valueOf(40d, CurrencyCode.RUB), Money.valueOf(45d, CurrencyCode.RUB));
        Position position3 = new Position(Money.valueOf(30d, CurrencyCode.RUB), Money.valueOf(35d, CurrencyCode.RUB));
        Position position4 = new Position(Money.valueOf(20d, CurrencyCode.RUB), Money.valueOf(25d, CurrencyCode.RUB));
        return new KeywordTrafaretData()
                .withKeyword(keyword)
                .withBidItems(
                        asList(
                                new TrafaretBidItem().withBid(position1.getBidPrice())
                                        .withPrice(position1.getAmnestyPrice())
                                        .withPositionCtrCorrection(
                                                PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM1)),
                                new TrafaretBidItem().withBid(position2.getBidPrice())
                                        .withPrice(position2.getAmnestyPrice())
                                        .withPositionCtrCorrection(
                                                PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM2)),
                                new TrafaretBidItem().withBid(position3.getBidPrice())
                                        .withPrice(position3.getAmnestyPrice())
                                        .withPositionCtrCorrection(
                                                PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM3)),
                                new TrafaretBidItem().withBid(position4.getBidPrice())
                                        .withPrice(position4.getAmnestyPrice())
                                        .withPositionCtrCorrection(
                                                PLACE_TRAFARET_MAPPING.get(BsTrafaretClient.Place.PREMIUM4))
                        )
                );
    }

    protected Keyword validClientKeyword1(AdGroupInfo adGroupInfo) {
        return clientKeyword(adGroupInfo, PHRASE_1, DEFAULT_PRICE);
    }

    protected Keyword validClientKeyword2(AdGroupInfo adGroupInfo) {
        return clientKeyword(adGroupInfo, PHRASE_2, DEFAULT_PRICE);
    }

    protected Keyword validClientKeyword3(AdGroupInfo adGroupInfo) {
        return clientKeyword(adGroupInfo, PHRASE_3, DEFAULT_PRICE);
    }

    protected Keyword clientKeyword(AdGroupInfo adGroupInfo, String phrase) {
        return clientKeyword(adGroupInfo, phrase, DEFAULT_PRICE);
    }

    /**
     * Создает модель фразы с пустыми ставками и приоритетом автобюджета.
     */
    protected Keyword newKeywordEmptyPrices(AdGroupInfo adGroupInfo, String phrase) {
        return clientKeyword(adGroupInfo, phrase)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
    }

    protected Keyword clientKeyword(AdGroupInfo adGroupInfo, String phrase, Long price) {
        Keyword clientKeyword = new Keyword();
        clientKeyword.withPhrase(phrase)
                .withIsAutotargeting(false)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPrice(BigDecimal.valueOf(price))
                .withPriceContext(BigDecimal.valueOf(price));
        return clientKeyword;
    }

    protected ModelChanges<Keyword> validModelChanges1(Long id) {
        return new ModelChanges<>(id, Keyword.class)
                .process(NEW_PHRASE_1, Keyword.PHRASE);
    }

    protected ModelChanges<Keyword> keywordModelChanges(Long id, String phrase) {
        return new ModelChanges<>(id, Keyword.class)
                .process(phrase, Keyword.PHRASE);
    }

    protected ModelChanges<Keyword> keywordModelChanges(Long id, String phrase, Long price) {
        return new ModelChanges<>(id, Keyword.class)
                .process(phrase, Keyword.PHRASE)
                .process(BigDecimal.valueOf(price), Keyword.PRICE);
    }

    @SuppressWarnings("unchecked")
    protected void checkValidationHasWarnings(MassResult<?> result, Defect defect,
                                              boolean... hasWarningFlags) {
        checkState(result.getResult().size() == hasWarningFlags.length);
        for (int i = 0; i < hasWarningFlags.length; i++) {
            Matcher matcher = hasWarningFlags[i] ?
                    hasDefectDefinitionWith(validationError(path(), defect)) :
                    not(hasDefectDefinitionWith(validationError(path(), defect)));
            assertThat(result.get(i).getValidationResult(), matcher);
        }
    }
}
