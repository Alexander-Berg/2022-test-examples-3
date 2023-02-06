package ru.yandex.market.books.diff.solver;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * @author commince
 * @date 26.03.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SolverTestHelper {

    private SolverTestHelper() {

    }

    public static IntSet getDefaultAllowedCategories() {
        IntSet result = new IntOpenHashSet();
        result.add(1);
        result.add(2);
        return result;
    }
}
