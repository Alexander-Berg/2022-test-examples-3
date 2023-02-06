package ru.yandex.autotests.direct.httpclient.banners.getadgroup;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupRequestParametersBuilder;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupResponse;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupTab;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.util.BeanLoadHelper.loadCmdBean;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка отображения баннеров в ответе контроллера getAdGroup при различных значениях параметра tab")
@Stories(TestFeatures.Banners.GET_AD_GROUP)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GetAdGroupTabParameterTest {

    private static final String CLIENT = "at-direct-b-getadgroup-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public String description;
    public Strategies strategy;
    public GetAdGroupTab getAdGroupTab;
    public String expectedResponseTemplate;
    @Rule
    public DirectCmdRule cmdRule;
    private GetAdGroupRequestParameters getAdGroupRequestParameters;
    private Long adGroupId;
    private TextBannersRule bannersRule;
    private CampaignStrategy ajaxStrategy;
    private GetAdGroupResponse getAdGroupResponseExpected;
    private List<String> expectedBannerIds;
    private Long[] bannerIds = new Long[2];

    public GetAdGroupTabParameterTest(String description, Strategies strategy, GetAdGroupTab getAdGroupTab, String expectedResponseTemplate) {
        this.description = description;
        this.strategy = strategy;
        this.getAdGroupTab = getAdGroupTab;
        this.expectedResponseTemplate = expectedResponseTemplate;
        ajaxStrategy = CmdStrategyBeans.getStrategyBean(strategy, User.get(CLIENT).getCurrency());
        bannersRule = new TextBannersRule().convertCurrency(true)
                .overrideCampTemplate(new SaveCampRequest().withJsonStrategy(ajaxStrategy))
                .withUlogin(CLIENT);
        Group group = bannersRule.getGroup();
        group.getBanners().add(loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2, Group.class).getBanners().get(0));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(CLIENT);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Проверка отображения баннеров с контекстными показами при параметре tab=base",
                        Strategies.SHOWS_DISABLED_MAX_COVERADGE, GetAdGroupTab.BASE, "getAdGroupContextBanners"}, // нету параметра tab
                {"Проверка отображения баннеров с показами на поиске при параметре tab=base",
                        Strategies.HIGHEST_POSITION_DEFAULT, GetAdGroupTab.BASE, "getAdGroupSearchBanners"},
                {"Проверка отображения баннеров с показами на поиске при параметре tab=search",
                        Strategies.HIGHEST_POSITION_DEFAULT, GetAdGroupTab.SEARCH, "getAdGroupSearchBanners"},
                {"Проверка отображения баннеров с контекстными показами при параметре tab=context",
                        Strategies.SHOWS_DISABLED_MAX_COVERADGE, GetAdGroupTab.CONTEXT, "getAdGroupContextBanners"}
        });
    }

    @Before
    public void before() {
        PropertyLoader<GetAdGroupResponse> propertyLoader = new PropertyLoader<>(GetAdGroupResponse.class);

        Group group = bannersRule.getCurrentGroup();
        bannerIds = group.getBanners().stream()
                .map(Banner::getBid)
                .collect(toList()).toArray(bannerIds);

        adGroupId = Long.parseLong(group.getAdGroupID());

        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        getAdGroupRequestParameters = new GetAdGroupRequestParametersBuilder().
                setAdGroupId(String.valueOf(adGroupId)).createGetAdGroupRequestParameters();
        getAdGroupResponseExpected = propertyLoader.getHttpBean(expectedResponseTemplate);
        getAdGroupRequestParameters.setTab(getAdGroupTab);

        expectedBannerIds = new LinkedList<>();
        expectedBannerIds.add(String.valueOf(bannerIds[0]));
        expectedBannerIds.add(String.valueOf(bannerIds[1]));
    }

    @Test
    @Description("Проверка списка баннеров в ответе контроллера getAdGroup")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10200")
    public void checkGetAdGroupResponseWithContextTabContextBanners() {
        GetAdGroupResponse getAdGroupResponse = cmdRule.oldSteps().getAdGroupSteps().
                getAdGroup(getAdGroupRequestParameters);
        assertThat("Список баннеров в ответе контроллера getAdGroup не совпадает с ожидаемым",
                getAdGroupResponse.getBids(), equalTo(expectedBannerIds));
    }

}
