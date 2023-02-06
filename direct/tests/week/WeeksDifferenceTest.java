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
 * @author xy6er
 */

@Aqua.Test(title = WEEKS_DIFFERENCE_TITLE)
@Feature(Features.WEEKS)
public class WeeksDifferenceTest extends BaseTestClass {
    private ReportTable reportTable;

    public WeeksDifferenceTest(Word word) {
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
            createReportFiles(WeeksDifferenceTest.class.getSimpleName(), WEEKS_DIFFERENCE_TITLE);
        }
    };

    @Rule
    public final TestConfiguration testConfiguration = new TestConfiguration() {
        @Override
        protected void after() {
            addReportTable(reportTable, String.format(WORD_TITLE_FORMAT, WEEKS_DIFFERENCE_TITLE, word), COMPARE_WEEKS_HEADER);
        }
    };

    /**
     * Тест сравнивает кол-во показов по неделям, которых не оказалость в продакшене
     * Сцерарий теста:
     *  1. Перебирается каждая неделя из беты и если нету этой недели в продакшене то выполняется шаг № 2
     *  2. Если это первая неделя, которой нет в продакшене, то сравнивает его с последней неделей из продакшена.
     *  3. Иначе неделя сравнивается с предыдущей неделей, которой не оказалось в продакшене
     * Разница показов должна быть меньше 50%
     */
    @Test
    public void weeksValueTest() {
        WeeklyHist prodWeeklyHist = getWeeklyHist(ApiMode.PROD, word);
        WeeklyHist betaWeeklyHist = getWeeklyHist(ApiMode.BETA, word);
        checkTainted(prodWeeklyHist.isTainted(), betaWeeklyHist.isTainted());

        reportTable = new ReportTable();
        WeeklyHist.Request.Hist compareCandidate = prodWeeklyHist.getHists().get(prodWeeklyHist.getWeeksCount() - 1);

        for (WeeklyHist.Request.Hist betaHist : betaWeeklyHist.getHists()) {
            if (prodWeeklyHist.findHist(betaHist.monday) == null) {
                String name = compareCandidate.monday + " "  + betaHist.monday;
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
