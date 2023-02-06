package ru.yandex.autotests.direct.tests.login.mol;

import java.io.File;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatGroupEnum;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatTypeEnum;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.tests.ReportBackToBackBase;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.tests.XlsTestData.CLIENT_WITH_SEARCH_QUERIES_STAT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public abstract class MolReportTestBase extends ReportBackToBackBase {
    private static final String COMMON_COLUMNS =
            "agoalcost,agoalincome,bounce_ratio,sum,av_sum,aconv,adepth,agoalnum,clicks,"
                    + "aprgoodmultigoal,fp_clicks_avg_pos,agoalroi";

    private static final String ADDITIONAL_COLUMNS = "ctr,shows,fp_shows_avg_pos";

    private static final String COMMON_SLICES = "targettype,connection_type,gender,image_size,detailed_device_type,"
            + "campaign_type,banner,device_type,tags,physical_region,"
            + "ssp,banner_type,campaign,position,region,banner_image_type,adgroup,age";

    private static final String SLICES_1 = "contextcond_orig,contexttype_orig,retargeting_coef,"
            + "click_place,match_type";

    private static final String SLICES_2 = "page_group";

    private static final String FIRST_COLUMNS_SET = COMMON_COLUMNS;
    private static final String SECOND_COLUMNS_SET = COMMON_COLUMNS + "," + ADDITIONAL_COLUMNS;
    private static final String FIRST_SLICES_SET = COMMON_SLICES + "," + SLICES_1;
    private static final String SECOND_SLICES_SET = COMMON_SLICES + "," + SLICES_2;

    private final String START_DATE = "2021-03-02";
    private final String END_DATE = "2021-03-29";
    private final int MINIMAL_FIELDS_AMOUNT = 3;

    private static ShowStatRequest request() {
        return new ShowStatRequest().withColumns(FIRST_COLUMNS_SET)
                .withGroupBy(FIRST_SLICES_SET)
                .withColumnsPosition(FIRST_COLUMNS_SET)
                .withGroupByPositions(FIRST_SLICES_SET);
    }

    @Override
    protected File getReport(DirectCmdSteps steps) {
        ShowStatRequest request = getRequest()
                .withStatType(StatTypeEnum.MOL)
                .withDateFrom(START_DATE)
                .withDateTo(END_DATE)
                .withShowStat("1")
                .withUlogin(CLIENT_WITH_SEARCH_QUERIES_STAT.getLogin());
        return steps.excelStatisticSteps().exportShowStatStatisticReport(request);
    }

    protected abstract ShowStatRequest getRequest();

    protected ShowStatRequest getGroupByDayRequest() {
        return request().withGroupByDate(StatGroupEnum.DAY.toString());
    }

    protected ShowStatRequest getGroupByWeekRequest() {
        return request().withGroupByDate(StatGroupEnum.WEEK.toString());
    }

    protected ShowStatRequest getGroupByMonthRequest() {
        return request().withGroupByDate(StatGroupEnum.MONTH.toString());
    }

    protected ShowStatRequest getRequestWithNds() {
        return request().withWithNds(PerlBoolean.ONE.toString());
    }

    protected ShowStatRequest getComplexRequest() {
        return request()
                .withGroupBy(SECOND_SLICES_SET)
                .withGroupByPositions(SECOND_SLICES_SET)
                .withColumns(SECOND_COLUMNS_SET)
                .withColumnsPosition(SECOND_COLUMNS_SET);
    }

    public void test() {
        assumeThat("отчет первой беты соответствует условию", beta1Result, hasSize(greaterThan(MINIMAL_FIELDS_AMOUNT)));
        assumeThat("отчет второй беты соответствует условию", beta2Result, hasSize(greaterThan(MINIMAL_FIELDS_AMOUNT)));
        super.test();
    }
}
