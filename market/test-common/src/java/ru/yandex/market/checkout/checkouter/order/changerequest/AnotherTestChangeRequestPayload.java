package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Objects;

public class AnotherTestChangeRequestPayload extends AbstractChangeRequestPayload {

    private String address;
    private Long anotherField;

    public AnotherTestChangeRequestPayload() {
        super(ChangeRequestType.DELIVERY_ADDRESS);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getAnotherField() {
        return anotherField;
    }

    public void setAnotherField(Long anotherField) {
        this.anotherField = anotherField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AnotherTestChangeRequestPayload that = (AnotherTestChangeRequestPayload) o;
        return Objects.equals(address, that.address) &&
                Objects.equals(anotherField, that.anotherField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address, anotherField);
    }
}
