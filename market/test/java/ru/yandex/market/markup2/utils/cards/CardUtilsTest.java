package ru.yandex.market.markup2.utils.cards;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import ru.yandex.market.markup2.utils.report.ReportCard;

import java.util.Set;

/**
 * @author inenakhov
 */
public class CardUtilsTest extends TestCase {
    private static final int ID = 1;
    private static final String TITLE = "title";
    private static final String IMAGE_URL = "http://image.com";
    private static final String DESCRIPTION = "description";
    private static final long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "name";
    private static final CardType CARD_TYPE = CardType.MODEL;

    private Card someCard = new Card(ID, TITLE, IMAGE_URL, DESCRIPTION, CATEGORY_ID, CARD_TYPE);
    private ReportCard someReportCard = new ReportCard(ID, TITLE, IMAGE_URL, DESCRIPTION,
                                                       CATEGORY_NAME, CATEGORY_ID, CardType.MODEL);
    public void testToReportCard() throws Exception {
        ReportCard obtainedReportCard = CardUtils.toReportCard(someCard, CATEGORY_NAME);
        assertReportCardsAreEqual(someReportCard, obtainedReportCard);

    }

    public void testToReportCards() throws Exception {
        Set<Card> cards = CardUtils.toCards(Lists.newArrayList(someReportCard));
        assertCardsAreEqual(someCard, cards.iterator().next());
    }

    public void testToCards() throws Exception {
        Set<ReportCard> reportCards = CardUtils.toReportCards(Lists.newArrayList(someCard), CATEGORY_NAME);
        assertReportCardsAreEqual(someReportCard, reportCards.iterator().next());
    }

    private void assertReportCardsAreEqual(ReportCard expected, ReportCard second) {
        assertEquals(expected.getId(), second.getId());
        assertEquals(expected.getTitle(), second.getTitle());
        assertEquals(expected.getImageUrl(), second.getImageUrl());
        assertEquals(expected.getDescription(), second.getDescription());
        assertEquals(expected.getCategoryName(), second.getCategoryName());
        assertEquals(expected.getCategoryId(), second.getCategoryId());
        assertEquals(expected.getType(), second.getType());
    }

    private void assertCardsAreEqual(Card expected, Card second) {
        assertEquals(expected.getId(), second.getId());
        assertEquals(expected.getTitle(), second.getTitle());
        assertEquals(expected.getImageUrl(), second.getImageUrl());
        assertEquals(expected.getDescription(), second.getDescription());
        assertEquals(expected.getCategoryId(), second.getCategoryId());
        assertEquals(expected.getType(), second.getType());
    }
}
