package ru.yandex.autotests.reporting.api.tests;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.stat.util.ParametersUtils;
import ru.yandex.autotests.reporting.api.beans.BuildReportJob;
import ru.yandex.autotests.reporting.api.steps.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by kateleb on 26.12.16
 */
@RunWith(Parameterized.class)
@Feature("Reporting api")
@Aqua.Test(title = "Build cpa report")
public class ReportingApiBuildCpaReportTest {

    private static final String CPA_BIG_SHOP_MANY_PARAMS = "Cpa big shop many params";
    private static final String CPA_BIG_SHOP_3_MONTHS = "Cpa big shop 3 months";
    private static final String ALL_SLIDES_BIG_SHOP = "All slides big shop";
    private static final String CPA_BIG_SHOP = "Cpa big shop";
    private static ReportingApiHandleSteps steps = new ReportingApiHandleSteps();
    private static ReportingApiFileSteps files = new ReportingApiFileSteps();
    private static ReportingApiMetaSteps meta = new ReportingApiMetaSteps();
    private static ReportingApiParamsProvider pp = new ReportingApiParamsProvider(steps);
    private String name;

    public ReportingApiBuildCpaReportTest(String name) {
        this.name = name;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<String> testCases = new ArrayList<>();
        testCases.add(CPA_BIG_SHOP);
        testCases.add(ALL_SLIDES_BIG_SHOP);
        testCases.add(CPA_BIG_SHOP_3_MONTHS);
        testCases.add(CPA_BIG_SHOP_MANY_PARAMS);
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
            case CPA_BIG_SHOP:
                return ReportingApiParams.forBigShop(pp).withCpaSlide();
            case ALL_SLIDES_BIG_SHOP:
                return ReportingApiParams.forBigShop(pp).withAllSlides();
            case CPA_BIG_SHOP_3_MONTHS:
                return ReportingApiParams.forBigShop(pp).withCpaSlide().forMonths(3);
            case CPA_BIG_SHOP_MANY_PARAMS:
                return ReportingApiParams.forBigShopWithManyParams(pp).withCpaSlide();
            default:
                throw new IllegalArgumentException("unknown test case");
        }
    }
}
