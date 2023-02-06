package ru.yandex.direct.oneshot.oneshots.flatcpcstrategyfromyt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.base.YtInputData;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.oneshot.oneshots.flatcpcstrategyfromyt.entity.ytmodels.generated.YtFlatCPCBidsByCidRow;
import ru.yandex.direct.oneshot.oneshots.flatcpcstrategyfromyt.service.FlatCpcStrategyFromYtMigrationOneshotService;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns.fullDynamicCampaign;
import static ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign;

@OneshotTest
@RunWith(SpringRunner.class)
public class FlatCpcStrategyFromYtMigrationOneshotTest {

    private final TextCampaign campaign =
            fullTextCampaign().withStrategy((DbStrategy) defaultStrategy().withStrategy(null));
    private final DynamicCampaign dynamicCampaign =
            fullDynamicCampaign().withStrategy((DbStrategy) defaultStrategy().withStrategy(null));
    private final TextCampaign archivedCampaign =
            fullTextCampaign().withStrategy((DbStrategy) defaultStrategy().withStrategy(null)).withStatusArchived(true);

    private UserInfo defaultUser;
    private YtOperator ytOperator;
    private FlatCpcStrategyFromYtMigrationOneshot oneshot;
    private int shard;

    @Autowired
    Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    FlatCpcStrategyFromYtMigrationOneshotService oneshotService;
    @Autowired
    DslContextProvider dslContextProvider;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        shard = defaultUser.getShard();

        YtProvider ytProvider = mock(YtProvider.class);
        ytOperator = mock(YtOperator.class);

        when(ytProvider.getOperator(any(YtCluster.class))).thenReturn(ytOperator);
        when(ytOperator.readTableRowCount(any())).thenReturn(1L);

