package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import org.joda.time.DateTime;
import ru.yandex.autotests.market.checkouter.beans.testdata.DeliveryDates;

import javax.annotation.Generated;
import java.sql.Timestamp;

public class DeliveryDatesBuilder implements Cloneable {
    private static final String DELIVERY_DATE_FORMAT = "dd-MM-YYYY";

    protected DeliveryDatesBuilder self;

    protected String fromDate;
    private boolean isSetFromDate;

    protected String toDate;
    private boolean isSetToDate;

    protected String reservedUntil;
    private boolean isSetReservedUntil;

    public DeliveryDatesBuilder() {
        self = this;
    }

    public DeliveryDatesBuilder withFromDate(String value) {
        this.fromDate = value;
        this.isSetFromDate = true;
        return self;
    }

    public DeliveryDatesBuilder withToDate(String value) {
        this.toDate = value;
        this.isSetToDate = true;
        return self;
    }

    public DeliveryDatesBuilder withReservedUntil(String value) {
        this.reservedUntil = value;
        this.isSetReservedUntil = true;
        return self;
    }

    @Override
    public Object clone() {
        try {
            DeliveryDatesBuilder result = (DeliveryDatesBuilder)super.clone();
            result.self = result;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    public DeliveryDatesBuilder but() {
        return (DeliveryDatesBuilder)clone();
    }

    public DeliveryDatesBuilder copy(DeliveryDates deliveryDates) {
        withFromDate(deliveryDates.getFromDate());
        withToDate(deliveryDates.getToDate());
        withReservedUntil(deliveryDates.getReservedUntil());
        return self;
    }

    public DeliveryDates build() {
        try {
            DeliveryDates result = new DeliveryDates();
            if (isSetFromDate) {
                result.setFromDate(fromDate);
            }
            if (isSetToDate) {
                result.setToDate(toDate);
            }
            if (isSetReservedUntil) {
                result.setReservedUntil(reservedUntil);
            }
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }


    public DeliveryDatesBuilder withFromDate(DateTime fromDate) {
        return this.withFromDate((String) fromDate.toString(DELIVERY_DATE_FORMAT));
    }

    public DeliveryDatesBuilder withFromDate(Timestamp fromDate) {
        return this.withFromDate((String) (new DateTime(fromDate.getTime())).toString(DELIVERY_DATE_FORMAT));
    }

    public DeliveryDatesBuilder withReservedUntil(DateTime reservedUntil) {
        return this.withReservedUntil((String) reservedUntil.toString(DELIVERY_DATE_FORMAT));
    }

    public DeliveryDatesBuilder withReservedUntil(Timestamp reservedUntil) {
        return this.withReservedUntil((String) (new DateTime(reservedUntil.getTime())).toString(DELIVERY_DATE_FORMAT));
    }

    public DeliveryDatesBuilder withToDate(DateTime toDate) {
        return this.withToDate((String)toDate.toString(DELIVERY_DATE_FORMAT));
    }

    public DeliveryDatesBuilder withToDate(Timestamp toDate) {
        return this.withToDate((String)(new DateTime(toDate.getTime())).toString(DELIVERY_DATE_FORMAT));
    }
}
