package ru.yandex.autotests.market.checkouter.beans.testdata;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by
 * strangelet on 03.08.16.
 */

public class DeliveryDates extends ru.yandex.autotests.market.place.beans.common.DeliveryDates {

    private String reservedUntil;

    public DeliveryDates() {
        super();
    }

    public DeliveryDates(DeliveryDates dates) {
        super();
        setFromDate(dates.getFromDate());
        setToDate(dates.getToDate());
        setReservedUntil(dates.getReservedUntil());
    }

    public DeliveryDates(String fromDate, String reservedUntil) {
        setFromDate(fromDate);
        this.reservedUntil = reservedUntil;
    }

    public String getReservedUntil() {
        return reservedUntil;
    }

    public DateTime getReservedUntilDateTime() {
        return this.reservedUntil == null ? null : DateTimeFormat.forPattern("dd-MM-YYYY").parseDateTime(this.reservedUntil);
    }

    public void setReservedUntil(String reservedUntil) {
        this.reservedUntil = reservedUntil;
    }

    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, new String[0]);
    }

    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, new String[0]);
    }
}