        oneshot = new FlatCpcStrategyFromYtMigrationOneshot(ytProvider, oneshotService, ppcPropertiesSupport);
    }

    @Test
    public void execute_PriceContextInBidsAllFromYt() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(), campaign);
        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid);
        steps.keywordSteps().createKeywords(List.of(keywordInfo));
        buildTableMock(cid, adGroupInfo.getAdGroupId(), false);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContext = BigDecimal.valueOf(0.5);
        BigDecimal actualPriceContext = keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context поменялся на нужный", 0, actualPriceContext.compareTo(expectedPriceContext));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_PriceContextInBidsBase() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(), campaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(adGroupInfo, BigDecimal.ONE, BigDecimal.ZERO);

        //Здесь главное чтобы строка для cid просто была не пустая, данные из неё не берутся
        buildTableMock(cid, adGroupInfo.getAdGroupId(), false);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContext = BigDecimal.ONE;
        BigDecimal actualPriceContext =
                bidRepository.getBidsWithRelevanceMatchByCampaignIds(shard, List.of(cid)).get(0).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context поменялся на нужный", 0, actualPriceContext.compareTo(expectedPriceContext));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_PriceContextInBidsNotAllFromYt() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(), campaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid);
        KeywordInfo keywordInfoNotInYt = buildKeywordInfo(adGroupInfo, BigInteger.TWO, cid);

        steps.keywordSteps().createKeywords(List.of(keywordInfo, keywordInfoNotInYt));
        buildTableMock(cid, adGroupInfo.getAdGroupId(), false);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContextFromYt = BigDecimal.valueOf(0.5);
        BigDecimal expectedPriceContextNotFromYt = BigDecimal.ONE;
        BigDecimal actualPriceContextFromYt =
                keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getPriceContext();
        BigDecimal actualPriceContextNotFromYt =
                keywordRepository.getKeywordsByCampaignId(shard, cid).get(1).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context для ставки из YT поменялся на нужный", 0,
                actualPriceContextFromYt.compareTo(expectedPriceContextFromYt));
        assertEquals("Price context для ставки не из YT поменялся на нужный", 0,
                actualPriceContextNotFromYt.compareTo(expectedPriceContextNotFromYt));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_PriceContextInBidFromYtArchivedCampaign() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(),
                archivedCampaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid);
        KeywordInfo keywordInfoNotInYt = buildKeywordInfo(adGroupInfo, BigInteger.TWO, cid);
        Long adGroupId = adGroupInfo.getAdGroupId();

        steps.keywordSteps().createKeywords(List.of(keywordInfo, keywordInfoNotInYt));

        List<Long> keywordIds = List.of(keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getId(),
                keywordRepository.getKeywordsByCampaignId(shard, cid).get(1).getId());

        keywordRepository.archiveKeywords(dslContextProvider.ppc(adGroupInfo.getShard()).configuration(), keywordIds);

        buildTableMock(cid, adGroupId, false);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContextFromYt = BigDecimal.valueOf(0.5);
        BigDecimal expectedPriceContextNotFromYt = BigDecimal.ONE;
        BigDecimal actualPriceContextFromYt = keywordRepository.getArchivedKeywordsByAdGroupIds(shard, null,
                List.of(cid), List.of(adGroupId)).get(adGroupId).get(0).getPriceContext();
        BigDecimal actualPriceContextNotFromYt = keywordRepository.getArchivedKeywordsByAdGroupIds(shard, null,
                List.of(cid), List.of(adGroupId)).get(adGroupId).get(1).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context для ставки из YT поменялся на нужный", 0,
                actualPriceContextFromYt.compareTo(expectedPriceContextFromYt));
        assertEquals("Price context для ставки не из YT поменялся на нужный", 0,
                actualPriceContextNotFromYt.compareTo(expectedPriceContextNotFromYt));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    //Следующие три теста проверяют коррекность выставления ставок в случае, когда json из YT приходит равным []
    @Test
    public void execute_PriceContextInBidsWithEmptyBidsFromYt() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(), campaign);
        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        System.out.println(adGroupInfo.getAdGroupId());
        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid);
        steps.keywordSteps().createKeywords(List.of(keywordInfo));
        buildTableMock(cid, adGroupInfo.getAdGroupId(), true);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContext = BigDecimal.ONE;
        BigDecimal actualPriceContext = keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context поменялся на нужный", 0, actualPriceContext.compareTo(expectedPriceContext));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_PriceContextInBidsBaseWithEmptyBidsFromYt() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(), campaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(adGroupInfo, BigDecimal.ONE, BigDecimal.ZERO);

        //Тест на самом деле очень почти такой же как и PriceContextInBidsBase, проверяет что bids_base корректно
        // заполняется даже если не приходят ставки из yt
        buildTableMock(cid, adGroupInfo.getAdGroupId(), true);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContext = BigDecimal.ONE;
        BigDecimal actualPriceContext =
                bidRepository.getBidsWithRelevanceMatchByCampaignIds(shard, List.of(cid)).get(0).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context поменялся на нужный", 0, actualPriceContext.compareTo(expectedPriceContext));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_PriceContextInBidsArcWithEmptyBids() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(),
                archivedCampaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid);
        Long adGroupId = adGroupInfo.getAdGroupId();

        steps.keywordSteps().createKeywords(List.of(keywordInfo));

        List<Long> keywordIds = List.of(keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getId());

        keywordRepository.archiveKeywords(dslContextProvider.ppc(adGroupInfo.getShard()).configuration(), keywordIds);

        buildTableMock(cid, adGroupId, true);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContext = BigDecimal.ONE;
        BigDecimal actualPriceContext = keywordRepository.getArchivedKeywordsByAdGroupIds(shard, null,
                List.of(cid), List.of(adGroupId)).get(adGroupId).get(0).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context поменялся на нужный", 0,
                actualPriceContext.compareTo(expectedPriceContext));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_CampaignWithZeroPriceBids() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(), campaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(adGroupInfo, BigDecimal.ZERO, BigDecimal.ONE);

        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid, BigDecimal.ZERO, BigDecimal.ONE);
        steps.keywordSteps().createKeywords(List.of(keywordInfo));

        buildTableMock(cid, adGroupInfo.getAdGroupId(), true);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedBidsBasePriceContext = BigDecimal.ZERO;
        BigDecimal actualBidsBasePriceContext =
                bidRepository.getBidsWithRelevanceMatchByCampaignIds(shard, List.of(cid)).get(0).getPriceContext();
        System.out.println(actualBidsBasePriceContext);

        BigDecimal actualBidsPriceContext = keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getPriceContext();


        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context в bids_base поменялся на нужный", 0, actualBidsBasePriceContext.compareTo(expectedBidsBasePriceContext));
        assertEquals("Price context в bids поменялся на нужный", null, actualBidsPriceContext);


        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_ArchivedCampaignWithZeroPriceBids() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(),
                archivedCampaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid, BigDecimal.ZERO, BigDecimal.ONE);
        Long adGroupId = adGroupInfo.getAdGroupId();

        steps.keywordSteps().createKeywords(List.of(keywordInfo));

        List<Long> keywordIds = List.of(keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getId());

        keywordRepository.archiveKeywords(dslContextProvider.ppc(adGroupInfo.getShard()).configuration(), keywordIds);

        buildTableMock(cid, adGroupId, true);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal actualPriceContext = keywordRepository.getArchivedKeywordsByAdGroupIds(shard, null,
                List.of(cid), List.of(adGroupId)).get(adGroupId).get(0).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context поменялся на нужный", null, actualPriceContext);

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());
        assertEquals("Context limit поменялся успешно", expectedCampaign.getContextLimit(),
                actualCampaign.getContextLimit());
        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    @Test
    public void execute_DynamicCampaign() {
        CampaignInfo campaignInfo = steps.dynamicCampaignSteps().createCampaign(defaultUser.getClientInfo(),
                dynamicCampaign);

        Long cid = campaignInfo.getCampaignId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo keywordInfo = buildKeywordInfo(adGroupInfo, BigInteger.ONE, cid);
        KeywordInfo keywordInfoNotInYt = buildKeywordInfo(adGroupInfo, BigInteger.TWO, cid);

        steps.keywordSteps().createKeywords(List.of(keywordInfo, keywordInfoNotInYt));
        buildTableMock(cid, adGroupInfo.getAdGroupId(), false);

        oneshot.execute(createInputData(), null, shard);

        BigDecimal expectedPriceContextFromYt = BigDecimal.valueOf(0.5);
        BigDecimal expectedPriceContextNotFromYt = BigDecimal.ONE;
        BigDecimal actualPriceContextFromYt =
                keywordRepository.getKeywordsByCampaignId(shard, cid).get(0).getPriceContext();
        BigDecimal actualPriceContextNotFromYt =
                keywordRepository.getKeywordsByCampaignId(shard, cid).get(1).getPriceContext();

        TextCampaign expectedCampaign =
                campaign.withStrategy(defaultStrategy()).withContextLimit(0).withEnableCpcHold(false);
        DynamicCampaign actualCampaign =
                (DynamicCampaign) campaignTypedRepository.getTypedCampaigns(shard, List.of(cid)).get(0);

        assertEquals("Price context для ставки из YT поменялся на нужный", 0,
                actualPriceContextFromYt.compareTo(expectedPriceContextFromYt));
        assertEquals("Price context для ставки не из YT поменялся на нужный", 0,
                actualPriceContextNotFromYt.compareTo(expectedPriceContextNotFromYt));

        assertEquals("Стратегия поменялась успешно", expectedCampaign.getStrategy(), actualCampaign.getStrategy());

        assertEquals("Enable cpc hold поменялся успешно", expectedCampaign.getEnableCpcHold(),
                actualCampaign.getEnableCpcHold());
    }

    private KeywordInfo buildKeywordInfo(AdGroupInfo adGroupInfo, BigInteger phraseBsId, Long cid) {
        return buildKeywordInfo(adGroupInfo, phraseBsId, cid, BigDecimal.ONE, BigDecimal.ZERO);
    }


    private KeywordInfo buildKeywordInfo(AdGroupInfo adGroupInfo, BigInteger phraseBsId, Long cid, BigDecimal price, BigDecimal priceContext) {
        return new KeywordInfo()
                .withAdGroupInfo(adGroupInfo)
                .withKeyword(defaultKeyword()
                        .withCampaignId(cid)
                        .withPhraseBsId(phraseBsId)
                        .withAdGroupId(adGroupInfo.getAdGroupId())
                        .withPrice(price)
                        .withPriceContext(priceContext)
                );
    }


    private YtInputData createInputData() {
        YtInputData inputData = new YtInputData();
        inputData.setYtCluster(YtCluster.HAHN.getName());
        inputData.setTablePath("");
        return inputData;
    }

    private void buildTableMock(Long cid, Long adGroupId, boolean isEmptyBids) {
        YtFlatCPCBidsByCidRow tableRow = new YtFlatCPCBidsByCidRow();
        String bids = isEmptyBids ? "[]" :
                "[\n" +
                        "    {\n" +
                        "        \"GroupExportID\": " + adGroupId + ",\n" +
                        "        \"PhraseID\": 1,\n" +
                        "        \"PriceContext\": 0.5\n" +
                        "    }\n" +
                        "]";
        tableRow.setBids(bids);
        tableRow.setCid(cid);
        tableRow.setShard(shard);

        doAnswer(invocation -> {
                    Consumer consumer = invocation.getArgument(1);
                    consumer.accept(tableRow);
                    return null;
                }
        ).when(ytOperator).readTableByRowRange(
                any(YtTable.class),
                any(Consumer.class),
                any(YtFlatCPCBidsByCidRow.class),
                anyLong(),
                anyLong());
    }

}
