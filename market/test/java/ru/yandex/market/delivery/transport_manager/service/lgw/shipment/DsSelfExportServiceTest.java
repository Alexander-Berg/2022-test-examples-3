package ru.yandex.market.delivery.transport_manager.service.lgw.shipment;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.transport_manager.converter.lgw.WeightVolumeConverter;
import ru.yandex.market.delivery.transport_manager.converter.lgw.delivery.LgwDsResourceIdConverter;
import ru.yandex.market.delivery.transport_manager.converter.lgw.delivery.LgwDsShipmentConverter;
import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Address;
import ru.yandex.market.delivery.transport_manager.domain.entity.LogisticsPointMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.util.ConverterUtil;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DsSelfExportServiceTest {
    @Mock
    private DeliveryClient lgwDeliveryClient;

    @Test
    public void createSelfExportTest() throws GatewayApiException {
        IdPrefixConverter idPrefixConverter = new IdPrefixConverter();
        LgwDsResourceIdConverter resourceIdConverter = new LgwDsResourceIdConverter(idPrefixConverter);
        DsSelfExportService dsSelfExportService = new DsSelfExportService(
            lgwDeliveryClient,
            new LgwDsShipmentConverter(resourceIdConverter),
            new WeightVolumeConverter()
        );

        TransportationUnit inboundUnit = new TransportationUnit()
            .setId(2L)
            .setExternalId("externalId1")
            .setLogisticPointId(5L)
            .setPartnerId(11L)
            .setPlannedIntervalStart(LocalDateTime.now())
            .setPlannedIntervalEnd(LocalDateTime.now().plusHours(1));

        LogisticsPointMetadata logisticsPointMetadata = new LogisticsPointMetadata()
            .setSchedules(Collections.emptySet())
            .setTransportationUnitId(inboundUnit.getId());
        TransportationMetadata transportationMetadata = new TransportationMetadata().setAddressTo(new Address());

        dsSelfExportService.createShipment(
            inboundUnit,
            logisticsPointMetadata,
            transportationMetadata,
            ConverterUtil.getShipmentInterval(inboundUnit),
            100000,
            3400,
            null
        );

        ArgumentCaptor<SelfExport> captor = ArgumentCaptor.forClass(SelfExport.class);
        verify(lgwDeliveryClient, times(1))
            .createSelfExport(captor.capture(), any());
        assertThat(captor.getValue().getCourier()).isNotNull();
        assertThat(captor.getValue().getVolume()).isEqualTo(0.1f);
        assertThat(captor.getValue().getWeight()).isEqualTo(3.4f);
    }
}
