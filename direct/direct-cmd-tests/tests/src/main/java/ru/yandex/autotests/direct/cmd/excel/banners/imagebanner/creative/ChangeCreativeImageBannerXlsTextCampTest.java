package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.creative;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Изменение параметров ГО баннера в группе через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(CampTypeTag.TEXT)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class ChangeCreativeImageBannerXlsTextCampTest extends ChangeCreativeImageBannerXlsTestBase {

    private static final String NEW_HREF = "https://gismeteo.ru";

    public ChangeCreativeImageBannerXlsTextCampTest() {
        super(CampaignTypeEnum.TEXT);
    }

    @Test
    @Description("Изменение ссылки ГО баннера с креативом в группе через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9694")
    public void changeHrefViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.HREF, 1, NEW_HREF);
        uploadExcel();

        List<Banner> actBanners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, bannersRule.getCampaignId());

        assumeThat("графическим является второй баннер", actBanners.get(1).getAdType(), equalTo(BannerType.IMAGE_AD.toString()));
        assertThat("ссылка успешно заменилась", actBanners.get(1).getHref(), equalTo(NEW_HREF));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9695")
    public void changeCreativeViaXls() {
        super.changeCreativeViaXls();
    }
}
