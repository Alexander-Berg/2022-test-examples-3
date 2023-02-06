package ru.yandex.market.ir.parser.matcher;

import junit.framework.TestCase;
import org.junit.Test;
import ru.yandex.market.ir.parser.Entity;
import ru.yandex.market.ir.parser.Type;
import ru.yandex.utils.Triple;
import ru.yandex.utils.string.DefaultStringProcessor;
import ru.yandex.utils.string.indexed.IndexedStringFactory;
import ru.yandex.utils.string.indexed.Position;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class NumberMatcherTest extends TestCase {
    private IndexedStringFactory factory;
    private NumberMatcher matcher = new NumberMatcher();

    @Override
    protected void setUp() throws Exception {
        factory = new IndexedStringFactory();
        factory.setStringProcessor(new DefaultStringProcessor());
    }

    @Test
    public void testUsualNumbers() {
        checkResults(
            getMatchedNumbers("ахаха 20 уруру 30 охохо 40 эхехе"),
            new Triple<>(20d, 2, 2),
            new Triple<>(30d, 6, 6),
            new Triple<>(40d, 10, 10)
        );
        checkResults(
            getMatchedNumbers("ахаха 10.01 уруру 20.02 охохо 30.03 эхехе"),
            new Triple<>(10.01d, 2, 4),
            new Triple<>(20.02d, 8, 10),
            new Triple<>(30.03d, 14, 16)
        );
    }

    @Test
    public void testAddedNumbers() {
        checkResults(
            getMatchedNumbers("ахаха 512 + 512 эхехе"),
            new Triple<>(512d, 2, 2),
            new Triple<>(512d, 6, 6),
            new Triple<>(1024d, 2, 6)
        );
        checkResults(
            getMatchedNumbers("ахаха 512 ++ 512 эхехе"),
            new Triple<>(512d, 2, 2),
            new Triple<>(512d, 6, 6)
        );
        checkResults(
            getMatchedNumbers("ахаха 512 +* 512 эхехе"),
            new Triple<>(512d, 2, 2),
            new Triple<>(512d, 6, 6)
        );
        checkResults(
            getMatchedNumbers("ахаха 512 + 512 +  12 эхехе"),
            new Triple<>(512d, 2, 2),
            new Triple<>(512d, 6, 6),
            new Triple<>(12d, 10, 10),
            new Triple<>(1036d, 2, 10)
        );
        checkResults(
            getMatchedNumbers("3+ 2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 3, 3)
        );
        checkResults(
            getMatchedNumbers("3+  2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 3, 3)
        );
    }

    @Test
    public void testTripleAddedNumbers() {
        checkResults(
            getMatchedNumbers("Hugo Boss Element Hugo man набор 60+50+50"),
            new Triple<>(60d, 12, 12),
            new Triple<>(50d, 14, 14),
            new Triple<>(50d, 16, 16),
            new Triple<>(160d, 12, 16)
        );
        checkResults(
            getMatchedNumbers("Hugo Boss Element Hugo man набор 60 + 50 + 50"),
            new Triple<>(60d, 12, 12),
            new Triple<>(50d, 16, 16),
            new Triple<>(50d, 20, 20),
            new Triple<>(160d, 12, 20)
        );
        checkResults(
            getMatchedNumbers("Hugo Boss Element Hugo man набор 60+ 50+ 50"),
            new Triple<>(60d, 12, 12),
            new Triple<>(50d, 15, 15),
            new Triple<>(50d, 18, 18)
        );

        checkResults(
            getMatchedNumbers("10+20+30+40"),
            new Triple<>(10d, 0, 0),
            new Triple<>(20d, 2, 2),
            new Triple<>(30d, 4, 4),
            new Triple<>(40d, 6, 6)
        );
        checkResults(
            getMatchedNumbers("10+20 эхе охо 30+40"),
            new Triple<>(10d, 0, 0),
            new Triple<>(20d, 2, 2),
            new Triple<>(30d, 8, 8),
            new Triple<>(40d, 10, 10),
            new Triple<>(30d, 0, 2),
            new Triple<>(70d, 8, 10)
        );
        checkResults(
            getMatchedNumbers("10+20 30+40"),
            new Triple<>(10d, 0, 0),
            new Triple<>(20d, 2, 2),
            new Triple<>(30d, 4, 4),
            new Triple<>(40d, 6, 6),
            new Triple<>(30d, 0, 2),
            new Triple<>(70d, 4, 6)
        );
        checkResults(
            getMatchedNumbers("10 + 20 + 30 + 40"),
            new Triple<>(10d, 0, 0),
            new Triple<>(20d, 4, 4),
            new Triple<>(30d, 8, 8),
            new Triple<>(40d, 12, 12)
        );
        checkResults(
            getMatchedNumbers("10+20+30 20+40 10 + 20 + 30 10 + 40"),
            new Triple<>(10d, 0, 0),
            new Triple<>(20d, 2, 2),
            new Triple<>(30d, 4, 4),
            new Triple<>(20d, 6, 6),
            new Triple<>(40d, 8, 8),
            new Triple<>(10d, 10, 10),
            new Triple<>(20d, 14, 14),
            new Triple<>(30d, 18, 18),
            new Triple<>(10d, 20, 20),
            new Triple<>(40d, 24, 24),
            new Triple<>(60d, 0, 4),
            new Triple<>(60d, 6, 8),
            new Triple<>(60d, 10, 18),
            new Triple<>(50d, 20, 24)
        );
    }

    @Test
    public void testTripleMultipliedNumbers() {
        checkResults(
            getMatchedNumbers("Габариты 30х40х50"),
            new Triple<>(30d, 2, 2),
            new Triple<>(40d, 4, 4),
            new Triple<>(50d, 6, 6)
        );
        checkResults(
            getMatchedNumbers("Габариты 30 х 40 х 50"),
            new Triple<>(30d, 2, 2),
            new Triple<>(40d, 6, 6),
            new Triple<>(50d, 10, 10)
        );
    }

    @Test
    public void testMultipliedNumbers() {
        checkResults(
            getMatchedNumbers("ахаха 2 x 512 эхехе"),
            new Triple<>(2d, 2, 2),
            new Triple<>(512d, 6, 6),
            new Triple<>(1024d, 2, 6)
        );
        checkResults(
            getMatchedNumbers("ахаха 2 * 512 эхехе"),
            new Triple<>(2d, 2, 2),
            new Triple<>(512d, 6, 6),
            new Triple<>(1024d, 2, 6)
        );
        checkResults(
            getMatchedNumbers("ахаха 2Х512 эхехе"),
            new Triple<>(2d, 2, 2),
            new Triple<>(512d, 4, 4),
            new Triple<>(1024d, 2, 4)
        );
        checkResults(
            getMatchedNumbers("3x, 2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 4, 4)
        );
        checkResults(
            getMatchedNumbers("3x2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 2, 2),
            new Triple<>(6d, 0, 2)
        );
        checkResults(
            getMatchedNumbers("3 x 2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 4, 4),
            new Triple<>(6d, 0, 4)
        );
        checkResults(
            getMatchedNumbers("3x 2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 3, 3)
        );
        checkResults(
            getMatchedNumbers("3x  2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 3, 3)
        );
        checkResults(
            getMatchedNumbers("3 x2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 3, 3)
        );
        checkResults(
            getMatchedNumbers("3 x2"),
            new Triple<>(3d, 0, 0),
            new Triple<>(2d, 3, 3)
        );
        checkResults(
            getMatchedNumbers("ахаха 2 * 3x4 эхехе"),
            new Triple<>(2d, 2, 2),
            new Triple<>(3d, 6, 6),
            new Triple<>(4d, 8, 8),
            new Triple<>(12d, 6, 8),
            new Triple<>(6d, 2, 6)
        );
        checkResults(
            getMatchedNumbers("2x20 3x30"),
            new Triple<>(2d, 0, 0),
            new Triple<>(20d, 2, 2),
            new Triple<>(3d, 4, 4),
            new Triple<>(30d, 6, 6),
            new Triple<>(40d, 0, 2),
            new Triple<>(90d, 4, 6)
        );
        checkResults(
            getMatchedNumbers("2x30 2x3x40"),
            new Triple<>(2d, 0, 0),
            new Triple<>(30d, 2, 2),
            new Triple<>(2d, 4, 4),
            new Triple<>(3d, 6, 6),
            new Triple<>(40d, 8, 8),
            new Triple<>(60d, 0, 2)
        );
        checkResults(
            getMatchedNumbers("2x30 2x40 2 x 30 2 x 40"),
            new Triple<>(2d, 0, 0),
            new Triple<>(30d, 2, 2),
            new Triple<>(2d, 4, 4),
            new Triple<>(40d, 6, 6),
            new Triple<>(2d, 8, 8),
            new Triple<>(30d, 12, 12),
            new Triple<>(2d, 14, 14),
            new Triple<>(40d, 18, 18),
            new Triple<>(60d, 0, 2),
            new Triple<>(80d, 4, 6),
            new Triple<>(60d, 8, 12),
            new Triple<>(80d, 14, 18)
        );
    }

    private MatchResult[] getMatchedNumbers(String sourceString) {
        return matcher.match(
            new Entity(
                0, new Type.Builder().setEntityId(0).setName("").setEntityMatcher(matcher).build(),
                "", 0
            ),
            factory.createIndexedString(sourceString), null, null
        ).getMatchResults();
    }

    private void checkResults(MatchResult[] vmrs, Triple<Double, Integer, Integer>... expectedResults) {
        for (int i = 0; i < vmrs.length; i++) {
            Triple<Double, Integer, Integer> expectedResult = expectedResults[i];
            checkResult((ValueMatchResult) vmrs[i], expectedResult.getFirst(), expectedResult.getSecond(),
                    expectedResult.getThird());
        }
        assertEquals(expectedResults.length, vmrs.length);
    }

    private void checkResult(ValueMatchResult vmr, double value, int start, int end) {
        assertFalse(vmr.isComplex());
        assertTrue(vmr.isMatched());
        assertEquals("Value differ!", value, vmr.getNumberValue());
        Position position = vmr.getPosition();
        assertTrue(position.getStart() <= position.getEnd());
        assertEquals("Start differ!", start, position.getStart());
        assertEquals("End differ!", end, position.getEnd());
    }
}
