package ru.yandex.autotests.direct.cmd.images.mobile.xls;

import org.hamcrest.Matcher;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Удаление картинки в мобильном баннере через excel")
@Stories(TestFeatures.BannerImages.CREATE_BANNER_IMAGE_VIA_EXCEL)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.PRE_IMPORT_CAMP_XLS)
@Tag(CmdTag.IMPORT_CAMP_XLS)
@Tag(CmdTag.CONFIRM_SAVE_CAMP_XLS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MOBILE)
public class DeleteImageAtMobileBannerViaExcelTest extends UploadMobileBannerWithImageViaExcelBase {

    @Test
    @Description("Удаление картинки в мобильном баннере через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9879")
    public void deleteImageFromMobileCampViaExcel() {
        super.test();
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

        ExcelUtils.modifyLabel(excelFileTemplate, excelFile, ExcelColumnsEnum.IMAGE, 0, "");
    }

    @Override
    protected String getCampaignIdToUploadXls() {
        return mobileBannersRule.getCampaignId().toString();
    }

    @Override
    protected ImportCampXlsRequest.DestinationCamp getXlsCampaignUploadDestination() {
        return ImportCampXlsRequest.DestinationCamp.OLD;
    }

    @Override
    protected Matcher<Banner> getBannerMatcher() {
        CompareStrategy compareStrategy = onlyExpectedFields().
                forFields(newPath("image")).useMatcher(nullValue()).
                forFields(newPath("imageType")).useMatcher(nullValue()).
                forFields(newPath("imageName")).useMatcher(nullValue()).
                forFields(newPath("imageStatusModerate")).useMatcher(nullValue());
        return beanDiffer(new Banner()).useCompareStrategy(compareStrategy);
    }
}
