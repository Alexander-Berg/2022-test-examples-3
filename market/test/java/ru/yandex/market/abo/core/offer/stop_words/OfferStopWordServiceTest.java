package ru.yandex.market.abo.core.offer.stop_words;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.common.report.model.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * created on 23.11.16.
 */
public class OfferStopWordServiceTest extends EmptyTest {
    private static final int TICKET_ID = 100500;

    @Autowired
    private OfferStopWordService stopWordsService;

    @Test
    public void testDao() {
        String category = "apple";
        String category2 = "xiaomi";
        OfferStopWord whiteWord = constructStopWord("white", category, true, false);
        whiteWord = stopWordsService.mergeStopWord(whiteWord);

        Map<String, List<String>> stopWordsByCategories = stopWordsService.getStopWordsByCategories();
        assertTrue(stopWordsByCategories.containsKey(category));
        assertEquals(1, stopWordsByCategories.get(category).size());

        OfferStopWord redWord = constructStopWord("red", category, false, true);
        redWord = stopWordsService.mergeStopWord(redWord);
        assertEquals(2, stopWordsService.getStopWordsByCategories().get(category).size());

        whiteWord.setCategory(category2);
        whiteWord = stopWordsService.mergeStopWord(whiteWord);
        assertEquals(2, stopWordsService.getStopWordsByCategories().size());

        List<OfferStopWord> whiteWords = stopWordsService.findAllByColor(Color.GREEN);
        assertEquals(1, whiteWords.size());
        assertTrue(whiteWords.contains(whiteWord));

        List<OfferStopWord> redWords = stopWordsService.findAllByColor(Color.RED);
        assertEquals(1, redWords.size());
        assertTrue(redWords.contains(redWord));

        stopWordsService.deleteStopWord(whiteWord.getId());
        stopWordsService.deleteStopWord(redWord.getId());
        assertTrue(stopWordsService.getStopWordsByCategories().isEmpty());
    }

    @Test
    public void testDaoCounters() {
        Map<String, Integer> countersMap = new HashMap<>();
        countersMap.put("foo", 1);
        countersMap.put("bar", 2);
        stopWordsService.saveStopWordsCounters(countersMap, TICKET_ID, OfferStopWordUsage.PREMOD);
        Map<String, Integer> stopWordsCountersFromDb = stopWordsService.findByTicketId(TICKET_ID).stream()
                .collect(Collectors.toMap(OfferStopWordCount::getWord, OfferStopWordCount::getCount, (w1, w2) -> w1));
        assertEquals(countersMap, stopWordsCountersFromDb);
    }

    protected static OfferStopWord constructStopWord(String word, String category) {
        return constructStopWord(word, category, true, true);
    }

    private static OfferStopWord constructStopWord(String word, String category, boolean white, boolean red) {
        OfferStopWord stopWord = new OfferStopWord();
        stopWord.setWord(word);
        stopWord.setCategory(category);
        stopWord.setWhite(white);
        stopWord.setRed(red);
        stopWord.setTurbo(false);
        return stopWord;
    }
}
