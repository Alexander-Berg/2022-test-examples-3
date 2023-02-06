package ru.yandex.market.marketpromo.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OfferIdTest {

    @Test
    void shouldSortIdsByOfferId() {
        OfferId expected = OfferId.of("06bb10cdf1ed5963460560d8fd63d853-bmV3LmRpbWVuc2lvbnMuNDgxMzM5", 10778372);

        Set<OfferId> ids = Set.of(
                OfferId.of("06bb10cdf1ed5963460560d8fd63d853-bmV3LmRpbWVuc2lvbnMuNDgxMzM5", 10785995),
                expected
        );

        final List<OfferId> offerIds = ids.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        assertThat(offerIds.get(0),
                is(expected));
    }
}
