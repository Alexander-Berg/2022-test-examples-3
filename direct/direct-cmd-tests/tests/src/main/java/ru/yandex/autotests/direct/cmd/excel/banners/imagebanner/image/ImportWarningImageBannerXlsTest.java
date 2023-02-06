package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Загрузка невалидниго xls файла с графическим объявлением")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ImportWarningImageBannerXlsTest {

    private static final String CLIENT = "at-direct-excel-image-2";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public String xlsName;
    @Parameterized.Parameter(1)
    public Integer rowNum;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Parameterized.Parameters(name = "Загрузка невалидниго xls файла с графическим объявлением. Xls файл: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"excel/image/one_tex-image-mobile_banner.xls", 12},
                {"excel/image/text_and_mobile_image_banner.xls", 14},
        });
    }

    @Test
    @Description("Загрузка невалидниго xls файла с графическим объявлением")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9725")
    public void createNewCampWithImageAdFromXlsWarningsTest() {
        File campFile = ResourceUtils.getResourceAsFile(xlsName);
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(campFile.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("предупреждение соответсвует ожиданию", preImportResponse.getWarnings(),
                hasItem(containsString("Строка " + rowNum + ": " + ImageAdErrors.MOBILE_IMAGE_AD_WARNING.getErrorText())));
    }

}
