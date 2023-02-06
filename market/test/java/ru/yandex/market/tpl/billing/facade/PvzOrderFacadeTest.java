package ru.yandex.market.tpl.billing.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.pvz.client.billing.dto.BillingOrderDto;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.tpl.billing.converter.PvzOrderConverter;
import ru.yandex.market.tpl.billing.dao.PvzOrderDao;
import ru.yandex.market.tpl.billing.model.entity.PvzOrder;
import ru.yandex.market.tpl.billing.model.yt.YtOnDeliveryPaymentDto;
import ru.yandex.market.tpl.billing.queue.ytExport.YtExportDataProducer;
import ru.yandex.market.tpl.billing.repository.EnvironmentRepository;
import ru.yandex.market.tpl.billing.repository.PvzOrderRepository;
import ru.yandex.market.tpl.billing.service.PvzOrderImportService;
import ru.yandex.market.tpl.billing.service.yt.exports.YtBillingExportService;
import ru.yandex.market.tpl.common.util.EnumConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: Replace by the DBUnit test (https://st.yandex-team.ru/MARKETTPLBILL-82).
@ExtendWith(SpringExtension.class)
class PvzOrderFacadeTest {

    private static final long BATCH_SIZE = 20000;
    @Mock
    private PvzClient pvzClient;
    @Mock
    private PvzOrderRepository pvzOrderRepository;
    @Mock
    private PvzOrderDao pvzOrderDao;
    @Mock
    private YtBillingExportService<YtOnDeliveryPaymentDto> ytPaymentExportService;
    @Mock
    private EnvironmentRepository environmentRepository;
    @Mock
    private YtExportDataProducer ytExportDataProducer;

    @Test
    void importDeliveredOrders() {
        LocalDate date = LocalDate.parse("2020-10-10");
        List<BillingOrderDto> orders = List.of(
            BillingOrderDto.builder()
                .id(1001L)
                .externalId("EXT-1001")
                .deliveryServiceId(107L)
                .paymentStatus("PAID")
                .paymentType("PREPAID")
                .pickupPointId(1000001L)
                .deliveredAt(OffsetDateTime.parse("2021-01-18T10:15:30+00:00"))
                .paymentSum(BigDecimal.valueOf(800.50))
                .deliveryServiceType(DeliveryServiceType.DBS)
                .yandexDelivery(true)
                .build()
        );
        when(environmentRepository.getIntValueOrDefault(anyString(), anyInt())).thenReturn((int) BATCH_SIZE);
        when(pvzClient.getDeliveredOrders(date, date, BATCH_SIZE, 0L)).thenReturn(orders);
        PvzOrderImportService pvzOrderImportService = new PvzOrderImportService(
                pvzClient, pvzOrderRepository, pvzOrderDao, new PvzOrderConverter(new EnumConverter()), environmentRepository, ytExportDataProducer
        );
        pvzOrderImportService.importDeliveredOrders(date, date);

        ArgumentCaptor<ArrayList<PvzOrder>> listCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(pvzOrderDao).saveAllPvzOrders(listCaptor.capture());

        ArrayList<PvzOrder> savedPvzOrders = listCaptor.getValue();
        assertEquals(1, savedPvzOrders.size());
        assertEquals("EXT-1001", savedPvzOrders.get(0).getMarketOrderId());
    }
}
