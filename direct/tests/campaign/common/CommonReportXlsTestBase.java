package ru.yandex.autotests.direct.tests.campaign.common;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatEnum;
import ru.yandex.autotests.direct.cmd.data.showcampstat.TypesEnum;
import ru.yandex.autotests.direct.steps.utils.Converters;
import ru.yandex.autotests.direct.steps.utils.ShowCampStatRequestBuilder;
import ru.yandex.autotests.direct.tests.ReportBackToBackBase;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.tests.XlsTestData.CLIENT_WITH_TEXT_CAMPAIGN_STAT;
import static ru.yandex.autotests.direct.utils.model.PerlBoolean.ONE;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public abstract class CommonReportXlsTestBase extends ReportBackToBackBase {
    private final String START_DATE = "01-03-2017";
    private final String END_DATE = "01-04-2017";
    private final int MINIMAL_FIELDS_AMOUNT = 3;


    @Override
    protected File getReport(DirectCmdSteps steps) {
        ShowCampStatRequest request = ShowCampStatRequestBuilder.requestBuilder(getRequest())
                .dateFrom(START_DATE)
                .dateTo(END_DATE)
                .get()
                .withXls(ONE)
                .withIsStat(StatEnum.STAT_ON)
                .withTypes(TypesEnum.TOTAL)
                .withCid(CLIENT_WITH_TEXT_CAMPAIGN_STAT.getCid())
                .withUlogin(CLIENT_WITH_TEXT_CAMPAIGN_STAT.getLogin());
        return steps.excelStatisticSteps().exportToXlsShowCampStatStatisticReport(request);
    }

    protected abstract ShowCampStatRequest getRequest();

    @Override
    protected Function<File, List<List<String>>> provideFileParser() {
        return Converters::tableFromFirstExcelSheet;
    }

    public void commonReportXlsTest() {
        assumeThat("отчет первой беты соответствует условию", beta1Result, hasSize(greaterThan(MINIMAL_FIELDS_AMOUNT)));
        assumeThat("отчет второй беты соответствует условию", beta2Result, hasSize(greaterThan(MINIMAL_FIELDS_AMOUNT)));
        super.test();
    }
}
