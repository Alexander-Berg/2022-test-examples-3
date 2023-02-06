package ru.yandex.autotests.direct.cmd.banners.greenurl.xls;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Валидация отображаемой ссылки при загрузке через excel (негативные кейсы)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class AddDisplayHrefViaExcelValidationNegativeTest extends DisplayHrefViaExcelBaseTest {

    public String displayHref;

    public AddDisplayHrefViaExcelValidationNegativeTest(String displayHref, String bannerText, Geo geo) {
        this.displayHref = displayHref;
        bannersRule = new TextBannersRule().
                overrideBannerTemplate(new Banner().
                        withTitle(bannerText).
                        withBody(bannerText)).
                overrideGroupTemplate(new Group().withGeo(geo.getGeo())).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Отображаемая ссылка: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // длина
                {"abcdefghijklmnopqrstu", "Русский текст", Geo.RUSSIA},
                // позиция недопустимого символа
                {"\\bcdefghijklmnopqrs", "Русский текст", Geo.RUSSIA},
                {"abcdefghijklmnopqrs]", "Русский текст", Geo.RUSSIA},
                {"abcdefghijk;mnopqrst", "Русский текст", Geo.RUSSIA},
                // недопустимые символы
                {"abcde.", "Русский текст", Geo.RUSSIA},
                {"abcde\\", "Русский текст", Geo.RUSSIA},
                {"abcde?", "Русский текст", Geo.RUSSIA},
                {"abcde_", "Русский текст", Geo.RUSSIA}
        });
    }

    @Test
    @Description("Валидация отображаемой ссылки при загрузке через excel (негативные кейсы)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9208")
    public void testAddDisplayHrefViaExcelValidationNegative() {
        PreImportCampXlsRequest preImportRequest = new PreImportCampXlsRequest()
                .withImportFormat(PreImportCampXlsRequest.ImportFormat.XLS)
                .withJson(true)
                .withXls(excelFileDest.toString())
                .withUlogin(CLIENT);
        PreImportCampXlsResponse response = cmdRule.cmdSteps().excelSteps().preImportCampaign(preImportRequest);

        assertThat("в ответе получена ошибка", response.getErrors(), not(emptyIterable()));
    }

    @Override
    protected String getDisplayHrefToSetViaExcel() {
        return displayHref;
    }
}
