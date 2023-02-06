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
 * Created by kateleb on 14.12.16
 */
@RunWith(Parameterized.class)
@Feature("Reporting api")
@Aqua.Test(title = "Build assortment report")
public class ReportingApiBuildAssortmentReportTest {

    private static final String ASSORTMENT_BIG_SHOP = "Assortment for big shop";
    private static final String ASSORTMENT_FOR_BIG_SHOP_FOR_19_MODELS = "Assortment for big shop for 19 models";
    private static final String ASSORTMENT_BIG_SHOP_3_MONTHS = "Assortment for big shop for 3 months";
    private static final String ASSORTMENT_FOR_BIG_SHOP_3_MONTHS_GROUPED = "Assortment for big shop 3 months grouped";
    private static final String ASSORTMENT_FOR_BIG_SHOP_3_MONTHS_GROUPED_13_MODELS = "Assortment for big shop 3 months grouped 13 models";
    private static final String ASSORTMENT_FOR_BIG_SHOP_WITH_MANY_PARAMS = "Assortment for big shop with many params";
    private static ReportingApiHandleSteps steps = new ReportingApiHandleSteps();
    private static ReportingApiFileSteps files = new ReportingApiFileSteps();
    private static ReportingApiMetaSteps meta = new ReportingApiMetaSteps();
    private static ReportingApiParamsProvider pp = new ReportingApiParamsProvider(steps);
    private String name;

    public ReportingApiBuildAssortmentReportTest(String name) {
        this.name = name;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<String> testCases = new ArrayList<>();
        testCases.add(ASSORTMENT_BIG_SHOP);
        testCases.add(ASSORTMENT_FOR_BIG_SHOP_FOR_19_MODELS);
        testCases.add(ASSORTMENT_BIG_SHOP_3_MONTHS);
        testCases.add(ASSORTMENT_FOR_BIG_SHOP_3_MONTHS_GROUPED);
        testCases.add(ASSORTMENT_FOR_BIG_SHOP_3_MONTHS_GROUPED_13_MODELS);
        testCases.add(ASSORTMENT_FOR_BIG_SHOP_WITH_MANY_PARAMS);
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
            case ASSORTMENT_BIG_SHOP:
                return ReportingApiParams.forBigShop(pp).withAssortment();
            case ASSORTMENT_FOR_BIG_SHOP_FOR_19_MODELS:
                return ReportingApiParams.forBigShop(pp).withAssortment().numModels(19);
            case ASSORTMENT_BIG_SHOP_3_MONTHS:
                return ReportingApiParams.forBigShop(pp).withAssortment().forMonths(3);
            case ASSORTMENT_FOR_BIG_SHOP_3_MONTHS_GROUPED:
                return ReportingApiParams.forBigShop(pp).withAssortment().forMonths(3).groupedByMonth();
            case ASSORTMENT_FOR_BIG_SHOP_3_MONTHS_GROUPED_13_MODELS:
                return ReportingApiParams.forBigShop(pp).withAssortment().forMonths(3).groupedByMonth().numModels(13);
            case ASSORTMENT_FOR_BIG_SHOP_WITH_MANY_PARAMS:
                return ReportingApiParams.forBigShopWithManyParams(pp).withAssortment();
            default:
                throw new IllegalArgumentException("unknown test case");
        }
    }
}
