package ru.yandex.autotests.direct.cmd.banners.additions.video.negative;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Добавление удаленного видеодополнения к баннеру")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag("DIRECT-63700")
public class SaveVideoAdditionErrorDeletedCreativeTest extends SaveVideoAdditionErrorBaseTest {

    @Override
    protected Group modifyTestGroup(Group group) {
        Long creative_id =
                dbSteps.perfCreativesSteps().saveDefaultVideoCreative(Long.valueOf(User.get(CLIENT).getClientID()), 1L);

        dbSteps.perfCreativesSteps().deletePerfCreatives(creative_id);

        group.getBanners().get(0).addDefaultVideoAddition(creative_id);
        return group;
    }

    @Test
    @Description("При добавлении к баннеру удаленного видеодополнения получаем страницу с ошибкой")
    @TestCaseId("10943")
    public void testResponseSaveDeletedVideoAddition() {
        super.saveAndCheckResponse();
    }
}
