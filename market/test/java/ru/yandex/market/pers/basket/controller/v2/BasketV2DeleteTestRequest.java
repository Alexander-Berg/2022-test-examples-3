package ru.yandex.market.pers.basket.controller.v2;

/**
 * @author ifilippov5
 */
public class BasketV2DeleteTestRequest extends BasketV2TestRequest {

    private long itemId;

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public BasketV2DeleteTestRequest clone() {
        BasketV2DeleteTestRequest request = new BasketV2DeleteTestRequest();
        request.setItemId(this.getItemId());
        return request;
    }

}
