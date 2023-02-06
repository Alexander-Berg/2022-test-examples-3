package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Objects;

public class TestChangeRequestPayload extends AbstractChangeRequestPayload {

    private String name;

    public TestChangeRequestPayload() {
        super(ChangeRequestType.RECIPIENT);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        TestChangeRequestPayload that = (TestChangeRequestPayload) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public String toString() {
        return "TestChangeRequestPayload{" +
                "name='" + name + '\'' +
                '}';
    }
}
