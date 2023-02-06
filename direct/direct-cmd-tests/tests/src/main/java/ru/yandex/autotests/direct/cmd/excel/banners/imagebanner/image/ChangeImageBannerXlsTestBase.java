package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.IMAGE_UPLOAD_CONDITION;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.allImageUploadTasksProcessed;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public abstract class ChangeImageBannerXlsTestBase {

    protected static final String CLIENT = "at-direct-excel-image-3";
    private static final Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
    private static final String NEW_IMAGE_URL = "https://images.wallpaperscraft.ru/image/single/vozvrashchenie_fraza_slova_220093_240x400.jpg";


    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannersRule;
    protected File tempExcel;
    protected File excelToUpload;

    public ChangeImageBannerXlsTestBase(CampaignTypeEnum campaignType) {
        bannersRule = new ImageBannerRule(campaignType)
                .withImageUploader((NewImagesUploadHelper) new NewImagesUploadHelper()
                        .withImageParams(new ImageParams().withFormat(ImageUtils.ImageFormat.JPG).withWidth(240).withHeight(400)))
                .withUlogin(CLIENT);
        bannersRule.getGroup().getBanners().add(BannersFactory.getDefaultBanner(campaignType));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        tempExcel = cmdRule.cmdSteps().excelSteps().exportXlsCampaign(bannersRule.getCampaignId(), CLIENT);
        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xls");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }
    }

    @After
    public void delete() {
        FileUtils.deleteQuietly(tempExcel);
        FileUtils.deleteQuietly(excelToUpload);
    }

    @Description("Изменение картинки ГО баннера в группе через excel")
    public void changeImageViaXls() {
        List<Banner> defBanners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, bannersRule.getCampaignId());

        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.IMAGE, 1, NEW_IMAGE_URL);
        uploadExcelAndRunScript();
        List<Banner> actBanners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, bannersRule.getCampaignId());

        assumeThat("графическим является второй баннер", actBanners.get(1).getAdType(), equalTo(BannerType.IMAGE_AD.toString()));
        assertThat("картинка успешно заменилась", actBanners.get(1).getImageAd().getHash(),
                not(equalTo(defBanners.get(1).getImageAd().getHash())));
    }

    protected void uploadExcelAndRunScript() {
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(excelToUpload, CLIENT, bannersRule.getCampaignId().toString(),
                ImportCampXlsRequest.DestinationCamp.OLD);
        IMAGE_UPLOAD_CONDITION.until(allImageUploadTasksProcessed(cmdRule, clientId));
        assumeThat("количество баннеров не изменилось", cmdRule.cmdSteps().groupsSteps()
                .getBanners(CLIENT, bannersRule.getCampaignId()), hasSize(2));
    }
}
