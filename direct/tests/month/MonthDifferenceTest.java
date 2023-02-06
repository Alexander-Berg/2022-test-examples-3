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
 * @author xy6er
 */

@Aqua.Test(title = MONTH_DIFFERENCE_TITLE)
@Feature(Features.MONTH)
public class MonthDifferenceTest extends BaseTestClass {
    private ReportTable reportTable;

    public MonthDifferenceTest(Word word) {
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
            createReportFiles(MonthDifferenceTest.class.getSimpleName(), MONTH_DIFFERENCE_TITLE);
        }
    };

    @Rule
    public final TestConfiguration testConfiguration = new TestConfiguration() {
        @Override
        protected void after() {
            addReportTable(reportTable, String.format(WORD_TITLE_FORMAT, MONTH_DIFFERENCE_TITLE, word), COMPARE_MONTH_HEADER);
        }
    };

    /**
     * Тест сравнивает кол-во показов по месяцам, которых не оказалость в продакшене
     * Сцерарий теста:
     *  1. Перебирается каждый месяц из беты и если нету этого месяца в продакшене то выполняется шаг № 2
     *  2. Если это первый месяц которого нет в продакшене, то сравнивает его с последнем месяцем из продакшена.
     *  3. Иначе месяц сравнивается с предыдущем месяцем, которого не оказалось в продакшене
     * Разница показов должна быть меньше 50%
     */
    @Test
    public void monthValueTest() {
        MonthlyHist prodMonthlyHist = getMonthlyHist(ApiMode.PROD, word);
        MonthlyHist betaMonthlyHist = getMonthlyHist(ApiMode.BETA, word);
        checkTainted(prodMonthlyHist.isTainted(), betaMonthlyHist.isTainted());

        reportTable = new ReportTable();
        MonthlyHist.Request.Hist compareCandidate = prodMonthlyHist.getHists().get(prodMonthlyHist.getMonthCount() - 1);

        for (MonthlyHist.Request.Hist betaHist : betaMonthlyHist.getHists()) {
            if (prodMonthlyHist.findHist(betaHist.month, betaHist.year) == null) {
                String name = String.format("%d-%d %d-%d",
                        compareCandidate.month, compareCandidate.year,
                        betaHist.month, betaHist.year);
                if (reportTable.getReportRows().isEmpty()) {
                    name = "lastProd " + name;
                }

                reportRow = new ReportRow();
                reportRow.setName(name);
                reportRow.setBetaValue(betaHist.total_count);
                reportRow.setProdValue(compareCandidate.total_count);
                setTotalCountDiff(reportTable, compareCandidate.total_count, betaHist.total_count);

                reportTable.getReportRows().add(reportRow);
                compareCandidate = betaHist;
            }
        }
    }

}
