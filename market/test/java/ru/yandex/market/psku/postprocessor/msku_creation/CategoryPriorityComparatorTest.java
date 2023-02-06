package ru.yandex.market.psku.postprocessor.msku_creation;


import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class CategoryPriorityComparatorTest {

    private long HIGH_PRIORITY_CATEGORY_ID_1 = 1001L;
    private long HIGH_PRIORITY_CATEGORY_ID_2 = 1002L;
    private long NO_PRIORITY_CATEGORY_ID_1 = 2001L;
    private long NO_PRIORITY_CATEGORY_ID_2 = 2002L;

    private CategoryPriorityComparator comparator;

    @Before
    public void setUp() throws Exception {
        comparator = new CategoryPriorityComparator(ImmutableSet.of(
            HIGH_PRIORITY_CATEGORY_ID_1, HIGH_PRIORITY_CATEGORY_ID_2
        ));
    }

    @Test
    public void testSamePriority() {
        Assertions.assertThat(comparator.compare(HIGH_PRIORITY_CATEGORY_ID_1, HIGH_PRIORITY_CATEGORY_ID_2))
            .isEqualTo(0);
        Assertions.assertThat(comparator.compare(NO_PRIORITY_CATEGORY_ID_1, NO_PRIORITY_CATEGORY_ID_2))
            .isEqualTo(0);
    }

    @Test
    public void testDiffPriority() {
        // correct order: low priority hid and then high one
        Assertions.assertThat(comparator.compare(NO_PRIORITY_CATEGORY_ID_1, HIGH_PRIORITY_CATEGORY_ID_2))
            .isEqualTo(-1);
        Assertions.assertThat(comparator.compare(HIGH_PRIORITY_CATEGORY_ID_2, NO_PRIORITY_CATEGORY_ID_1))
            .isEqualTo(1);
    }

}
