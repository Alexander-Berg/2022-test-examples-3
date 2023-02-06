package ru.yandex.market.tpl.core.service.delivery.ds.proxy;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.Inbound;
import ru.yandex.market.logistic.api.model.common.InboundStatus;
import ru.yandex.market.logistic.api.model.common.InboundStatusHistory;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.logistic.api.model.common.Outbound;
import ru.yandex.market.logistic.api.model.common.OutboundStatus;
import ru.yandex.market.logistic.api.model.common.OutboundStatusHistory;
import ru.yandex.market.logistic.api.model.common.OutboundType;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.Status;
import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.logistic.api.model.delivery.request.GetInboundRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetInboundStatusHistoryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetInboundStatusRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOutboundRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOutboundStatusHistoryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOutboundStatusRequest;
import ru.yandex.market.logistic.api.model.delivery.request.PutInboundRegistryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.PutInboundRequest;
import ru.yandex.market.logistic.api.model.delivery.request.PutOutboundRegistryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.PutOutboundRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundRegistryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutOutboundRegistryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutOutboundResponse;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.external.delivery.sc.ScClient;
import ru.yandex.market.tpl.core.service.delivery.LogisticApiRequestProcessingConfiguration;
import ru.yandex.market.tpl.core.service.delivery.ds.DsRequestReader;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.inbound.GetInboundDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.inbound.GetInboundStatusDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.inbound.GetInboundStatusHistoryDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.inbound.PutInboundDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.inbound.PutInboundRegistryDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.outbound.GetOutboundDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.outbound.GetOutboundStatusDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.outbound.GetOutboundStatusHistoryDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.outbound.PutOutboundDsApiProcessor;
import ru.yandex.market.tpl.core.service.delivery.ds.proxy.outbound.PutOutboundRegistryDsApiProcessor;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
@ContextConfiguration(classes = {
        LogisticApiRequestProcessingConfiguration.class
})
public class OpkoProxyTest {

    private final PutInboundDsApiProcessor putInboundDsApiProcessor;
    private final PutInboundRegistryDsApiProcessor putInboundRegistryDsApiProcessor;
    private final GetInboundDsApiProcessor getInboundDsApiProcessor;
    private final GetInboundStatusDsApiProcessor getInboundStatusDsApiProcessor;
    private final GetInboundStatusHistoryDsApiProcessor getInboundStatusHistoryDsApiProcessor;

    private final PutOutboundDsApiProcessor putOutboundDsApiProcessor;
    private final PutOutboundRegistryDsApiProcessor putOutboundRegistryDsApiProcessor;
    private final GetOutboundDsApiProcessor getOutboundDsApiProcessor;
    private final GetOutboundStatusDsApiProcessor getOutboundStatusDsApiProcessor;
    private final GetOutboundStatusHistoryDsApiProcessor getOutboundStatusHistoryDsApiProcessor;

    private final PartnerRepository<DeliveryService> dsRepository;
    private final PartnerRepository<SortingCenter> scRepository;
    private final DsRequestReader dsRequestReader;

    @MockBean
    private ScClient scClient;
    @MockBean
    private Clock clock;

    private DeliveryService dsPartner;
    private SortingCenter scPartner;

