package ru.yandex.autotests.market.checkouter.beans.testdata;

public class TestDataParcelItem {
    private Long itemId;
    private Integer count;

    public TestDataParcelItem(Long itemId, Integer count) {
        this.itemId = itemId;
        this.count = count;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "TestDataParcelItem{" +
                "itemId=" + itemId +
                ", count=" + count +
                '}';
    }
}
