package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import java.io.File;
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
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Выгрузка/загрузка кампании с ГО через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ExportImportCampWithImageAdXlsTest {

    private static final String CLIENT = "at-direct-excel-image-1";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private File exportedCamp;
    private Long campaignId;
    private BannersRule bannerRule;

    public ExportImportCampWithImageAdXlsTest(CampaignTypeEnum campaignType) {
        bannerRule = new ImageBannerRule(campaignType).withImageUploader(
                (NewImagesUploadHelper) new NewImagesUploadHelper().withImageParams(new ImageParams()
                        .withFormat(ImageUtils.ImageFormat.JPG)
                        .withWidth(970)
                        .withHeight(250))).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
    }

    @Parameterized.Parameters(name = "Выгрузка/загрузка кампании с ГО через excel. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = bannerRule.getCampaignId();
    }

    @After
    public void after() {
        FileUtils.deleteQuietly(exportedCamp);
    }

    @Test
    @Description("Выгрузка/загрузка кампании с ГО через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9723")
    public void exportImportNewCampWithImageAdXlsTest() {
        ExportCampXlsRequest request = new ExportCampXlsRequest()
                .withCid(campaignId.toString())
                .withSkipArch(true)
                .withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLSX)
                .withUlogin(CLIENT);
        exportedCamp = cmdRule.cmdSteps().excelSteps().exportCampaignIgnoringLock(request);

        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                exportedCamp, CLIENT, campaignId.toString(), ImportCampXlsRequest.DestinationCamp.OLD)
                .getLocationParamAsLong(LocationParam.CID);

        List<Banner> banners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, campaignId);

        assertThat("ГО баннер не изменился", banners.get(0),
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    private Banner getExpectedBanner() {
        return new Banner()
                .withAdType(BannerType.IMAGE_AD.toString())
                .withImageAd(bannerRule.getBanner().getImageAd());
    }
}
