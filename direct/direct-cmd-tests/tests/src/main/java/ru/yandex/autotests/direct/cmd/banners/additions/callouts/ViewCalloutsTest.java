package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.SearchBannersResponse;
import ru.yandex.autotests.direct.cmd.data.banners.SearchWhat;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatResponse;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatEnum;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdditionsItemCalloutsStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersAdditionsAdditionsType;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
 * todo javadoc
 */
@Aqua.Test
@Description("Просмотр дополнений на различных контроллерах")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.CALLOUTS)
@Tag(CampTypeTag.TEXT)
public class ViewCalloutsTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    public String ulogin = "at-direct-banners-callouts-1";
    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(ulogin);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    protected SaveCampRequest saveCampRequest;
    private String[] texts = {"BMW", "bmw"};
    private CalloutsTestHelper helper;

    @Before
    public void before() {
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), bannersRule.getCampaignId().toString());

        helper.clearCalloutsForClient();

        helper.saveCallouts(helper.getRequestFor(helper.existingGroupAndSet(texts)));

    }

    @Test
    @Tag(CmdTag.SHOW_CAMP)
    @ru.yandex.qatools.allure.annotations.TestCaseId("9105")
    public void calloutsOnShowCamp() {
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(ulogin,
                bannersRule.getCampaignId().toString());
        List<String> callouts = helper.getCalloutsList(response);

        assertThat("В ответе присутствуют текстовые дополнения", callouts, containsInAnyOrder(texts));
    }

    @Test
    @Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
    @ru.yandex.qatools.allure.annotations.TestCaseId("9106")
    public void calloutsOnShowCampMultiEdit() {
        Banner banner = helper.getFirstBanner();

        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forSingleBanner(ulogin, bannersRule.getCampaignId(),
                banner.getAdGroupId(), banner.getBid());

        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(request);

        List<String> callouts = helper.getCalloutsList(response);

        assertThat("В ответе присутствуют текстовые дополнения", callouts, containsInAnyOrder(texts));
    }

    @Test
    @Tag(CmdTag.SHOW_CAMP_STAT)
    @ru.yandex.qatools.allure.annotations.TestCaseId("9108")
    public void calloutsOnShowCampStat() {
        String callout1 = "BMW";
        String callout2 = "bmw";

        ulogin = "fsinr82";
        Long clientId = 54038453L;
        Long cid = 59856318L;
        Long bid = 10415374358L;

        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();

        //stat
        //https://12060.beta5.direct.yandex.ru/registered/main.pl?cmd=showCampStat&cid=7754727&stat_periods=2014-02-05%3A2014-02-05%2C2014-02-01%3A2016-02-29%2C2013-12-02%3A2014-12-02&currency_archive=&ulogin=zzclickdirect-13415-ia9gtvr&isStat=1&y1=2014&m1=02&d1=05&y2=2014&m2=02&d2=05&sort=text&save_nds=1&show_banners_stat=1&showstat_button=1&offline_stat=0
        // Кажется, что для теста подойдёт любой ТГО баннер со статистикой, который есть на ТС. В DIRECT-97099
        // планируем разобраться, так ли это

        int shard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);

        Long calloutId1 = TestEnvironment.newDbSteps().useShard(shard).bannerAdditionsSteps().saveAdditionsItemCallouts(
                clientId, callout1, AdditionsItemCalloutsStatusmoderate.Yes);
        Long calloutId2 = TestEnvironment.newDbSteps().useShard(shard).bannerAdditionsSteps().saveAdditionsItemCallouts(
                clientId, callout2, AdditionsItemCalloutsStatusmoderate.Yes);
        TestEnvironment.newDbSteps().useShard(shard).bannerAdditionsSteps().saveBannerAdditions(bid, calloutId1,
                BannersAdditionsAdditionsType.callout);
        TestEnvironment.newDbSteps().useShard(shard).bannerAdditionsSteps().saveBannerAdditions(bid, calloutId2,
                BannersAdditionsAdditionsType.callout);

        ShowCampStatRequest request = new ShowCampStatRequest()
                .withD1("09")
                .withM1("07")
                .withY1("2021")
                .withD2("09")
                .withM2("07")
                .withY2("2021")
                .withShowBannersStat(PerlBoolean.ONE)
                .withOfflineStat(PerlBoolean.ZERO)
                .withIsStat(StatEnum.STAT_ON)
                .withCid(String.valueOf(cid))
                .withUlogin(ulogin);

        ShowCampStatResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampStat(request);
        List<String> callouts = response.getBanners()
                .stream()
                .filter(b -> bid.equals(b.getBid()))
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет ожидаемого баннера"))
                .getCallouts()
                .stream()
                .map(Callout::getCalloutText)
                .collect(toList());

        assertThat("В ответе присутствуют текстовые дополнения", callouts, containsInAnyOrder(callout1, callout2));
    }

    @Test
    @Tag(CmdTag.SEARCH_BANNERS)
    @ru.yandex.qatools.allure.annotations.TestCaseId("9109")
    public void calloutsOnSearchBanners() {
        Banner banner = helper.getFirstBanner();

        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps()
                .postSearchBanners(SearchWhat.NUM.getName(), banner.getBid().toString());
        List<String> callouts = response.getBanners()
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Не найден баннер"))
                .getCallouts()
                .stream()
                .map(Callout::getCalloutText)
                .collect(toList());

        assertThat("В ответе присутствуют текстовые дополнения", callouts, containsInAnyOrder(texts));
    }
}
