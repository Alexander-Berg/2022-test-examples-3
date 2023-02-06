package ru.yandex.autotests.direct.cmd.images.mobile.xls;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentRequest;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentResponse;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.IMAGE_UPLOAD_CONDITION;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.allImageUploadTasksProcessed;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

/**
 * 1. создает мобильную кампанию с картинкой через cmd
 * 2. загружает через xls новую кампанию с той картинкой, которая была загружена на шаге 1
 * 3. достает баннер из загруженной кампании, смотрит что там лежит эта картинка
 * <p>
 * с помощью переопределения методов можно, например:
 * 1. создать мобильную кампанию с картинкой одному пользователю, а через xls загрузить новую кампанию с той же картинкой другому
 * 2. загрузить через xls не новую кампанию, а изменения в существующую
 */
public abstract class UploadMobileBannerWithImageViaExcelBase {

    protected static final String CLIENT = "at-direct-cmd-rmp-xls-71";
    private static final Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
    protected static final String EXCEL_TEMPLATE_NAME = "excel/rmp/rmp-with-image.xls";
    protected static final ImageParams IMAGE_PARAMS_WIDE = new ImageParams().
            withFormat(ImageUtils.ImageFormat.JPG).
            withWidth(1080).
            withHeight(607).
            withResizeX1(0).
            withResizeX2(1080).
            withResizeY1(0).
            withResizeY2(607);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected ImageUploadHelper imageUploader;
    protected MobileBannersRule mobileBannersRule;
    @Rule
    public DirectCmdRule cmdRule = createDirectCmdRule();
    protected File excelFile;

    protected Long campaignId;
    protected Long groupId;
    protected Long bannerId;

    protected Banner banner;

    @Before
    public void before() {

        createExcelFile();
    }

    @After
    public void after() {
        if (campaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, campaignId);
        }
        FileUtils.deleteQuietly(excelFile);
    }

    public void test() {
        uploadXlsCampaign();
        IMAGE_UPLOAD_CONDITION.until(allImageUploadTasksProcessed(cmdRule, clientId));
        getCreatedBanner();
        assertThat("поля картинки в созданном через excel баннере соответствуют ожидаемым",
                banner, getBannerMatcher());
    }

    protected DirectCmdRule createDirectCmdRule() {
        final String client = getClientToUploadImage();
        imageUploader = (ImageUploadHelper) new ImageUploadHelper().
                withImageParams(IMAGE_PARAMS_WIDE).
                withUploadType(ImageUploadHelper.UploadType.FILE).
                withClient(client);
        mobileBannersRule = new MobileBannersRule().
                withImageUploader(imageUploader).
                withUlogin(client);
        return DirectCmdRule.defaultRule().withRules(mobileBannersRule);
    }


    protected void uploadXlsCampaign() {
        final String client = getClientToUploadXlsCampaign();
        final String cid = getCampaignIdToUploadXls();
        final ImportCampXlsRequest.DestinationCamp destination = getXlsCampaignUploadDestination();
        campaignId = cmdRule.cmdSteps().excelSteps().
                safeImportCampaignFromXls(excelFile, client, cid, destination).
                getLocationParamAsLong(LocationParam.CID);

        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(client, campaignId.toString());
        groupId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в загруженной кампании есть группа"))
                .getAdGroupId();
        bannerId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в загруженной кампании есть баннер"))
                .getBid();
    }

    protected void createExcelFile() {
        try {
            excelFile = File.createTempFile("rmp-image", ".xls");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }

        ExcelUtils.modifyLabel(ResourceUtils.getResourceAsFile(EXCEL_TEMPLATE_NAME),
                excelFile, ExcelColumnsEnum.IMAGE, 0, getImageUrlToSendInXls());
    }

    protected void getCreatedBanner() {
        // todo use static method forSingleBanner from another branch
        EditAdGroupsMobileContentRequest request = new EditAdGroupsMobileContentRequest().
                withCid(campaignId).
                withAdGroupIds(groupId.toString()).
                withBid(bannerId.toString()).
                withBannerStatus("all").
                withUlogin(getClientToUploadXlsCampaign());

        EditAdGroupsMobileContentResponse response =
                cmdRule.cmdSteps().groupsSteps().getEditAdGroupsMobileContent(request);
        banner = response.getCampaign().getGroups().get(0).getBanners().get(0);
    }

    protected Matcher<Banner> getBannerMatcher() {
        String sourceImageUrl = getImageUrlToSendInXls();
        String sourceImageName = sourceImageUrl.substring(sourceImageUrl.lastIndexOf("/") + 1);
        return beanDiffer(new Banner().
                withImage(getUploadedImageToSendInXls()).
                withImageType(ImageType.WIDE.getName()).
                withImageName(sourceImageName)).useCompareStrategy(onlyExpectedFields());
    }

    protected String getClientToUploadImage() {
        return CLIENT;
    }

    protected String getClientToUploadXlsCampaign() {
        return CLIENT;
    }

    /**
     * id кампании, в которую будем загружать данные из excel-файла
     * (в случае новой кампании - пустая строка)
     */
    protected String getCampaignIdToUploadXls() {
        return "";
    }

    /**
     * В какую кампанию будем загружать данные из excel-файла,
     * в новую (NEW) или существующую (OLD)
     */
    protected ImportCampXlsRequest.DestinationCamp getXlsCampaignUploadDestination() {
        return ImportCampXlsRequest.DestinationCamp.NEW;
    }

    protected String getUploadedImageToSendInXls() {
        return imageUploader.getResizeResponse().getImage();
    }

    protected String getUploadedImageGroupToSendInXls() {
        return imageUploader.getResizeResponse().getMdsGroupId();
    }

    protected String getImageUrlToSendInXls() {
        return cmdRule.cmdSteps().context().getBaseUrl() + "/images/direct/"+ getUploadedImageGroupToSendInXls() + "/" + getUploadedImageToSendInXls();
    }
}
