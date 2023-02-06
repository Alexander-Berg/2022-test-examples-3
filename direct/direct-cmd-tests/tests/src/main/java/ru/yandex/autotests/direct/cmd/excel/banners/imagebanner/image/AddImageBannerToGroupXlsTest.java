package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.IMAGE_UPLOAD_CONDITION;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.allImageUploadTasksProcessed;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


@Aqua.Test
@Description("Добавление ГО баннера в группу через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AddImageBannerToGroupXlsTest {
    private static final String EXCEL_PATH = "excel/image/";
    private static final String DEFAULT_EXCEL_FILE =
            EXCEL_PATH + "AddImageBannerToGroupXlsTest_text_banner.xls";
    private static final String NEW_EXCEL_FILE =
            EXCEL_PATH + "AddImageBannerToGroupXlsTest_text_banner_add_image_banner.xls";
    private static final String DEFAULT_EXCEL_FILE_OLD_TITLE_COLUMN =
            EXCEL_PATH + "AddImageBannerToGroupXlsTest_text_banner_with_old_title_column.xls";
    private static final String NEW_EXCEL_FILE_OLD_TITLE_COLUMN =
            EXCEL_PATH + "AddImageBannerToGroupXlsTest_text_banner_add_image_banner_with_old_title_column.xls";
    private static final String CLIENT = "at-direct-xls-add-img-b-2";
    private static final Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private CampaignRule campaignRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    private Long campaignId;
    private File patchedNewExcelFile;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public String defaultExcelFile;

    @Parameterized.Parameter(2)
    public String newExcelFile;
    private Long adGroupId;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Название колонки с заголовком - \"Заголовок\"", DEFAULT_EXCEL_FILE_OLD_TITLE_COLUMN,
                        NEW_EXCEL_FILE_OLD_TITLE_COLUMN},
                {"Название колонки с заголовком - \"Заголовок 1\"", DEFAULT_EXCEL_FILE, NEW_EXCEL_FILE}
        });
    }

    @Before
    public void before() throws IOException {
        campaignId = campaignRule.getCampaignId();

        File tmp = File.createTempFile(RandomUtils.getString(10), ".xls");

        File campFile = ResourceUtils.getResourceAsFile(defaultExcelFile);
        ExcelUtils.setCampaignId(campFile, tmp, campaignId);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                tmp, CLIENT, campaignId.toString(), ImportCampXlsRequest.DestinationCamp.OLD);

        List<Group> groups = getGroups();
        assumeThat("в кампании одна группа", groups, hasSize(1));
        Group group = groups.get(0);
        adGroupId = Long.parseLong(group.getAdGroupID());

        List<Banner> banners = group.getBanners();
        assumeThat("в группе один баннер", banners, hasSize(1));
        Banner banner = banners.get(0);
        List<Phrase> phrases = group.getPhrases();
        assumeThat("в группе одна фраза", phrases, hasSize(1));
        Phrase phrase = phrases.get(0);

        // Подготовим Excel для загрузки: проставим ID созданной группы, баннера и фразы
        File newCampFile = ResourceUtils.getResourceAsFile(newExcelFile);
        Long bannerId = banner.getBid();
        Long phraseId = phrase.getId();
        patchedNewExcelFile = File.createTempFile(RandomUtils.getString(10), ".xls");
        ExcelUtils.updateRow(newCampFile, patchedNewExcelFile, 0, campaignId, adGroupId, bannerId, phraseId);
        ExcelUtils.setCellValue(patchedNewExcelFile, ExcelColumnsEnum.GROUP_ID, 1, String.valueOf(adGroupId));
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(patchedNewExcelFile);
    }

    @Test
    @Description("Добавление ГО баннера в группу через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9707")
    public void addImageBannerToGroup() {
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                patchedNewExcelFile, CLIENT, campaignId.toString(), ImportCampXlsRequest.DestinationCamp.OLD);

        IMAGE_UPLOAD_CONDITION.until(allImageUploadTasksProcessed(cmdRule, clientId));
        List<Banner> actualBanners = getBanners();

        assertThat("баннер успешно добавлен в группу", actualBanners.size(), equalTo(2));
    }

    private List<Group> getGroups() {
        return cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignId);
    }

    private List<Banner> getBanners() {
        return cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, campaignId, adGroupId);
    }
}
