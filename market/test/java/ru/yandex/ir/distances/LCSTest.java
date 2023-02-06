package ru.yandex.ir.distances;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Arina Timofeeva, <a href="mailto:atimofeeva@yandex-team.ru"/>
 */
public class LCSTest {
    @Test
    public void testLCS() {
        IntList seq1 = new IntArrayList();
        seq1.add(4);
        IntList seq2 = new IntArrayList();
        seq2.add(4);
        seq2.add(5);
        seq2.add(98);
        seq2.add(7);
        seq2.add(99);
        seq2.add(1);
        seq2.add(92);
        seq2.add(100);
        seq2.add(40);
        seq2.add(101);
        seq2.add(20);
        seq2.add(102);
        seq2.add(103);
        seq2.add(104);
        assertEquals(LCS.calc(seq1.toIntArray(), seq2.toIntArray()), 1);
    }
}
