package ru.yandex.autotests.direct.cmd.banners.greenurl;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannerDisplayHrefsStatusmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Редактирование отображаемой ссылки в баннере (saveTextAdGroups)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class EditDisplayHrefTest extends DisplayHrefBaseTest {

    private static final String DISPLAY_HREF2 = "editedlink";

    @Test
    @Description("Редактирование отображаемой ссылки в баннере (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9199")
    public void testEditDisplayHrefAtBanner() {
        editDisplayHref();
        assertThat("отображаемая ссылка сохранилась правильно", getDisplayHref(), equalTo(DISPLAY_HREF2));
    }

    @Test
    @Description("Статус модерации отображаемой ссылки при редактировании (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9200")
    public void testEditDisplayHrefModerationOfLink() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerDisplayHrefsSteps()
                .updateStatusModeration(bannerId, BannerDisplayHrefsStatusmoderate.Yes);
        editDisplayHref();
        assertThat("статус модерации отобр. ссылки при редактировании соответствует ожидаемому",
                getDisplayHrefStatusModerate(), equalTo(Status.READY));
    }

    @Test
    @Description("Статус модерации баннера не изменяется " +
            "при редактировании отображаемой ссылки (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9201")
    public void testEditDisplayHrefModerationOfBanner() {
        makeAllModerated();
        editDisplayHref();
        assertThat("статус модерации баннера не изменился при редактировании отображаемой ссылки",
                getBannerStatusModerate(), equalTo(Status.YES));
    }

    @Test
    @Description("Статус statusBsSynced баннера не сбрасывается " +
            "при редактировании отображаемой ссылки (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9202")
    public void testEditDisplayHrefStatusBsSynced() {
        makeAllModerated();
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES);
        editDisplayHref();
        assertThat("статус баннера statusBsSynced не сбросился при редактировании отображаемой ссылки",
                getBannerStatusBsSynced(), equalTo(Status.YES));
    }

    @Override
    protected String getDisplayHrefToCreateBannerWith() {
        return DISPLAY_HREF;
    }

    @Override
    protected String getDisplayHrefToAddToCreatedBanner() {
        return DISPLAY_HREF2;
    }
}
