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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Удаление отображаемой ссылки из баннера через excel")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class DeleteDisplayHrefViaExcelTest extends DisplayHrefViaExcelBaseTest {

    @Test
    @Description("Удаление отображаемой ссылки из баннера через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9213")
    public void testDeleteDisplayHrefViaExcel() {
        assumeThat("отображаемая ссылка сохранилась в баннере при создании группы",
                getDisplayHref(), equalTo(DISPLAY_HREF));
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("отображаемая ссылка удалена из баннера через excel",
                getDisplayHref(), nullValue());
    }

    @Test
    @Description("Статус модерации баннера не изменяется при удалении отображаемой ссылки через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9212")
    public void testDeleteDisplayHrefViaExcelBannerModeration() {
        assumeThat("отображаемая ссылка сохранилась в баннере при создании группы",
                getDisplayHref(), equalTo(DISPLAY_HREF));
        makeAllModerated();
        TestEnvironment.newDbSteps(CLIENT).bannerDisplayHrefsSteps().updateStatusModeration(bannerId, BannerDisplayHrefsStatusmoderate.Yes);
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус модерации баннера не изменился при удалении отображаемой ссылки",
                getBannerStatusModerate(), equalTo(Status.YES));
    }

    @Test
    @Description("Статус баннера statusBsSynced сбрасывается при удалении отображаемой ссылки через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9214")
    public void testDeleteDisplayHrefViaExcelStatusBsSynced() {
        assumeThat("отображаемая ссылка сохранилась в баннере при создании группы",
                getDisplayHref(), equalTo(DISPLAY_HREF));
        makeAllModerated();
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannersRule.getBannerId(), Status.YES);
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус баннера statusBsSynced сбросился при удалении отображаемой ссылки",
                getBannerStatusBsSynced(), equalTo(Status.NO));
    }

    @Override
    protected String getDisplayHrefToSetViaCmd() {
        return DISPLAY_HREF;
    }

    @Override
    protected String getDisplayHrefToSetViaExcel() {
        return "";
    }
}
