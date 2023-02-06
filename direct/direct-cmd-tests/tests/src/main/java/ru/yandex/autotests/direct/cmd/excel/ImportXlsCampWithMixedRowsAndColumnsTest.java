package ru.yandex.autotests.direct.cmd.excel;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.OrgDetails;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PhrasesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.cmd.util.BeanLoadHelper.loadCmdBean;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка загрузки эксель-файла с перемешанными строками/столбцами")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(BusinessProcessTag.EXCEL)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class ImportXlsCampWithMixedRowsAndColumnsTest {

    private static final String CLIENT = "direct-test-xls";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter
    public String excelFileType;
    private Group expectedGroup;
    private Long newCampId;

    @Parameterized.Parameters(name = "Формат excel файла: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"xls"},
                {"xlsx"},
        });
    }

    @Before
    public void before() {
        expectedGroup = loadCmdBean("cmd.excel.import.defaultGroupForExcel", Group.class);
    }

    @Test
    @Description("Проверка корректности загрузки эксель-файла с перемешанными столбцами заголовка")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9685")
    public void importFileWithMixedTitleColumnsTest() {
        Long groupId = uploadCampaign("excel/additions/file_with_mixed_title_columns." + excelFileType);

        Group newGroup = cmdRule.cmdSteps().groupsSteps().getGroup(CLIENT, newCampId, groupId);
        prepareGroup(expectedGroup);
        assertThat("загруженная группа соответствует ожидаемому",
                newGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @After
    public void delete() {
        if (newCampId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCampId);
        }
    }

    private Long uploadCampaign(String excelFilePath) {
        File campFile = ResourceUtils.getResourceAsFile(excelFilePath);
        newCampId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, CLIENT, "",
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsLong(LocationParam.CID);
        List<Long> newGroupIDs =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps().getPhrasesByCid(newCampId)
                        .stream().map(PhrasesRecord::getPid).collect(Collectors.toList());
        assumeThat("в кампании есть 1 группа", newGroupIDs, hasSize(1));
        return newGroupIDs.get(0);
    }

    private void prepareGroup(Group expectedGroup) {
        expectedGroup.getBanners().stream().map(Banner::getContactInfo)
                .forEach(contactInfo -> {
                    contactInfo.withOGRN(null);
                    contactInfo.withOrgDetails(new OrgDetails(null, contactInfo.getOGRN()));
                });
        expectedGroup.getPhrases().forEach(phrase -> phrase.withGuarantee(null).withPremium(null));
    }
}
