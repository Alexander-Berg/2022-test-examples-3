package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.image;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.excel.ConfirmSaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.errors.ImageAdErrors;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.DbqueueJobArchiveStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.DbqueueJobArchiveRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.IMAGE_UPLOAD_CONDITION;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.allImageUploadTasksProcessed;
import static ru.yandex.autotests.direct.db.steps.DBQueueSteps.TYPE_BANNER_IMAGES;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public abstract class ChangeImageBannerXlsValidationTestBase {

    protected static final String CLIENT = "at-direct-excel-image-7";
    private static final Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
    private static final String CREATIVE_IMAGE_PREFIX = "http://canvas.yandex.ru/creatives/000/";
    private static final String ANOTHER_IMAGE_URL = "http://wallpaperscraft.ru/image/fon_tekstura_uzory_temnyy_znak_simvol_radiaciya_29898_240x400.jpg";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected File tempExcel;
    protected File excelToUpload;
    protected File excelToUpload2;
    private BannersRule bannersRule;

    public ChangeImageBannerXlsValidationTestBase(CampaignTypeEnum campaignType) {
        bannersRule = new ImageBannerRule(campaignType)
                .withImageUploader((NewImagesUploadHelper) new NewImagesUploadHelper()
                        .withImageParams(new ImageParams()
                                .withWidth(640)
                                .withHeight(100)
                                .withFormat(ImageUtils.ImageFormat.JPG)))
                .withUlogin(CLIENT);
        bannersRule.getGroup().getBanners().add(BannersFactory.getDefaultBanner(campaignType));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        tempExcel = cmdRule.cmdSteps().excelSteps().exportXlsCampaign(bannersRule.getCampaignId(), CLIENT);
        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xls");
            excelToUpload2 = File.createTempFile(RandomUtils.getString(10), ".xls");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }
    }

    @After
    public void delete() {
        FileUtils.deleteQuietly(tempExcel);
        FileUtils.deleteQuietly(excelToUpload);
        FileUtils.deleteQuietly(excelToUpload2);
    }

    @Description("Изменение картинки ГО баннера на другой размер в группе через excel")
    public void changeImageAnotherSizeViaXls() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).dbqueueSteps()
                .deleteDbqueueJobArchiveRecords(clientId, TYPE_BANNER_IMAGES);
        ExcelUtils.changeHyperLink(tempExcel, excelToUpload, 1, ExcelColumnsEnum.IMAGE, ANOTHER_IMAGE_URL);
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                excelToUpload, CLIENT, bannersRule.getCampaignId().toString(), ImportCampXlsRequest.DestinationCamp.OLD);

        IMAGE_UPLOAD_CONDITION.until(allImageUploadTasksProcessed(cmdRule, clientId));

        List<DbqueueJobArchiveRecord> records = TestEnvironment.newDbSteps().dbqueueSteps()
                .getDbqueueJobArchiveRecords(clientId, TYPE_BANNER_IMAGES);
        assumeThat("в архиве всего одна запись", records, hasSize(1));
        assertThat("в истории есть ошибка", records.get(0).getStatus(), equalTo(DbqueueJobArchiveStatus.Failed));
    }

    @Description("Изменение картинки ГО баннера на креатив через excel")
    public void changeImageToCreativeViaXls() {
        Long creativeId = createCanvasCreative();
        ExcelUtils.changeHyperLink(tempExcel, excelToUpload, 1, ExcelColumnsEnum.IMAGE, CREATIVE_IMAGE_PREFIX + creativeId);
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        ImportCampXlsRequest newRequest = getImportCampXlsRequest(excelToUpload.getName());
        newRequest.withReleaseCampLock(true).withsVarsName(preImportResponse.getsVarsName());
        ImportCampXlsResponse importResponse = cmdRule.cmdSteps().excelSteps().importCampaign(newRequest);

        ConfirmSaveCampRequest confirmRequest = new ConfirmSaveCampRequest()
                .withCid(bannersRule.getCampaignId().toString())
                .withsVarsName(importResponse.getsVarsName())
                .withConfirm(true)
                .withUlogin(CLIENT);
        ErrorResponse response = cmdRule.cmdSteps().excelSteps().confirmSaveCampXlsErrorResponse(confirmRequest);

        assertThat("ошибка", response.getError(), containsString("невозможно изменить тип изображения"));
    }

    @Description("Изменение типа ГО баннера в группе через excel")
    public void changeBannerTypeViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.BANNER_TYPE, 1, "Текстово-графическое");
        ExcelUtils.setCellValue(excelToUpload, excelToUpload2, ExcelColumnsEnum.TEXT, 1, "Для перехода на следующий шаг"); //правка по DIRECT-57553
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload2.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        ImportCampXlsRequest newRequest = getImportCampXlsRequest(excelToUpload2.getName());
        newRequest.withReleaseCampLock(true).withsVarsName(preImportResponse.getsVarsName());

        ImportCampXlsResponse importResponse = cmdRule.cmdSteps().excelSteps().importCampaign(newRequest);

        assertThat("ошибка соответсвует ожиданию", importResponse.getErrors(),
                hasItem(containsString("Строка 13: " + ImageAdErrors.CANNOT_CHANGE_BANNER_TYPE.getErrorText())));
    }

    @Description("Удаление картинки ГО баннера в группе через excel")
    public void deleteImageViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.IMAGE, 1, "");
        PreImportCampXlsResponse preImportResponse = cmdRule.cmdSteps().excelSteps()
                .preImportCampaign(excelToUpload.toString(), PreImportCampXlsRequest.ImportFormat.XLS, CLIENT);

        assertThat("ошибка соответсвует ожиданию", preImportResponse.getErrors(),
                hasItem(containsString("Строка 13: " + ImageAdErrors.NEED_IMAGE.getErrorText())));
    }

    private Long createCanvasCreative() {
        Long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.valueOf(User.get(CLIENT).getClientID()));
        PerfCreativesRecord record = TestEnvironment.newDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        record.setWidth((short) 640);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().updatePerfCreatives(record);
        return creativeId;
    }

    private ImportCampXlsRequest getImportCampXlsRequest(String xlsName) {
        return new ImportCampXlsRequest()
                .withCid(bannersRule.getCampaignId().toString())
                .withDestinationCamp(ImportCampXlsRequest.DestinationCamp.OLD)
                .withGeo("")
                .withSendToModeration(true)
                .withLostBanners("change")
                .withLostPhrases("change")
                .withXls(xlsName)
                .withUlogin(CLIENT);
    }
}
