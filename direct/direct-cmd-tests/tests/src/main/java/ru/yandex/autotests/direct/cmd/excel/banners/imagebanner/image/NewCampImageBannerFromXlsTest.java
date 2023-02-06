package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.IMAGE_UPLOAD_CONDITION;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.allImageUploadTasksProcessed;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание новой кампании с ГО через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class NewCampImageBannerFromXlsTest {

    private static final String CLIENT = "at-direct-excel-image-1";
    private static final Long clientId = Long.valueOf(User.get(CLIENT).getClientID());

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(value = 0)
    public String xlsName;
    @Parameterized.Parameter(value = 1)
    public int bannerCount;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    private Long newCampaignId;

    @Parameterized.Parameters(name = "Создание новой кампании с ГО через excel. Xls файл: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"excel/image/one_text-image_banner.xls", 1},
                {"excel/image/one_mobile-image_banner.xls", 1},
                {"excel/image/two_image_banners.xls", 2},
                {"excel/image/image_and_text_banners_one_group.xls", 2},
                {"excel/image/image_and_empty_type_banner_one_group.xls", 2},
                {"excel/image/image_and_empty_banners_in_various_groups.xls", 4},
        });
    }

    @After
    public void delete() {
        if (newCampaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCampaignId);
        }
    }

    @Test
    @Description("Создание новой кампании с ГО через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9726")
    public void createNewCampWithImageAdFromXlsTest() {
        File campFile = ResourceUtils.getResourceAsFile(xlsName);
        newCampaignId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(campFile, CLIENT, "",
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsLong(LocationParam.CID);

        IMAGE_UPLOAD_CONDITION.until(allImageUploadTasksProcessed(cmdRule, clientId));
        List<Banner> banners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, newCampaignId);

        assertThat("баннеры успешно создались", banners, Matchers.hasSize(bannerCount));
    }
}
