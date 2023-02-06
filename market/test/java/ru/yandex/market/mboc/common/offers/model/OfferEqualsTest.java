package ru.yandex.market.mboc.common.offers.model;

import java.util.List;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.utils.MbocComparators;

/**
 * @author s-ermakov
 */
public class OfferEqualsTest {

    private static final long SEED = 15486;

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .exclude(new FieldDefinition<>("marketParameterValues", List.class, Offer.class))
        .seed(SEED)
        .overrideDefaultInitialization(true)
        .build();

    private static void assertEquals(Offer offer1, Offer offer2) {
        // проверяем, что при копировании объекты равны друг другу
        Assertions.assertThat(offer2).isEqualToComparingFieldByField(offer1);
        // также проверяем, что тестовый компаратор тоже будет возвращать равенство
        Assertions.assertThat(offer2)
            .usingComparator(MbocComparators.OFFERS_GENERAL)
            .isEqualTo(offer1)
            .usingComparator(MbocComparators.OFFERS_WITH_CREATED_UPDATED)
            .isEqualTo(offer1);
        // также проверяем, что тестовый ассерт тоже будет возвращать равенство
        MbocAssertions.assertThat(offer2).isEqualTo(offer1).isEqualWithCreatedUpdated(offer1);
    }

    private static void assertNotEquals(Offer offer1, Offer offer2) {
        // также проверяем, что тестовый компаратор тоже НЕ будет возвращать равенство
        Assertions.assertThat(offer2)
            .usingComparator(MbocComparators.OFFERS_GENERAL)
            .isNotEqualTo(offer1)
            .usingComparator(MbocComparators.OFFERS_WITH_CREATED_UPDATED)
            .isNotEqualTo(offer1);
        // также проверяем, что тестовый ассерт тоже НЕ будет возвращать равенство
        MbocAssertions.assertThat(offer2).isNotEqualTo(offer1).isNotEqualWithCreatedUpdated(offer1);
    }

    @Test
    public void testCopyWorksCorrectly() {
        for (int i = 0; i < 100; i++) {
            Offer offer = getRandomValue();
            Offer copy = new Offer(offer);

            assertEquals(offer, copy);
        }
    }

    @Test
    public void testOffersComparatorWillCorrectlyCompareDifferentObjects() {
        Offer prevValue = getRandomValue();
        for (int i = 0; i < 100; i++) {
            Offer offer = getRandomValue();

            assertNotEquals(offer, prevValue);
            prevValue = offer;
        }
    }

    private Offer getRandomValue() {
        return random.nextObject(Offer.class);
    }
}
