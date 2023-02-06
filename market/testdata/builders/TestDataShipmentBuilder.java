package ru.yandex.autotests.market.checkouter.beans.testdata.builders;

import ru.yandex.autotests.market.checkouter.beans.DeliveryServiceStatus;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataShipment;

import java.util.Optional;

/**
 * @author mmetlov
 */
public class TestDataShipmentBuilder implements Cloneable {

    private Optional<Long> shopShipmentId = Optional.empty();

    private Optional<Long> weight = Optional.empty();

    private Optional<Long> width = Optional.empty();

    private Optional<Long> height = Optional.empty();

    private Optional<Long> depth = Optional.empty();

    private Optional<DeliveryServiceStatus> status = Optional.empty();

    private Optional<String> labelURL = Optional.empty();

    public TestDataShipmentBuilder withShopShipmentId(Long value) {
        shopShipmentId = Optional.ofNullable(value);
        return this;
    }

    public TestDataShipmentBuilder withWeight(Long value) {
        weight = Optional.ofNullable(value);
        return this;
    }

    public TestDataShipmentBuilder withWidth(Long value) {
        width = Optional.ofNullable(value);
        return this;
    }

    public TestDataShipmentBuilder withHeight(Long value) {
        height = Optional.ofNullable(value);
        return this;
    }

    public TestDataShipmentBuilder withDepth(Long value) {
        depth = Optional.ofNullable(value);
        return this;
    }

    public TestDataShipmentBuilder withStatus(DeliveryServiceStatus value) {
        status = Optional.ofNullable(value);
        return this;
    }

    public TestDataShipmentBuilder withLabelURL(String value) {
        labelURL = Optional.ofNullable(value);
        return this;
    }

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    public TestDataShipmentBuilder but() {
        return (TestDataShipmentBuilder) (this.clone());
    }

    public TestDataShipmentBuilder copy(TestDataShipment shipment) {
        withShopShipmentId(shipment.getShopShipmentId());
        withWeight(shipment.getWeight());
        withWidth(shipment.getWidth());
        withHeight(shipment.getHeight());
        withDepth(shipment.getDepth());
        withStatus(shipment.getStatus());
        withLabelURL(shipment.getLabelURL());
        return this;
    }

    public TestDataShipment build() {
        TestDataShipment result = new TestDataShipment();
        shopShipmentId.ifPresent(value -> result.setShopShipmentId(value));
        weight.ifPresent(value -> result.setWeight(value));
        width.ifPresent(value -> result.setWidth(value));
        height.ifPresent(value -> result.setHeight(value));
        depth.ifPresent(value -> result.setDepth(value));
        status.ifPresent(value -> result.setStatus(value));
        labelURL.ifPresent(value -> result.setLabelURL(value));
        return result;
    }
}
