package ru.yandex.market.core.util.spring.db;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Vadim Lyalin
 */
public class MbiBeanPropertySqlParameterSourceTest {
    @Test
    public void testGetValue() {
        JavaTimeSource javaTimeSource = new JavaTimeSource();
        MbiBeanPropertySqlParameterSource parameterSource = new MbiBeanPropertySqlParameterSource(javaTimeSource);
        Object localDate = parameterSource.getValue("localDate");
        Object localDateTime = parameterSource.getValue("localDateTime");
        Object offsetDateTime = parameterSource.getValue("offsetDateTime");
        Object zonedDateTime = parameterSource.getValue("zonedDateTime");
        Object instant = parameterSource.getValue("instant");

        assertThat(localDate, instanceOf(Date.class));
        assertThat(localDateTime, instanceOf(Timestamp.class));
        assertThat(offsetDateTime, instanceOf(Timestamp.class));
        assertThat(zonedDateTime, instanceOf(Timestamp.class));
        assertThat(instant, instanceOf(Timestamp.class));

        assertThat(((Date) localDate).toLocalDate(), is(javaTimeSource.getLocalDate()));
        assertThat(((Timestamp) localDateTime).toLocalDateTime(), is(javaTimeSource.getLocalDateTime()));
        assertThat(((Timestamp) zonedDateTime).toLocalDateTime(), is(javaTimeSource.getLocalDateTime()));
        assertThat(((Timestamp) offsetDateTime).toLocalDateTime(), is(javaTimeSource.getLocalDateTime()));
        assertThat(((Timestamp) instant).toInstant(), is(javaTimeSource.getInstant()));
    }

    private static class JavaTimeSource {
        LocalDate localDate = LocalDate.of(2017, Month.JANUARY, 1);
        LocalDateTime localDateTime = LocalDateTime.of(2017, Month.JANUARY, 1, 2, 3, 4, 5);
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2017, Month.JANUARY.getValue(), 1, 2, 3, 4, 5, ZoneOffset.ofHours(1));
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2017, Month.JANUARY.getValue(), 1, 2, 3, 4, 5, ZoneOffset.ofHours(1));
        Instant instant = Instant.ofEpochMilli(1500000000000L);

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(LocalDate localDate) {
            this.localDate = localDate;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public void setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public OffsetDateTime getOffsetDateTime() {
            return offsetDateTime;
        }

        public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
            this.offsetDateTime = offsetDateTime;
        }

        public ZonedDateTime getZonedDateTime() {
            return zonedDateTime;
        }

        public void setZonedDateTime(ZonedDateTime zonedDateTime) {
            this.zonedDateTime = zonedDateTime;
        }

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }
    }
}
