package ru.yandex.market.deepmind.common.repository;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.utils.TestUtils;

public class MskuRepositoryFilterTest {

    private static final int REPEAT = 10;
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestUtils.createMskuRandom();
    }

    @Test
    public void filterCopyShouldReturnIdenticalFilter() {
        for (int i = 0; i < REPEAT; i++) {
            MskuFilter filter = random.nextObject(MskuFilter.class);
            Assertions.assertThat(new MskuFilter(filter))
                .isEqualToComparingFieldByFieldRecursively(filter);
        }
        MskuFilter filter = random.nextObject(MskuFilter.class, "marketSkuIds", "categoryIds");
        Assertions.assertThat(new MskuFilter(filter)).isEqualToIgnoringNullFields(filter);
        Assertions.assertThat(new MskuFilter(filter).getMarketSkuIds()).isNull();
        Assertions.assertThat(new MskuFilter(filter).getCategoryIds()).isNull();
    }

    @Test
    public void filterCopyShouldIgnoreMskuIdsIfInstructedToFilter() {
        for (int i = 0; i < REPEAT; i++) {
            MskuFilter filter = random.nextObject(MskuFilter.class);
            MskuFilter copy = new MskuFilter(filter, false, true);
            Assertions.assertThat(copy)
                .isEqualToIgnoringGivenFields(filter, "marketSkuIds");
            Assertions.assertThat(copy.getMarketSkuIds()).isNull();
        }
    }

    @Test
    public void filterCopyShouldIgnoreCategoryIdsIfInstructedToFilter() {
        for (int i = 0; i < REPEAT; i++) {
            MskuFilter filter = random.nextObject(MskuFilter.class);
            MskuFilter copy = new MskuFilter(filter, true, false);
            Assertions.assertThat(copy)
                .isEqualToIgnoringGivenFields(filter, "categoryIds");
            Assertions.assertThat(copy.getCategoryIds()).isNull();
        }
    }
}
