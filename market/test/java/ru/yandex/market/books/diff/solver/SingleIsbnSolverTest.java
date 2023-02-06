package ru.yandex.market.books.diff.solver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import junit.framework.TestCase;
import ru.yandex.market.books.dao.CardsDiff;
import ru.yandex.market.books.diff.IsbnIndex;
import ru.yandex.market.books.diff.dao.BookCard;
import ru.yandex.market.books.diff.dao.Offer;
import ru.yandex.market.books.diff.dao.rkp.novelties.RkpEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SingleIsbnSolverTest extends TestCase {
    private static final IsbnIndex<RkpEntry> EMPTY_RKP_INDEX = new IsbnIndex<>(Collections.emptyList());
    private SingleIsbnSolver solver;
    private CardsDiff diff;

    protected void setUp() throws Exception {
        super.setUp();
        solver = new SingleIsbnSolver();
        diff = new CardsDiff();
    }

    public void testWithEmptyIsbns() {
        LongOpenHashSet isbns = new LongOpenHashSet();
        IsbnIndex<Offer> offersIndex = new IsbnIndex<Offer>(Collections.<Offer>emptyList());
        IsbnIndex<BookCard> cardIsbnIndex = new IsbnIndex<BookCard>(Collections.<BookCard>emptyList());
        assertFalse(solver.tryToSolve(isbns, offersIndex, cardIsbnIndex, EMPTY_RKP_INDEX, diff));
    }

    public void testUsualCase() {
        LongSet linkedIsbns = LongSets.singleton(978517016767L);
        IsbnIndex<Offer> offersIndex = new IsbnIndex<Offer>(Arrays.asList(new Offer(
            "1111",
            new LongOpenHashSet(new long[]{978517016767L}),
            "Под пологом пьяного леса",
            "Д. Даррелл",
            null, new IntArrayList(new int[]{2003}),
            "АСТ",
            2222,
            3333
        )));
        IsbnIndex<BookCard> cardIsbnIndex = new IsbnIndex<BookCard>(Collections.<BookCard>emptyList());
        assertTrue(solver.tryToSolve(linkedIsbns, offersIndex, cardIsbnIndex, EMPTY_RKP_INDEX, diff));
        assertEquals(1, diff.getNewCards().size());
    }

    public void testWithExistingCard() {
        LongSet linkedIsbns = LongSets.singleton(978517016767L);
        IsbnIndex<Offer> offersIndex = new IsbnIndex<Offer>(Arrays.asList(new Offer(
            "1111",
            new LongOpenHashSet(new long[]{978517016767L}),
            "Под пологом пьяного леса",
            "Д. Даррелл",
            null,
            new IntArrayList(new int[]{2003}),
            "АСТ",
            2222,
            3333
        )));
        IsbnIndex<BookCard> cardIsbnIndex = new IsbnIndex<BookCard>(Arrays.asList(new BookCard(
            111111, LongSets.singleton(978517016767L), LongSets.EMPTY_SET,
            "Под пологом пьяного леса", "Д. Даррелл", null, "Зеленая серия",
            IntLists.singleton(2003), "АСТ", IntLists.singleton(222222),
            -1, 5, Collections.<String>emptySet()
        )));
        assertTrue(solver.tryToSolve(linkedIsbns, offersIndex, cardIsbnIndex, EMPTY_RKP_INDEX, diff));
        assertEquals(0, diff.getNewCards().size());
    }

    public void testWithMoreThenOneLinkedIsbn() {
        LongSet linkedIsbns = new LongOpenHashSet(new long[]{978517016767L, 9785271050386L});
        IsbnIndex<Offer> offersIndex = new IsbnIndex<Offer>(Arrays.asList(new Offer(
            "1111",
            new LongOpenHashSet(new long[]{5170167679L}),
            "Под пологом пьяного леса",
            "Д. Даррелл",
            null,
            new IntArrayList(new int[]{2003}),
            "АСТ",
            2222,
            3333
        )));
        IsbnIndex<BookCard> cardIsbnIndex = new IsbnIndex<BookCard>(Collections.<BookCard>emptyList());
        assertFalse(solver.tryToSolve(linkedIsbns, offersIndex, cardIsbnIndex, EMPTY_RKP_INDEX, diff));
    }
}
