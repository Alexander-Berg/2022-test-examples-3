package ru.yandex.autotests.direct.cmd.banners.greenurl.xls;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusModerate;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание баннера (кампании с баннером) с отображаемой ссылкой через excel")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class CreateBannerWithDisplayHrefViaExcelTest extends DisplayHrefViaExcelBaseTest {

    @After
    public void after() {
        super.after();
        if (!campaignId.equals(bannersRule.getCampaignId())) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, campaignId);
        }
    }

    @Test
    @Description("Создание баннера (кампании с баннером) с отображаемой ссылкой через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9210")
    public void testCreateBannerWithDisplayHrefViaExcel() {
        createCampaignViaExcel();
        assertThat("отображаемая ссылка сохранилась правильно", getDisplayHref(), equalTo(DISPLAY_HREF));
    }

    @Test
    @Description("Статус модерации при создании баннера (кампании с баннером) с отображаемой ссылкой через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9211")
    public void testCreateBannerWithDisplayHrefViaExcelBannerModeration() {
        createCampaignViaExcel();
        assertThat("статус модерации отобр. ссылки при создании баннера соответствует ожидаемому",
                Arrays.asList(StatusModerate.READY.toString(),
                        StatusModerate.SENDING.toString(), StatusModerate.SENT.toString()),
                hasItem(getDisplayHrefStatusModerate()));
    }

    private void createCampaignViaExcel() {
        RedirectResponse redirect = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                excelFileDest, CLIENT, "", ImportCampXlsRequest.DestinationCamp.NEW);
        campaignId = redirect.getLocationParamAsLong(LocationParam.CID);

        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId.toString());
        groupId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в загруженной кампании есть группа"))
                .getAdGroupId();
        bannerId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в загруженной кампании есть баннер"))
                .getBid();
    }
}
