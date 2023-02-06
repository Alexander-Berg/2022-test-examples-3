package ru.yandex.market.rg.asyncreport.turnover;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.rg.asyncreport.turnover.model.JxlsTurnoverItem;

/**
 * Тесты для {@link TurnoverSheetsIterator}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TurnoverSheetsIteratorTest {

    @Test
    void testEmpty() {
        Iterator<JxlsTurnoverItem> originalIterator = Collections.emptyIterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        Assertions.assertFalse(iterator.hasNext());
    }

    @Test
    void singleElement() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1"))
        );
        checkSheets(iterator, expected);
    }

    @Test
    void twoElementsFromSameCategory() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_2")
                        .setCategoryId(100L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1", "offer_2"))
        );
        checkSheets(iterator, expected);
    }

    @Test
    void threeElementsFromSameCategory() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_2")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_3")
                        .setCategoryId(100L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1", "offer_2", "offer_3"))
        );
        checkSheets(iterator, expected);
    }

    @Test
    void twoElementFromDifferentCategories() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_2")
                        .setCategoryId(101L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1")),
                Pair.of("offer_2", List.of("offer_2"))
        );
        checkSheets(iterator, expected);
    }

    @Test
    void threeElementFromDifferentCategories() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_2")
                        .setCategoryId(101L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_3")
                        .setCategoryId(102L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1")),
                Pair.of("offer_2", List.of("offer_2")),
                Pair.of("offer_3", List.of("offer_3"))
        );
        checkSheets(iterator, expected);
    }

    @Test
    void twoElementFromFirstCategory() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_2")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_3")
                        .setCategoryId(101L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1", "offer_2")),
                Pair.of("offer_3", List.of("offer_3"))
        );
        checkSheets(iterator, expected);
    }

    @Test
    void twoElementFromSecondCategory() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_2")
                        .setCategoryId(101L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_3")
                        .setCategoryId(101L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1")),
                Pair.of("offer_2", List.of("offer_2", "offer_3"))
        );
        checkSheets(iterator, expected);
    }

    @Test
    void twoElementFromSecondCategory2() {
        Iterator<JxlsTurnoverItem> originalIterator = List.of(
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_1")
                        .setCategoryId(100L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_2")
                        .setCategoryId(101L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_3")
                        .setCategoryId(101L)
                        .build(),
                JxlsTurnoverItem.builder()
                        .setOfferId("offer_4")
                        .setCategoryId(102L)
                        .build()
        ).iterator();
        TurnoverSheetsIterator iterator = new TurnoverSheetsIterator(originalIterator);

        List<Pair<String, List<String>>> expected = List.of(
                Pair.of("offer_1", List.of("offer_1")),
                Pair.of("offer_2", List.of("offer_2", "offer_3")),
                Pair.of("offer_4", List.of("offer_4"))
        );
        checkSheets(iterator, expected);
    }

    void checkSheets(TurnoverSheetsIterator sheetsIterator, List<Pair<String, List<String>>> expected) {
        for (var sheet : expected) {
            Assertions.assertTrue(sheetsIterator.hasNext());
            var nextSheet = sheetsIterator.next();
            Assertions.assertEquals(sheet.getKey(), nextSheet.getFirstItemInCategory().getOfferId());
            checkItems(nextSheet.getItems().iterator(), sheet.getValue());
        }
        Assertions.assertFalse(sheetsIterator.hasNext());
    }

    void checkItems(Iterator<JxlsTurnoverItem> itemsIterator, List<String> expectedOfferIds) {
        for (String offerId : expectedOfferIds) {
            Assertions.assertTrue(itemsIterator.hasNext());
            Assertions.assertEquals(offerId, itemsIterator.next().getOfferId());
        }
        Assertions.assertFalse(itemsIterator.hasNext());
    }
}
