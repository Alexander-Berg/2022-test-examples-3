package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.creative;

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
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public abstract class ChangeCreativeImageBannerXlsTestBase {

    protected static final String CLIENT = "at-direct-excel-cr-image";
    private static final String CREATIVE_IMAGE_PREFIX = "http://canvas.yandex.ru/creatives/000/";


    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected CreativeBannerRule bannersRule;
    protected File tempExcel;
    protected File excelToUpload;
    private Long newCreativeId;

    public ChangeCreativeImageBannerXlsTestBase(CampaignTypeEnum campaignType) {
        bannersRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        bannersRule.getGroup().getBanners().add(BannersFactory.getDefaultBanner(campaignType));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        newCreativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.valueOf(User.get(CLIENT).getClientID()));
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
        if (newCreativeId != null) {
            PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, newCreativeId);
        }
    }

    @Description("Изменение креатива ГО баннера в группе через excel")
    public void changeCreativeViaXls() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.IMAGE, 1, CREATIVE_IMAGE_PREFIX + newCreativeId);
        uploadExcel();
        List<Banner> actBanners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, bannersRule.getCampaignId());

        assumeThat("графическим является второй баннер", actBanners.get(1).getAdType(), equalTo(BannerType.IMAGE_AD.toString()));
        assertThat("креатив успешно заменился", actBanners.get(1).getCreativeBanner().getCreativeId(),
                equalTo(newCreativeId));
    }

    protected void uploadExcel() {
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(excelToUpload, CLIENT,
                bannersRule.getCampaignId().toString(), ImportCampXlsRequest.DestinationCamp.OLD);
        assumeThat("количество баннеров не изменилось", cmdRule.cmdSteps().groupsSteps()
                .getBanners(CLIENT, bannersRule.getCampaignId()), hasSize(2));
    }
}
