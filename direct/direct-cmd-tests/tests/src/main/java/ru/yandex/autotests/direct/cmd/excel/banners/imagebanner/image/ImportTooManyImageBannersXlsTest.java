package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.errors.ImageAdErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Загрузка xls файла с больше чем 50 графическими объявлениями")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class ImportTooManyImageBannersXlsTest {

    private static final String CLIENT = "at-direct-excel-image-2";
    private static final String TOO_MANY_BANNERS_XLS = "excel/image/too_many_banners.xls";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Test
    @Description("Загрузка невалидниго xls файла с более чем 50 графическими объявлениями")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9724")
    public void createNewCampWithTooManyImageAdXlsTest() {
        File campFile = ResourceUtils.getResourceAsFile(TOO_MANY_BANNERS_XLS);
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(campFile.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString(ImageAdErrors.MAX_BANNERS_COUNT_REACHED.getErrorText())));
    }
}
