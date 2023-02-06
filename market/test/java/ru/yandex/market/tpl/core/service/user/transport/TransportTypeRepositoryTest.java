package ru.yandex.market.tpl.core.service.user.transport;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.routing.tag.RoutingOrderTagType;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.api.model.transport.TransportTypeParams;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagRepository;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class TransportTypeRepositoryTest {

    private final TransportTypeRepository transportTypeRepository;
    private final RoutingOrderTagRepository routingOrderTagRepository;

    @BeforeEach
    void init() {
        mockData();
    }

    private RoutingOrderTag makeRoutingOrderTag(String name, RoutingOrderTagType tag) {
        return routingOrderTagRepository.save(new RoutingOrderTag(
                name,
                name,
                BigDecimal.ONE,
                BigDecimal.ONE,
                tag,
                Set.of()
        ));
    }

    private void mockData() {
        RoutingOrderTag bulkyCargoTag = makeRoutingOrderTag("bulky_cargo", RoutingOrderTagType.DIMENSIONS_CLASS);
        RoutingOrderTag pvzTag = makeRoutingOrderTag("pvz", RoutingOrderTagType.ORDER_TYPE);
        RoutingOrderTag lockerTag = makeRoutingOrderTag("locker", RoutingOrderTagType.ORDER_TYPE);

        TransportType kolymaga = new TransportType(
                "kolymaga",
                BigDecimal.TEN,
                Set.of(bulkyCargoTag),
                100,
                RoutingVehicleType.COMMON,
                1,
                null
        );
        transportTypeRepository.save(kolymaga);

        TransportType fordTransit = new TransportType(
                "ford transit",
                BigDecimal.TEN,
                Set.of(bulkyCargoTag, pvzTag),
                1000,
                RoutingVehicleType.YANDEX_DRIVE,
                1,
                null
        );
        transportTypeRepository.save(fordTransit);

        TransportType matiz = new TransportType(
                "matiz",
                BigDecimal.TEN,
                Set.of(pvzTag, lockerTag),
                50,
                RoutingVehicleType.COMMON,
                1,
                null
        );
        transportTypeRepository.save(matiz);
    }

    @Test
    void findBySpecification() {

        List<TransportType> bulkyCargoTransportTypes = transportTypeRepository.findAll(
                TransportTypeSpecification.of(
                        TransportTypeParams.builder()
                                .dimensionsClassTag(Set.of("bulky_cargo"))
                                .build()
                )
        );
        assertThat(bulkyCargoTransportTypes).extracting(TransportType::getName)
                .containsExactlyInAnyOrder("kolymaga", "ford transit");

        List<TransportType> bulkyCargoAndPvzTransportTypes = transportTypeRepository.findAll(
                TransportTypeSpecification.of(TransportTypeParams.builder()
                        .orderTypeTag(Set.of("pvz"))
                        .dimensionsClassTag(Set.of("bulky_cargo"))
                        .build())
        );
        assertThat(bulkyCargoAndPvzTransportTypes).extracting(TransportType::getName)
                .containsExactlyInAnyOrder("ford transit");

        List<TransportType> bulkyCargoAndNameEqKolymagaTransportTypes = transportTypeRepository.findAll(
                TransportTypeSpecification.of(TransportTypeParams.builder()
                        .dimensionsClassTag(Set.of("bulky_cargo"))
                        .transportTypeName("kolymaga")
                        .build()
                )
        );
        assertThat(bulkyCargoAndNameEqKolymagaTransportTypes).extracting(TransportType::getName)
                .containsExactlyInAnyOrder("kolymaga");

        List<TransportType> pvzAndLockerTransportTypes = transportTypeRepository.findAll(
                TransportTypeSpecification.of(TransportTypeParams.builder()
                        .orderTypeTag(Set.of("pvz", "locker"))
                        .build()
                )
        );

        assertThat(pvzAndLockerTransportTypes).extracting(TransportType::getName)
                .containsExactlyInAnyOrder("matiz", "ford transit");
    }

}
