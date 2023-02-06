package ru.yandex.market.books.diff.solver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import junit.framework.TestCase;
import ru.yandex.market.books.dao.CardsDiff;
import ru.yandex.market.books.dao.Publisher;
import ru.yandex.market.books.dao.PublisherDao;
import ru.yandex.market.books.diff.IsbnIndex;
import ru.yandex.market.books.diff.dao.BookCard;
import ru.yandex.market.books.diff.dao.Offer;
import ru.yandex.market.books.diff.dao.rkp.novelties.RkpEntry;

import java.util.Arrays;
import java.util.Collections;

/**
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
@SuppressWarnings("checkstyle:magicnumber")
public class TrivialVolumesSolverTest extends TestCase {
    private static final IsbnIndex<RkpEntry> EMPTY_RKP_INDEX = new IsbnIndex<>(Collections.emptyList());
    private TrivialVolumesSolver solver;
    private CardsDiff diff;

    protected void setUp() throws Exception {
        super.setUp();
        solver = new TrivialVolumesSolver();
        diff = new CardsDiff();
        solver.setPublisherDao(new PublisherDao() {
            @Override
            public Publisher getPublisherByIsbn(String isbnString) {
                return new Publisher("АСТ");
            }
        });
    }

    public void testWithEmptyIsbns() {
        LongOpenHashSet isbns = new LongOpenHashSet();
        IsbnIndex<Offer> offersIndex = new IsbnIndex<Offer>(Collections.<Offer>emptyList());
        IsbnIndex<BookCard> cardIsbnIndex = new IsbnIndex<BookCard>(Collections.<BookCard>emptyList());
        assertFalse(solver.tryToSolve(isbns, offersIndex, cardIsbnIndex, EMPTY_RKP_INDEX, diff));
    }

    public void testUsualCase() {
        LongSet linkedIsbns = new LongOpenHashSet();

        linkedIsbns.add(978517016767L);
        linkedIsbns.add(978517016778L);
        linkedIsbns.add(978517016779L);

        IsbnIndex<Offer> offersIndex = new IsbnIndex<Offer>(Arrays.asList(
                new Offer("1111",
                        new LongOpenHashSet(new long[]{978517016779L, 978517016767L}),
                        "Под пологом пьяного леса",
                        "Д. Даррелл",
                        null,
                        new IntArrayList(new int[]{2003}),
                        "АСТ",
                        2222,
                        3333),
                new Offer("1112",
                        new LongOpenHashSet(new long[]{978517016778L, 978517016767L}),
                        "Под пологом пьяного леса - 2. Судный пень.",
                        "Д. Даррелл",
                        null,
                        new IntArrayList(new int[]{2003}),
                        "АСТ",
                        2222,
                        3333))
        );
        IsbnIndex<BookCard> cardIsbnIndex = new IsbnIndex<BookCard>(Collections.<BookCard>emptyList());
        assertTrue(solver.tryToSolve(linkedIsbns, offersIndex, cardIsbnIndex, EMPTY_RKP_INDEX, diff));
        assertEquals(2, diff.getNewCards().size());
    }
}
