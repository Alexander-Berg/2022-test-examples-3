package ru.yandex.ir.modelsclusterizer.persistence;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * @author mkrasnoperov
 */
public class MinMovesPairMatcherTest {
    @Test
    public void testDivisionTwo() throws IOException {
        MinMovesPairMatcher pm = new MinMovesPairMatcher();
        int[][] matrix = {{1, 1}};
        IdPersistenceInfo[] matching = pm.createPairMatching(matrix);
        assertNotEquals(matching[0].getSaveId(), -1);
    }

    @Test
    public void testUnionTwo() throws IOException {
        MinMovesPairMatcher pm = new MinMovesPairMatcher();
        int[][] matrix = {{1}, {1}};
        IdPersistenceInfo[] matching = pm.createPairMatching(matrix);
        assertTrue(matching[0].getSaveId() != -1 || matching[1].getSaveId() != -1);
    }

    @Test
    public void testTwoTwo() throws IOException {
        MinMovesPairMatcher pm = new MinMovesPairMatcher();
        int[][] matrix = {{11, 50}, {10, 1}};
        IdPersistenceInfo[] matching = pm.createPairMatching(matrix);
        assertTrue(matching[0].getSaveId() == 1 || matching[1].getSaveId() == 0);
    }

    @Test
    public void testNoMatch() throws IOException {
        MinMovesPairMatcher pm = new MinMovesPairMatcher();
        int[][] matrix = {{5, 5, 5, 5}, {6, 6, 6, 6}};
        IdPersistenceInfo[] matching = pm.createPairMatching(matrix);
        assertTrue(matching[0].getSaveId() == -1 || matching[1].getSaveId() == -1);
    }

    @Test
    public void testTwoThree() throws IOException {
        MinMovesPairMatcher pm = new MinMovesPairMatcher();
        int[][] matrix = {{5, 5, 10}, {10, 1, 0}};
        IdPersistenceInfo[] matching = pm.createPairMatching(matrix);
        assertTrue(matching[0].getSaveId() == 2 || matching[1].getSaveId() == 1);
    }

    @Test
    public void testZero() throws IOException {
        MinMovesPairMatcher pm = new MinMovesPairMatcher();
        int[][] matrix = {{0, 0, 0}, {0, 0, 0}};
        IdPersistenceInfo[] matching = pm.createPairMatching(matrix);
        assertTrue(matching[0].getSaveId() == -1 || matching[1].getSaveId() == -1);
    }

    /**
     * Тот самый тест на фиктивные верщины в построение парсоча
     * Без них бы вначале максимизировалось бы кол-во пар попавших в парсоч
     * и в итоге тут был бы парсоч из 2х пар и
     * 31 НЕ переехавший оффер вместо 50 НЕ переехавших
     * @throws IOException
     */
    @Test
    public void testNotFullFlow() throws IOException {
        MinMovesPairMatcher pm = new MinMovesPairMatcher();
        int[][] matrix = {{0, 1, 0}, {0, 50, 30}};
        IdPersistenceInfo[] matching = pm.createPairMatching(matrix);
        assertTrue(matching[0].getSaveId() == -1 || matching[1].getSaveId() == 1);
    }
}
