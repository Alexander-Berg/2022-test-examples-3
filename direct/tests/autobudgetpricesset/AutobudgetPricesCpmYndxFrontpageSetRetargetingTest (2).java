package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;
import java.util.List;

import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleOperatorEnum;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListTypeEnum;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesAdgroupType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRetargetingRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.enums.AdGroupType;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.CpmBannerCampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.CpmBannerCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.CpmBannerCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.CpmBannerCampaignStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.retargetinglists.AddRequestMap;
import ru.yandex.autotests.directapi.model.api5.retargetinglists.RetargetingListAddItemMap;
import ru.yandex.autotests.directapi.model.api5.retargetinglists.RetargetingListRuleArgumentItemMap;
import ru.yandex.autotests.directapi.model.api5.retargetinglists.RetargetingListRuleItemMap;
import ru.yandex.autotests.directapi.model.common.RegionIDValues;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import org.joda.time.DateTime;

import static org.hamcrest.Matchers.closeTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * Authors: lightelfik
 * Date: 29.03.19
 * https://st.yandex-team.ru/DIRECT-94801
 */
@Aqua.Test(title = "AutobudgetPrices.set - ретаргетинг для кампании на Главной")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesCpmYndxFrontpageSetRetargetingTest {
    private AutobudgetPricesSetRequest request;
    private Long pid;

    private static Long clientId;
    private static Long rlId;
    private static int shard;
    private static DirectJooqDbSteps jooqDbSteps;

    private static final String LOGIN = Logins.LOGIN_RUB;
    private static final double EPSILON = 1e-6;

    @ClassRule
    public static final ApiSteps API = new ApiSteps().wsdl(APIPort_PortType.class).as(LOGIN);

    @ClassRule
    public static final SemaphoreRule SEMAPHORE = Semaphore.getSemaphore();

    @BeforeClass
    public static void beforeClass() {
        shard = API.userSteps.clientFakeSteps().getUserShard(LOGIN);
        jooqDbSteps = API.userSteps.getDirectJooqDbSteps().useShard(shard);
        List<Long> goalIds = API.userSteps.retargetingSteps().getRetargetingGoalIDs(LOGIN);
        RetargetingListAddItemMap retargetingListAddItemMap = new RetargetingListAddItemMap()
                .withType(RetargetingListTypeEnum.AUDIENCE)
                .withName("Name" + RandomStringUtils.randomAlphabetic(3))
                .withDescription("Description" + RandomStringUtils.randomAlphabetic(3))
                .withRules(new RetargetingListRuleItemMap()
                        .withArgumentItems(new RetargetingListRuleArgumentItemMap()
                                .withMembershipLifeSpan(org.apache.commons.lang3.RandomUtils.nextInt(1, 90))
                                .withExternalId(goalIds.get(0)))
                        .withOperator(RetargetingListRuleOperatorEnum.ANY));
        rlId = API.userSteps.retargetingListsSteps().add(LOGIN,
                new AddRequestMap().withRetargetingLists(retargetingListAddItemMap)
        ).get(0);
        clientId = Long.parseLong(User.get(LOGIN).getClientID());
    }

    @Before
    public void init() {

        CampaignAddItemMap campaignMap = new CampaignAddItemMap()
                .withName("Name" + RandomStringUtils.randomAlphabetic(3))
                .withStartDate(DateTime.now().toString("yyyy-MM-dd"))
                .withCpmBannerCampaign(new CpmBannerCampaignAddItemMap()
                        .withBiddingStrategy(new CpmBannerCampaignStrategyAddMap()
                                .withSearch(new CpmBannerCampaignSearchStrategyAddMap().defaultServingOff())
                                .withNetwork(new CpmBannerCampaignNetworkStrategyAddMap()
                                        .defaultStrategyWbMaximumImpressions(Currency.RUB))
                        ));
        Long campaignId = API.userSteps.campaignSteps()
                .addCampaign(campaignMap);

        Long crId = jooqDbSteps.perfCreativesSteps().saveDefaultCanvasCreativesForClient(clientId);

        pid = API.userSteps.adGroupsSteps().addDefaultGroup(campaignId, AdGroupType.CPM_BANNER);
        API.userSteps.adsSteps().addDefaultCpmBannerAdBuilderAd(pid, crId);
        API.userSteps.audienceTargetsSteps().addWithRetargetingList(pid, rlId);

        jooqDbSteps.campaignsSteps().setType(campaignId, CampaignsType.cpm_yndx_frontpage);
        jooqDbSteps.campaignsSteps().setCampaignCpmYndxFrontpageAllowedTypes(campaignId, "frontpage_mobile");
        jooqDbSteps.adGroupsSteps().setType(pid, PhrasesAdgroupType.cpm_yndx_frontpage);

        //в качестве phraseId для ретаргетинга используется pid из bids_retargeting
        List<BidsRetargetingRecord> bids = jooqDbSteps.bidsRetargetingSteps().getBidsRetargetingByPid(pid);
        long phraseIDFake = bids.get(0).getRetCondId();

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPhraseID(BigInteger.valueOf(phraseIDFake));
        request.setCurrency(1);
        request.setContextType(2);
    }

    @Test
    public void autobudgetPricesSetRetargetingTest() {
        jooqDbSteps.adGroupsSteps().setGeo(pid, RegionIDValues.RUSSIA_REGION_ID);
        Money price = Money.valueOf(180);
        request.setPrice(price.floatValue());
        request.setContextPrice(price.floatValue());
        API.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        List<BidsRetargetingRecord> bids = jooqDbSteps.bidsRetargetingSteps().getBidsRetargetingByPid(pid);
        assertThat("Удалось установить верное значение поля price_context",
                bids.get(0).getPriceContext().doubleValue(), closeTo(price.doubleValue(), EPSILON));
    }

    @Test
    public void autobudgetPricesSetRetargetingBelowMinPriceTest() {
        jooqDbSteps.adGroupsSteps().setGeo(pid, RegionIDValues.RUSSIA_REGION_ID);
        Money price = Money.valueOf(180);
        request.setPrice(price.floatValue() - 0.1f);
        request.setContextPrice(price.floatValue() - 0.1f);
        API.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        List<BidsRetargetingRecord> bids = jooqDbSteps.bidsRetargetingSteps().getBidsRetargetingByPid(pid);
        assertThat("Удалось установить верное значение поля price_context не менее минимального",
                bids.get(0).getPriceContext().doubleValue(), closeTo(price.doubleValue(), EPSILON));
    }
}


