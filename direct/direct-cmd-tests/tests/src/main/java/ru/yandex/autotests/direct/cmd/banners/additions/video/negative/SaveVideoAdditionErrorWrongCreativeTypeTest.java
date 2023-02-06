package ru.yandex.autotests.direct.cmd.banners.additions.video.negative;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Добавление видеодополнения с неправильным типом креатива к баннеру")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag("DIRECT-63700")
public class SaveVideoAdditionErrorWrongCreativeTypeTest extends SaveVideoAdditionErrorBaseTest {

    @Rule
    public CreativeBannerRule creativeBannerRule;

    public SaveVideoAdditionErrorWrongCreativeTypeTest() {
        creativeBannerRule = new CreativeBannerRule(CampaignTypeEnum.TEXT).withUlogin(CLIENT);
        creativeBannerRule.withDirectCmdSteps(cmdRule.cmdSteps());
    }

    @Override
    protected Group modifyTestGroup(Group group) {
        group.getBanners().get(0).addDefaultVideoAddition(creativeBannerRule.getCreativeId());
        return group;
    }

    @Test
    @Description("При добавлении к баннеру видеодополнения с креативом неправильного типа получаем страницу с ошибкой")
    @TestCaseId("10945")
    public void testResponseSaveWrongCreativeTypeVideoAddition() {
        super.saveAndCheckResponse();
    }
}
