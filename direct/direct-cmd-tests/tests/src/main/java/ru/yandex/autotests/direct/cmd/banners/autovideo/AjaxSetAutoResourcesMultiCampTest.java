package ru.yandex.autotests.direct.cmd.banners.autovideo;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AjaxSetAutoResourcesRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AutoVideoAction;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesCreativeType;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Включение/выключение авто-видеорекламы для пользователя с несколькими кампаниями(cmd=ajaxSetAutoResources)")
@Stories(TestFeatures.Banners.MEDIA_RESOURCES)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.AJAX_SET_AUTO_RESOURCES)
@Tag(ObjectTag.AUTO_VIDEO)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class AjaxSetAutoResourcesMultiCampTest {

    private static final String CLIENT = "at-direct-auto-resources";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    private TextBannersRule bannersRule2 = new TextBannersRule()
            .overrideGroupTemplate(BeanLoadHelper.loadCmdBean(
                    CmdBeans.COMMON_REQUEST_GROUP_TEXT_WITH_TWO_BANNERS, Group.class))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule, bannersRule2);

    private void runSetAutoResourcesScript() {
        cmdRule.darkSideSteps().getRunScriptSteps().runSetAutoResources(
                TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT),
                User.get(CLIENT).getClientID());
    }

    @Test
    @Description("Включение авто-видеорекламы для всех баннеров кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10664")
    public void enableAutoResourcesForAllBanners() {
        AjaxSetAutoResourcesRequest request = new AjaxSetAutoResourcesRequest()
                .withCid(bannersRule2.getCampaignId().toString())
                .withAction(AutoVideoAction.SET.toString())
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().ajaxSetAutoResourcesSteps().postAjaxSetAutoResources(request);
        assumeThat("получили success=1 для action=set", response.getSuccess(), equalTo("1"));

        runSetAutoResourcesScript();

        List<Long> bannerVideoAdditionIds =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                        .findBannersPerformanceIds(bannersRule.getBannerId(), PerfCreativesCreativeType.video_addition);
        assumeThat("для первой кампании видеодополнений нет",
                bannerVideoAdditionIds, hasSize(0));

        List<Banner> bannerList = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, bannersRule2.getCampaignId());

        bannerList.stream().forEach(b -> {
            List<Long> bannerVideoAdditionIds2 =
                    TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                            .findBannersPerformanceIds(b.getBid(), PerfCreativesCreativeType.video_addition);
            assertThat("у баннеров второй кампании есть видедополнение",
                    bannerVideoAdditionIds2, hasSize(1));

        });
    }

}
