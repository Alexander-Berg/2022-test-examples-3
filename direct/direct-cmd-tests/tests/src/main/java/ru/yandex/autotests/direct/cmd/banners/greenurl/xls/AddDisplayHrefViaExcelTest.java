package ru.yandex.autotests.direct.cmd.banners.greenurl.xls;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.enums.StatusModerate;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Добавление отображаемой ссылки в баннер через excel")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class AddDisplayHrefViaExcelTest extends DisplayHrefViaExcelBaseTest {

    @Test
    @Description("Добавление отображаемой ссылки в баннер через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9203")
    public void testAddDisplayHrefViaExcel() {
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("отображаемая ссылка сохранилась правильно", getDisplayHref(), equalTo(DISPLAY_HREF));
    }

    @Test
    @Description("Статус модерации отображаемой ссылки при добавлении в баннер через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9204")
    public void testAddDisplayHrefViaExcelLinkModeration() {
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус модерации отобр. ссылки при добавлении в баннер соответствует ожидаемому",
                getDisplayHrefStatusModerate(), equalTo(StatusModerate.READY.toString()));
    }

    @Test
    @Description("Статус модерации баннера не изменяется при добавлении отображаемой ссылки через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9205")
    public void testAddDisplayHrefViaExcelBannerModeration() {
        makeAllModerated();
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус модерации баннера не изменился при добавлении отображаемой ссылки",
                getBannerStatusModerate(), equalTo(Status.YES));
    }

    @Test
    @Description("Статус statusBsSynced баннера не сбрасывается при добавлении отображаемой ссылки через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9206")
    public void testAddDisplayHrefViaExcelStatusBsSynced() {
        makeAllModerated();
        cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(bannerId, Status.YES);
        uploadXls(campaignId, ImportCampXlsRequest.DestinationCamp.OLD);
        assertThat("статус баннера statusBsSynced не сбросился при добавлении отображаемой ссылки",
                getBannerStatusBsSynced(), equalTo(Status.YES));
    }
}
