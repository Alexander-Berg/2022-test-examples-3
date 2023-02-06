package ru.yandex.autotests.direct.cmd.data.phrases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;

public class PhrasesFactory {

    public static final int WORD_MAX_SIZE = 35;
    public static final int MAX_PHRASE_SIZE = 4096;
    public static final int MAX_PHRASE_COUNT = 200;
    public static final int MAX_WORD_COUNT = 7;


    public static String phrasesWithWhitespaces() {
        return "asds   sadas,   dasdas aaa    , adaaa";
    }

    public static String correctedPhrasesWithWhitespaces() {
        return "asds sadas, dasdas aaa, adaaa";
    }

    public static String phrasesWithDuplicates() {
        return "asds asds, asds asds";
    }
    public static String phraseWithCommaInPrefix() {
        return ",mama";
    }

    public static String phraseWithCommaAndWhiteSpaceInPrefix() {
        return ", mama";
    }

    public static String phraseWithoutCommas() {
        return "mama";
    }
    public static String correctedPhrasesWithDuplicates() {
        return "asds asds";
    }

    public static String normalPhrases() {
        return "asds asdss, asdds asdsds, asdsas";
    }

    public static String getSortedMinusWords(int maxLength) {
        List<String> minusWords = new ArrayList<>();
        int overallLength = 0;
        int i = 0;
        String newMinusWord = "";
        while (overallLength + newMinusWord.length() < maxLength) {
            newMinusWord = " -" + "минус" + i;
            minusWords.add(newMinusWord);
            overallLength += newMinusWord.length();
            i++;
        }
        if (maxLength - overallLength > 2) {
            minusWords.add(" -" + RandomStringUtils.randomAlphabetic(maxLength - overallLength - 2));
        } else {
            minusWords.set(i-1, minusWords.get(i-1) + RandomStringUtils.randomNumeric(maxLength - overallLength));
        }
        Collections.sort(minusWords);
        return StringUtils.join(minusWords, "");
    }

    public static Phrase getDefaultPhrase() {
        return new Phrase()
                .withId(0L)
                .withPhrase("новые автомобили")
                .withIsSuspended("0")
                .withAutobudgetPriority(3d)
                .withMinPrice("0.3")
                .withPrice(250d)
                .withPriceContext(0.3d)
                .withMinusWords(Collections.emptyList())
                .withParam1("")
                .withParam2("");
    }

    public static Phrase getDefaultEngPhrase() {
        return getDefaultPhrase()
                .withPhrase("new auto test phrase")
                .withMinPrice("3.1")
                .withPrice(20d);
    }
}
