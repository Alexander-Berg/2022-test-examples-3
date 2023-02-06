package ru.yandex.autotests.direct.cmd.banners.additions.video.negative;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Добавление видеодополнения другого клиента к баннеру")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag("DIRECT-63700")
public class SaveVideoAdditionErrorOtherClientCreativeTest extends SaveVideoAdditionErrorBaseTest {

    private static final String OTHER_CLIENT = "at-direct-video-addition-2";
    @Rule
    public final VideoAdditionCreativeRule otherClientVideoAdditionCreativeRule =
            new VideoAdditionCreativeRule(OTHER_CLIENT);

    @Override
    protected Group modifyTestGroup(Group group) {
        group.getBanners().get(0).addDefaultVideoAddition(otherClientVideoAdditionCreativeRule.getCreativeId());
        return group;
    }

    @Test
    @Description("При добавлении к баннеру чужого видеодополнения получаем страницу с ошибкой")
    @TestCaseId("10944")
    public void testResponseSaveOtherClientVideoAddition() {
        super.saveAndCheckResponse();
    }
}
