package ru.yandex.market.tpl.core.test.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.external.routing.api.RoutingApiEvent;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponse;
import ru.yandex.market.tpl.core.external.routing.vrp.model.SolutionMetrics;

import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@Service
@RequiredArgsConstructor
public class TestTplRoutingFactory {

    private final RoutingLogDao routingLogDao;

    public void mockRoutingLogRecord(RoutingRequest routingRequest, RoutingResult routingResult) {
        var rawResponse = new MvrpResponse();
        rawResponse.setMetrics(OBJECT_GENERATOR.nextObject(SolutionMetrics.class));
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));
        routingLogDao.updateAtProcessingStart(routingRequest.getRequestId(), routingResult.getProcessingId());
        routingLogDao.updateRawResponse(new RoutingApiEvent.ResponseReceived(routingRequest.getRequestId(),
                rawResponse));
        routingLogDao.updateAtFinished(
                routingRequest.getRequestId(),
                routingResult,
                rawResponse);
    }
}
