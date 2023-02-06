package ru.yandex.autotests.direct.cmd.images.mobile.xls;

import java.io.File;

import org.hamcrest.Matcher;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Добавление картинки в мобильный баннер через excel")
@Stories(TestFeatures.BannerImages.CREATE_BANNER_IMAGE_VIA_EXCEL)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MOBILE)
public class AddImageToMobileBannerViaExcelTest extends UploadMobileBannerWithImageViaExcelBase {

    public MobileBannersRule mobileBannersRuleForImage;

    @Test
    @Description("Добавление картинки в мобильный баннер через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9875")
    public void addImageToMobileCampViaExcel() {
        super.test();
    }


    /**
     * Картинку загружаем, но в созданный баннер не добавляем
     */
    @Override
    protected DirectCmdRule createDirectCmdRule() {
        final String client = getClientToUploadImage();
        imageUploader = (ImageUploadHelper) new ImageUploadHelper().
                withImageParams(new ImageParams().
                        withFormat(ImageUtils.ImageFormat.JPG).
                        withWidth(1080).
                        withHeight(607).
                        withResizeX1(0).
                        withResizeX2(1080).
                        withResizeY1(0).
                        withResizeY2(607)).
                withUploadType(ImageUploadHelper.UploadType.FILE).
                withClient(client);
        mobileBannersRuleForImage = new MobileBannersRule().
                withImageUploader(imageUploader).
                withUlogin(client);
        mobileBannersRule = new MobileBannersRule().withUlogin(client);
        return DirectCmdRule.defaultRule().withRules(mobileBannersRuleForImage, mobileBannersRule);
    }

    /**
     * Используем не шаблон, а скачанную кампанию в xls, чтобы там были выставлены все id
     */
    @Override
    protected void createExcelFile() {
        File excelFileTemplate = cmdRule.cmdSteps().excelSteps().exportCampaign(new ExportCampXlsRequest().
                withCid(mobileBannersRule.getCampaignId().toString()).
                withSkipArch(true).
                withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLS).
                withUlogin(CLIENT));

        try {
            excelFile = File.createTempFile("rmp-image", ".xls");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }

        ExcelUtils.modifyLabel(excelFileTemplate, excelFile, ExcelColumnsEnum.IMAGE, 0, getImageUrlToSendInXls());
    }

    @Override
    protected Matcher<Banner> getBannerMatcher() {
        String sourceImageName = imageUploader.getResizeResponse().getName();
        return beanDiffer(new Banner().
                withImage(getUploadedImageToSendInXls()).
                withImageType(ImageType.WIDE.getName()).
                withImageName(sourceImageName).
                withImageStatusModerate("New")).useCompareStrategy(onlyExpectedFields());
    }

    @Override
    protected String getCampaignIdToUploadXls() {
        return mobileBannersRule.getCampaignId().toString();
    }

    /**
     * Обновляем существующую кампанию
     */
    @Override
    protected ImportCampXlsRequest.DestinationCamp getXlsCampaignUploadDestination() {
        return ImportCampXlsRequest.DestinationCamp.OLD;
    }
}
