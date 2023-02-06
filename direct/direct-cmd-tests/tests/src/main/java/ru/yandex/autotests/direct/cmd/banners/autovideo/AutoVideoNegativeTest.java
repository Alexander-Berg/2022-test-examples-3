package ru.yandex.autotests.direct.cmd.banners.autovideo;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AjaxSetAutoResourcesRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AutoVideoAction;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannerResourcesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("аудио/видео ресурсы негативные сценарии")
@Stories(TestFeatures.Banners.MEDIA_RESOURCES)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.AJAX_SET_AUTO_RESOURCES)
@Tag(ObjectTag.AUTO_VIDEO)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class AutoVideoNegativeTest {
    private static final String CLIENT = "at-direct-auto-resources";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private void runSetAutoResourcesScript() {
        cmdRule.darkSideSteps().getRunScriptSteps().runSetAutoResources(
                TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT),
                User.get(CLIENT).getClientID());
    }

    @Test
    @Description("Нельзя включить авто-видеорекламу для чужого cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10668")
    public void enableAutoResourcesForWrongCid() {
        AjaxSetAutoResourcesRequest request = new AjaxSetAutoResourcesRequest()
                .withCid("1234")
                .withAction(AutoVideoAction.SET.toString())
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().ajaxSetAutoResourcesSteps().postAjaxSetAutoResources(request);
        assumeThat("запрос не вернул успешного ответа", response.getSuccess(), nullValue());

        runSetAutoResourcesScript();
        BannerResourcesRecord bannerResources = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannerResourcesSteps().getBannerResourceByBid(bannersRule.getBannerId());
        assertThat("запись в banner_resources не добавилась", bannerResources, nullValue());
    }
}
