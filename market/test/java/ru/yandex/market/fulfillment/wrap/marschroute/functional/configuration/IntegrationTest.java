package ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fulfillment.wrap.marschroute.repository.AnomalyInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.DeliveryMappingHistoryRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.DeliveryMappingRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.DeliveryServiceMetaRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.InboundInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.MarschrouteRawServiceRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.MarschrouteServiceRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.OrderInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.OutboundDetailsInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.OutboundDispatchInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.OutboundHistorySyncQueueRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.OutboundInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.OutboundStatusInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.ServiceSyncLogRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.SystemPropertyRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.TransportInfoRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.UpdateProductJobRepository;


public abstract class IntegrationTest extends MarschrouteWrapTest {
    @MockBean
    protected OutboundDetailsInfoRepository outboundDetailsInfoRepository;

    @MockBean
    protected OutboundInfoRepository outboundInfoRepository;

    @MockBean
    protected InboundInfoRepository inboundInfoRepository;

    @MockBean
    protected UpdateProductJobRepository updateProductJobRepository;

    @MockBean
    protected MarschrouteServiceRepository marschrouteServiceRepository;

    @MockBean
    protected MarschrouteRawServiceRepository marschrouteRawServiceRepository;

    @MockBean
    protected ServiceSyncLogRepository serviceSyncLogRepository;

    @MockBean
    protected DeliveryServiceMetaRepository deliveryServiceMetaRepository;

    @MockBean
    protected OrderInfoRepository orderInfoRepository;

    @MockBean
    protected OutboundHistorySyncQueueRepository outboundHistorySyncQueueRepository;

    @MockBean
    protected OutboundStatusInfoRepository outboundStatusInfoRepository;

    @MockBean
    protected OutboundDispatchInfoRepository outboundDispatchInfoRepository;

    @MockBean
    protected JdbcTemplate jdbcTemplate;

    @MockBean
    protected TransportInfoRepository transportRepository;

    @MockBean
    protected SystemPropertyRepository systemPropertyRepository;

    @MockBean
    protected DeliveryMappingRepository deliveryMappingRepository;

    @MockBean
    protected DeliveryMappingHistoryRepository deliveryMappingHistoryRepository;

    @MockBean
    protected AnomalyInfoRepository anomalyInfoRepository;
}
