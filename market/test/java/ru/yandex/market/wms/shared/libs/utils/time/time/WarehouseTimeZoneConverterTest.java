package ru.yandex.market.wms.shared.libs.utils.time.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.shared.libs.utils.time.WarehouseTimeZoneConverter;

class WarehouseTimeZoneConverterTest {

    private static final String DEFAULT_TIMEZONE_ID = "Europe/Moscow";
    private static final String EKB_TIMEZONE_ID = "Asia/Yekaterinburg";

    private static final int EKB_OFFSET = 5;
    private static final int MOSCOW_OFFSET = 3;

    private WarehouseTimeZoneConverter warehouseTimeZoneConverter;

    @BeforeEach
    void initialize() {
        warehouseTimeZoneConverter = new WarehouseTimeZoneConverter(DEFAULT_TIMEZONE_ID);
    }

    @Test
    void convertFromUTCWhenTimeZoneDefault() {
        LocalDateTime utcTime = LocalDateTime.of(2021, 8, 11, 0, 0, 0);
        final LocalDateTime warehouseTime = warehouseTimeZoneConverter.convertFromUTC(utcTime);
        LocalDateTime moscowTime = utcTime.plusHours(MOSCOW_OFFSET);
        Assertions.assertEquals(moscowTime, warehouseTime);
    }

    @Test
    void convertFromUTCWhenUTCTimeIsNull() {
        LocalDateTime expected = warehouseTimeZoneConverter.convertFromUTC(null);
        Assertions.assertNull(expected);
    }

    @Test
    void convertFromUTCWhenTimeEKB() {
        LocalDateTime utcTime = LocalDateTime.of(2021, 8, 11, 0, 0, 0);
        warehouseTimeZoneConverter = new WarehouseTimeZoneConverter(EKB_TIMEZONE_ID);
        final LocalDateTime warehouseTime = warehouseTimeZoneConverter.convertFromUTC(utcTime);
        LocalDateTime ekbTime = utcTime.plusHours(EKB_OFFSET);
        Assertions.assertEquals(ekbTime, warehouseTime);
    }

    @Test
    void convertFromStringToUTCWithoutNano() {
        String originalString = "2021-08-12 11:02:03";
        String expectedString = "2021-08-12 08:02:03";
        final String result = warehouseTimeZoneConverter.convertFromStringToUTC(originalString);
        Assertions.assertEquals(expectedString, result);
    }

    @Test
    void convertFromStringWhenOriginalStringIsNull() {
        String originalString = null;
        final String expected = warehouseTimeZoneConverter.convertFromStringToUTC(originalString);
        Assertions.assertNull(expected);
    }

    @Test
    void convertFromStringThrowsDateTimeParseExceptionForUnsupportedFormat() {
        String originalString = "2021-08-12";
        Assertions.assertThrows(
                DateTimeParseException.class,
                () -> warehouseTimeZoneConverter.convertFromStringToUTC(originalString)
        );
    }
}
