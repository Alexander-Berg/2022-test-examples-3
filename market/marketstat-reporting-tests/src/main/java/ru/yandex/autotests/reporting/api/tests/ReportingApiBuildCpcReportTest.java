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
 * Created by kateleb on 16.11.16.
 */
@RunWith(Parameterized.class)
@Feature("Reporting api")
@Aqua.Test(title = "Build cpc report")
public class ReportingApiBuildCpcReportTest {

    private static final String CPC_FOR_BIG_SHOP_1_SLIDE = "Cpc for big shop 1 slide";
    private static final String CPC_FOR_BIG_SHOP_2_SLIDE = "Cpc for big shop 2 slide";
    private static final String CPC_FOR_BIG_SHOP_3_MONTHS = "Cpc for big shop 3 months";
    private static final String CPC_FOR_BIG_SHOP_MANY_PARAMS = "Cpc for big shop many params";
    private static ReportingApiHandleSteps steps = new ReportingApiHandleSteps();
    private static ReportingApiFileSteps files = new ReportingApiFileSteps();
    private static ReportingApiMetaSteps meta = new ReportingApiMetaSteps();
    private static ReportingApiParamsProvider pp = new ReportingApiParamsProvider(steps);
    private String name;

    public ReportingApiBuildCpcReportTest(String name) {
        this.name = name;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<String> testCases = new ArrayList<>();
        testCases.add(CPC_FOR_BIG_SHOP_1_SLIDE);
        testCases.add(CPC_FOR_BIG_SHOP_2_SLIDE);
        testCases.add(CPC_FOR_BIG_SHOP_3_MONTHS);
        testCases.add(CPC_FOR_BIG_SHOP_MANY_PARAMS);
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
            case CPC_FOR_BIG_SHOP_1_SLIDE:
                return ReportingApiParams.forBigShop(pp).withCpcSlide().cpcSlide1only();
            case CPC_FOR_BIG_SHOP_2_SLIDE:
                return ReportingApiParams.forBigShop(pp).withCpcSlide().cpcSlide2only();
            case CPC_FOR_BIG_SHOP_3_MONTHS:
                return ReportingApiParams.forBigShop(pp).withCpcSlide().forMonths(3);
            case CPC_FOR_BIG_SHOP_MANY_PARAMS:
                return ReportingApiParams.forBigShopWithManyParams(pp).withCpcSlide();
            default:
                throw new IllegalArgumentException("unknown test case");
        }
    }
}
