package ru.yandex.market.logistics.management.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.management.domain.converter.PartnerCapacityConverter;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacity;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacityDayOff;
import ru.yandex.market.logistics.management.domain.specification.PartnerDayOffSpecification;
import ru.yandex.market.logistics.management.repository.PartnerCapacityDayOffRepository;
import ru.yandex.market.logistics.management.repository.PartnerCapacityRepository;
import ru.yandex.market.logistics.management.repository.querydsl.QuerydslBuilder;
import ru.yandex.market.logistics.management.service.client.PartnerCapacityDayOffService;
import ru.yandex.market.logistics.management.service.combinator.LogisticServiceCapacityService;
import ru.yandex.market.logistics.management.util.tskv.CapacityDayOffHistoryLogger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит тест проверки вызова capacityDayOffHistoryLogger через PartnerCapacityDayoffService")
@ParametersAreNonnullByDefault
public class PartnerCapacityDayoffServiceTest {

    private PartnerCapacityDayOffService partnerCapacityDayOffService;

    @Mock
    private PartnerCapacityRepository partnerCapacityRepository;

    @Mock
    private PartnerCapacityDayOffRepository partnerCapacityDayOffRepository;

    @Mock
    private QuerydslBuilder querydslBuilder;

    @Mock
    private PartnerCapacityConverter partnerCapacityConverter;

    @Mock
    private PartnerDayOffSpecification partnerDayOffSpecification;

    @Mock
    private CapacityDayOffHistoryLogger capacityDayOffHistoryLogger;

    @Mock
    private LogisticServiceCapacityService logisticServiceCapacityService;

    @Mock
    private Clock clock;

    @BeforeEach
    void before() {
        partnerCapacityDayOffService = new PartnerCapacityDayOffService(
            partnerCapacityRepository,
            partnerCapacityDayOffRepository,
            querydslBuilder,
            partnerCapacityConverter,
            partnerDayOffSpecification,
            capacityDayOffHistoryLogger,
            logisticServiceCapacityService,
            clock
        );
    }

    @Test
    @DisplayName("Проверка вызова логгера при создании дейоффа")
    void capacityDayOffCreateLoggerCallTest() {
        PartnerCapacity partnerCapacity = new PartnerCapacity();
        partnerCapacity.setId(1L);
        when(partnerCapacityRepository.findById(1L))
            .thenReturn(Optional.of(partnerCapacity));
        when(partnerCapacityDayOffRepository.save(any())).thenReturn(new PartnerCapacityDayOff());
        partnerCapacityDayOffService.createCapacityDayOff(1L, LocalDate.MIN);
        ArgumentCaptor<PartnerCapacityDayOff> partnerCapacityDayOffArgumentCaptor =
            ArgumentCaptor.forClass(PartnerCapacityDayOff.class);
        verify(partnerCapacityDayOffRepository).save(partnerCapacityDayOffArgumentCaptor.capture());
        PartnerCapacityDayOff value = partnerCapacityDayOffArgumentCaptor.getValue();
        Assertions.assertThat(value)
            .extracting(PartnerCapacityDayOff::getCapacity)
            .extracting(PartnerCapacity::getId)
            .isEqualTo(partnerCapacity.getId());
        Assertions.assertThat(value)
            .extracting(PartnerCapacityDayOff::getDay)
            .isEqualTo(LocalDate.MIN);
        verify(capacityDayOffHistoryLogger).logDayOffHappenRow(value);
    }

    @Test
    @DisplayName("Проверка вызова логгера при удалении дейоффа")
    void capacityDayOffDeleteLoggerCallTest() {
        PartnerCapacityDayOff partnerCapacityDayOff = getPartnerCapacityDayOff();
        PartnerCapacity partnerCapacity = getPartnerCapacity(partnerCapacityDayOff);
        when(partnerCapacityRepository.findById(1L))
            .thenReturn(Optional.of(partnerCapacity));
        partnerCapacityDayOffService.deleteCapacityDayOff(1L, LocalDate.MIN);
        verify(capacityDayOffHistoryLogger).logDayOffRevertRow(partnerCapacityDayOff);
    }

    @Nonnull
    private PartnerCapacity getPartnerCapacity(PartnerCapacityDayOff partnerCapacityDayOff) {
        PartnerCapacity partnerCapacity = new PartnerCapacity();
        partnerCapacity.addPartnerCapacityDayOff(partnerCapacityDayOff);
        return partnerCapacity;
    }

    @Nonnull
    private PartnerCapacityDayOff getPartnerCapacityDayOff() {
        PartnerCapacityDayOff partnerCapacityDayOff = new PartnerCapacityDayOff();
        partnerCapacityDayOff.setDay(LocalDate.MIN);
        return partnerCapacityDayOff;
    }
}

