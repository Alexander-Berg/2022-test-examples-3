package ru.yandex.autotests.direct.cmd.banners.greenurl.xls;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannerDisplayHrefsStatusmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.enums.StatusModerate;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Редактирование отображаемой ссылки в баннере через excel")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class EditDisplayHrefViaExcelTest extends DisplayHrefViaExcelBaseTest {

    private static final String DISPLAY_HREF2 = "after";

    @Test
    @Description("Редактирование отображаемой ссылки в баннере через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9215")
    public void testEditDisplayHrefViaExcel() {
        assumeThat("отображаемая ссылка сохранилась в баннере при создании группы",
                getDisplayHref(), equalTo(DISPLAY_HREF));
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("отображаемая ссылка сохранилась правильно",
                getDisplayHref(), equalTo(DISPLAY_HREF2));
    }

    @Test
    @Description("Статус модерации отображаемой ссылки сбрасывается при редактировании через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9216")
    public void testEditDisplayHrefViaExcelLinkModeration() {
        assumeThat("отображаемая ссылка сохранилась в баннере при создании группы",
                getDisplayHref(), equalTo(DISPLAY_HREF));
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannerDisplayHrefsSteps().updateStatusModeration(bannerId, BannerDisplayHrefsStatusmoderate.Yes);
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус модерации отобр. ссылки сбросился при редактировании через excel",
                getDisplayHrefStatusModerate(), equalTo(StatusModerate.READY.toString()));
    }

    @Test
    @Description("Статус модерации баннера не изменяется при редактировании отображаемой ссылки через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9217")
    public void testEditDisplayHrefViaExcelBannerModeration() {
        assumeThat("отображаемая ссылка сохранилась в баннере при создании группы",
                getDisplayHref(), equalTo(DISPLAY_HREF));
        makeAllModerated();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerDisplayHrefsSteps()
                .updateStatusModeration(bannerId, BannerDisplayHrefsStatusmoderate.Yes);
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус модерации баннера не изменился при редактировании отображаемой ссылки",
                getBannerStatusModerate(), equalTo(Status.YES));
    }

    @Test
    @Description("Статус баннера statusBsSynced не сбрасывается при редактировании отображаемой ссылки через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9218")
    public void testEditDisplayHrefViaExcelStatusBsSynced() {
        assumeThat("отображаемая ссылка сохранилась в баннере при создании группы",
                getDisplayHref(), equalTo(DISPLAY_HREF));
        makeAllModerated();
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannersRule.getBannerId(), Status.YES);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannerDisplayHrefsSteps().updateStatusModeration(bannerId, BannerDisplayHrefsStatusmoderate.Yes);
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус баннера statusBsSynced не сбросился при редактировании отображаемой ссылки",
                getBannerStatusBsSynced(), equalTo(Status.YES));
    }

    @Override
    protected String getDisplayHrefToSetViaCmd() {
        return DISPLAY_HREF;
    }

    @Override
    protected String getDisplayHrefToSetViaExcel() {
        return DISPLAY_HREF2;
    }
}
