package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import one.util.streamex.StreamEx;
import org.junit.Assume;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperation;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithTitle;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.mock.BsTrafaretClientMockUtils;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.repository.TestSitelinkSetRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;
import ru.yandex.direct.core.testing.stub.CanvasClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Базовый класс для всех тестов комплексной операции.
 * От него наследуются базовые классы для тестирования баннеров,
 * условий показа и т.п., а уже от них - сами тесты.
 * <p>
 * Здесь создаются только клиент и кампания.
 */
public class ComplexAdGroupUpdateOperationTestBase {

    protected static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(710L).setScale(2, RoundingMode.UNNECESSARY);

    @Autowired
    protected ComplexAdGroupUpdateOperationFactory complexAdGroupUpdateOperationFactory;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected KeywordRepository keywordRepository;

    @Autowired
    protected TestKeywordRepository testKeywordRepository;

    @Autowired
    protected BannerTypedRepository bannerTypedRepository;

    @Autowired
    protected VcardRepository vcardRepository;

    @Autowired
    protected CreativeRepository creativeRepository;

    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    protected OfferRetargetingRepository offerRetargetingRepository;

    @Autowired
    protected TestSitelinkSetRepository testSitelinkSetRepository;

    @Autowired
    protected DslContextProvider dslContextProvider;

    @Autowired
    protected GeoTreeFactory geoTreeFactory;

    @Autowired
    protected Steps steps;

    @Autowired
    protected CanvasClientStub canvasClientStub;

    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;

    protected GeoTree geoTree;

    protected CampaignInfo campaignInfo;
    protected AdGroupInfo adGroupInfo1;
    protected AdGroupInfo adGroupInfo2;
    protected AdGroupInfo adGroupInfo3;

    protected RetConditionInfo retConditionInfo;

    protected long campaignId;
    protected Long retConditionId;
    protected Long operatorUid;
    protected long clientUid;
    protected ClientId clientId;
    protected int shard;

    @Before
    public void before() {
        BsTrafaretClientMockUtils.setDefaultMockOnBsTrafaretClient(bsTrafaretClient);

        geoTree = geoTreeFactory.getGlobalGeoTree();
        campaignInfo = steps.campaignSteps().createActiveTextCampaign();

        retConditionInfo = steps.retConditionSteps().createDefaultRetCondition(campaignInfo.getClientInfo());
        retConditionId = retConditionInfo.getRetConditionId();

        campaignId = campaignInfo.getCampaignId();
        clientId = campaignInfo.getClientId();
        clientUid = campaignInfo.getUid();
        shard = campaignInfo.getShard();
        createFirstAdGroup();
    }

    protected void createFirstAdGroup() {
        adGroupInfo1 = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
    }

    protected void createSecondAdGroup() {
        adGroupInfo2 = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
    }

    protected void createThirdAdGroup() {
        adGroupInfo3 = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
    }

    protected KeywordInfo createKeyword(AdGroupInfo adGroupInfo) {
        Keyword keyword = defaultKeyword()
                .withPhrase(randomAlphabetic(10));
        return steps.keywordSteps().createKeyword(adGroupInfo, keyword);
    }

    protected AdGroup getAdGroup(Long adGroupId) {
        return adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
    }

    /**
     * Для тестов на корректность обновления данных
     * здесь должны быть изменены все поля.
     */
    protected ComplexTextAdGroup createValidAdGroupForUpdate(AdGroupInfo sourceAdGroupInfo) {
        AdGroup sourceAdGroup = sourceAdGroupInfo.getAdGroup();
        String newName = sourceAdGroup.getName() + " UPDATED";

        List<Long> newGeo = new ArrayList<>(sourceAdGroup.getGeo());
        newGeo.add(111L);

        List<String> newMinusKeywords = new ArrayList<>(sourceAdGroup.getMinusKeywords());
        newMinusKeywords.add("новое минус слово 123");

        AdGroup adGroupForUpdate = new TextAdGroup()
                .withId(sourceAdGroup.getId())
                .withType(AdGroupType.BASE)
                .withName(newName)
                .withGeo(newGeo)
                .withMinusKeywords(newMinusKeywords);
        return new ComplexTextAdGroup().withAdGroup(adGroupForUpdate);
    }

