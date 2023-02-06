package ru.yandex.autotests.direct.cmd.banners.autovideo;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AjaxSetAutoResourcesRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AutoVideoAction;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesCreativeType;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

//https://st.yandex-team.ru/DIRECT-61605
@Aqua.Test
@Description("Включение/выключение авто-видеорекламы (cmd=ajaxSetAutoResources)")
@Stories(TestFeatures.Banners.MEDIA_RESOURCES)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.AJAX_SET_AUTO_RESOURCES)
@Tag(ObjectTag.AUTO_VIDEO)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
public class AjaxSetAutoResourcesTest {
    private static final String CLIENT = "at-direct-auto-resources";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule textBannersRule = new TextBannersRule().withUlogin(CLIENT);
    private MobileBannersRule mobileBannersRule = new MobileBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(textBannersRule)
            .withRules(mobileBannersRule);

    private void runSetAutoResourcesScript() {
        cmdRule.darkSideSteps().getRunScriptSteps().runSetAutoResources(
                TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT),
                User.get(CLIENT).getClientID());
    }

    @Test
    @Description("Включение авто-видеорекламы для текстового баннера")
    @TestCaseId("11053")
    public void enableAutoResourcesText() {
        CommonResponse response = cmdRule.cmdSteps().ajaxSetAutoResourcesSteps().postAjaxSetAutoResources(
                textBannersRule.getCampaignId().toString(),
                AutoVideoAction.SET.toString(),
                CLIENT
        );
        assumeThat("получили success=1 для action=set", response.getSuccess(), equalTo("1"));

        runSetAutoResourcesScript();
        List<Long> videoAdditionIds = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                .findBannersPerformanceIds(textBannersRule.getBannerId(), PerfCreativesCreativeType.video_addition);
        assertThat("у баннера появилось видеодополнение",
                videoAdditionIds, hasSize(1));
    }

    @Test
    @Description("Включение авто-видеорекламы для рмп-баннера")
    @TestCaseId("11054")
    public void enableAutoResourcesMobile() {
        CommonResponse response = cmdRule.cmdSteps().ajaxSetAutoResourcesSteps().postAjaxSetAutoResources(
                mobileBannersRule.getCampaignId().toString(),
                AutoVideoAction.SET.toString(),
                CLIENT
        );
        assumeThat("получили success=1 для action=set", response.getSuccess(), equalTo("1"));

        runSetAutoResourcesScript();
        List<Long> videoAdditionIds = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                .findBannersPerformanceIds(mobileBannersRule.getBannerId(), PerfCreativesCreativeType.video_addition);
        assertThat("у баннера появилось видеодополнение",
                videoAdditionIds, hasSize(1));
    }

    @Test
    @Description("Выключение авто-видеорекламы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10666")
    public void disableAutoResources() {
        CommonResponse response = cmdRule.cmdSteps().ajaxSetAutoResourcesSteps().postAjaxSetAutoResources(
                textBannersRule.getCampaignId().toString(),
                AutoVideoAction.SET.toString(),
                CLIENT
        );
        assumeThat("получили success=1 для action=set", response.getSuccess(), equalTo("1"));
        runSetAutoResourcesScript();
        List<Long> videoAdditionIds = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                .findBannersPerformanceIds(textBannersRule.getBannerId(), PerfCreativesCreativeType.video_addition);
        assumeThat("у баннера есть видеодополнение",
                videoAdditionIds, hasSize(1));

        response = cmdRule.cmdSteps().ajaxSetAutoResourcesSteps().postAjaxSetAutoResources(
                textBannersRule.getCampaignId().toString(),
                AutoVideoAction.RESET.toString(),
                CLIENT
        );
        assumeThat("получили success=1 для action=reset", response.getSuccess(), equalTo("1"));
        runSetAutoResourcesScript();

        videoAdditionIds = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                .findBannersPerformanceIds(textBannersRule.getBannerId(), PerfCreativesCreativeType.video_addition);
        assertThat("у баннера пропало видеодополнение",
                videoAdditionIds, hasSize(0));
    }

    @Test
    @Description("При пустом action, включаем видео-рекламу") //по дефолту береться "set"
    @ru.yandex.qatools.allure.annotations.TestCaseId("10667")
    public void checkAjaxSetAutoResourcesWithEmptyAction() {
        AjaxSetAutoResourcesRequest request = new AjaxSetAutoResourcesRequest()
                .withCid(textBannersRule.getCampaignId().toString())
                .withAction("")
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().ajaxSetAutoResourcesSteps().postAjaxSetAutoResources(request);
        assumeThat("получили success=1 для пустого action", response.getSuccess(), equalTo("1"));

        runSetAutoResourcesScript();
        List<Long> videoAdditionIds = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                .findBannersPerformanceIds(textBannersRule.getBannerId(), PerfCreativesCreativeType.video_addition);
        assertThat("у баннера появилось видеодополнение",
                videoAdditionIds, hasSize(1));
    }


}
