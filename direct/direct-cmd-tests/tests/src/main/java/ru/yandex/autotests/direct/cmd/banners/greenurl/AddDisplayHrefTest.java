package ru.yandex.autotests.direct.cmd.banners.greenurl;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Добавление отображаемой ссылки в баннер (saveTextAdGroups)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class AddDisplayHrefTest extends DisplayHrefBaseTest {

    @Test
    @Description("Добавление отображаемой ссылки в баннер (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9186")
    public void testAddDisplayHrefToBanner() {
        editDisplayHref();
        assertThat("отображаемая ссылка сохранилась правильно", getDisplayHref(), equalTo(DISPLAY_HREF));
    }

    @Test
    @Description("Статус модерации отображаемой ссылки при добавлении в баннер (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9187")
    public void testAddDisplayHrefToBannerModerationOfLink() {
        editDisplayHref();
        assertThat("статус модерации отобр. ссылки при добавлении в баннер соответствует ожидаемому",
                getDisplayHrefStatusModerate(), equalTo(Status.READY));
    }

    @Test
    @Description("Статус модерации баннера не изменяется при добавлении отображаемой ссылки (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9188")
    public void testAddDisplayHrefToBannerModerationOfBanner() {
        makeAllModerated();
        editDisplayHref();
        assertThat("статус модерации баннера не изменился при добавлении отображаемой ссылки",
                getBannerStatusModerate(), equalTo(Status.YES));
    }

    @Test
    @Description("Статус statusBsSynced баннера не сбрасывается при добавлении отображаемой ссылки (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9189")
    public void testAddDisplayHrefToBannerStatusBsSynced() {
        makeAllModerated();
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES);
        editDisplayHref();
        assertThat("статус баннера statusBsSynced не сбросился при добавлении отображаемой ссылки",
                getBannerStatusBsSynced(), equalTo(Status.YES));
    }
}
