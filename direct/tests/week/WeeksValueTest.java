package ru.yandex.autotests.direct.tests.week;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.data.ApiMode;
import ru.yandex.autotests.direct.objects.WeeklyHist;
import ru.yandex.autotests.direct.objects.advqstrings.Word;
import ru.yandex.autotests.direct.objects.report.ReportRow;
import ru.yandex.autotests.direct.objects.report.ReportTable;
import ru.yandex.autotests.direct.tests.BaseTestClass;
import ru.yandex.autotests.direct.tests.Features;
import ru.yandex.autotests.direct.utils.TestConfiguration;

import static ru.yandex.autotests.direct.data.ReportConstants.*;
import static ru.yandex.autotests.direct.utils.YamlHelper.getWeeklyHist;

/**
 * User: xy6er
 * Date: 02.06.13
 * Time: 16:51
 */

@Aqua.Test(title = WEEKS_VALUE_TITLE)
@Feature(Features.WEEKS)
public class WeeksValueTest extends BaseTestClass {
    private ReportTable reportTable;

    public WeeksValueTest(Word word) {
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
            createReportFiles(WeeksValueTest.class.getSimpleName(), WEEKS_VALUE_TITLE);
        }
    };

    @Rule
    public final TestConfiguration testConfiguration = new TestConfiguration() {
        @Override
        protected void after() {
            addReportTable(reportTable, String.format(WORD_TITLE_FORMAT, WEEKS_VALUE_TITLE, word),  WEEK_HEADER);
        }
    };

    /**
     * Тест сравнивает кол-во показов по месяцам, в понедельном хроносрезе
     * Сценарий теста:
     *  1. Находит недели, которые есть и в продакшене и на бете.
     *  2. Сравнивает кол-во показов по найденым неделям.
     * Разница показов должна быть меньше 50%
     */
    @Test
    public void weeksValueTest() {
        WeeklyHist prodWeeklyHist = getWeeklyHist(ApiMode.PROD, word);
        WeeklyHist betaWeeklyHist = getWeeklyHist(ApiMode.BETA, word);
        checkTainted(prodWeeklyHist.isTainted(), betaWeeklyHist.isTainted());

        reportTable = new ReportTable();
        for (WeeklyHist.Request.Hist betaHist : betaWeeklyHist.getHists()) {
            reportRow = new ReportRow();
            reportRow.setName(betaHist.monday);
            reportRow.setBetaValue(betaHist.total_count);

            WeeklyHist.Request.Hist prodHist = prodWeeklyHist.findHist(betaHist.monday);
            if (prodHist != null) {
                reportRow.setProdValue(prodHist.total_count);
                setTotalCountDiff(reportTable, prodHist.total_count, betaHist.total_count);
            }

            reportTable.getReportRows().add(reportRow);
        }
    }

}
