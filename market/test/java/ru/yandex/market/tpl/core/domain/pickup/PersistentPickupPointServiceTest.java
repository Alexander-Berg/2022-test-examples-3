package ru.yandex.market.tpl.core.domain.pickup;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.core.external.lms.ExternalPartnerResponseService;
import ru.yandex.market.tpl.core.external.lms.LMSPickupPointService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.PickupPointType.PICKUP_POINT;

@RequiredArgsConstructor
class PersistentPickupPointServiceTest extends TplAbstractTest {

    private final PickupPointService pickupPointService;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final LMSPickupPointService lmsPickupPointService;
    private final LMSClient lmsClient;
    private final ExternalPartnerResponseService externalPartnerResponseService;

    private static Address getAddress() {
        return Address.newBuilder()
                .addressString("село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2")
                .shortAddressString("село Зудово, Солнечная улица, 9A, 2")
                .locationId(133543)
                .latitude(BigDecimal.valueOf(55.822463D))
                .longitude(BigDecimal.valueOf(84.258002D))
                .postCode("633372")
                .region("Новосибирская область")
                .subRegion("Болотнинский район")
                .settlement("Зудово")
                .street("Солнечная")
                .house("6")
                .housing("2")
                .building("А")
                .apartment("318")
                .build();
    }

    @Test
    void getOrCreateCreatesPickupPointForUnknownCode() {
        String unknownCode = "unknown";
        PickupPoint newlyCreated = pickupPointService.getOrCreate(unknownCode, -111L);
        assertThat(newlyCreated)
                .extracting(PickupPoint::getCode, PickupPoint::getPartnerSubType)
                .isEqualTo(List.of(unknownCode, PartnerSubType.LOCKER));
    }

    @Test
    void merge() {
        PickupPoint persistedPickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L);
        PickupPoint expected = generateExpectedPickupPoint(persistedPickupPoint);
        LogisticsPointResponse logisticsPointResponse = generateLogisticPointResponse(persistedPickupPoint);
        when(lmsClient.getLogisticsPoint(persistedPickupPoint.getLogisticPointId()))
                .thenReturn(Optional.of(logisticsPointResponse));

        Optional<PickupPoint> mergedPickupPoint =
                pickupPointService.pullAndMergeLogisticPoint(expected.getLogisticPointId());
        assertThat(mergedPickupPoint).isPresent();
        assertThat(mergedPickupPoint.get())
                .extracting(PickupPoint::getPhoneNumber, PickupPoint::getType)
                .isEqualTo(List.of(expected.getPhoneNumber(), PickupPointType.PVZ));
        assertThat(mergedPickupPoint.get())
                .extracting(PickupPoint::getAddress)
                .isEqualTo(expected.getAddress());
    }

    @Test
    void mergeAll() {
        PickupPoint persistedPickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L);
        PickupPoint persistedPickupPoint2 = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 2L, 2L);

        PickupPoint expectedPickupPoint = generateExpectedPickupPoint(persistedPickupPoint);
        expectedPickupPoint.setPartnerSubType(PartnerSubType.LAVKA);
        expectedPickupPoint.setPartnerId(1L);
        PickupPoint expectedPickupPoint2 = generateExpectedPickupPoint(persistedPickupPoint2);
        expectedPickupPoint2.setPartnerSubType(PartnerSubType.PVZ);
        expectedPickupPoint2.setPartnerId(2L);

        LogisticsPointResponse logisticsPointResponse = generateLogisticPointResponse(persistedPickupPoint);
        LogisticsPointResponse logisticsPointResponse2 = generateLogisticPointResponse(persistedPickupPoint2);
        when(lmsPickupPointService.getAll(Set.of(persistedPickupPoint.getLogisticPointId(),
                persistedPickupPoint2.getLogisticPointId())))
                .thenReturn(List.of(logisticsPointResponse, logisticsPointResponse2));
        when(lmsClient.getPartner(logisticsPointResponse.getPartnerId()))
                .thenReturn(Optional.of(PartnerResponse.newBuilder()
                        .subtype(PartnerSubtypeResponse.newBuilder()
                                .id(8)
                                .build())
                        .build()));
        when(lmsClient.getPartner(logisticsPointResponse2.getPartnerId()))
                .thenReturn(Optional.of(PartnerResponse.newBuilder()
                        .subtype(PartnerSubtypeResponse.newBuilder()
                                .id(3)
                                .build())
                        .build()));
        pickupPointService.pullAndMergeLogisticPoints(Set.of(expectedPickupPoint.getLogisticPointId(),
                expectedPickupPoint2.getLogisticPointId()));
        List<Pair<PickupPoint, PickupPoint>> actualExpectedPickupPointPairs = Streams.zip(
                StreamEx.of(pickupPointRepository.findAllByLogisticPointIdIn(Set.of(1L, 2L)))
                        .sorted(Comparator.comparing(PickupPoint::getLogisticPointId)),
                List.of(expectedPickupPoint, expectedPickupPoint2).stream()
                        .sorted(Comparator.comparing(PickupPoint::getLogisticPointId)),
                Pair::of
        ).collect(Collectors.toList());
        actualExpectedPickupPointPairs.forEach(actualExpected -> {
            PickupPoint actual = actualExpected.getLeft();
            PickupPoint expected = actualExpected.getRight();
            assertThat(actual)
                    .extracting(PickupPoint::getPhoneNumber, PickupPoint::getPartnerSubType)
                    .isEqualTo(List.of(expected.getPhoneNumber(), expected.getPartnerSubType()));
            assertThat(actual)
                    .extracting(PickupPoint::getAddress)
                    .isEqualTo(expected.getAddress());
        });
    }

    private PickupPoint generateExpectedPickupPoint(PickupPoint persistedPickupPoint) {
        PickupPoint externalPickupPoint = new PickupPoint();
        externalPickupPoint.setCode(persistedPickupPoint.getCode());
        externalPickupPoint.setPhoneNumber("new phone number");
        externalPickupPoint.setPartnerSubType(persistedPickupPoint.getPartnerSubType());
        externalPickupPoint.setType(PickupPointType.PVZ);
        externalPickupPoint.setLogisticPointId(persistedPickupPoint.getLogisticPointId());
        externalPickupPoint.setPartnerId(persistedPickupPoint.getPartnerId());
        externalPickupPoint.setAddress("село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная " +
                "улица, 9A, 2");
        return externalPickupPoint;
    }

    private LogisticsPointResponse generateLogisticPointResponse(PickupPoint persistedPickupPoint) {
        return LogisticsPointResponse.newBuilder()
                .externalId(persistedPickupPoint.getCode())
                .phones(Set.of(new Phone("new phone number", null, null, PhoneType.PRIMARY)))
                .partnerId(persistedPickupPoint.getDeliveryServiceId())
                .id(persistedPickupPoint.getLogisticPointId())
                .pickupPointType(PICKUP_POINT)
                .address(getAddress())
                .build();
    }

}
