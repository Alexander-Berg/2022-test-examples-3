package ru.yandex.market.pers.basket.controller.v2;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ifilippov5
 */
public class BasketV2GetTestRequest extends BasketV2TestRequest {

    private String page;
    private String pageSize;
    private String offset;
    private String limit;
    private List<BasketV2PostTestRequest.BasketRefItem> refs = Collections.emptyList();

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public List<BasketV2PostTestRequest.BasketRefItem> getRefs() {
        return refs;
    }

    public void setRefs(List<BasketV2PostTestRequest.BasketRefItem> refs) {
        this.refs = refs;
    }

    public BasketV2GetTestRequest clone() {
        BasketV2GetTestRequest request = new BasketV2GetTestRequest();
        request.setReqIdHeader(this.getReqIdHeader());
        request.setPlatformHeader(this.getPlatformHeader());
        request.setUserIdType(this.getUserIdType());
        request.setUserAnyId(this.getUserAnyId());
        request.setRgb(this.getRgb());
        request.setPage(this.getPage());
        request.setPageSize(this.getPageSize());
        request.setOffset(this.getOffset());
        request.setLimit(this.getLimit());
        request.setRefs(this.getRefs().stream()
            .map(BasketV2PostTestRequest.BasketRefItem::clone)
            .collect(Collectors.toList())
        );
        return request;
    }

}
