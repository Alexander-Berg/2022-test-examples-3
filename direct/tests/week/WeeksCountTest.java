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

import static ru.yandex.autotests.direct.data.ReportConstants.WEEKS_COUNT_TITLE;
import static ru.yandex.autotests.direct.data.ReportConstants.WORD_HEADER;
import static ru.yandex.autotests.direct.utils.YamlHelper.getWeeklyHist;

/**
 * User: xy6er
 * Date: 02.06.13
 * Time: 16:51
 */

@Aqua.Test(title = WEEKS_COUNT_TITLE)
@Feature(Features.WEEKS)
public class WeeksCountTest extends BaseTestClass {
    private static ReportTable reportTable;

    public WeeksCountTest(Word word) {
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
            addReportTable(reportTable, WEEKS_COUNT_TITLE, WORD_HEADER);
            createReportFiles(WeeksCountTest.class.getSimpleName(), WEEKS_COUNT_TITLE);
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
     * Тест сравнивает количество недель в понедельном хроносрезе
     * Сцерарий теста: Для каждой фразы, сравнивает кол-во недель с продакшена и беты.
     * На продакшене не должно быть больше недель, чем на бете
     * На бете не должно быть больше недель чем на +1
     * Разница по кол-ву недель, должна совпадать по всем фразам
     */
    @Test
    public void weeksCountTest() {
        WeeklyHist prodWeeklyHist = getWeeklyHist(ApiMode.PROD, word);
        WeeklyHist betaWeeklyHist = getWeeklyHist(ApiMode.BETA, word);
        checkTainted(prodWeeklyHist.isTainted(), betaWeeklyHist.isTainted());

        reportRow = new ReportRow();
        reportRow.setName(word.getValue());
        reportRow.setProdValue(prodWeeklyHist.getWeeksCount());
        reportRow.setBetaValue(betaWeeklyHist.getWeeksCount());
        setCountDiff(reportTable, prodWeeklyHist.getWeeksCount(), betaWeeklyHist.getWeeksCount(), "недель");
    }

}
