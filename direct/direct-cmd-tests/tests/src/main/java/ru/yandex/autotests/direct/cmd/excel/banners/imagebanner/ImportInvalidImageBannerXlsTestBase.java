package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.ResourceUtils;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public abstract class ImportInvalidImageBannerXlsTestBase {

    protected static final String CLIENT = "at-direct-excel-image-5";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    protected File defaultFile;
    protected File excelToUpload;

    @Before
    public void before() {
        defaultFile = ResourceUtils.getResourceAsFile(getDefaultXls());
        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xls");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }
    }

    @After
    public void delete() {
        FileUtils.deleteQuietly(excelToUpload);
    }

    protected abstract String getDefaultXls();

    protected abstract String getImageErrorText();

    public void createNewCampImageBannerWithoutImageTest() {
        ExcelUtils.setCellValue(defaultFile, excelToUpload, ExcelColumnsEnum.IMAGE, 0, "");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString(getImageErrorText())));
    }

}
