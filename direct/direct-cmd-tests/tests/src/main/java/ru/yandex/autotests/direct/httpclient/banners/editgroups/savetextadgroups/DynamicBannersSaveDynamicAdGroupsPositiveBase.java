package ru.yandex.autotests.direct.httpclient.banners.editgroups.savetextadgroups;

import java.util.Arrays;
import java.util.Collection;

import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyTypeEnum;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.DynamicGroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.campaigns.AddRequestMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignAddItemMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.DynamicTextCampaignStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.StrategyMaximumClicksAddMap;
import ru.yandex.autotests.directapi.model.api5.general.ExpectedResult;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;
import static ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraints.ignore;

@RunWith(Parameterized.class)
public abstract class DynamicBannersSaveDynamicAdGroupsPositiveBase {


    public static final String CLIENT_LOGIN = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public static CSRFToken csrfToken;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    public Long campaignId;
    public DynamicGroupsCmdBean expectedGroups;
    @Parameterized.Parameter(value = 0)
    public String description;
    @Parameterized.Parameter(value = 1)
    public String saveBean;
    @Parameterized.Parameter(value = 2)
    public String editBean;
    @Parameterized.Parameter(value = 3)
    public boolean isAutobudget;

    private long groupId;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Проверка сохранения группы с уcловием на все страницы ",
                        "simpleDynamicBannerForPositiveSaveNew2", "simpleDynamicBannerForPositiveSaveEditNew2", false},
                {"Проверка сохранения группы с большим набором уловий ",
                        "hardDynamicBannerForSaveNewArrayCondition2", "hardDynamicBannerForSaveEditNewArrayCondition2", false},
                {"Проверка сохранения группы с уcловием на все страницы для автобюджетной кампании",
                        "simpleDynamicBannerForPositiveSaveAutoBudgetNew2", "simpleDynamicBannerForPositiveSaveEditAutoBudgetNew2", true},
                {"Проверка сохранения группы с большим набором уловий для автобюджетной кампании",
                        "hardDynamicBannerForSaveAutoBudgetNewArrayCondition2", "hardDynamicBannerForSaveEditAutoBudgetNewArrayCondition2", true}
        });
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        csrfToken = getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());

        if (isAutobudget) {
            Long weeklySpendLimit = MoneyCurrency.get(Currency.RUB).getMaxPrice().multiply(10).bidLong().longValue();
            DynamicTextCampaignStrategyAddMap strategyMap = new DynamicTextCampaignStrategyAddMap()
                    .withSearch(new DynamicTextCampaignSearchStrategyAddMap()
                            .withBiddingStrategyType(DynamicTextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                            .withWbMaximumClicks(new StrategyMaximumClicksAddMap()
                                    .withWeeklySpendLimit(weeklySpendLimit)
                            ))
                    .withNetwork(new DynamicTextCampaignNetworkStrategyAddMap().defaultServingOff());

            AddResponse response = cmdRule.apiSteps().campaignSteps().shouldGetResultOnAdd(
                    new AddRequestMap().withCampaigns(new CampaignAddItemMap()
                            .defaultCampaignAddItem()
                            .withDynamicTextCampaign(new DynamicTextCampaignAddItemMap()
                                    .withBiddingStrategy(strategyMap))),
                    CLIENT_LOGIN,
                    ExpectedResult.success());

            assumeThat("получили результаты добавления кампании", response.getAddResults(), hasSize(1));
            campaignId = response.getAddResults().get(0).getId();
        } else {
            campaignId = cmdRule.apiSteps().campaignSteps().addDefaultDynamicTextCampaign(CLIENT_LOGIN);
        }
    }

    @Description("Проверка сохранения группы ")
    public void editDynamicBannerPositiveTest() {
        expectedGroups = new PropertyLoader<>(DynamicGroupsCmdBean.class).getHttpBean(saveBean);
        saveBannerResponse();
        editBanner(editBean);
        DirectResponse response = getResponseFirstDynamicGroupFromCampaign();
        DynamicGroupsCmdBean actualResponse = JsonPathJSONPopulater.eval(response.getResponseContent().asString(),
                new DynamicGroupsCmdBean(), BeanType.RESPONSE, Arrays.asList("filter_schema_performance", "filter_schema_dynamic", "static_file_hashsums"));
        assertThat("баннер в ответе контроллера соответствует сохраненному ",
                actualResponse,
                beanEquivalent(expectedGroups).fields(ignore(
                        "groups[0]/adGroupID",
                        "groups[0]/banners[0]/bannerID",
                        "groups[0]/banners[0]/image",
                        ".*dynamicConditionId",
                        ".*hrefParams",
                        "groups[0]/tags")));
    }

    /**
     * Сохранение группы
     */
    protected void saveBannerResponse() {
        GroupsParameters requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(String.valueOf(campaignId));
        expectedGroups.getGroups().get(0).getBanners().get(0).setTitle("{Динамический заголовок}");
        expectedGroups.getGroups().get(0).setBannersQuantity("1");
        requestParams.setJsonGroups(expectedGroups.toJson());
        requestParams.setNewGroup("1");
        requestParams.setIsGroupsCopyAction("0");
        requestParams.setSaveDraft("0");

        DirectResponse response = cmdRule.oldSteps().groupsSteps().saveDynamicAdGroups(csrfToken, requestParams);
        Matcher toShowCampRedirectMatcher = allOf(
                containsString("cmd=" + CMD.SHOW_CAMP),
                containsString("cid=" + campaignId));
        cmdRule.oldSteps().commonSteps().checkJsonRedirect(response, toShowCampRedirectMatcher);
    }

    protected abstract void editBanner(String beanName);

    /**
     * Получение ответа ручки editDynamicAdGroups
     */
    protected DirectResponse getResponseFirstDynamicGroupFromCampaign() {

        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT_LOGIN, campaignId.toString());
        groupId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"))
                .getAdGroupId();
        GroupsParameters requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setAdgroupIds(Long.toString(groupId));
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setBannerStatus("all");
        return cmdRule.oldSteps().groupsSteps().editDynamicAdGroups(csrfToken, requestParams);
    }

    @After
    public void deleteCampaign() {
        if (campaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT_LOGIN, campaignId);
        }
    }
}
