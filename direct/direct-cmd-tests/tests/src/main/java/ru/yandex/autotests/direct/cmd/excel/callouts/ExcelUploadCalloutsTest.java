package ru.yandex.autotests.direct.cmd.excel.callouts;

import org.junit.After;
import org.junit.Before;
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
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdditionsItemCalloutsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Загрузка уточнений к баннерам через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ExcelUploadCalloutsTest {

    private static final String CLIENT = "direct-test-xls-callouts";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public List<String> additions;
    @Parameterized.Parameter(1)
    public String excelFileName;

    private Integer newCampId;

    @Parameterized.Parameters(name = "Уточнения: {0}, имя файла: {1}")
    public static Collection<Object[]> testData() {
        String path = "excel/callouts/";
        return Arrays.asList(new Object[][]{
                {Arrays.asList("BMW", "bmw"), path + "callouts_2_phrases.xlsx"},
                {Arrays.asList("verylongcallout1234567890"), path + "callouts_25_symbols.xlsx"},
                {Arrays.asList("very long callout12345678"), path + "callouts_25_symbols_2.xlsx"},
                {Arrays.asList("english", "русское"), path + "callouts_rus_eng.xlsx"},
                {Arrays.asList("русский текст"), path + "callouts_another_language.xlsx"},
                {Arrays.asList("[ё,./;'\"#№$%&*_]"), path + "callouts_different_symbols.xlsx"},
        });
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps(CLIENT).bannerAdditionsSteps().clearCalloutsForClient(
                Long.valueOf(User.get(CLIENT).getClientID()));
    }

    @After
    public void delete() {
        if (newCampId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, (long)newCampId);
        }
    }

    @Test
    @Description("Загрузка уточнений к баннерам через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9734")
    public void importAdditionsTest() {
        long bannerId = upload(ResourceUtils.getResourceAsFile(excelFileName));
        List<AdditionsItemCalloutsRecord> newBannerAdditions = TestEnvironment.newDbSteps(CLIENT)
                .bannerAdditionsSteps().getBannerCallouts(bannerId);

        List<String> newAdditionsTexts = newBannerAdditions
                .stream()
                .map(AdditionsItemCalloutsRecord::getCalloutText)
                .collect(toList());

        assertThat("Загруженные уточнения соответствуют ожидаемым", newAdditionsTexts, beanDiffer(additions));
    }

    private Long upload(File campFile) {
        newCampId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, CLIENT, "",
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsInteger(LocationParam.CID);
        return cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, newCampId.toString()).getGroups()
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Группа не сохранилась"))
                .getBid();
    }
}
