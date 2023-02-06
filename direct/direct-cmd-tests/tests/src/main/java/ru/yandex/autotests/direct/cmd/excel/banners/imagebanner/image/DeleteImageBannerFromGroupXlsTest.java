package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.IMAGE_UPLOAD_CONDITION;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.allImageUploadTasksProcessed;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Удаление ГО баннера из группы через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class DeleteImageBannerFromGroupXlsTest {
    private static final String EXCEL_PATH = "excel/image/";
    private static final String DEFAULT_EXCEL_FILE = EXCEL_PATH + "20638368_text_banner_add_image_banner.xls";
    private static final String NEW_EXCEL_FILE = EXCEL_PATH + "20638368_text_banner_delete_image_banner.xls";
    private static final String CLIENT = "at-direct-xls-delete-img-b";
    private static final Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
    private static final Long CAMPAIGN_ID = 20638368L;
    private static final Long GROUP_ID = 1787198109L;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Before
    public void before() {
        cmdRule.apiSteps().campaignSteps().campaignsUnarchive(CLIENT, CAMPAIGN_ID);

        File campFile = ResourceUtils.getResourceAsFile(DEFAULT_EXCEL_FILE);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                campFile, CLIENT, CAMPAIGN_ID.toString(), ImportCampXlsRequest.DestinationCamp.OLD);
        IMAGE_UPLOAD_CONDITION.until(allImageUploadTasksProcessed(cmdRule, clientId));
        assumeThat("в группе два баннера", cmdRule.cmdSteps().groupsSteps()
                .getGroup(CLIENT, CAMPAIGN_ID, GROUP_ID).getBanners(), hasSize(2));
    }

    @Test
    @Description("Удаление ГО баннера из группы через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9722")
    public void deleteImageBannerFromGroup() {
        File campFile = ResourceUtils.getResourceAsFile(NEW_EXCEL_FILE);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                campFile, CLIENT, CAMPAIGN_ID.toString(), ImportCampXlsRequest.DestinationCamp.OLD);

        List<Banner> banners = cmdRule.cmdSteps().groupsSteps().getBanners(
                CLIENT,
                Long.valueOf(CAMPAIGN_ID),
                GROUP_ID);

        assertThat("баннер успешно удален из группы", banners.size(), equalTo(1));
    }
}
