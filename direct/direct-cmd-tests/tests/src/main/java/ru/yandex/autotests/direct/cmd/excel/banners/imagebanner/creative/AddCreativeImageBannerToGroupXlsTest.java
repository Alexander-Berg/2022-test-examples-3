package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.creative;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

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
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


@Aqua.Test
@Description("Добавление ГО баннера с креативом в группу через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class AddCreativeImageBannerToGroupXlsTest {

    private static final String CREATIVE_IMAGE_PREFIX = "http://canvas.yandex.ru/creatives/000/";
    private static final String EXCEL_PATH = "excel/imagecreative/";
    private static final String DEFAULT_EXCEL_FILE = EXCEL_PATH +
            "AddCreativeImageBannerToGroupXlsTest_text_banner.xls";
    private static final String NEW_EXCEL_FILE = EXCEL_PATH +
            "AddCreativeImageBannerToGroupXlsTest_text_banner_add_image_banner.xls";
    private static final String CLIENT = "at-direct-xls-addimg-cr";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private CampaignRule campaignRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);

    private File excelToUpload;
    private File excelToUploadNewGroup;
    private Long creativeId;
    private Long campaignId;
    private Long adGroupId;

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();

        File campFile = ResourceUtils.getResourceAsFile(DEFAULT_EXCEL_FILE);
        try {
            excelToUploadNewGroup = File.createTempFile(RandomUtils.getString(10), ".xls");
            FileUtils.copyFile(campFile, excelToUploadNewGroup);
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xls");
        } catch (IOException e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }
        ExcelUtils.setCampaignId(excelToUploadNewGroup, campaignId);

        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                excelToUploadNewGroup, CLIENT, campaignId.toString(), ImportCampXlsRequest.DestinationCamp.OLD);
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
        File newCampFile = ResourceUtils.getResourceAsFile(NEW_EXCEL_FILE);
        Long bannerId = banner.getBid();
        Long phraseId = phrase.getId();
        ExcelUtils.updateRow(newCampFile, excelToUpload, 0, campaignId, adGroupId, bannerId, phraseId);
    }

    @After
    public void delete() {
        FileUtils.deleteQuietly(excelToUploadNewGroup);
        FileUtils.deleteQuietly(excelToUpload);
        if (creativeId != null) {
            PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, creativeId);
        }
    }

    @Test
    @Description("Добавление ГО баннера с креативом в группу через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9691")
    public void addCreativeImageBannerToGroup() {
        creativeId = createCanvasCreative();
        ExcelUtils.setCellValue(excelToUpload, ExcelColumnsEnum.IMAGE, 1,
                CREATIVE_IMAGE_PREFIX + creativeId);
        ExcelUtils.setCellValue(excelToUpload, ExcelColumnsEnum.GROUP_ID, 1, String.valueOf(adGroupId));
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                excelToUpload, CLIENT, campaignId.toString(), ImportCampXlsRequest.DestinationCamp.OLD);

        List<Banner> actualBanners = getBanners();
        assertThat("баннер успешно добавлен в группу", actualBanners.size(), equalTo(2));
    }

    private List<Group> getGroups() {
        return cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignId);
    }

    private List<Banner> getBanners() {
        return cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, campaignId);
    }

    private Long createCanvasCreative() {
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.parseLong(User.get(CLIENT).getClientID()));
    }
}
