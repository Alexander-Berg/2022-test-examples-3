package ru.yandex.market.abo.core.export.rating;

import java.text.SimpleDateFormat;
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
 * @date 28.06.18.
 */
public class ShopRatingWithDatesProcessorTest extends RatingCalculationCurrentTest {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    @InjectMocks
    private ShopRatingWithDatesProcessor shopRatingWithDatesProcessor;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void swapExceptional() {
        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CUTOFFS_IN_RATING))
                .thenReturn(Collections.singleton(SHOP_ID));

        Collection<ShopRatingWithDates> ratings = shopRatingWithDatesProcessor.getCollection();
        assertEquals(1, ratings.size());

        ShopRatingWithDates rating = ratings.iterator().next();
        assertEquals(RatingMode.ACTUAL.getId(), rating.getrMode());
        assertEquals(1, rating.getIsEnabled());
        assertEquals(exportFormat(RatingMode.ACTUAL.getId(), 1), String.valueOf(rating));
    }

    @Test
    public void notExceptional() {
        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CUTOFFS_IN_RATING))
                .thenReturn(Collections.emptySet());

        Collection<ShopRatingWithDates> ratings = shopRatingWithDatesProcessor.getCollection();
        assertEquals(1, ratings.size());

        ShopRatingWithDates rating = ratings.iterator().next();
        assertEquals(INITIAL_R_MODE_ID, rating.getrMode());
        assertEquals(0, rating.getIsEnabled());
        assertEquals(exportFormat(INITIAL_R_MODE_ID, 0), String.valueOf(rating));
    }

    private static String exportFormat(int rMode, int enabled) {
        return Stream.of(SHOP_ID, RATING, DATE_FORMAT.format(R_DATE), enabled, rMode)
                .map(String::valueOf).collect(Collectors.joining("\t"));
    }
}
