package ru.yandex.autotests.direct.cmd.excel.minusphrases;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка загрузки минус-фраз через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@RunWith(Parameterized.class)
public class ExcelAddMinusPhraseTest {

    private static final String EXCEL_PATH = "excel/minusphrases/";
    private static final String CLIENT = "at-backend-minusphrases";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private Long newCampId;

    @Parameterized.Parameter(0)
    public List<String> expCampMinusPhrases;

    @Parameterized.Parameter(1)
    public List<String> expGroupMinusPhrases;

    @Parameterized.Parameter(2)
    public String excelFileName;


    @Parameterized.Parameters(name = "Минус фразы для кампании: {0} и группы: {1}, имя файла: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Arrays.asList("бронировать заранее"), Arrays.asList("купить недорого"), EXCEL_PATH + "minus_phrases.xls"},
                {Arrays.asList("бронировать заранее", "дешевые билеты"), Arrays.asList("заказать он-лайн", "купить недорого"), EXCEL_PATH + "minus_phrases2.xls"},
                {Arrays.asList("Рано утром !на рассвете"), Arrays.asList("!все тонкости"), EXCEL_PATH + "minus_phrases_mobile.xls"},
        });
    }

    @After
    public void delete() {
        if (newCampId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCampId);
        }
    }

    @Test
    @Description("Сохранение минус-фраз через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10695")
    public void saveMinusPhraseFromExcel() {
        File campFile = ResourceUtils.getResourceAsFile(excelFileName);
        newCampId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, CLIENT, "",
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsLong(LocationParam.CID);

        List campMinusPhrases = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT,
                String.valueOf(newCampId)).getMinusKeywords();
        assertThat("Сохраненные минус-фразы совпадают с ожидаемыми", campMinusPhrases, equalTo(expCampMinusPhrases));

        Long groupId = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT,
                String.valueOf(newCampId)).getGroups().get(0).getAdGroupId();
        List groupMinusPhrases = cmdRule.cmdSteps().groupsSteps().getGroup(
                CLIENT, newCampId, groupId).getMinusWords();

        assertThat("Сохраненные минус-фразы совпадают с ожидаемыми", groupMinusPhrases, equalTo(expGroupMinusPhrases));

    }
}
