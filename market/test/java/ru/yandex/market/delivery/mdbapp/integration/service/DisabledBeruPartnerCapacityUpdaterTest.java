package ru.yandex.market.delivery.mdbapp.integration.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.mdbapp.components.queue.order.to.ship.dto.OrderToShipDto;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.CapacityCountersUpdater;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.PartnerCapacityUpdater;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PartnerCapacity;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityCountingType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityType;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderToShipRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PartnerCapacityRepository;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.enums.PlatformClient;
import ru.yandex.market.delivery.mdbapp.util.NumberUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.enums.PlatformClient.BERU;
import static ru.yandex.market.delivery.mdbapp.enums.PlatformClient.YANDEX_DELIVERY;

@Slf4j
@DisplayName("Отключение импорта настроек капасити для заказов Беру")
public class DisabledBeruPartnerCapacityUpdaterTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private LMSClient lmsClient;

    @Mock
    private CapacityCountersUpdater capacityCountersUpdater;

    @Mock
    private PartnerCapacityRepository partnerCapacityRepository;

    @Mock
    private OrderToShipRepository orderToShipRepository;

    @Mock
    private QueueProducer<OrderToShipDto> producer;

    private final FeatureProperties properties = new FeatureProperties();

    private PartnerCapacityUpdater updater;

    @Test
    @DisplayName("Не импортируются только настройки капасити для платформы Беру")
    public void disabledPartnerCapacityImport() {
        PartnerCapacityDto beruCapacity = partnerCapacityDto(1, BERU);
        PartnerCapacityDto ydCapacity = partnerCapacityDto(3, YANDEX_DELIVERY);
        List<PartnerCapacityDto> lmsCapacities = List.of(beruCapacity, ydCapacity);
        List<PartnerCapacity> expectedCapacities = List.of(partnerCapacity(ydCapacity));

        doReturn(1L).when(producer).enqueue(any());
        when(lmsClient.getPartnerCapacities()).thenReturn(lmsCapacities);

        properties.setDisableBeruCapacityCount(true);

        updater = new PartnerCapacityUpdater(
            lmsClient,
            partnerCapacityRepository,
            capacityCountersUpdater,
            orderToShipRepository,
            producer,
            new TestableClock(),
            properties
        );

        updater.updatePartnerCapacities();
        verify(capacityCountersUpdater).updateProcessedCounters(eq(ydCapacity.getPartnerId()), eq(expectedCapacities));
        verify(capacityCountersUpdater).deleteUnusedCapacities(eq(expectedCapacities), eq(List.of()));
        verify(lmsClient).getPartnerCapacities();
        verifyNoMoreInteractions(capacityCountersUpdater, lmsClient);
    }

    @Nonnull
    private PartnerCapacityDto partnerCapacityDto(long partnerId, PlatformClient platformClient) {
        return PartnerCapacityDto.newBuilder()
            .id(partnerId)
            .partnerId(partnerId)
            .day(LocalDate.now())
            .value(10L)
            .locationFrom(1)
            .locationTo(213)
            .platformClientId(platformClient.getId())
            .build();
    }

    @Nonnull
    private PartnerCapacity partnerCapacity(PartnerCapacityDto dto) {
        return new PartnerCapacity()
            .setCapacityId(dto.getId())
            .setPartnerId(dto.getPartnerId())
            .setLocationFromId(NumberUtils.convertAnyNumberToLongNullSafely(dto.getLocationFrom()).orElse(null))
            .setLocationToId(NumberUtils.convertAnyNumberToLongNullSafely(dto.getLocationTo()).orElse(null))
            .setPlatformClientId(dto.getPlatformClientId())
            .setValue(dto.getValue())
            .setCountingType(Optional.ofNullable(dto.getCountingType())
                .map(CountingType::getName)
                .map(CapacityCountingType::valueOf)
                .orElse(null))
            .setDay(dto.getDay())
            .setCapacityType(CapacityType.REGULAR);
    }

}
