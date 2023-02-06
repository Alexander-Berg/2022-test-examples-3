package ru.yandex.market.deepmind.common.repository;

import java.util.Collection;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;

public class MskuAvailabilityMatrixRepositoryFilterTest {

    private static final int REPEAT = 10;
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestUtils.createMskuRandom();
    }

    @Test
    public void filterCopyShouldReturnIdenticalFilter() {
        for (int i = 0; i < REPEAT; i++) {
            MskuAvailabilityMatrixRepository.Filter filter = random
                .nextObject(MskuAvailabilityMatrixRepository.Filter.class);
            Assertions.assertThat(new MskuAvailabilityMatrixRepository.Filter(filter))
                .isEqualToComparingFieldByFieldRecursively(filter);
        }
        MskuAvailabilityMatrixRepository.Filter filter = random
            .nextObject(MskuAvailabilityMatrixRepository.Filter.class)
            .setMskuIds((Collection<Long>) null);
        Assertions.assertThat(new MskuAvailabilityMatrixRepository.Filter(filter)).isEqualToIgnoringNullFields(filter);
        Assertions.assertThat(new MskuAvailabilityMatrixRepository.Filter(filter).getMskuIds()).isNull();
    }

    @Test
    public void filterCopyShouldIgnoreMskuIdsIfInstructedToFilter() {
        for (int i = 0; i < REPEAT; i++) {
            MskuAvailabilityMatrixRepository.Filter filter = random
                .nextObject(MskuAvailabilityMatrixRepository.Filter.class);
            MskuAvailabilityMatrixRepository.Filter copy = new MskuAvailabilityMatrixRepository.Filter(filter, false);
            Assertions.assertThat(copy)
                .isEqualToIgnoringGivenFields(filter, "mskuIds");
            Assertions.assertThat(copy.getMskuIds()).isNull();
        }
    }
}
