package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.creative;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
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
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание новой кампании с ГО с креативом через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class NewCampCreativeImageBannerFromXlsTest {

    private static final String CREATIVE_IMAGE_PREFIX = "http://canvas.yandex.ru/creatives/000/";
    private static final String CLIENT = "at-direct-excel-cr-image";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(value = 0)
    public String xlsName;
    @Parameterized.Parameter(value = 1)
    public int bannerCount;
    @Parameterized.Parameter(value = 2)
    public List<Integer> creativeRows;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    private Long newCampaignId;
    private File excelToUpload;
    private List<Long> creativeIds;

    @Parameterized.Parameters(name = "Создание новой кампании с ГО через excel. Xls файл: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"excel/image/one_text-image_banner.xls", 1, singletonList(0)},
                {"excel/image/one_mobile-image_banner.xls", 1, singletonList(0)},
                {"excel/image/two_image_banners.xls", 2, Arrays.asList(0, 1)},
                {"excel/image/image_and_text_banners_one_group.xls", 2, singletonList(1)},
                {"excel/image/image_and_empty_type_banner_one_group.xls", 2, singletonList(1)},
                {"excel/image/image_and_empty_banners_in_various_groups.xls", 4, Arrays.asList(0, 1)},
        });
    }

    @Before
    public void before() {
        creativeIds = new ArrayList<>();
        File campFile = ResourceUtils.getResourceAsFile(xlsName);
        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xls");
            FileUtils.copyFile(campFile, excelToUpload);
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }

        fillCreativeUrls();
    }

    @After
    public void delete() {
        FileUtils.deleteQuietly(excelToUpload);
        if (newCampaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCampaignId);
        }
        if (creativeIds != null && !creativeIds.isEmpty()) {
            creativeIds.forEach(id -> PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, id));
        }
    }

    @Test
    @Description("Создание новой кампании с ГО с креативом через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9700")
    public void createNewCampWithCreativeImageAdFromXlsTest() {
        newCampaignId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(excelToUpload, CLIENT, "",
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsLong(LocationParam.CID);

        List<Banner> banners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, newCampaignId);

        assertThat("баннеры успешно создались", banners, Matchers.hasSize(bannerCount));
    }

    private void fillCreativeUrls() {
        for (int rowId : creativeRows) {
            Long creativeId = createCanvasCreative();
            creativeIds.add(creativeId);
            ExcelUtils.setCellValue(excelToUpload, excelToUpload, ExcelColumnsEnum.IMAGE, rowId,
                    CREATIVE_IMAGE_PREFIX + creativeId);
        }
    }

    private Long createCanvasCreative() {
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.valueOf(User.get(CLIENT).getClientID()));
    }
}
