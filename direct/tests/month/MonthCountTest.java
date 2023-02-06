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

import static ru.yandex.autotests.direct.data.ReportConstants.MONTH_COUNT_TITLE;
import static ru.yandex.autotests.direct.data.ReportConstants.WORD_HEADER;
import static ru.yandex.autotests.direct.utils.YamlHelper.getMonthlyHist;

/**
 * User: xy6er
 * Date: 02.06.13
 * Time: 16:51
 */

@Aqua.Test(title = MONTH_COUNT_TITLE)
@Feature(Features.MONTH)
public class MonthCountTest extends BaseTestClass {
    private static ReportTable reportTable;

    public MonthCountTest(Word word) {
        super(word);
    }

    @ClassRule
    public static final TestConfiguration TEST_CLASS_CONFIGURATION = new TestConfiguration() {
        @Override
        protected void before() {
            checkApiIsWork();
            reportTable = new ReportTable();
        }

        @Override
        protected void after() {
            addReportTable(reportTable, MONTH_COUNT_TITLE, WORD_HEADER);
            createReportFiles(MonthCountTest.class.getSimpleName(), MONTH_COUNT_TITLE);
        }
    };

    @Rule
    public final TestConfiguration testConfiguration = new TestConfiguration() {
        @Override
        protected void after() {
            if (reportRow != null) {
                reportTable.getReportRows().add(reportRow);
            }
        }
    };

    /**
     * Тест сравнивает количество месяцев в помесячном хроносрезе
     * Сцерарий теста: Для каждой фразы, сравнивает кол-во месяцев с продакшена и беты.
     * На продакшене не должно быть больше месяцев, чем на бете
     * На бете не должно быть больше месяцев чем на +1
     * Разница по кол-ву месяцев, должна совпадать по всем фразам
     */
    @Test
    public void monthCountTest() {
        MonthlyHist prodMonthlyHist = getMonthlyHist(ApiMode.PROD, word);
        MonthlyHist betaMonthlyHist = getMonthlyHist(ApiMode.BETA, word);
        checkTainted(prodMonthlyHist.isTainted(), betaMonthlyHist.isTainted());

        reportRow = new ReportRow();
        reportRow.setName(word.getValue());
        reportRow.setProdValue(prodMonthlyHist.getMonthCount());
        reportRow.setBetaValue(betaMonthlyHist.getMonthCount());
        setCountDiff(reportTable, prodMonthlyHist.getMonthCount(), betaMonthlyHist.getMonthCount(), "месяцев");
    }

}
