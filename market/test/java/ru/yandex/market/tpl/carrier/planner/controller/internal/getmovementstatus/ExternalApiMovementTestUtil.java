package ru.yandex.market.tpl.carrier.planner.controller.internal.getmovementstatus;

import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.request.AbstractRequest;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.delivery.request.GetMovementStatusHistoryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetMovementStatusRequest;

@UtilityClass
public class ExternalApiMovementTestUtil {

    public static <T extends AbstractRequest> RequestWrapper<T> wrap(T request) {
        return new RequestWrapper<>(new Token("ds_token"), "aa", "bb", request);
    }

    public static GetMovementStatusRequest prepareGetMovementStatus(List<ResourceId> movementIds) {
        return new GetMovementStatusRequest(movementIds);
    }

    public static GetMovementStatusHistoryRequest prepareGetMovementStatusHistory(List<ResourceId> movementIds) {
        return new GetMovementStatusHistoryRequest(movementIds);
    }

}
