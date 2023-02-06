package ru.yandex.autotests.direct.tests.includingphrases;

import ch.lambdaj.function.convert.Converter;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ch.lambdaj.Lambda.convert;
import static ru.yandex.autotests.direct.data.AdvqConstants.MAX_INCLUDING_PHRASES_DIFF;
import static ru.yandex.autotests.direct.data.ReportConstants.*;
import static ru.yandex.autotests.direct.utils.YamlHelper.getWordStat;

/**
 * Created by semkagtn on 12/10/14.
 */
@Aqua.Test(title = INCLUDING_PHRASES_TITLE)
@Feature(Features.INCLUDING_PHRASES)
public class IncludingPhrasesCountTest extends BaseTestClass {

    private static ReportTable reportTable;

    public IncludingPhrasesCountTest(Word word) {
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
            addReportTable(reportTable, INCLUDING_PHRASES_TITLE, WORD_HEADER,
                    "Исходный размер", "Размер пересечения", "Разница");
            createReportFiles(IncludingPhrasesCountTest.class.getSimpleName(), INCLUDING_PHRASES_TITLE);
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

    @Test
    public void test() {
        WordStat prodWordStat = getWordStat(ApiMode.PROD, word, 50);
        WordStat betaWordStat = getWordStat(ApiMode.BETA, word, 50);
        checkTainted(prodWordStat.isTainted(), betaWordStat.isTainted());

        reportRow = new ReportRow();
        reportRow.setName(word.getValue());
        setIncludingPhrasesDiff(reportTable, prodWordStat.getIncludingPhrases(), betaWordStat.getIncludingPhrases());
    }

    /**
     * Расчитывает число общих элементов в множестве суперфраз на бете и продакшене.
     * Проверяет, что число общих суперфраз не меньше допустимового.
     * @param prodIncludingPhrases Суперфразы с продакшена
     * @param betaIncludingPhrases Суперфразы с беты
     */
    private void setIncludingPhrasesDiff(ReportTable reportTable,
                                           WordStat.Request.Stat.IncludingPhrase[] prodIncludingPhrases,
                                           WordStat.Request.Stat.IncludingPhrase[] betaIncludingPhrases) {
        Converter<WordStat.Request.Stat.IncludingPhrase, String> phraseToString =
                new Converter<WordStat.Request.Stat.IncludingPhrase, String>() {
                    @Override
                    public String convert(WordStat.Request.Stat.IncludingPhrase from) {
                        return from.phrase;
                    }
                };
        List<String> prodPhrases = convert(prodIncludingPhrases, phraseToString);
        List<String> betaPhrases = convert(betaIncludingPhrases, phraseToString);
        int maxSize = Math.max(prodPhrases.size(), betaPhrases.size());

        Set<String> intersection = new HashSet<>(prodPhrases);
        intersection.retainAll(betaPhrases);
        int intersectionSize = intersection.size();

        double diff = (1 - (double) intersectionSize / maxSize) * 100;

        reportRow.setProdValue(maxSize);
        reportRow.setBetaValue(intersectionSize);
        reportRow.setDiff(String.format("%.2f%%", diff));

        if (diff > MAX_INCLUDING_PHRASES_DIFF) {
            LOGGER.error(String.format("Множества суперфраз должны отличаться не более, чем на %.2f%% элементов",
                    MAX_INCLUDING_PHRASES_DIFF));
            reportTable.setStatus(FAIL);
        }
    }
}
