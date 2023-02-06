package ru.yandex.market.pvz.core.test.factory.mapper;

import one.util.streamex.StreamEx;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequest;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestItem;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestItemParams;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ReturnRequestTestParamsMapper {

    @Mapping(target = "expirationDate", ignore = true)
    public abstract ReturnRequestParams map(TestReturnRequestFactory.ReturnRequestTestParams testParams);

    protected abstract ReturnRequestItemParams map(TestReturnRequestFactory.ReturnRequestItemTestParams testParams);

    protected abstract ReturnRequest mapEntity(TestReturnRequestFactory.ReturnRequestTestParams testParams);

    protected abstract ReturnRequestItem mapEntity(TestReturnRequestFactory.ReturnRequestItemTestParams testParams);

    public ReturnRequestParams mapParams(TestReturnRequestFactory.ReturnRequestTestParams requestTestParams) {
        var returnRequestParams = map(requestTestParams);
        returnRequestParams.setItems(StreamEx.of(requestTestParams.getItems()).map(this::map).toList());
        returnRequestParams.setOrderId(requestTestParams.getExternalOrderId());
        returnRequestParams.setDispatchedAt(requestTestParams.getDispatchedAt());
        returnRequestParams.setArrivedAt(requestTestParams.getArrivedAt());
        return returnRequestParams;
    }

    public ReturnRequest mapToEntity(TestReturnRequestFactory.ReturnRequestTestParams requestTestParams) {
        var returnRequest = mapEntity(requestTestParams);
        returnRequest.setItems(StreamEx.of(requestTestParams.getItems()).map(this::mapEntity).toList());
        return returnRequest;
    }


}
