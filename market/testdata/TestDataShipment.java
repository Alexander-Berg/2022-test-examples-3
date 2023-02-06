package ru.yandex.autotests.market.checkouter.beans.testdata;

import ru.yandex.autotests.market.checkouter.beans.BaseBean;
import ru.yandex.autotests.market.checkouter.beans.DeliveryServiceStatus;
import ru.yandex.autotests.market.checkouter.beans.testdata.builders.TestDataShipmentBuilder;

import java.util.List;

/**
 * @author mmetlov
 */
public class TestDataShipment extends BaseBean {

    private Long id;

    private Long shopShipmentId;

    private Long weight;

    private Long width;

    private Long height;

    private Long depth;

    private DeliveryServiceStatus status;

    private String labelURL;

    private List<TestDataTrack> tracks;

    private List<TestDataParcelItem> parcelItems;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShopShipmentId() {
        return shopShipmentId;
    }

    public void setShopShipmentId(Long shopShipmentId) {
        this.shopShipmentId = shopShipmentId;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    public Long getWidth() {
        return width;
    }

    public void setWidth(Long width) {
        this.width = width;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public Long getDepth() {
        return depth;
    }

    public void setDepth(Long depth) {
        this.depth = depth;
    }

    public DeliveryServiceStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryServiceStatus status) {
        this.status = status;
    }

    public String getLabelURL() {
        return labelURL;
    }

    public void setLabelURL(String labelURL) {
        this.labelURL = labelURL;
    }

    public List<TestDataTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<TestDataTrack> tracks) {
        this.tracks = tracks;
    }

    public List<TestDataParcelItem> getParcelItems() {
        return parcelItems;
    }

    public void setParcelItems(List<TestDataParcelItem> parcelItems) {
        this.parcelItems = parcelItems;
    }

    public TestDataShipmentBuilder but() {
        return new TestDataShipmentBuilder().copy(this);
    }
}
