package ru.yandex.autotests.direct.httpclient.banners.editgroups.showcampmultiedit;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка контроллера showCampMultiEdit при редактировнии одиночного баннера")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class ExistedSingleBannerShowCampMultiEditTest extends ShowCampMultiEditTestBase {

    public void init() {
        expectedGroups = new PropertyLoader<>(GroupsCmdBean.class).getHttpBean("singleGroupForShowCampMultiEdit");
        requestParams.setAdgroupIds(bannersRule.getGroupId().toString());
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10151")
    public void showCampMultiEditResponseTest() {
        super.showCampMultiEditResponseTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10150")
    public void showCampMultiEditLightResponseTest() {
        super.showCampMultiEditLightResponseTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10152")
    public void goBackShowCampMultiEditResponseTest() {
        super.goBackShowCampMultiEditResponseTest();
    }
}