    protected MassResult<Long> updateAndCheckFirstItemIsInvalid(ComplexTextAdGroup... adGroupsForUpdate) {
        return updateAndCheckFirstItemIsInvalid(asList(adGroupsForUpdate));
    }

    protected MassResult<Long> updateAndCheckFirstItemIsInvalid(List<ComplexTextAdGroup> adGroupsForUpdate) {
        return updateAndCheckElementResults(adGroupsForUpdate, Sets.newHashSet(0));
    }

    protected MassResult<Long> updateAndCheckSecondItemIsInvalid(List<ComplexTextAdGroup> adGroupsForUpdate) {
        return updateAndCheckElementResults(adGroupsForUpdate, Sets.newHashSet(1));
    }

    protected MassResult<Long> updateAndCheckSecondItemIsInvalid(ComplexTextAdGroup... adGroupsForUpdate) {
        return updateAndCheckSecondItemIsInvalid(asList(adGroupsForUpdate));
    }

    protected MassResult<Long> updateAndCheckBothItemsAreInvalid(List<ComplexTextAdGroup> adGroupsForUpdate) {
        return updateAndCheckElementResults(adGroupsForUpdate, Sets.newHashSet(0, 1));
    }

    protected void updateAndCheckResultIsEntirelySuccessful(ComplexTextAdGroup... adGroupForUpdate) {
        updateAndCheckResultIsEntirelySuccessful(asList(adGroupForUpdate), false);
    }

    protected void updateAndCheckResultIsEntirelySuccessful(List<ComplexTextAdGroup> adGroupsForUpdate) {
        updateAndCheckResultIsEntirelySuccessful(adGroupsForUpdate, false);
    }

    protected void updateWithAutoPricesAndCheckResultIsEntirelySuccessful(ComplexTextAdGroup... adGroupsForUpdate) {
        updateAndCheckResultIsEntirelySuccessful(asList(adGroupsForUpdate), true);
    }

    private void updateAndCheckResultIsEntirelySuccessful(List<ComplexTextAdGroup> adGroupsForUpdate,
                                                          boolean autoPrices) {
        MassResult<Long> result = createOperation(adGroupsForUpdate, false, autoPrices).prepareAndApply();
        result.getValidationResult().flattenErrors().forEach(System.out::println);
        assumeThat("результат операции и всех групп не должен содержать ошибок",
                result.getValidationResult(), hasNoDefectsDefinitions());
        assumeThat("результат операции отрицательный, а ожидается положительный",
                result.isSuccessful(), is(true));
        assumeThat("количество поэлементных результатов отличается от количества обновляемых групп",
                result.getResult(), hasSize(adGroupsForUpdate.size()));
        for (int i = 0; i < result.getResult().size(); i++) {
            assumeThat("результат для группы номер %s отрицательный, а ожидается положительный",
                    result.getResult().get(i).isSuccessful(), is(true));
        }
    }

    protected MassResult<Long> updateAndCheckElementResults(List<ComplexTextAdGroup> adGroupsForUpdate,
                                                            Set<Integer> invalidElementIndexes) {
        MassResult<Long> result = createOperation(adGroupsForUpdate).prepareAndApply();
        result.getValidationResult().flattenErrors().forEach(System.out::println);
        assumeThat("результат операции должен быть положительным", result.isSuccessful(), is(true));
        assumeThat("количество поэлементных результатов отличается от количества обновляемых групп",
                result.getResult(), hasSize(adGroupsForUpdate.size()));
        for (int i = 0; i < result.getResult().size(); i++) {
            boolean expectedSuccess = !invalidElementIndexes.contains(i);
            boolean actualSuccess = result.getResult().get(i).isSuccessful();
            assumeThat(String.format("результат группы %s отличается от ожидаемого", i),
                    actualSuccess, is(expectedSuccess));
        }
        return result;
    }

