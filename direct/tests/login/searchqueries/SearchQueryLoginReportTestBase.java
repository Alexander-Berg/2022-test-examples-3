package ru.yandex.autotests.direct.tests.login.searchqueries;

import java.io.File;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatGroupEnum;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatTypeEnum;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.tests.ReportBackToBackBase;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.tests.XlsTestData.CLIENT_WITH_SEARCH_QUERIES_STAT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public abstract class SearchQueryLoginReportTestBase extends ReportBackToBackBase {
    private static final String ALL_COLUMNS = "shows,clicks,ctr,sum,av_sum,fp_shows_avg_pos,fp_clicks_avg_pos"
            + ",bounce_ratio,adepth,aconv,agoalcost,agoalnum,agoalroi,agoalincome";
    private static final String ALL_SLICES = "sim_distance,search_query,campaign_type,campaign,adgroup,banner,"
            + "contextcond_orig,contexttype_orig,match_type,matched_phrase,page_group";

    private final String START_DATE = "2021-08-10";
    private final String END_DATE = "2021-08-20";
    private final int MINIMAL_FIELDS_AMOUNT = 3;

    @Override
    protected File getReport(DirectCmdSteps steps) {
        ShowStatRequest request = getRequest()
                .withStatType(StatTypeEnum.SEARCH_QUERIES)
                .withDateFrom(START_DATE)
                .withDateTo(END_DATE)
                .withShowStat("1")
                .withColumns(ALL_COLUMNS)
                .withGroupBy(ALL_SLICES)
                .withColumnsPosition(ALL_COLUMNS)
                .withGroupByPositions(ALL_SLICES)
                .withUlogin(CLIENT_WITH_SEARCH_QUERIES_STAT.getLogin());
        return steps.excelStatisticSteps().exportShowStatStatisticReport(request);
    }

    protected ShowStatRequest getGroupByDayRequest() {
        return new ShowStatRequest().withGroupByDate(StatGroupEnum.DAY.toString());
    }

    protected ShowStatRequest getGroupByWeekRequest() {
        return new ShowStatRequest().withGroupByDate(StatGroupEnum.WEEK.toString());
    }

    protected ShowStatRequest getGroupByMonthRequest() {
        return new ShowStatRequest().withGroupByDate(StatGroupEnum.MONTH.toString());
    }

    protected ShowStatRequest getRequestWithNds() {
        return new ShowStatRequest().withWithNds("1");
    }

    protected abstract ShowStatRequest getRequest();

    public void test() {
        assumeThat("отчет первой беты соответствует условию", beta1Result, hasSize(greaterThan(MINIMAL_FIELDS_AMOUNT)));
        assumeThat("отчет второй беты соответствует условию", beta2Result, hasSize(greaterThan(MINIMAL_FIELDS_AMOUNT)));
        super.test();
    }
}
