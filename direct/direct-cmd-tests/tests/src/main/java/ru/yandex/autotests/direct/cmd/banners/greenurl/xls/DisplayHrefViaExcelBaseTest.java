package ru.yandex.autotests.direct.cmd.banners.greenurl.xls;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;

public class DisplayHrefViaExcelBaseTest {

    protected static final String CLIENT = "at-backend-display-href";
    protected static final String DISPLAY_HREF = "somelink";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule = new TextBannersRule().
            overrideBannerTemplate(new Banner().withDisplayHref(getDisplayHrefToSetViaCmd())).
            withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    protected Long campaignId;
    protected Long groupId;
    protected Long bannerId;

    protected File excelFileSource;
    protected File excelFileDest;

    @Before
    public void before() {
        createExcelFile();

        campaignId = bannersRule.getCampaignId();
        groupId = bannersRule.getGroupId();
        bannerId = bannersRule.getBannerId();
    }

    @After
    public void after() {
        if (excelFileSource != null) {
            excelFileSource.delete();
        }
        if (excelFileDest != null) {
            excelFileDest.delete();
        }
    }

    private void createExcelFile() {
        excelFileSource = cmdRule.cmdSteps().excelSteps().exportCampaign(new ExportCampXlsRequest().
                withCid(bannersRule.getCampaignId().toString()).
                withSkipArch(true).
                withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLS).
                withUlogin(CLIENT));
        excelFileDest = new File(excelFileSource.getAbsolutePath() + "-new.xls");
        ExcelUtils.setCellValue(excelFileSource, excelFileDest, ExcelColumnsEnum.DISPLAY_HREF, 0, getDisplayHrefToSetViaExcel());
    }

    protected String getDisplayHrefToSetViaCmd() {
        return null;
    }

    protected String getDisplayHrefToSetViaExcel() {
        return DISPLAY_HREF;
    }

    protected void makeAllModerated() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(campaignId);
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(groupId);
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerId);
    }

    protected void uploadXls(Long campaignId, ImportCampXlsRequest.DestinationCamp destinationCamp) {
        String campaignIdStr = campaignId != null ? campaignId.toString() : "";
        cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(
                excelFileDest, CLIENT, campaignIdStr, destinationCamp);
    }

    protected final String getBannerStatusBsSynced() {
        BannersRecord banner = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        return banner.getStatusbssynced().getLiteral();
    }

    protected String getDisplayHref() {
        return getShowCampMultiEdit().getCampaign().getGroups().get(0).
                getBanners().get(0).getDisplayHref();
    }

    protected String getDisplayHrefStatusModerate() {
        return getShowCampMultiEdit().getCampaign().getGroups().get(0).
                getBanners().get(0).getDisplayHrefStatusModerate();
    }

    protected String getBannerStatusModerate() {
        return getShowCampMultiEdit().getCampaign().getGroups().get(0).
                getBanners().get(0).getStatusModerate();
    }

    protected ShowCampMultiEditResponse getShowCampMultiEdit() {
        return cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(
                CLIENT, campaignId, groupId, bannerId);
    }
}
