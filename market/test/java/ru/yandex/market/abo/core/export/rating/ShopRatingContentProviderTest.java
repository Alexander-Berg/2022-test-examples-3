package ru.yandex.market.abo.core.export.rating;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.rating.RatingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 29.06.18.
 */
public class ShopRatingContentProviderTest extends RatingCalculationCurrentTest {
    @Autowired
    @InjectMocks
    private ShopRatingContentProvider shopRatingContentProvider;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void hasExceptions() {
        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CUTOFFS_IN_RATING))
                .thenReturn(Collections.singleton(SHOP_ID));
        check(RatingMode.ACTUAL, RATING);
    }

    @Test
    public void noExceptions() {
        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CUTOFFS_IN_RATING))
                .thenReturn(Collections.emptySet());
        check(INITIAL_R_MODE, 0);
    }

    private void check(RatingMode expectedMode, double expectedTotal) {
        Collection<ShopRatingContentProvider.ShopRatingOpinion> ratings = shopRatingContentProvider.getCollection();
        assertEquals(1, ratings.size());

        ShopRatingContentProvider.ShopRatingOpinion rating = ratings.iterator().next();
        assertEquals(SHOP_ID, rating.getShopId());
        assertEquals(expectedMode.getId(), rating.getrMode());
        assertEquals(expectedTotal, rating.getTotal());
        assertEquals(exportFormat(expectedTotal), String.valueOf(rating));
    }

    private static String exportFormat(double expectedTotal) {
        return Stream.of(SHOP_ID, expectedTotal, 0).map(String::valueOf).collect(Collectors.joining(" "));
    }
}
