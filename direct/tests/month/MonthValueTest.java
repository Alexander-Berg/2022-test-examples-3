package ru.yandex.autotests.direct.tests.month;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.data.ApiMode;
import ru.yandex.autotests.direct.objects.MonthlyHist;
import ru.yandex.autotests.direct.objects.advqstrings.Word;
import ru.yandex.autotests.direct.objects.report.ReportRow;
import ru.yandex.autotests.direct.objects.report.ReportTable;
import ru.yandex.autotests.direct.tests.BaseTestClass;
import ru.yandex.autotests.direct.tests.Features;
import ru.yandex.autotests.direct.utils.TestConfiguration;

import static ru.yandex.autotests.direct.data.ReportConstants.*;
import static ru.yandex.autotests.direct.utils.YamlHelper.getMonthlyHist;

/**
 * User: xy6er
 * Date: 02.06.13
 * Time: 16:51
 */

@Aqua.Test(title = MONTH_VALUE_TITLE)
@Feature(Features.MONTH)
public class MonthValueTest extends BaseTestClass {
    private ReportTable reportTable;

    public MonthValueTest(Word word) {
        super(word);
    }

    @ClassRule
    public static final TestConfiguration TEST_CLASS_CONFIGURATION = new TestConfiguration() {
        @Override
        protected void before() {
            checkApiIsWork();
        }

        @Override
        protected void after() {
            createReportFiles(MonthValueTest.class.getSimpleName(), MONTH_VALUE_TITLE);
        }
    };

    @Rule
    public final TestConfiguration testConfiguration = new TestConfiguration() {
        @Override
        protected void after() {
            addReportTable(reportTable, String.format(WORD_TITLE_FORMAT, MONTH_VALUE_TITLE, word), MONTH_HEADER);
        }
    };

    /**
     * Тест сравнивает кол-во показов по месяцам, в помесячном хроносрезе
     * Сценарий теста:
     *  1. Находит месяцы, которые есть и в продакшене и на бете.
     *  2. Сравнивает кол-во показов по найденым месяцам.
     * Разница показов должна быть меньше 50%
     */
    @Test
    public void monthValueTest() {
        MonthlyHist prodMonthlyHist = getMonthlyHist(ApiMode.PROD, word);
        MonthlyHist betaMonthlyHist = getMonthlyHist(ApiMode.BETA, word);
        checkTainted(prodMonthlyHist.isTainted(), betaMonthlyHist.isTainted());

        reportTable = new ReportTable();
        for (MonthlyHist.Request.Hist betaHist : betaMonthlyHist.getHists()) {
            reportRow = new ReportRow();
            reportRow.setName(String.valueOf(betaHist.month) + "-" + String.valueOf(betaHist.year));
            reportRow.setBetaValue(betaHist.total_count);

            MonthlyHist.Request.Hist prodHist = prodMonthlyHist.findHist(betaHist.month, betaHist.year);
            if (prodHist != null) {
                reportRow.setProdValue(prodHist.total_count);
                setTotalCountDiff(reportTable, prodHist.total_count, betaHist.total_count);
            }

            reportTable.getReportRows().add(reportRow);
        }
    }

}
