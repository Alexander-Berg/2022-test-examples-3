package ru.yandex.market.abo.core.export.rating;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
public class ClassifierShopRatingContentProviderTest extends RatingCalculationCurrentTest {
    @Autowired
    @InjectMocks
    private ClassifierShopRatingContentProvider provider;

    @Mock
    private ExceptionalShopsService exceptionalShopsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void hasExceptions() throws Exception {
        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CUTOFFS_IN_RATING))
                .thenReturn(Collections.singleton(SHOP_ID));
        check(RatingMode.ACTUAL);
    }

    @Test
    public void noExceptions() throws IOException {
        when(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CUTOFFS_IN_RATING))
                .thenReturn(Collections.emptySet());
        check(INITIAL_R_MODE);
    }

    private void check(RatingMode expectedMode) throws IOException {
        InputStream inputStream = provider.getInputStream();
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        try (CSVParser parser = new CSVParser(reader, CSVFormat.TDF.withHeader())) {
            for (CSVRecord record : parser) {
                assertEquals(SHOP_ID, (long) Long.valueOf(record.get("SHOP_ID")));
                assertEquals(expectedMode.getId(), (int) Integer.valueOf(record.get("R_MODE")));
            }
            assertEquals(1, parser.getCurrentLineNumber());
        }
    }
}
