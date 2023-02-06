package ru.yandex.market.wms.picking.modules.service;


import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.model.dto.SortStationHasBeenDefinedResponseDto;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.picking.modules.async.PickingContainerTransportService;
import ru.yandex.market.wms.picking.modules.service.transport.TransportationIntegrationService;
import ru.yandex.market.wms.transportation.client.TransportationClient;
import ru.yandex.market.wms.transportation.core.model.response.GetTransportOrdersResponseWithCursor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants.YM_ASYNC_TO_CONV_ON;

public class PickingContainerTransportServiceTest extends IntegrationTest {

    @Autowired
    @MockBean
    private JmsTemplate jmsTemplate;

    @Autowired
    @SpyBean
    private TransportationIntegrationService transportationService;


    @Autowired
    @MockBean
    private TransportationClient transportationClient;
    @Autowired
    private PickingContainerTransportService service;


    @Test
    @DatabaseSetup(value = "/service/container-async-to-create/before.xml")
    public void createTOAsync() {
        SortStationHasBeenDefinedResponseDto responseDto = SortStationHasBeenDefinedResponseDto
                .builder()
                .containerId("PLT123")
                .selectedLine("INDUCT-1")
                .sortStation("S01")
                .orders(Set.of("ORD0777"))
                .build();

        Mockito.when(transportationClient.getTransportOrders(anyInt(), anyString(), any(), any(), any()))
                .thenReturn(GetTransportOrdersResponseWithCursor.buildWithCursor()
                        .content(Collections.emptyList())
                        .build());


        service.processStationDefineCallback(responseDto);

        Mockito.verify(transportationService, Mockito.atLeastOnce())
                .createTransportOrderWithCheckPath(any(), Mockito.eq("PLT123"), any());
        Mockito.verify(jmsTemplate, Mockito.atLeastOnce())
                .convertAndSend(Mockito.eq(YM_ASYNC_TO_CONV_ON), (Object) any());
    }
}
