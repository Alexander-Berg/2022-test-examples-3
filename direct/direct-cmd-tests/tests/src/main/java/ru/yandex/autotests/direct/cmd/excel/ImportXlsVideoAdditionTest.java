package ru.yandex.autotests.direct.cmd.excel;

import java.io.File;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Сохранение видеодополнения при выгрузке и загрузке обратно эксель-файла")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(CampTypeTag.TEXT)
public class ImportXlsVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    private TextBannersRule bannersRule = new TextBannersRule()
            .withVideoAddition(videoAdditionCreativeRule)
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(videoAdditionCreativeRule, bannersRule);
    private File tempExcel;

    @Before
    public void before() {
        tempExcel = cmdRule.cmdSteps().excelSteps().exportXlsCampaign(bannersRule.getCampaignId(), CLIENT);

        ShowCampResponse response =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, bannersRule.getCampaignId().toString());
        Banner banner = response.getGroups().get(0);

        assumeThat("video_resources перед импортом присутствует в ответе showCamp",
                banner.getVideoResources(), notNullValue());
    }

    @After
    public void after() {
        if (tempExcel != null) {
            tempExcel.delete();
        }
    }

    @Test
    @Description("При выгрузке и загрузке обратно excel файла с кампанией с видеодополнением, видеодополнение не исчезает и не меняется")
    @TestCaseId("10951")
    public void testNoChangeVideoAdditionImportXls() {
        String campaignId = bannersRule.getCampaignId().toString();

        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                tempExcel, CLIENT, campaignId, ImportCampXlsRequest.DestinationCamp.OLD);

        ShowCampResponse response =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId);
        Banner banner = response.getGroups().get(0);

        assumeThat("video_resources после импорта присутствует в ответе showCamp",
                banner.getVideoResources(), notNullValue());

        assertThat("видеодополнение соответствует ожидаемому", banner.getVideoResources().getId(),
                equalTo(videoAdditionCreativeRule.getCreativeId()));
    }
}