    void setUp() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        dsPartner = dsRepository.findByIdOrThrow(100500L);
        scPartner = scRepository.findByIdOrThrow(100501L);
        mockInboundScRequests();
        mockOutboundScRequests();
    }

    private void mockInboundScRequests() {
        doReturn(new PutInboundResponse(new ResourceId("1", "2")))
                .when(scClient).putInbound(any(), any(), eq(scPartner.getToken()));
        doReturn(new PutInboundRegistryResponse(new ResourceId("1", "2")))
                .when(scClient).putInboundRegistry(any(), eq(scPartner.getToken()));
        doReturn(new GetInboundResponse(
                new Inbound(
                        new ResourceId("1", "2"),
                        InboundType.CROSSDOCK,
                        new DateTimeInterval(
                                OffsetDateTime.now(clock),
                                OffsetDateTime.now(clock)
                        ),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                null,
                null)
        )
                .when(scClient).getInbound(any(), eq(scPartner.getToken()));
        doReturn(new GetInboundStatusResponse(List.of(new InboundStatus(
                new ResourceId("1", "2"),
                Status.builder(StatusCode.ACCEPTED, DateTime.fromOffsetDateTime(OffsetDateTime.now(clock))).build()
        ))))
                .when(scClient).getInboundStatus(any(), eq(scPartner.getToken()));
        doReturn(new GetInboundStatusHistoryResponse(List.of(new InboundStatusHistory(
                new ResourceId("1", "2"),
                List.of(Status.builder(StatusCode.ACCEPTED,
                        DateTime.fromOffsetDateTime(OffsetDateTime.now(clock))).build()))
        )))
                .when(scClient).getInboundStatusHistory(any(), eq(scPartner.getToken()));
    }

    private void mockOutboundScRequests() {
        doReturn(new PutOutboundResponse(new ResourceId("1", "2")))
                .when(scClient).putOutbound(any(), eq(scPartner.getToken()));
        doReturn(new PutOutboundRegistryResponse(new ResourceId("1", "2")))
                .when(scClient).putOutboundRegistry(any(), eq(scPartner.getToken()));
        doReturn(new GetOutboundResponse(new Outbound(
                new ResourceId("1", "2"), OutboundType.XDOC,
                new DateTimeInterval(OffsetDateTime.now(clock), OffsetDateTime.now(clock)),
                null, null, null, null, null
        ), null))
                .when(scClient).getOutbound(any(), eq(scPartner.getToken()));
        doReturn(new GetOutboundStatusResponse(List.of(new OutboundStatus(
                new ResourceId("1", "2"),
                Status.builder(StatusCode.ACCEPTED, DateTime.fromOffsetDateTime(OffsetDateTime.now(clock))).build()
        ))))
                .when(scClient).getOutboundStatus(any(), eq(scPartner.getToken()));
        doReturn(new GetOutboundStatusHistoryResponse(List.of(new OutboundStatusHistory(
                new ResourceId("1", "2"),
                List.of(Status.builder(StatusCode.ACCEPTED,
                        DateTime.fromOffsetDateTime(OffsetDateTime.now(clock))).build()))
        )))
                .when(scClient).getOutboundStatusHistory(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void putInbound() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/putInbound.xml",
                PutInboundRequest.class
        );
        putInboundDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).putInbound(any(), any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void putInboundRegistry() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/putInboundRegistry.xml",
                PutInboundRegistryRequest.class
        );
        putInboundRegistryDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).putInboundRegistry(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void getInbound() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/getInbound.xml",
                GetInboundRequest.class
        );
        getInboundDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).getInbound(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void getInboundStatus() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/getInboundStatus.xml",
                GetInboundStatusRequest.class
        );
        getInboundStatusDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).getInboundStatus(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void getInboundStatusHistory() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/getInboundStatusHistory.xml",
                GetInboundStatusHistoryRequest.class
        );
        getInboundStatusHistoryDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).getInboundStatusHistory(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void putOutbound() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/putOutbound.xml",
                PutOutboundRequest.class
        );
        putOutboundDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).putOutbound(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void putOutboundRegistry() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/putOutboundRegistry.xml",
                PutOutboundRegistryRequest.class
        );
        putOutboundRegistryDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).putOutboundRegistry(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void getOutbound() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/getOutbound.xml",
                GetOutboundRequest.class
        );
        getOutboundDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).getOutbound(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void getOutboundStatus() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/getOutboundStatus.xml",
                GetOutboundStatusRequest.class
        );
        getOutboundStatusDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).getOutboundStatus(any(), eq(scPartner.getToken()));
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void getOutboundStatusHistory() throws Exception {
        setUp();
        var request = dsRequestReader.readRequest(
                "/ds/opko/getOutboundStatusHistory.xml",
                GetOutboundStatusHistoryRequest.class
        );
        getOutboundStatusHistoryDsApiProcessor.apiCall(request, dsPartner);
        verify(scClient).getOutboundStatusHistory(any(), eq(scPartner.getToken()));
    }


}
