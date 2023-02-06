package ru.yandex.market.pers.basket.controller.v2;

import ru.yandex.market.pers.basket.model.CategoryPostRequest;

import java.util.Collections;

/**
 * @author maratik
 */
public class CategoryPostTestRequest extends BasketV2TestRequest {

    private CategoryPostRequest data =
        new CategoryPostRequest(Collections.emptyList());

    public CategoryPostRequest getData() {
        return data;
    }

    public void setData(CategoryPostRequest data) {
        this.data = data;
    }

    public CategoryPostTestRequest clone() {
        CategoryPostTestRequest request = new CategoryPostTestRequest();
        request.setReqIdHeader(this.getReqIdHeader());
        request.setPlatformHeader(this.getPlatformHeader());
        request.setUserIdType(this.getUserIdType());
        request.setUserAnyId(this.getUserAnyId());
        request.setRgb(this.getRgb());
        request.setData(this.getData());
        return request;
    }

}
