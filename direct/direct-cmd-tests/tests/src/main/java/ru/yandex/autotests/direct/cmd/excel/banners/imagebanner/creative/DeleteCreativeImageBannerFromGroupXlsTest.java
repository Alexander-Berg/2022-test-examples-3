package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.creative;

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
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Удаление ГО с креативом из группы через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.IMAGE_AD)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class DeleteCreativeImageBannerFromGroupXlsTest {
    private static final String CLIENT = "at-direct-excel-cr-image";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    private CreativeBannerRule bannersRule;
    private File tempExcel;
    private File excelToUpload;

    public DeleteCreativeImageBannerFromGroupXlsTest(CampaignTypeEnum campaignType) {
        bannersRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        bannersRule.getGroup().getBanners().add(BannersFactory.getDefaultBanner(campaignType));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Удаление ГО баннера с креативом через excel. Тип кампании: {0}")
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
        String imageBannerId = bannersRule.getCurrentGroup().getBanners().stream()
                .filter(t -> t.getAdType().equals(BannerType.IMAGE_AD.toString()))
                .findFirst().get().getBid().toString();
        ExcelUtils.removeRow(tempExcel, excelToUpload, ExcelColumnsEnum.BANNER_ID, imageBannerId);
    }

    @After
    public void delete() {
        FileUtils.deleteQuietly(tempExcel);
        FileUtils.deleteQuietly(excelToUpload);
    }

    @Test
    @Description("Удаление ГО баннера с креативом из группы через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9698")
    public void deleteCreativeImageBannerFromGroup() {
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                excelToUpload, CLIENT, bannersRule.getCampaignId().toString(), ImportCampXlsRequest.DestinationCamp.OLD);

        List<Banner> banners = cmdRule.cmdSteps().groupsSteps().getBanners(
                CLIENT,
                bannersRule.getCampaignId(),
                bannersRule.getGroupId());

        assertThat("баннер успешно удален из группы", banners.size(), equalTo(1));
        assertThat("был удален ГО баннер с креативом", banners.get(0).getAdType(),
                not(equalTo(BannerType.IMAGE_AD.toString())));
    }
}
