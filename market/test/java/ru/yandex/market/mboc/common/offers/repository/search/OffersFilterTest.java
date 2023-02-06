package ru.yandex.market.mboc.common.offers.repository.search;

import java.util.stream.IntStream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author yuramalinov
 * @created 01.10.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OffersFilterTest {
    private EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(42).build();

    @Test
    public void testCopy() {
        IntStream.range(0, 100).forEach(n -> {
            OffersFilter filter = random.nextObject(OffersFilter.class, "availabilities");
            OffersFilter filterCopy = filter.copy();
            Assertions.assertThat(filterCopy).isEqualToIgnoringGivenFields(filter,
                "supplierIds", "trackerTickets");
            Assertions.assertThat(filterCopy.getBusinessIds())
                .containsExactlyInAnyOrderElementsOf(filter.getBusinessIds());
            Assertions.assertThat(filterCopy.getTrackerTickets())
                .containsExactlyInAnyOrderElementsOf(filter.getTrackerTickets());
        });
    }
}
