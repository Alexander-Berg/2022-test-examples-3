package ru.yandex.market.logistics.management.service;

import java.time.LocalDate;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.management.domain.converter.PartnerCapacityConverter;
import ru.yandex.market.logistics.management.domain.dto.front.capacity.PartnerCapacityUpdateDto;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacity;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.specification.PartnerCapacitySpecification;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.exception.BadRequestException;
import ru.yandex.market.logistics.management.repository.PartnerCapacityRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.repository.PlatformClientRepository;
import ru.yandex.market.logistics.management.repository.querydsl.QuerydslBuilder;
import ru.yandex.market.logistics.management.service.client.PartnerCapacityService;
import ru.yandex.market.logistics.management.service.combinator.LogisticServiceCapacityService;
import ru.yandex.market.logistics.management.service.validation.PartnerCapacityValidationService;
import ru.yandex.market.logistics.management.util.tskv.CapacityDayOffHistoryLogger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит тест проверки вызова capacityDayOffHistoryLogger через PartnerCapacityService")
public class PartnerCapacityServiceTest {

    private PartnerCapacityService partnerCapacityService;

    @Mock
    private PartnerCapacityRepository partnerCapacityRepository;

    @Mock
    private QuerydslBuilder querydslBuilder;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private PlatformClientRepository platformClientRepository;

    @Mock
    private PartnerCapacityConverter partnerCapacityConverter;

    @Mock
    private PartnerCapacitySpecification partnerCapacitySpecification;

    @Mock
    private CapacityDayOffHistoryLogger capacityDayOffHistoryLogger;

    @Mock
    private LogisticServiceCapacityService serviceCapacityService;

    @BeforeEach
    public void before() {
        PartnerCapacityValidationService capacityValidationService = new PartnerCapacityValidationService();
        partnerCapacityService = new PartnerCapacityService(
            partnerCapacityRepository,
            querydslBuilder,
            partnerRepository,
            platformClientRepository,
            partnerCapacityConverter,
            partnerCapacitySpecification,
            capacityValidationService,
            capacityDayOffHistoryLogger,
            serviceCapacityService
        );
    }

    @Test
    @DisplayName("Проверка вызова логгера при обновлении размера капасити")
    public void testUpdateValueLoggerCallTest() {
        PartnerCapacity partnerCapacity = new PartnerCapacity();
        Partner partner = new Partner();
        partner.setId(1L);
        partnerCapacity.setPartner(partner);
        when(partnerCapacityRepository.findById(1L)).thenReturn(Optional.of(partnerCapacity));
        PlatformClient platformClient = new PlatformClient();
        platformClient.setId(1L);
        when(platformClientRepository.findById(1L)).thenReturn(Optional.of(platformClient));
        partnerCapacityService.update(
            1L,
            new PartnerCapacityUpdateDto(
                1,
                1,
                CapacityType.REGULAR,
                DeliveryType.COURIER,
                CountingType.ITEM,
                null,
                1L,
                LocalDate.MIN,
                1L
            )
        );
        verify(capacityDayOffHistoryLogger).logCapacityChangeRow(partnerCapacity);
    }

    @Test
    @DisplayName("Проверка вызова логгера при создании капасити")
    public void testCreateLoggerCallTest() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(new Partner()));
        when(platformClientRepository.findById(1L)).thenReturn(Optional.of(new PlatformClient()));
        when(partnerCapacityRepository.save(any())).thenReturn(new PartnerCapacity());
        partnerCapacityService.create(PartnerCapacityDto.newBuilder().partnerId(1L).platformClientId(1L).build());
        ArgumentCaptor<PartnerCapacity> capacityArgumentCaptor = ArgumentCaptor.forClass(PartnerCapacity.class);
        verify(partnerCapacityRepository).save(capacityArgumentCaptor.capture());
        PartnerCapacity partnerCapacity = new PartnerCapacity();
        partnerCapacity.setPartner(new Partner());
        partnerCapacity.setCountingType(CountingType.ORDER);
        partnerCapacity.setServiceType(CapacityService.SHIPMENT);
        partnerCapacity.setPlatformClient(new PlatformClient());
        Assertions.assertThat(capacityArgumentCaptor.getValue()).isEqualToComparingFieldByField(partnerCapacity);
        verify(capacityDayOffHistoryLogger).logCapacityChangeRow(partnerCapacity);
    }

    @Test
    @DisplayName("Проверка вызова логгера при обновлении капасити")
    public void testUpdateLoggerCallTest() {
        PartnerCapacity partnerCapacity = new PartnerCapacity();
        partnerCapacity.setPartner(new Partner());
        when(partnerCapacityRepository.findById(1L)).thenReturn(Optional.of(partnerCapacity));
        when(platformClientRepository.findById(1L)).thenReturn(Optional.of(new PlatformClient()));
        partnerCapacityService.update(1L, new PartnerCapacityUpdateDto(
            1,
            1,
            CapacityType.REGULAR,
            DeliveryType.COURIER,
            CountingType.ITEM,
            null,
            1L,
            LocalDate.MIN,
            1L
        ));
        verify(capacityDayOffHistoryLogger).logCapacityChangeRow(partnerCapacity);
    }

    @Test
    @DisplayName("Проверка locationFrom для DELIVERY капасити")
    public void testCheckingFromToLocationsForDeliveryCapacityTest() {
        Assertions.assertThatThrownBy(() ->
            partnerCapacityService.update(1L, new PartnerCapacityUpdateDto(
                1,
                1,
                CapacityType.REGULAR,
                DeliveryType.COURIER,
                CountingType.ITEM,
                CapacityService.DELIVERY,
                1L,
                LocalDate.MIN,
                1L
            ))
        )
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Некорректное значение location_from для капасити с типом Доставка."
                + " Допустимое значение: 225");
    }
}
