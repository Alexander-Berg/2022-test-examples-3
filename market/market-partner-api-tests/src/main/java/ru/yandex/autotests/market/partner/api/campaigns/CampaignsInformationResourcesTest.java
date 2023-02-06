package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONCompareMode;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.common.comparator.IgnoreAttrFilter;
import ru.yandex.autotests.market.common.comparator.IgnoreFieldsComparator;
import ru.yandex.autotests.market.common.comparator.IgnoreFieldsFilter;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.PartnerApiCompareSteps;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignCategoriesRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignFeedInfoRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignFeedsRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignInfoRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignLoginsRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignLoginsRequestForV1;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignRegionRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignSettingsRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.campaignUploadFeedInfoRequest;
import static ru.yandex.autotests.market.partner.api.data.b2b.CampaignInfoRequestData.feedCategoriesRequest;

/**
 * User: jkt
 * Date: 01.11.12
 * Time: 11:50
 */
@Feature("Campaign resources")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@Stories("Campaigns info")
@RunWith(Parameterized.class)
public class CampaignsInformationResourcesTest {

    private static ProjectConfig config = new ProjectConfig();

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private PartnerApiCompareSteps tester = new PartnerApiCompareSteps();

    private PartnerApiRequestData request;

    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                campaignInfoRequest()
                , campaignCategoriesRequest()
                , feedCategoriesRequest()
                , campaignFeedInfoRequest()
                , campaignUploadFeedInfoRequest()
                , campaignFeedsRequest()
                , campaignLoginsRequest()
                , campaignLoginsRequestForV1()
                , campaignRegionRequest()
                , campaignSettingsRequest()
        );
    }

    public CampaignsInformationResourcesTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }

    @Before
    public void getResponse() {
        if (new ProjectConfig().isOverwriteResponse()) {
            // save result to elliptics
            tester.saveExpectedToStorage(request);
            tester.saveExpectedToStorage(request.withFormat(Format.JSON));
            tester.saveExpectedToStorage(request.withVersion(ApiVersion.V1));
            tester.saveExpectedToStorage(request.withFormat(Format.JSON).withVersion(ApiVersion.V1));
        }
    }

    @Test
    public void compareWithStoredResult() {
        tester.compareWithStoredResult(request,
                new IgnoreFieldsFilter("total-holidays", "period"),
                new IgnoreAttrFilter("published-time"));
    }

    @Test
    public void compareWithJsonStoredResult() {
        tester.compareWithJsonStoredResult(request.withFormat(Format.JSON),
                new IgnoreFieldsComparator(JSONCompareMode.NON_EXTENSIBLE,
                        "settings.localRegion.delivery.schedule.totalHolidays",
                        "settings.localRegion.delivery.schedule.period",
                        "feed.publication.full.publishedTime",
                        "feed.publication.priceAndStockUpdate.publishedTime",
                        "feeds[id=" + config.getFeedId() + "].publication.full.publishedTime",
                        "feeds[id=" + config.getFeedId() + "].publication.priceAndStockUpdate.publishedTime"));
    }

    @Test
    public void compareWithV1StoredResult() {
        tester.compareWithStoredResult(request.withVersion(ApiVersion.V1));
    }

    @Test
    public void compareWithV1JsonStoredResult() {
        tester.compareWithJsonStoredResult(request.withFormat(Format.JSON).withVersion(ApiVersion.V1));
    }
}
