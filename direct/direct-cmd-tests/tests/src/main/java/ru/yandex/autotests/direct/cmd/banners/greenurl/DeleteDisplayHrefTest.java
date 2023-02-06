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
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Удаление отображаемой ссылки из баннера (saveTextAdGroups)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class DeleteDisplayHrefTest extends DisplayHrefBaseTest {

    @Test
    @Description("Удаление отображаемой ссылки из баннера (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9196")
    public void testDeleteDisplayHrefAtBanner() {
        editDisplayHref();
        assertThat("отображаемая ссылка удалилась", getDisplayHref(), nullValue());
    }

    @Test
    @Description("Статус модерации баннера не изменяется при удалении отображаемой ссылки (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9197")
    public void testDeleteDisplayHrefModerationOfBanner() {
        makeAllModerated();
        editDisplayHref();
        assertThat("статус модерации баннера при удалении отображаемой ссылки не изменился",
                getBannerStatusModerate(), equalTo(Status.YES));
    }

    @Test
    @Description("Статус statusBsSynced баннера сбрасывается при удалении отображаемой ссылки (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9198")
    public void testDeleteDisplayHrefStatusBsSynced() {
        makeAllModerated();
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES);
        editDisplayHref();
        assertThat("статус баннера statusBsSynced сбросился при удалении отображаемой ссылки",
                getBannerStatusBsSynced(), equalTo(Status.NO));
    }

    @Override
    protected String getDisplayHrefToCreateBannerWith() {
        return DISPLAY_HREF;
    }

    @Override
    protected String getDisplayHrefToAddToCreatedBanner() {
        return "";
    }
}
