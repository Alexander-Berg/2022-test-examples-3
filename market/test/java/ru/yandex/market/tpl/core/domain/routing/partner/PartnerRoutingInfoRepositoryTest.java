package ru.yandex.market.tpl.core.domain.routing.partner;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.external.routing.api.RoutingApiEvent;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponse;
import ru.yandex.market.tpl.core.external.routing.vrp.model.SolutionMetrics;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@RequiredArgsConstructor
class PartnerRoutingInfoRepositoryTest extends TplAbstractTest {
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final RoutingLogDao routingLogDao;
    private final PartnerRoutingInfoRepository partnerRoutingInfoRepository;

    @Test
    void findByRoutingIdLastOne() {
        //given
        var rawResponse = new MvrpResponse();
        rawResponse.setMetrics(OBJECT_GENERATOR.nextObject(SolutionMetrics.class));
        var processingId = UUID.randomUUID().toString();

        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, LocalDate.now(), 77, 0)
                .withProfileType(RoutingProfileType.GROUP);
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));
        routingLogDao.updateAtProcessingStart(routingRequest.getRequestId(), processingId);
        routingLogDao.updateRawResponse(new RoutingApiEvent.ResponseReceived(routingRequest.getRequestId(),
                rawResponse));
        RoutingRequest routingRequest2 = routingApiDataHelper.getRoutingRequest(234, LocalDate.now(), 88, 0)
                .withProfileType(RoutingProfileType.GROUP_FALLBACK_1);
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest2, routingRequest2.getProfileType()));
        routingLogDao.updateAtProcessingStart(routingRequest2.getRequestId(), processingId);
        routingLogDao.updateRawResponse(new RoutingApiEvent.ResponseReceived(routingRequest2.getRequestId(),
                rawResponse));

        //when
        Optional<PartnerRoutingInfo> result = partnerRoutingInfoRepository.findByRoutingIdLastOne(processingId);

        //then
        assertThat(result).isPresent();
        assertThat(result.get().getRequestId()).isEqualTo(routingRequest2.getRequestId());
    }
}
