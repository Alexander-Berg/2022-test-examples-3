package ru.yandex.market.delivery.transport_manager.service.external.lgw;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.Movement;
import ru.yandex.market.logistic.gateway.common.model.common.MovementSubtype;
import ru.yandex.market.logistic.gateway.common.model.common.MovementType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.Party;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.TripInfo;
import ru.yandex.market.logistic.gateway.common.model.common.TripType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Trip;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class LgwPutTripServiceTest extends AbstractContextualTest {
    @Autowired
    private LgwPutTripService lgwPutTripService;
    @Autowired
    private DeliveryClient deliveryClient;

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
        "/repository/trip/second_trip.xml",
    })
    @Test
    @SneakyThrows
    void putTrip() {
        lgwPutTripService.putTrip(2L);
        verify(deliveryClient).putTrip(
            new Trip(
                ResourceId.builder().setYandexId("TMT2").build(),
                List.of(
                    new Movement(
                        ResourceId.builder().setYandexId("TMM201").build(),
                        new DateTimeInterval(
                            OffsetDateTime.of(2021, 11, 26, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                            OffsetDateTime.of(2021, 11, 26, 17, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        ),
                        BigDecimal.ZERO,
                        null,
                        Party.builder(
                            LogisticPoint.builder(ResourceId.builder().setYandexId("10").setPartnerId("LP1").build())
                                .setContact(Person.builder("ABC").setSurname("CBA").setPatronymic("AA").build())
                                .setPhones(Collections.emptyList())
                                .build()
                        ).build(),
                        Party.builder(
                            LogisticPoint.builder(ResourceId.builder().setYandexId("20").setPartnerId("LP2").build())
                                .setContact(Person.builder("ABC").setSurname("CBA").setPatronymic("AA").build())
                                .setPhones(Collections.emptyList())
                                .build()
                        ).build(),
                        null,
                        33,
                        new DateTimeInterval(
                            OffsetDateTime.of(2021, 11, 26, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                            OffsetDateTime.of(2021, 11, 26, 11, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        ),
                        new DateTimeInterval(
                            OffsetDateTime.of(2021, 11, 26, 15, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                            OffsetDateTime.of(2021, 11, 26, 17, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        ),
                        new TripInfo(
                            ResourceId.builder().setYandexId("TMT2").build(),
                            null,
                            0,
                            1,
                            4,
                            null,
                            TripType.INTERWAREHOUSE_FIT
                        ),
                        Collections.emptyList(),
                        MovementType.INTERWAREHOUSE,
                        MovementSubtype.INTERWAREHOUSE_FIT
                    ),
                    new Movement(
                        ResourceId.builder().setYandexId("TMM202").build(),
                        new DateTimeInterval(
                            OffsetDateTime.of(2021, 11, 26, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                            OffsetDateTime.of(2021, 11, 26, 17, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        ),
                        BigDecimal.ZERO,
                        null,
                        Party.builder(
                            LogisticPoint.builder(ResourceId.builder().setYandexId("20").setPartnerId("LP2").build())
                                .setContact(Person.builder("ABC").setSurname("CBA").setPatronymic("AA").build())
                                .setPhones(Collections.emptyList())
                                .build()
                        ).build(),
                        Party.builder(
                            LogisticPoint.builder(ResourceId.builder().setYandexId("30").setPartnerId("LP3").build())
                                .setContact(Person.builder("ABC").setSurname("CBA").setPatronymic("AA").build())
                                .setPhones(Collections.emptyList())
                                .build()
                        ).build(),
                        null,
                        null,
                        new DateTimeInterval(
                            OffsetDateTime.of(2021, 11, 26, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                            OffsetDateTime.of(2021, 11, 26, 11, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        ),
                        new DateTimeInterval(
                            OffsetDateTime.of(2021, 11, 26, 15, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                            OffsetDateTime.of(2021, 11, 26, 17, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        ),
                        new TripInfo(
                            ResourceId.builder().setYandexId("TMT2").build(),
                            null,
                            2,
                            3,
                            4,
                            null,
                            TripType.INTERWAREHOUSE_FIT
                        ),
                        Collections.emptyList(),
                        MovementType.INTERWAREHOUSE,
                        MovementSubtype.INTERWAREHOUSE_FIT
                    )
                )
            ),
            new Partner(10000L)
        );
        verifyNoMoreInteractions(deliveryClient);
    }
}
