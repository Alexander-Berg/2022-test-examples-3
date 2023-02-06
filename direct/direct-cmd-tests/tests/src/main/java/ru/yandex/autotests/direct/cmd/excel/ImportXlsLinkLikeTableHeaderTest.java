package ru.yandex.autotests.direct.cmd.excel;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.utils.matchers.BeanEqualsAssert.assertThat;

/**
 * DIRECT-50518
 */
@Aqua.Test
@Description("Проверка загрузки эксель-файла с быстрыми ссылками, совпадающими с заголовками таблицы")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ImportXlsLinkLikeTableHeaderTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;

    private File xlsFile;

    public ImportXlsLinkLikeTableHeaderTest(CampaignTypeEnum campType, String xlsName) {
        this.xlsFile = ResourceUtils.getResourceAsFile(xlsName);
        cmdRule = DirectCmdRule.defaultRule().withRules(BannersRuleFactory.
                getBannersRuleBuilderByCampType(campType).
                withUlogin(CLIENT));
    }

    @Parameterized.Parameters(name = "Тип кампании: {0}; excel-файл: {1};")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, "excel/additions/text_camp_link_like_table_header.xls"},
                {CampaignTypeEnum.TEXT, "excel/additions/text_camp_links_like_table_header.xls"},
                {CampaignTypeEnum.MOBILE, "excel/additions/mobile_app_camp_link_like_table_header.xls"},
                {CampaignTypeEnum.MOBILE, "excel/additions/mobile_app_camp_links_like_table_header.xls"},
        });
    }

    @Before
    public void before() {
    }

    @Test
    @Description("Загрузки эксель-файла с быстрыми ссылками, совпадающими с заголовками таблицы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9686")
    public void linkLikeTableHeader() {
        PreImportCampXlsResponse response = cmdRule.cmdSteps().excelSteps().preImportCampaign(new PreImportCampXlsRequest().
                withImportFormat(PreImportCampXlsRequest.ImportFormat.XLS).
                withXls(xlsFile.toString()).
                withJson(true));

        assertThat("загрузка excel файла с быстрыми ссылками, совпадающими с заголовками таблицы, произошла без ошибок",
                response.getErrors(), hasSize(0));
    }
}
