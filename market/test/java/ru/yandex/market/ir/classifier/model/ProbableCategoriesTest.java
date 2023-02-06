package ru.yandex.market.ir.classifier.model;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Evgeny Anisimoff <a href="mailto:anisimoff@yandex-team.ru"/>
 * @since {19:15}
 */
public class ProbableCategoriesTest {

    @Test
    public void getTopProbableCategories() {
        ProbableCategories probableCategories = new ProbableCategories();
        IntSet evenAllowedCategories = new IntOpenHashSet();
        for (int i = 0; i < 100; i++) {
            probableCategories.addCategory(i, i / 100.);
            if (i % 2 == 0) {
                evenAllowedCategories.add(i);
            }
        }

        assertEquals(
                asList(98, 96, 94, 92, 90),
                probableCategories.getTopProbableCategories(5, evenAllowedCategories));
    }

    @Test
    public void addTest() {
        ProbableCategories probableCategories = new ProbableCategories();
        probableCategories.addCategory(1, .5);
        probableCategories.addCategory(2, .25);
        probableCategories.addCategory(3, .25);
        IntList categories = probableCategories.getTopProbableCategories(10, new IntOpenHashSet(asList(1, 2, 3)));

        assertEquals(3, categories.size());
    }
}
