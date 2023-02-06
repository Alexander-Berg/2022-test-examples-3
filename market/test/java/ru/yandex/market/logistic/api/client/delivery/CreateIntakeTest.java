package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.Intake;
import ru.yandex.market.logistic.api.model.delivery.Warehouse;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createOrderId;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createWarehouse;

class CreateIntakeTest extends CommonServiceClientTest {

    private static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-02-10T11:00:00+02:30/2019-02-12T11:00:00+02:30");
    private static final float VOLUME = 1.1f;
    private static final float WEIGHT = 5.5f;
    private static final String COMMENT = "comment";

    @Autowired
    DeliveryServiceClient deliveryServiceClient;

    @Test
    void testCreateIntakeSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_create_intake", PARTNER_URL);
        deliveryServiceClient.createIntake(createIntake(), getPartnerProperties());
    }

    @Test
    void testCreateIntakeWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_intake",
            "ds_create_intake_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.createIntake(createIntake(), getPartnerProperties())
        );
    }

    @Test
    void testCreateIntakeValidationFailed() {
        assertThrows(ValidationException.class, () -> {
            Intake intake = createIntake(null);
            deliveryServiceClient.createIntake(intake, getPartnerProperties());
        });
    }

    private static Intake createIntake() {
        return createIntake(createWarehouse());
    }

    private static Intake createIntake(Warehouse warehouse) {
        return new Intake.IntakeBuilder(createOrderId(), warehouse, DATE_TIME_INTERVAL)
            .setVolume(VOLUME)
            .setWeight(WEIGHT)
            .setComment(COMMENT)
            .build();
    }
}
