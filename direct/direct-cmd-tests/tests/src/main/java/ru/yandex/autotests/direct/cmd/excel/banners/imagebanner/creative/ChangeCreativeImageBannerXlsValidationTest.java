package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.creative;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

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
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.excel.ConfirmSaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Ошибки при изменении параметров ГО баннера через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ChangeCreativeImageBannerXlsValidationTest {

    protected static final String CLIENT = "at-direct-excel-cr-image";
    private static final String CREATIVE_IMAGE_PREFIX = "http://canvas.yandex.ru/creatives/000/";
    private static final String IMAGE_URL = "http://wallpaperscraft.ru/image/fon_tekstura_uzory_temnyy_znak_simvol_radiaciya_29898_240x400.jpg";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected File tempExcel;
    protected File excelToUpload;
    private BannersRule bannersRule;

    public ChangeCreativeImageBannerXlsValidationTest(CampaignTypeEnum campaignType) {
        bannersRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        bannersRule.getGroup().getBanners().add(BannersFactory.getDefaultBanner(campaignType));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
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

    @Test
    @Description("Изменение креатива ГО баннера на другой размер в группе через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9696")
    public void changeCreativeAnotherSizeViaXls() {
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

        assertThat("ошибка", response.getError(), containsString("Размеры нового и предыдущего изображений должны быть одинаковыми"));
    }

    @Test
    @Description("Изменение креатива ГО баннера на креатив через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9697")
    public void changeCreativeToImageViaXls() {
        ExcelUtils.changeHyperLink(tempExcel, excelToUpload, 1, ExcelColumnsEnum.IMAGE, IMAGE_URL);
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

    private Long createCanvasCreative() {
        Long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.valueOf(User.get(CLIENT).getClientID()));
        PerfCreativesRecord record = TestEnvironment.newDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
        record.setHeight((short) 300);
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
