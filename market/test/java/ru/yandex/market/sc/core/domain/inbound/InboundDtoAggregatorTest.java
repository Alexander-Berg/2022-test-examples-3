package ru.yandex.market.sc.core.domain.inbound;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sc.core.domain.inbound.model.PreparedInboundPartnerDto;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mors741
 */
class InboundDtoAggregatorTest {

    // RAW

    private static final PreparedInboundPartnerDto INBOUND_101_SATURN = PreparedInboundPartnerDto.builder()
            .id(101L)
            .externalId("ext-101")
            .externalIdOrInformationListCode("101")
            .inboundStatus(InboundStatus.ARRIVED)
            .car("XX101XX77")
            .startedAt(LocalDateTime.parse("2021-06-29T01:00:00"))
            .finishedAt(null)
            .arrivalIntervalStart(LocalDateTime.parse("2021-06-29T00:40:00"))
            .arrivalIntervalEnd(LocalDateTime.parse("2021-06-29T01:20:00"))
            .supplierName("Снигур Надежда Борисовна")
            .courierName("САТУРН")
            .courierId(1L)
            .totalAmount(700L)
            .acceptedAmount(700L)
            .plannedBoxAmount(300L)
            .boxAmount(300L)
            .plannedPalletAmount(400L)
            .palletAmount(400L)
            .build();

    private static final PreparedInboundPartnerDto INBOUND_102_SATURN = PreparedInboundPartnerDto.builder()
            .id(102L)
            .externalId("ext-102")
            .externalIdOrInformationListCode("102")
            .inboundStatus(InboundStatus.ARRIVED)
            .car("XX101XX77")
            .startedAt(LocalDateTime.parse("2021-06-29T01:00:00"))
            .finishedAt(null)
            .arrivalIntervalStart(LocalDateTime.parse("2021-06-29T00:30:00"))
            .arrivalIntervalEnd(LocalDateTime.parse("2021-06-29T01:00:00"))
            .supplierName("Мельникова Екатерина")
            .courierName("САТУРН")
            .courierId(1L)
            .totalAmount(700L)
            .plannedPalletAmount(100L)
            .build();

    private static final PreparedInboundPartnerDto INBOUND_103_PRAGMATIKA = PreparedInboundPartnerDto.builder()
            .id(103L)
            .externalId("ext-103")
            .externalIdOrInformationListCode("103")
            .inboundStatus(InboundStatus.CREATED)
            .car("XX103XX77")
            .startedAt(LocalDateTime.parse("2021-06-29T02:00:00"))
            .finishedAt(null)
            .arrivalIntervalStart(LocalDateTime.parse("2021-06-29T01:00:00"))
            .arrivalIntervalEnd(LocalDateTime.parse("2021-06-29T02:20:00"))
            .supplierName("Ашихмин Сергей")
            .courierName("ПРАГМАТИКА")
            .courierId(2L)
            .totalAmount(700L)
            .build();

    private static final PreparedInboundPartnerDto INBOUND_104_PRAGMATIKA = PreparedInboundPartnerDto.builder()
            .id(104L)
            .externalId("ext-104")
            .externalIdOrInformationListCode("104")
            .inboundStatus(InboundStatus.FIXED)
            .car("XX103XX77")
            .startedAt(LocalDateTime.parse("2021-06-29T00:30:00"))
            .finishedAt(LocalDateTime.parse("2021-06-29T00:40:00"))
            .arrivalIntervalStart(LocalDateTime.parse("2021-06-29T00:40:00"))
            .arrivalIntervalEnd(LocalDateTime.parse("2021-06-29T01:20:00"))
            .supplierName("Зуев Владислав")
            .courierName("ПРАГМАТИКА")
            .courierId(2L)
            .totalAmount(100L)
            .acceptedAmount(100L)
            .plannedPalletAmount(1L)
            .palletAmount(1L)
            .build();

