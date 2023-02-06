package ru.yandex.market.pers.basket.controller.v2;

/**
 * @author maratik
 */
public class CategoryGetTestRequest extends BasketV2GetTestRequest {
    public CategoryGetTestRequest clone() {
        CategoryGetTestRequest request = new CategoryGetTestRequest();
        request.setReqIdHeader(this.getReqIdHeader());
        request.setPlatformHeader(this.getPlatformHeader());
        request.setUserIdType(this.getUserIdType());
        request.setUserAnyId(this.getUserAnyId());
        request.setRgb(this.getRgb());
        return request;
    }
}
