package ru.yandex.market.logistic.gateway.service.converter.fulfillment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.Movement;
import ru.yandex.market.logistic.gateway.common.model.common.MovementType;
import ru.yandex.market.logistic.gateway.common.model.common.Party;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.TripInfo;
import ru.yandex.market.logistic.gateway.common.model.common.TripType;
import ru.yandex.market.logistic.gateway.service.converter.common.MovementConverter;

public class MovementConverterTest extends BaseTest {

    @Test
    public void fromMovementWithoutTrip() {
        Optional<ru.yandex.market.logistic.api.model.common.Movement> converted =
            MovementConverter.convertToApi(new Movement(
                ResourceId.builder().setYandexId("YA1").setPartnerId("PID1").build(),
                DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                Party.builder(LogisticPoint.builder(ResourceId.builder().setYandexId("P1").setPartnerId("P1").build())
                    .build()).build(),
                Party.builder(LogisticPoint.builder(ResourceId.builder().setYandexId("P2").setPartnerId("P2").build())
                    .build()).build(),
                "comment",
                33,
                DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                null,
                null,
                MovementType.LINEHAUL,
                null
            ));

        assertions.assertThat(converted.get()).isEqualTo(new ru.yandex.market.logistic.api.model.common.Movement(
                ru.yandex.market.logistic.api.model.common.ResourceId.builder()
                        .setYandexId("YA1")
                        .setPartnerId("PID1")
                        .build(),
                ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                ru.yandex.market.logistic.api.model.common.Party.builder(
                        ru.yandex.market.logistic.api.model.common.LogisticPoint
                                .builder(ru.yandex.market.logistic.api.model.common.ResourceId.builder()
                                        .setYandexId("P1")
                                        .setPartnerId("P1")
                                        .build())
                                .setPhones(List.of())
                                .build()).build(),
                ru.yandex.market.logistic.api.model.common.Party.builder(
                        ru.yandex.market.logistic.api.model.common.LogisticPoint
                                .builder(ru.yandex.market.logistic.api.model.common.ResourceId.builder()
                                        .setYandexId("P2")
                                        .setPartnerId("P2")
                                        .build())
                                .setPhones(List.of())
                                .build()).build(),
                "comment",
                33,
                ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                null,
                null,
                ru.yandex.market.logistic.api.model.common.MovementType.LINEHAUL,
                null
        ));
    }

    @Test
    public void fromMovementWithTrip() {
        Optional<ru.yandex.market.logistic.api.model.common.Movement> converted =
                MovementConverter.convertToApi(new Movement(
                        ResourceId.builder().setYandexId("YA1").setPartnerId("PID1").build(),
                        DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                        BigDecimal.valueOf(10),
                        BigDecimal.valueOf(100),
                        Party.builder(LogisticPoint.builder(ResourceId.builder().setYandexId("P1").setPartnerId("P1").build())
                                .build()).build(),
                        Party.builder(LogisticPoint.builder(ResourceId.builder().setYandexId("P2").setPartnerId("P2").build())
                                .build()).build(),
                        "comment",
                        33,
                        DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                        DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                        new TripInfo.TripInfoBuilder()
                            .setTripId(ResourceId.builder().setYandexId("TMT1").build())
                            .setRouteName("Маршрут №1")
                            .setFromIndex(0)
                            .setToIndex(1)
                            .setTotalCount(2)
                            .setType(TripType.MAIN)
                            .build(),
                        null,
                        MovementType.LINEHAUL,
                        null
                ));

        assertions.assertThat(converted.get()).isEqualTo(new ru.yandex.market.logistic.api.model.common.Movement(
                ru.yandex.market.logistic.api.model.common.ResourceId.builder()
                        .setYandexId("YA1")
                        .setPartnerId("PID1")
                        .build(),
                ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                ru.yandex.market.logistic.api.model.common.Party.builder(
                        ru.yandex.market.logistic.api.model.common.LogisticPoint
                                .builder(ru.yandex.market.logistic.api.model.common.ResourceId.builder()
                                        .setYandexId("P1")
                                        .setPartnerId("P1")
                                        .build())
                                .setPhones(List.of())
                                .build()).build(),
                ru.yandex.market.logistic.api.model.common.Party.builder(
                        ru.yandex.market.logistic.api.model.common.LogisticPoint
                                .builder(ru.yandex.market.logistic.api.model.common.ResourceId.builder()
                                        .setYandexId("P2")
                                        .setPartnerId("P2")
                                        .build())
                                .setPhones(List.of())
                                .build()).build(),
                "comment",
                33,
                ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue("2021-08-07/2021-08-10"),
                new ru.yandex.market.logistic.api.model.common.TripInfo.TripInfoBuilder()
                    .setTripId(ru.yandex.market.logistic.api.model.common.ResourceId.builder()
                            .setYandexId("TMT1")
                            .build())
                    .setRouteName("Маршрут №1")
                    .setFromIndex(0)
                    .setToIndex(1)
                    .setTotalCount(2)
                    .setType(ru.yandex.market.logistic.api.model.common.TripType.MAIN)
                    .build(),
                null,
                ru.yandex.market.logistic.api.model.common.MovementType.LINEHAUL,
                null
        ));
    }

}
