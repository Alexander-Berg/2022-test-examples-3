package ru.yandex.autotests.reporting.api.tests;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.stat.util.ParametersUtils;
import ru.yandex.autotests.reporting.api.beans.BuildReportJob;
import ru.yandex.autotests.reporting.api.steps.ReportingApiFileSteps;
import ru.yandex.autotests.reporting.api.steps.ReportingApiHandleSteps;
import ru.yandex.autotests.reporting.api.steps.ReportingApiMetaSteps;
import ru.yandex.autotests.reporting.api.steps.ReportingApiParams;
import ru.yandex.autotests.reporting.api.steps.ReportingApiParamsProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Bogdan Timofeev <timofeevb@yandex-team.ru>
 */
@RunWith(Parameterized.class)
@Feature("Reporting api")
@Aqua.Test(title = "Build forecaster")
public class ReportingApiBuildForecasterTest {

    private static final String FORECASTER_BIG_SHOP = "Forecaster report for big shop";
    private static ReportingApiHandleSteps steps = new ReportingApiHandleSteps();
    private static ReportingApiFileSteps files = new ReportingApiFileSteps();
    private static ReportingApiMetaSteps meta = new ReportingApiMetaSteps();
    private static ReportingApiParamsProvider pp = new ReportingApiParamsProvider(steps);

    private String name;

    public ReportingApiBuildForecasterTest(String name) {
        this.name = name;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<String> testCases = new ArrayList<>();
        testCases.add(FORECASTER_BIG_SHOP);
        return ParametersUtils.asParameters(testCases);
    }

    @Test
    public void buildReport() {
        ReportingApiParams testType = getDataForName(name);
        JsonObject response = steps.buildReportForSlides(testType);
        BuildReportJob targetJob = meta.getBuildReportJob(response);
        files.checkFiles(testType, targetJob.getFiles());
    }

    private ReportingApiParams getDataForName(String name) {
        switch (name) {
            case FORECASTER_BIG_SHOP:
                return ReportingApiParams.forBiggestShopAndRegion(pp).withForecaster();
            default:
                throw new IllegalArgumentException("unknown test case");
        }
    }
}
