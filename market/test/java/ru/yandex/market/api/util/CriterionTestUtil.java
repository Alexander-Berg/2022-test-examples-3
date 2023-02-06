package ru.yandex.market.api.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import org.jetbrains.annotations.Nullable;
import ru.yandex.market.api.domain.v2.criterion.Criterion;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author dimkarp93
 * @deprecated проверяет по equals + производит сортировку, лучше развивать CriterionMatcher
 * @see ru.yandex.market.api.matchers.CriterionMatcher
 */
//TODO отазатся в пользу CriterionMatcher
@Deprecated
public class CriterionTestUtil {
    private static final Comparator<Criterion> CRITERION_COMPARATOR = Ordering
            .from(Comparator.comparing(Criterion::getId))
            .compound(Comparator.comparing(Criterion::getValue));

    public static void assertCriterionEquals(@Nullable Collection<Criterion> expected, @Nullable Collection<Criterion> actual) {
        if (null == expected || null == actual) {
            assertTrue(null == expected && null == actual);
            return;
        }

        List<Criterion> exp = CommonCollections.asList(expected);
        Collections.sort(exp, CRITERION_COMPARATOR);

        List<Criterion> act = CommonCollections.asList(actual);
        Collections.sort(act, CRITERION_COMPARATOR);

        assertTrue(Iterables.elementsEqual(exp, act));
    }
}
