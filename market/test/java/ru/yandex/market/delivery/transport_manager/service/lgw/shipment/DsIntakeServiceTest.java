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
import ru.yandex.market.logistic.gateway.common.model.delivery.Intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DsIntakeServiceTest {
    @Mock
    private DeliveryClient lgwDeliveryClient;

    @Test
    public void createIntakeTest() throws GatewayApiException {
        DsIntakeService dsIntakeService = new DsIntakeService(
            lgwDeliveryClient,
            new LgwDsShipmentConverter(
                new LgwDsResourceIdConverter(new IdPrefixConverter())),
            new WeightVolumeConverter()
        );

        TransportationUnit outboundUnit = new TransportationUnit()
            .setId(3L)
            .setExternalId("externalId2")
            .setLogisticPointId(6L)
            .setPartnerId(10L)
            .setPlannedIntervalStart(LocalDateTime.now())
            .setPlannedIntervalEnd(LocalDateTime.now().plusHours(1));

        LogisticsPointMetadata logisticsPointMetadata =
            new LogisticsPointMetadata()
                .setSchedules(Collections.emptySet())
                .setTransportationUnitId(outboundUnit.getId());
        TransportationMetadata transportationMetadata = new TransportationMetadata().setAddressFrom(new Address());

        dsIntakeService.createShipment(
            new TransportationUnit().setPartnerId(12L).setId(2L),
            logisticsPointMetadata,
            transportationMetadata,
            ConverterUtil.getShipmentInterval(outboundUnit),
            110000,
            3400,
            null
        );

        ArgumentCaptor<Intake> captor = ArgumentCaptor.forClass(Intake.class);
        verify(lgwDeliveryClient, times(1)).createIntake(captor.capture(), any());
        assertThat(captor.getValue().getVolume()).isEqualTo(0.11f);
        assertThat(captor.getValue().getWeight()).isEqualTo(3.4f);
    }
}
