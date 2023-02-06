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

import java.util.*;

import static ch.lambdaj.Lambda.convert;
import static ru.yandex.autotests.direct.data.ReportConstants.*;
import static ru.yandex.autotests.direct.utils.YamlHelper.getWordStat;

/**
 * Created by semkagtn on 12/10/14.
 */
@Aqua.Test(title = INCLUDING_PHRASES_DIFFERENCE_TITLE)
@Feature(Features.INCLUDING_PHRASES)
public class IncludingPhrasesDifferenceTest extends BaseTestClass {

    private ReportTable reportTable;

    public IncludingPhrasesDifferenceTest(Word word) {
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
            createReportFiles(IncludingPhrasesDifferenceTest.class.getSimpleName(), INCLUDING_PHRASES_DIFFERENCE_TITLE);
        }
    };

    @Rule
    public final TestConfiguration testConfiguration = new TestConfiguration() {
        @Override
        protected void after() {
            addReportTable(reportTable,
                    String.format(WORD_TITLE_FORMAT, INCLUDING_PHRASES_DIFFERENCE_TITLE, word), WORD_HEADER);
        }
    };

    @Test
    public void test() {
        WordStat prodWordStat = getWordStat(ApiMode.PROD, word, 50);
        WordStat betaWordStat = getWordStat(ApiMode.BETA, word, 50);
        checkTainted(prodWordStat.isTainted(), betaWordStat.isTainted());

        reportTable = new ReportTable();
        setIncludingPhrasesCntDiff(reportTable, prodWordStat.getIncludingPhrases(), betaWordStat.getIncludingPhrases());
    }

    /**
     * Разница чисел суперфраз (cnt) для каждой фразы
     * @param prodIncludingPhrases Суперфразы с продакшена
     * @param betaIncludingPhrases Суперфразы с беты
     */
    private void setIncludingPhrasesCntDiff(ReportTable reportTable,
                                              WordStat.Request.Stat.IncludingPhrase[] prodIncludingPhrases,
                                              WordStat.Request.Stat.IncludingPhrase[] betaIncludingPhrases) {
        Map<String, Integer> prodPhrasesMap = new HashMap<>();
        for (WordStat.Request.Stat.IncludingPhrase phrase : prodIncludingPhrases) {
            prodPhrasesMap.put(phrase.phrase, phrase.cnt);
        }
        Map<String, Integer> betaPhrasesMap = new HashMap<>();
        for (WordStat.Request.Stat.IncludingPhrase phrase : betaIncludingPhrases) {
            betaPhrasesMap.put(phrase.phrase, phrase.cnt);
        }

        Converter<WordStat.Request.Stat.IncludingPhrase, String> phraseToString =
                new Converter<WordStat.Request.Stat.IncludingPhrase, String>() {
                    @Override
                    public String convert(WordStat.Request.Stat.IncludingPhrase from) {
                        return from.phrase;
                    }
                };
        List<String> prodPhrases = convert(prodIncludingPhrases, phraseToString);
        List<String> betaPhrases = convert(betaIncludingPhrases, phraseToString);
        Set<String> intersection = new HashSet<>(prodPhrases);
        intersection.retainAll(betaPhrases);

        for (String phrase : intersection) {
            int prodValue = prodPhrasesMap.get(phrase);
            int betaValue = betaPhrasesMap.get(phrase);
            reportRow = new ReportRow();
            reportRow.setName(phrase);
            reportRow.setProdValue(prodValue);
            reportRow.setBetaValue(betaValue);
            setTotalCountDiff(reportTable, prodValue, betaValue);
            reportTable.getReportRows().add(reportRow);
        }
    }
}
