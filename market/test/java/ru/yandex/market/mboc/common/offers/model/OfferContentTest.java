package ru.yandex.market.mboc.common.offers.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.utils.ObjectsEqualByFieldsUtil;

public class OfferContentTest {
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .seed(33333)
                .build();
    }

    @Test
    public void testEqualsByValue() {
        var equalityChecker = new ObjectsEqualByFieldsUtil<>(
                new OfferContent(),
                OfferContent::new,
                OfferContent::equalsContent,
                Set.of(
                    // DO NOT add fields here until you sure that it's not needed in equals!
                ),
                // Add here fields that can't be randomly generated (e.g. collections)
                Map.of(
                        "urls", () -> List.of("www"),
                        "extraShopFields", () -> Map.of("from", "to")
                )
        );
        equalityChecker.findEquality(random);
        Assertions.assertThat(equalityChecker.getEqualFields()).isEmpty();
    }
}
