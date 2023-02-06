package ru.yandex.market.delivery.partner.billing.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogisticPartnerBillingYtImportDaoTest extends FunctionalTest {

    @Autowired
    private LogisticPartnerOutgoingTxYtImportDao logisticPartnerOutgoingTxYtImportDao;

    @Test
    void testToYearMonth() {
        Instant actual = logisticPartnerOutgoingTxYtImportDao.toInstant("2020-11-10T12:55:40.964114+03:00");

        Instant expected = ZonedDateTime.of(2020, 11, 10, 9, 55, 40, 964114000, ZoneOffset.UTC).toInstant();

        assertEquals(expected, actual);
    }

    @Test
    void testGetTableDirectory() {
        String actual = logisticPartnerOutgoingTxYtImportDao.getTableDirectory("path", LocalDate.of(2020, 5, 1));

        assertEquals("path/2020-05-01", actual);
    }
}
