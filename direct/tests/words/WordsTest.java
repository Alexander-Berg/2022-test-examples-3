package ru.yandex.autotests.direct.tests.words;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.data.ApiMode;
import ru.yandex.autotests.direct.objects.WordStat;
import ru.yandex.autotests.direct.objects.advqstrings.Word;
import ru.yandex.autotests.direct.objects.report.ReportRow;
import ru.yandex.autotests.direct.objects.report.ReportTable;
import ru.yandex.autotests.direct.tests.BaseTestClass;
import ru.yandex.autotests.direct.tests.Features;
import ru.yandex.autotests.direct.utils.TestConfiguration;

import static ru.yandex.autotests.direct.data.ReportConstants.WORDS_TEST_TITLE;
import static ru.yandex.autotests.direct.data.ReportConstants.WORD_HEADER;
import static ru.yandex.autotests.direct.utils.YamlHelper.getWordStat;

/**
 * User: xy6er
 * Date: 02.06.13
 * Time: 16:51
 */

@Aqua.Test(title = WORDS_TEST_TITLE)
@Feature(Features.WORDS)
public class WordsTest extends BaseTestClass {
    private static ReportTable reportTable;

    public WordsTest(Word word) {
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
            addReportTable(reportTable, WORDS_TEST_TITLE, WORD_HEADER);
            createReportFiles(WordsTest.class.getSimpleName(), WORDS_TEST_TITLE);
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
     * Тест сравнивает количество показов по фразе
     * Сцерарий теста: Для каждой фразы, сравнивает кол-во показов с продакшена и беты.
     * Разница показов должна быть меньше 50%
     */
    @Test
    public void wordsTotalCountTest() {
        WordStat prodWordStat = getWordStat(ApiMode.PROD, word);
        WordStat betaWordStat = getWordStat(ApiMode.BETA, word);
        checkTainted(prodWordStat.isTainted(), betaWordStat.isTainted());

        reportRow = new ReportRow();
        reportRow.setName(word.getValue());
        reportRow.setProdValue(prodWordStat.getTotalCount());
        reportRow.setBetaValue(betaWordStat.getTotalCount());
        setTotalCountDiff(reportTable, prodWordStat.getTotalCount(), betaWordStat.getTotalCount());
    }

}
