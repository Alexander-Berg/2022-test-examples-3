package ru.yandex.market.tpl.core.service.routing;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.api.model.routing.PartnerRoutingParamDto;
import ru.yandex.market.tpl.api.model.routing.RoutingStatusType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.external.routing.api.RoutingApiEvent;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponse;
import ru.yandex.market.tpl.core.external.routing.vrp.model.SolutionMetrics;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED;
import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@RequiredArgsConstructor
class PartnerRoutingServiceTest extends TplAbstractTest {

    private final PartnerRoutingService partnerRoutingService;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final RoutingLogDao routingLogDao;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    @AfterEach
    void tearDown() {
        Mockito.reset(configurationProviderAdapter);
    }

    @Test
    @Sql("classpath:mockRoutingResult/oneResultWithoutDroppedPoint.sql")
    void findRoutingInfo_searchForNotPublished() {
        PartnerRoutingParamDto params = new PartnerRoutingParamDto();
        params.setSortingCenterIds(Set.of(SortingCenter.DEFAULT_SC_ID));
        params.setRoutingStatusType(RoutingStatusType.NOT_PUBLISHED);
        var result = partnerRoutingService.findRoutingInfo(
                params, Pageable.unpaged()
        );

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findRoutingInfo_whenTwoLogsWithSameProcessingId_newVersion() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED))
                .thenReturn(true);

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
                .withProfileType(RoutingProfileType.GROUP_DELAYED);
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest2, routingRequest2.getProfileType()));
        routingLogDao.updateAtProcessingStart(routingRequest2.getRequestId(), processingId);
        routingLogDao.updateRawResponse(new RoutingApiEvent.ResponseReceived(routingRequest2.getRequestId(),
                rawResponse));

        routingLogDao.updateAtFinished(
                routingRequest2.getRequestId(),
                routingApiDataHelper.mockResult(routingRequest2, false),
                rawResponse);
        routingLogDao.updatePublishingStatusByRequestId(routingRequest2.getRequestId(), RoutingResultStatus.SUCCESS);

        //when
        var result = partnerRoutingService.publishRoutingAsync(processingId);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getRoutingId()).isEqualTo(processingId);
        assertThat(result.getStrategy()).isEqualTo(ru.yandex.market.tpl.api.model.routing.RoutingProfileType.GROUP_DELAYED);
    }

    //TODO выпилить в рамках MARKETTPL-10494
    @Test
    void findRoutingInfo_whenTwoLogsWithSameProcessingId_oldVersion() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED))
                .thenReturn(false);

        var rawResponse = new MvrpResponse();
        rawResponse.setMetrics(OBJECT_GENERATOR.nextObject(SolutionMetrics.class));
        var processingId = UUID.randomUUID().toString();

        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, LocalDate.now(), 77, 0)
                .withProfileType(RoutingProfileType.GROUP_DELAYED);
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));
        routingLogDao.updateAtProcessingStart(routingRequest.getRequestId(), processingId);
        routingLogDao.updateRawResponse(new RoutingApiEvent.ResponseReceived(routingRequest.getRequestId(),
                rawResponse));

        routingLogDao.updateAtFinished(
                routingRequest.getRequestId(),
                routingApiDataHelper.mockResult(routingRequest, false),
                rawResponse);
        routingLogDao.updatePublishingStatusByRequestId(routingRequest.getRequestId(), RoutingResultStatus.SUCCESS);

        //when
        var result = partnerRoutingService.publishRoutingAsync(processingId);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getRoutingId()).isEqualTo(processingId);
        assertThat(result.getStrategy()).isEqualTo(ru.yandex.market.tpl.api.model.routing.RoutingProfileType.GROUP_DELAYED);
    }


}
