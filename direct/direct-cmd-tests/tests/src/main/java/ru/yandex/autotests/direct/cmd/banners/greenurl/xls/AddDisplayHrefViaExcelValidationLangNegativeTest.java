package ru.yandex.autotests.direct.cmd.banners.greenurl.xls;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelSteps;
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
@Description("Валидация соответствия отображаемой ссылки и геотаргетинга " +
        "при загрузке через excel (негативные кейсы)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("https://st.yandex-team.ru/DIRECT-53906")
public class AddDisplayHrefViaExcelValidationLangNegativeTest extends DisplayHrefViaExcelBaseTest {

    public String displayHref;

    public AddDisplayHrefViaExcelValidationLangNegativeTest(String displayHref, String bannerText, Geo geo) {
        this.displayHref = displayHref;
        bannersRule = new TextBannersRule().
                overrideBannerTemplate(new Banner().
                        withTitle(bannerText).
                        withBody(bannerText)).
                overrideGroupTemplate(new Group().withGeo(geo.getGeo())).
                withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    // кейсы закомментированы, т.к. это проблема транка и менять это прямо сейчас не будут
    @Parameterized.Parameters(name = "Отображаемая ссылка: {0}; гео: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // украинский язык
                {"абвгґії", "русский текст", Geo.RUSSIA},
//                {"абвгґії", "Қазақ мәтін", Geo.KAZAKHSTAN},
                {"абвгґії", "Türk şarkı sözleri", Geo.TURKEY},
                // казахский язык
                {"әғқңөұүһі", "русский текст", Geo.RUSSIA},
                {"әғқңөұүһі", "український текст", Geo.UKRAINE},
                {"әғқңөұүһі", "Türk şarkı sözleri", Geo.TURKEY},
                // турецкий язык
//                {"çğıiöşü", "русский текст", Geo.RUSSIA},
//                {"çğıiöşü", "український текст", Geo.UKRAINE},
//                {"çğıiöşü", "Қазақ мәтін", Geo.KAZAKHSTAN}
        });
    }

    @Test
    @Description("Валидация соответствия отображаемой ссылки и геотаргетинга " +
            "при загрузке через excel (негативные кейсы)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9207")
    public void testAddDisplayHrefViaExcelValidationLangNegative() {
        ExcelSteps excelSteps = cmdRule.cmdSteps().excelSteps();
        PreImportCampXlsResponse preImportResponse = excelSteps.preImportCampaign(new PreImportCampXlsRequest().
                withImportFormat(PreImportCampXlsRequest.ImportFormat.XLS).
                withJson(true).
                withXls(excelFileDest.toString()).
                withUlogin(CLIENT));

        ImportCampXlsResponse importResponse = excelSteps.importCampaign(new ImportCampXlsRequest().
                withCid(campaignId.toString()).
                withDestinationCamp(ImportCampXlsRequest.DestinationCamp.OLD).
                withGeo("0").
                withSendToModeration(true).
                withXls(excelFileDest.getName()).
                withReleaseCampLock(true).
                withsVarsName(preImportResponse.getsVarsName()).
                withUlogin(CLIENT));

        assertThat("в ответе получена ошибка", importResponse.getErrors(), not(emptyIterable()));
    }

    @Override
    protected String getDisplayHrefToSetViaExcel() {
        return displayHref;
    }
}
