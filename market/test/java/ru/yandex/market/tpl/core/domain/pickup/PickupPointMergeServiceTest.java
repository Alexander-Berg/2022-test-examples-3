package ru.yandex.market.tpl.core.domain.pickup;


import java.math.BigDecimal;
import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.external.lms.ExternalPartnerResponseService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class PickupPointMergeServiceTest extends TplAbstractTest {
    private final PickupPointMergeService pickupPointMergeService;

    private final Clock clock;
    @MockBean
    private ExternalPartnerResponseService externalPartnerResponseService;

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
    public void merge() {
        when(externalPartnerResponseService.getPartner(any())).thenReturn(new PartnerResponse(1, PartnerSubType.PVZ));
        PickupPoint persisted = new PickupPoint();
        persisted.setLogisticPointId(123L);
        LogisticsPointResponse external = LogisticsPointResponse.newBuilder()
                .instruction("instruction")
                .pickupPointType(PickupPointType.PICKUP_POINT)
                .build();
        pickupPointMergeService.merge(persisted, external);
        assertThat(persisted.getDescription()).isEqualTo("instruction");

        external = LogisticsPointResponse.newBuilder()
                .instruction("instruction")
                .pickupPointType(PickupPointType.PICKUP_POINT)
                .courierInstruction("")
                .build();
        pickupPointMergeService.merge(persisted, external);
        assertThat(persisted.getDescription()).isEqualTo("instruction");

        external = LogisticsPointResponse.newBuilder()
                .instruction("instruction")
                .pickupPointType(PickupPointType.PICKUP_POINT)
                .courierInstruction("       ")
                .address(getAddress())
                .build();
        pickupPointMergeService.merge(persisted, external);
        assertThat(persisted.getDescription()).isEqualTo("instruction");
        assertThat(persisted.getAddress()).isEqualTo("село Зудово, Болотнинский район, Новосибирская область, Россия," +
                " Солнечная улица, 9A, 2");

        external = LogisticsPointResponse.newBuilder()
                .instruction("instruction")
                .pickupPointType(PickupPointType.PICKUP_POINT)
                .courierInstruction("courierInstruction")
                .build();
        pickupPointMergeService.merge(persisted, external);
        assertThat(persisted.getDescription()).isEqualTo("courierInstruction");

    }
}
