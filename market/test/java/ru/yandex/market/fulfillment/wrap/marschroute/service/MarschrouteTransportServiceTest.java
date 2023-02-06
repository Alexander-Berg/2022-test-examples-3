package ru.yandex.market.fulfillment.wrap.marschroute.service;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yandex.market.fulfillment.wrap.marschroute.api.TransportClient;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.transport.TransportResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.transport.TransportResponseData;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.transport.TransportResponseParams;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarschrouteTransportServiceTest extends IntegrationTest {

    @MockBean
    private TransportClient transportClient;

    @Autowired
    private MarschrouteTransportService transportService;

    @Test
    void testSyncTransportInfo() {
        final LocalDate dateArrive = LocalDate.now();
        final TransportResponse response = createResponseWithOneItem();
        when(transportClient.getTransport(any(MarschrouteDate.class))).thenReturn(response);

        transportService.syncTransportInfo(dateArrive);

        verify(transportClient, times(1)).getTransport(any(MarschrouteDate.class));
        verify(transportRepository, times(1))
            .deleteByDateArrive(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(transportRepository, times(1)).saveAll(any(Iterable.class));
    }

    private TransportResponse createResponseWithOneItem() {
        TransportResponseData data = createTransportResponseDataHeavyTruck();
        TransportResponseParams params = new TransportResponseParams();
        params.setTotal(1);

        TransportResponse response = new TransportResponse();
        response.setSuccess(true);
        response.setData(Collections.singletonList(data));
        response.setParams(params);
        return response;
    }

    private TransportResponseData createTransportResponseDataHeavyTruck() {
        TransportResponseData data = new TransportResponseData();
        data.setTicketId(1);
        data.setDateArrive(MarschrouteDateTime.create("05.04.2018 18:55:56"));
        data.setNumberPlate("777");
        data.setTransportType(4);
        data.setPurpose(1);
        data.setStatus(16);
        data.setDateStatus(MarschrouteDateTime.create("05.04.2018 19:36:33"));
        data.setDateGate(MarschrouteDateTime.create("05.04.2018 20:23:17"));
        data.setDateComplete(MarschrouteDateTime.create("05.04.2018 21:45:18"));
        data.setDocsId(Lists.newArrayList(1523, 6478, 2884, 2226));
        data.setOrdersId(Lists.newArrayList("8473", "2562", "7842", "745"));
        return data;
    }
}
