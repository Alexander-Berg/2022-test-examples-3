package ru.yandex.market.wms.servicebus.api.internal.wms.server.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistic.api.model.fulfillment.CargoUnit;
import ru.yandex.market.logistic.api.model.fulfillment.UnitCargoType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitType;
import ru.yandex.market.logistic.api.model.fulfillment.WarehouseId;
import ru.yandex.market.logistic.api.model.fulfillment.WarehouseIdType;
import ru.yandex.market.logistic.api.model.fulfillment.request.PushCargoUnitsRequest;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.dropping.client.DroppingClient;
import ru.yandex.market.wms.dropping.core.model.CarrierPalletDto;
import ru.yandex.market.wms.dropping.core.model.CarrierStateRequest;
import ru.yandex.market.wms.dropping.core.model.CarrierStateResponse;
import ru.yandex.market.wms.dropping.core.model.PalletBoxDto;
import ru.yandex.market.wms.servicebus.api.external.logistics.client.LogisticsApiClient;
import ru.yandex.market.wms.servicebus.api.internal.wms.server.mapper.CargoUnitsMapper;

class CarrierStatePusherTest {

    public static final String CARRIER_CODE = "172";
    public static final ZoneOffset TZ = ZoneId.of("Europe/Moscow")
        .getRules()
        .getOffset(LocalDateTime.now());
    private CarrierStatePusher carrierStatePusher;
    private DroppingClient droppingClient;
    private LogisticsApiClient logisticsApiClient;
    private TestableClock clock;

    @BeforeEach
    void setUp() {
        droppingClient = Mockito.mock(DroppingClient.class);
        logisticsApiClient = Mockito.mock(LogisticsApiClient.class);
        clock = new TestableClock();
        clock.setFixed(
            LocalDateTime.of(2022, 7, 1, 10, 0).toInstant(TZ),
            TZ
        );

        carrierStatePusher = new CarrierStatePusher(
            droppingClient,
            logisticsApiClient,
            new CargoUnitsMapper(),
            clock
        );
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(droppingClient, logisticsApiClient);
    }

    @SuppressWarnings("checkstyle:methodlength")
    @Test
    void push() {
        CarrierStateRequest carrierStateRequest = new CarrierStateRequest(CARRIER_CODE);
        Mockito.when(droppingClient.getCarrierState(Mockito.eq(carrierStateRequest)))
            .thenReturn(new CarrierStateResponse(
                List.of(
                    new CarrierPalletDto(
                        "DRP001",
                        "outbound-0001",
                        LocalDateTime.of(2022, 6, 30, 10, 0),
                        OrderType.STANDARD
                    ),
                    new CarrierPalletDto(
                        "DRP002",
                        "outbound-0002",
                        LocalDateTime.of(2022, 6, 25, 12, 0),
                        OrderType.OUTBOUND_WH_2_WH
                    ),
                    new CarrierPalletDto(
                        "DRP003",
                        "outbound-0002",
                        LocalDateTime.of(2022, 6, 25, 12, 0),
                        OrderType.OUTBOUND_WH_2_WH
                    ),
                    new CarrierPalletDto(
                        "DRP004",
                        "outbound-0003",
                        LocalDateTime.of(2022, 6, 29, 11, 0),
                        OrderType.BBXD_ORDER
                    )
                ),
                List.of(
                    new PalletBoxDto(
                        "DRP002",
                        "P0001",
                        "outbound-0002",
                        LocalDateTime.of(2022, 6, 25, 12, 0),
                        OrderType.OUTBOUND_WH_2_WH
                    ),
                    new PalletBoxDto(
                        "DRP002",
                        "P0002",
                        "outbound-0002",
                        LocalDateTime.of(2022, 6, 25, 12, 0),
                        OrderType.OUTBOUND_WH_2_WH
                    ),
                    new PalletBoxDto(
                        "DRP003",
                        "P0003",
                        "outbound-0002",
                        LocalDateTime.of(2022, 6, 25, 12, 0),
                        OrderType.OUTBOUND_WH_2_WH
                    ),
                    new PalletBoxDto(
                        "DRP004",
                        "P0004",
                        "outbound-0003",
                        LocalDateTime.of(2022, 6, 29, 11, 0),
                        OrderType.BBXD_ORDER
                    ),
                    new PalletBoxDto(
                        null,
                        "P0005",
                        null,
                        null,
                        OrderType.ANOMALY_WITHDRAWAL
                    )
                )
            ));

        carrierStatePusher.push(CARRIER_CODE);

        Mockito.verify(droppingClient).getCarrierState(Mockito.eq(carrierStateRequest));
        Mockito.verify(logisticsApiClient).pushCargoUnits(Mockito.eq(new PushCargoUnitsRequest(
            DateTime.fromOffsetDateTime(
                LocalDateTime.of(2022, 7, 1, 10, 0).atOffset(TZ)
            ),
            new WarehouseId(Long.parseLong(CARRIER_CODE), WarehouseIdType.PARTNER),
            List.of(
                new CargoUnit(
                    "DRP001",
                    null,
                    UnitType.PALLET,
                    UnitCargoType.ORDER,
                    "outbound-0001",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 30, 10, 0)),
                    null
                ),
                new CargoUnit(
                    "DRP002",
                    null,
                    UnitType.PALLET,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "outbound-0002",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 25, 12, 0)),
                    null
                ),
                new CargoUnit(
                    "DRP003",
                    null,
                    UnitType.PALLET,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "outbound-0002",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 25, 12, 0)),
                    null
                ),
                new CargoUnit(
                    "DRP004",
                    null,
                    UnitType.PALLET,
                    UnitCargoType.XDOCK,
                    "outbound-0003",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 29, 11, 0)),
                    null
                ),

                new CargoUnit(
                    "P0001",
                    "DRP002",
                    UnitType.BOX,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "outbound-0002",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 25, 12, 0)),
                    null
                ),
                new CargoUnit(
                    "P0002",
                    "DRP002",
                    UnitType.BOX,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "outbound-0002",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 25, 12, 0)),
                    null
                ),
                new CargoUnit(
                    "P0003",
                    "DRP003",
                    UnitType.BOX,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "outbound-0002",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 25, 12, 0)),
                    null
                ),
                new CargoUnit(
                    "P0004",
                    "DRP004",
                    UnitType.BOX,
                    UnitCargoType.XDOCK,
                    "outbound-0003",
                    DateTime.fromLocalDateTime(LocalDateTime.of(2022, 6, 29, 11, 0)),
                    null
                ),
                new CargoUnit(
                    "P0005",
                    null,
                    UnitType.BOX,
                    UnitCargoType.ANOMALY,
                    null,
                    null,
                    null
                )
            )
        )));
    }
}
