package ru.yandex.market.logistic.api.model.delivery.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class UpdateItemsInstancesRequestParsingTest extends
    ParsingWrapperTest<RequestWrapper, UpdateItemsInstancesRequest> {
    public UpdateItemsInstancesRequestParsingTest() {
        super(
            RequestWrapper.class,
            UpdateItemsInstancesRequest.class,
            "fixture/request/ds_update_items_instances.xml"
        );
    }
}
