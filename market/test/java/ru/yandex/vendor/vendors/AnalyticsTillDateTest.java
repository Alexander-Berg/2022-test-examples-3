package ru.yandex.vendor.vendors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author fbokovikov
 */
class AnalyticsTillDateTest {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    void analyticsPostponedDate() {
        LocalDateTime date = LocalDateTime.of(2019, 5, 15, 0, 0);

        LocalDateTime analyticsTillDate = VendorService.getAnalyticsTillDate(date);
        Assertions.assertEquals(
                "2019-06-01",
                FORMAT.format(analyticsTillDate)
        );
    }
}