    protected ComplexAdGroupUpdateOperation createOperation(List<ComplexTextAdGroup> adGroupsForUpdate) {
        return createOperation(adGroupsForUpdate, true, false);
    }

    protected ComplexAdGroupUpdateOperation createOperation(List<ComplexTextAdGroup> adGroupsForUpdate,
                                                            boolean saveDraft) {
        return createOperation(adGroupsForUpdate, saveDraft, false);
    }

    private ComplexAdGroupUpdateOperation createOperation(List<ComplexTextAdGroup> adGroupsForUpdate,
                                                          boolean saveDraft, boolean autoPrices) {
        ShowConditionAutoPriceParams autoPriceParams = null;
        if (autoPrices) {
            autoPriceParams = new ShowConditionAutoPriceParams(
                    ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE),
                    keywordRequests -> emptyMap()
            );
        }
        return complexAdGroupUpdateOperationFactory.createTextAdGroupUpdateOperation(
                adGroupsForUpdate, geoTree, autoPrices, autoPriceParams, false, operatorUid == null ? clientUid :
                        operatorUid, clientId, clientUid, saveDraft);
    }

    // searching for changes

    protected int getClientKeywordsCount() {
        return testKeywordRepository.getClientPhrases(shard, clientId).size();
    }

    protected List<String> findKeywordsInAdGroup(long adGroupId) {
        List<Keyword> keywords = keywordRepository.getKeywordsByAdGroupId(shard, adGroupId);
        return mapList(keywords, Keyword::getPhrase);
    }

    protected List<Banner> findBanners(AdGroupInfo adGroupInfo) {
        return bannerTypedRepository.getBannersByGroupIds(shard, singletonList(adGroupInfo.getAdGroupId()));
    }

    protected List<String> findBannerTitles(AdGroupInfo adGroupInfo) {
        return StreamEx.of(findBanners(adGroupInfo))
                .select(BannerWithTitle.class)
                .map(BannerWithTitle::getTitle)
                .toList();
    }

    protected List<String> findBannerHrefs(AdGroupInfo adGroupInfo) {
        return StreamEx.of(findBanners(adGroupInfo))
                .map(b -> ((BannerWithHref) b).getHref())
                .toList();
    }

    protected int getClientBannersCount() {
        return dslContextProvider.ppc(shard)
                .selectCount()
                .from(BANNERS)
                .join(CAMPAIGNS).on(CAMPAIGNS.CID.eq(BANNERS.CID))
                .where(CAMPAIGNS.CLIENT_ID.eq(clientId.asLong()))
                .fetchOne()
                .value1();
    }

    // test data

    protected ComplexTextAdGroup fullAdGroup(Long adGroupId) {
        return ComplexTextAdGroupTestData.fullAdGroupForUpdate(adGroupId, retConditionId);
    }

//    protected ComplexBanner fullTextBanner(Long bannerId) {
//        return ComplexTextAdGroupTestData.fullTextBanner(bannerId);
//    }

    // проверки инвариантов

    protected void assumeManualStrategyWithDifferentPlaces() {
        Strategy strategy = campaignInfo.getCampaign().getStrategy();
        Assume.assumeThat("в кампании установлена ручная стратегия", strategy.isManual(), is(true));
        ManualStrategy manualStrategy = strategy.cast(ManualStrategy.class);
        Assume.assumeThat("в кампании включено раздельное управление ставками", manualStrategy.isSeparateBids(),
                is(true));
        Assume.assumeThat("кампания показывается на обеих площадках", manualStrategy.getPlatform(), is(
                CampaignsPlatform.BOTH));
    }
}
