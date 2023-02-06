package ru.yandex.autotests.direct.cmd.banners.greenurl.xls;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Просмотр отображаемой ссылки в баннере через excel")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.EXPORT_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class ShowDisplayHrefViaExcelTest {

    protected static final String CLIENT = "at-backend-display-href";
    protected static final String DISPLAY_HREF = "somelink";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannersRule;
    private String displayHref;
    private File excelFile;

    public ShowDisplayHrefViaExcelTest(String displayHref) {
        this.displayHref = displayHref;
        bannersRule = new TextBannersRule().
                overrideBannerTemplate(new Banner().withDisplayHref(displayHref)).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Отображаемая ссылка: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {""},
                {DISPLAY_HREF},
        });
    }

    @After
    public void after() {
        if (excelFile != null) {
            excelFile.delete();
        }
    }

    @Test
    @Description("Просмотр отображаемой ссылки в баннере через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9219")
    public void testShowDisplayHrefViaExcel() {
        excelFile = cmdRule.cmdSteps().excelSteps().exportCampaign(new ExportCampXlsRequest().
                withCid(bannersRule.getCampaignId().toString()).
                withSkipArch(true).
                withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLS).
                withUlogin(CLIENT));

        String actualDisplayHref = ExcelUtils.getCellValue(excelFile, ExcelColumnsEnum.DISPLAY_HREF, 0);
        assertThat("в выгруженном через excel баннере отображаемая ссылка соответствует ожидаемой",
                actualDisplayHref, equalTo(displayHref));
    }
}