    private static final PreparedInboundPartnerDto INBOUND_105_SATURN = PreparedInboundPartnerDto.builder()
            .id(105L)
            .externalId("ext-105")
            .externalIdOrInformationListCode("105")
            .inboundStatus(InboundStatus.ARRIVED)
            .car("XX101XX77")
            .startedAt(LocalDateTime.parse("2021-06-29T01:00:00"))
            .finishedAt(null)
            .arrivalIntervalStart(LocalDateTime.parse("2021-06-29T00:50:00"))
            .arrivalIntervalEnd(LocalDateTime.parse("2021-06-29T01:40:00"))
            .supplierName("Шевелев Станислав")
            .courierName("САТУРН")
            .courierId(1L)
            .totalAmount(100L)
            .acceptedAmount(100L)
            .build();

    // AGGREGATED

    private static final PreparedInboundPartnerDto AGGREGATED_SATURN = PreparedInboundPartnerDto.builder()
            .id(105L)
            .externalId("ext-105")
            .externalIdOrInformationListCode("105")
            .inboundStatus(InboundStatus.ARRIVED)
            .car("XX101XX77")
            .startedAt(LocalDateTime.parse("2021-06-29T01:00:00"))
            .finishedAt(null)
            .arrivalIntervalStart(LocalDateTime.parse("2021-06-29T00:30:00"))
            .arrivalIntervalEnd(LocalDateTime.parse("2021-06-29T01:40:00"))
            .supplierName("Снигур Надежда Борисовна, Мельникова Екатерина, Шевелев Станислав")
            .courierName("САТУРН")
            .courierId(1L)
            .totalAmount(1500L)
            .acceptedAmount(800L)
            .plannedBoxAmount(300L)
            .boxAmount(300L)
            .plannedPalletAmount(500L)
            .palletAmount(400L)
            .build();

    private static final PreparedInboundPartnerDto AGGREGATED_PRAGMATIKA = PreparedInboundPartnerDto.builder()
            .id(104L)
            .externalId("ext-104")
            .externalIdOrInformationListCode("104")
            .inboundStatus(InboundStatus.CREATED)
            .car("XX103XX77")
            .startedAt(LocalDateTime.parse("2021-06-29T00:30:00"))
            .finishedAt(null)
            .arrivalIntervalStart(LocalDateTime.parse("2021-06-29T00:40:00"))
            .arrivalIntervalEnd(LocalDateTime.parse("2021-06-29T02:20:00"))
            .supplierName("Ашихмин Сергей, Зуев Владислав")
            .courierName("ПРАГМАТИКА")
            .courierId(2L)
            .totalAmount(800L)
            .acceptedAmount(100L)
            .plannedPalletAmount(1L)
            .palletAmount(1L)
            .build();

    @Test
    void aggregateEmpty() {
        assertAggregation(
                List.of(),
                List.of(),
                dto -> true
        );
    }

    @Test
    void aggregateOnlyOne() {
        assertAggregation(
                List.of(INBOUND_101_SATURN),
                List.of(INBOUND_101_SATURN),
                dto -> true
        );
    }

    @Test
    void aggregateAll() {
        assertAggregation(
                List.of(
                        INBOUND_101_SATURN,
                        INBOUND_102_SATURN,
                        INBOUND_103_PRAGMATIKA,
                        INBOUND_104_PRAGMATIKA,
                        INBOUND_105_SATURN
                ),
                List.of(AGGREGATED_SATURN, AGGREGATED_PRAGMATIKA),
                dto -> true
        );
    }

    @Test
    void aggregateAllWithFilter() {
        assertAggregation(
                List.of(
                        INBOUND_101_SATURN,
                        INBOUND_102_SATURN,
                        INBOUND_103_PRAGMATIKA,
                        INBOUND_104_PRAGMATIKA,
                        INBOUND_105_SATURN
                ),
                List.of(AGGREGATED_PRAGMATIKA),
                dto -> dto.getExternalIdOrInformationListCode().equals("104")
        );
    }

    private void assertAggregation(
            List<PreparedInboundPartnerDto> rawInbounds,
            List<PreparedInboundPartnerDto> expectedAggregates,
            Predicate<PreparedInboundPartnerDto> dtoGroupFilter
    ) {
        List<PreparedInboundPartnerDto> actualAggregates = InboundDtoAggregator.aggregate(rawInbounds, dtoGroupFilter);
        assertThat(actualAggregates).hasSameElementsAs(expectedAggregates);
    }
}
