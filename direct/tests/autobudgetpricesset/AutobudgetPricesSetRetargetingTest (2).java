package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesAdgroupType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRetargetingRecord;
import ru.yandex.autotests.direct.db.steps.CampaignsSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.RetargetingGoal;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.model.campaigns.MetrikaGoals;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.closeTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * Authors: omaz, xy6er
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3980
 */
@Aqua.Test(title = "AutobudgetPrices.set - ретаргетинг")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetRetargetingTest {
    private AutobudgetPricesSetRequest request;
    private Long bid;
    private Long pid;
    //цель Метрики для привязки ретаргетинга
    private Integer goaldID = MetrikaGoals.getRandom();

    private static int shard;
    private static String login = Logins.LOGIN_RUB;
    private static CampaignsSteps campaignsSteps;

    private static final double EPSILON = 1e-6;

    @ClassRule
    public static final ApiSteps API = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static final SemaphoreRule SEMAPHORE = Semaphore.getSemaphore();

    @BeforeClass
    public static void beforeClass() {
        shard = API.userSteps.clientFakeSteps().getUserShard(login);
        campaignsSteps = API.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps();
    }

    @Before
    public void init() {
        Long campaignId = API.userSteps.campaignSteps()
                .addDefaultTextCampaignWithStrategies(
                        new TextCampaignSearchStrategyAddMap().defaultServingOff(),
                        new TextCampaignNetworkStrategyAddMap().defaultWbMaximumClicks(Currency.RUB),
                        login);

        campaignsSteps.addCampMetrikaGoals(campaignId, (long) goaldID, 200L, 0L);
        RetargetingGoal goal = new RetargetingGoal();
        goal.setLogin(login);
        goal.setGoalID(goaldID);
        goal.setName("goal name");
        int[] retargetingConditionIds = API.userSteps.retargetingSteps().addRetargetingConditions(
                API.userSteps.retargetingSteps().generateRandomRetargetingCondition(login, goal)
        );
        pid = API.userSteps.adGroupsSteps().addDefaultGroup(campaignId, login);
        bid = API.userSteps.adsSteps().addDefaultTextAd(pid, login);
        API.userSteps.keywordsSteps().addDefaultKeyword(login, pid);
        API.userSteps.retargetingSteps().addRetargetingToBanner(login, bid, retargetingConditionIds[0]);

        //в качестве phraseId для ретаргетинга используется pid из bids_retargeting
        long phraseIDFake = API.userSteps.getDirectJooqDbSteps().useShard(shard)
                .bidsRetargetingSteps()
                .getBidsRetargetingByBid(bid)
                .get(0)
                .getRetCondId();

        request = new AutobudgetPricesSetRequest();
        request.setGroupExportID(pid);
        request.setPhraseID(BigInteger.valueOf(phraseIDFake));
        request.setCurrency(1);
        request.setContextType(2);
    }

    @Test
    public void autobudgetPricesSetRetargetingTest() {
        Money price = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        request.setPrice(price.floatValue());
        request.setContextPrice(price.floatValue());
        API.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        List<BidsRetargetingRecord> bids = API.userSteps.getDirectJooqDbSteps().useShard(shard)
                .bidsRetargetingSteps().getBidsRetargetingByBid(bid);
        assertThat("Удалось установить верное значение поля price_context",
                bids.get(0).getPriceContext().doubleValue(), closeTo(price.doubleValue(), EPSILON));
    }

    @Test
    public void autobudgetPricesSetRetargetingTextCampBelowMinPriceTest() {
        retargetingCommon(null);
    }

    @Test
    public void autobudgetPricesSetRetargetingCpmBannerBelowMinPriceTest() {
        retargetingCommon(PhrasesAdgroupType.cpm_banner);
    }

    @Test
    public void autobudgetPricesSetRetargetingCpmVideoBelowMinPriceTest() {
        retargetingCommon(PhrasesAdgroupType.cpm_video);
    }

    @Test
    public void autobudgetPricesSetRetargetingCpmOutdoorBelowMinPriceTest() {
        retargetingCommon(PhrasesAdgroupType.cpm_outdoor);
    }

    private void retargetingCommon(PhrasesAdgroupType type) {
        Money price;
        if (type != null) {
            API.userSteps.getDirectJooqDbSteps().useShard(shard).adGroupsSteps()
                    .setType(pid, type);
            price = MoneyCurrency.get(Currency.RUB).getMinCpmPrice();
        } else {
            price = MoneyCurrency.get(Currency.RUB).getMinPrice();
        }
        request.setPrice(price.floatValue() - 0.1f);
        request.setContextPrice(price.floatValue() - 0.1f);
        API.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        List<BidsRetargetingRecord> bids = API.userSteps.getDirectJooqDbSteps().useShard(shard)
                .bidsRetargetingSteps().getBidsRetargetingByBid(bid);
        assertThat("Удалось установить верное значение поля price_context не менее минимального",
                bids.get(0).getPriceContext().doubleValue(), closeTo(price.doubleValue(), EPSILON));
    }

}

