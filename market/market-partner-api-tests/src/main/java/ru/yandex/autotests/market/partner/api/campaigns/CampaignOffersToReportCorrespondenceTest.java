package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.steps.CompareWithReportSteps;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;

/**
 * Created with IntelliJ IDEA.
 * User: belmatter
 * Date: 18.07.13
 * Time: 20:05
 * To change this template use File | Settings | File Templates.
 */
@Feature("Campaign offers resource")
@Aqua.Test(title = "Тесты сравнения выдачи офферов с репортом ")
@Issues({
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-3976")
})
public class CampaignOffersToReportCorrespondenceTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private ProjectConfig config = new ProjectConfig();

    private static CompareWithReportSteps user = new CompareWithReportSteps();

    @Test
    public void testOffersCount() {
        user.parseOffersFromReport(String.valueOf(config.getOffersShopId()), config.getOffersFeedId(), config.getOffersShopCategoryId());
        user.compareOffersCountWithReport();
    }

    @Test
    public void compareOffersWithReport() {
        user.parseOffersFromReport(String.valueOf(config.getOffersShopId()), config.getOffersFeedId(), config.getOffersShopCategoryId());
        user.compareOffersWithReport();
    }

    @Test
    public void testPagerPresence() {
        user.shouldSeePager();
    }

    @Test
    public void testSearchSummaryNotPresence() {
        user.shouldNotSeeSearchSummary();
    }

}