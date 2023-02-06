package ru.yandex.market.sc.core.resttest.infra;

import java.time.LocalDate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.sc.internal.client.ScIntClient;
import ru.yandex.market.sc.internal.client.ScIntClientConfiguration;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.model.CreateDemoOrderDto;

/**
 * @author valter
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
@Slf4j
public class RestTestFactory {

    public ScIntClient scIntClient;

    public RestTestFactory() {
        this.scIntClient = buildFromSettings(RestTestEnvironment.TESTING_SC_INT);
    }

    private static ScIntClient buildFromSettings(RestTestEnvironment.Settings settings) {
        return new ScIntClient(
                settings.getBaseUri(), settings.getPort(),
                ScIntClientConfiguration.restTemplate()
        );
    }

    public long createSortingCenter() {
        return runAndPrintException(() -> scIntClient.createSortingCenter().getId());
    }

    public String createDemoOrder(long sortingCenterId) {
        return runAndPrintException(() -> scIntClient.createDemoOrder(sortingCenterId));
    }

    public String createDemoOrder(CreateDemoOrderDto dto) {
        return runAndPrintException(() -> scIntClient.createDemoOrder(dto));
    }

    public RestTestFactory cancelOrder(String externalOrderId, long scId) {
        return runAndPrintException(() -> {
            scIntClient.cancelOrder(externalOrderId, scId);
            return this;
        });
    }

    public RestTestFactory updateShipmentDateForOrder(String externalOrderId, LocalDate shipmentDate, long scId) {
        return runAndPrintException(() -> {
            scIntClient.updateShipmentDateForOrder(externalOrderId, shipmentDate, scId);
            return this;
        });
    }

    public RestTestFactory updateCourier(String externalOrderId, CourierDto courierDto, long scId) {
        return runAndPrintException(() -> {
            scIntClient.updateCourier(externalOrderId, courierDto, scId);
            return this;
        });
    }

    public RestTestFactory acceptOrder(String externalOrderId, @Nullable String externalPlaceId, long scId) {
        return runAndPrintException(() -> {
            scIntClient.acceptOrder(externalOrderId, externalPlaceId, scId);
            return this;
        });
    }

    public RestTestFactory sortOrder(String externalOrderId, @Nullable String externalPlaceId, long cellId, long scId) {
        return runAndPrintException(() -> {
            scIntClient.sortOrder(externalOrderId, externalPlaceId, cellId, scId);
            return this;
        });
    }

    public RestTestFactory prepareToShipOrder(String externalOrderId, @Nullable String externalPlaceId,
                                              long cellId, long routeId, long scId) {
        return runAndPrintException(() -> {
            scIntClient.prepareToShipOrder(externalOrderId, externalPlaceId, cellId, routeId, scId);
            return this;
        });
    }

    public RestTestFactory keepOrder(String externalOrderId, @Nullable String externalPlaceId,
                                     long cellId, boolean ignoreTodayRoute, long scId) {
        return runAndPrintException(() -> {
            scIntClient.keepOrder(externalOrderId, externalPlaceId, cellId, ignoreTodayRoute, scId);
            return this;
        });
    }

    public RestTestFactory shipOrderToCourier(String externalOrderId, @Nullable String externalPlaceId,
                                              long courierId, long scId) {
        return runAndPrintException(() -> {
            scIntClient.shipOrder(externalOrderId, externalPlaceId, courierId, null, scId);
            return this;
        });
    }

    public RestTestFactory shipOrderToWarehouse(String externalOrderId, @Nullable String externalPlaceId,
                                                long warehouseId, long scId) {
        return runAndPrintException(() -> {
            scIntClient.shipOrder(externalOrderId, externalPlaceId, null, warehouseId, scId);
            return this;
        });
    }

    public RestTestFactory markAsDamagedOrder(String externalOrderId, long scId) {
        return runAndPrintException(() -> {
            scIntClient.markAsDamagedOrder(externalOrderId, scId);
            return this;
        });
    }

    private static <T> T runAndPrintException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            log.error("Got exception", e);
            throw e;
        }
    }

}
