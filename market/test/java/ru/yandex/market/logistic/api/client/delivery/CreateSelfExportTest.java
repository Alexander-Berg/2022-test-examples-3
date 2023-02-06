package ru.yandex.market.logistic.api.client.delivery;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.SelfExport;
import ru.yandex.market.logistic.api.model.delivery.Warehouse;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createCourier;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createDetailedCourier;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createDetailedWarehouse;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createResourceId;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createWarehouse;

class CreateSelfExportTest extends CommonServiceClientTest {

    private static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-02-10T11:00:00+02:30/2019-02-12T11:00:00+02:30");

    private static final float VOLUME = 1.1f;
    private static final float WEIGHT = 5.5f;

    private static final DateTimeInterval DATE_TIME_INTERVAL_FOR_DETAILED_TEST =
        DateTimeInterval.fromFormattedValue("2019-06-17T10:00:00+03:00/2019-06-17T22:00:00+03:00");
    private static final float VOLUME_FOR_DETAILED_TEST = 0.04f;
    private static final float WEIGHT_FOR_DETAILED_TEST = 5;

    @Autowired
    DeliveryServiceClient deliveryServiceClient;

    private static SelfExport createSelfExport() {
        return createSelfExport(createWarehouse());
    }

    private static SelfExport createSelfExport(Warehouse warehouse) {
        return new SelfExport.SelfExportBuilder(createResourceId("1", "ext1"), warehouse, DATE_TIME_INTERVAL)
            .setVolume(VOLUME)
            .setWeight(WEIGHT)
            .setCourier(createCourier())
            .setOrdersId(Arrays.asList(createResourceId("2", "ext2"), createResourceId("3", "ext3")))
            .build();
    }

    @Test
    void testCreateSelfExportSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_create_selfexport",
            PARTNER_URL);
        deliveryServiceClient.createSelfExport(createSelfExport(), getPartnerProperties());
    }

    @Test
    void testCreateSelfExportWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_selfexport",
            "ds_create_selfexport_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.createSelfExport(createSelfExport(), getPartnerProperties())
        );
    }

    @Test
    void testCreateSelfExportValidationFailed() {
        SelfExport selfExport = createSelfExport(null);
        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.createSelfExport(selfExport, getPartnerProperties())
        );

    }

    @Test
    void testCreateSelfExportDetailedData() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_selfexport_detailed",
            "ds_create_selfexport",
            PARTNER_URL
        );
        SelfExport detailedExport =
            new SelfExport.SelfExportBuilder(createResourceId("1", "ext1"), createDetailedWarehouse(),
                DATE_TIME_INTERVAL_FOR_DETAILED_TEST)
                .setCourier(createDetailedCourier())
                .setVolume(VOLUME_FOR_DETAILED_TEST)
                .setWeight(WEIGHT_FOR_DETAILED_TEST)
                .setOrdersId(Arrays.asList(
                    createResourceId("7888-YD7816631", "3586901"),
                    createResourceId("7890-YD7827236", "3589396"),
                    createResourceId("7891-YD7827338", "3589403"),
                    createResourceId("7892-YD7827521", "3589426"),
                    createResourceId("7893-YD7827668", "3589530")))
                .build();
        deliveryServiceClient.createSelfExport(detailedExport, getPartnerProperties());
    }


}
