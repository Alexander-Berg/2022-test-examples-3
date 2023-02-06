package ru.yandex.market.logistic.api.model.fulfillment.request;

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.CargoUnit;
import ru.yandex.market.logistic.api.model.fulfillment.UnitCargoType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitType;
import ru.yandex.market.logistic.api.model.fulfillment.WarehouseId;
import ru.yandex.market.logistic.api.model.fulfillment.WarehouseIdType;
import ru.yandex.market.logistic.api.utils.DateTime;

import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

public class PushCargoUnitsRequestTest {
    private static final int HISTORY_MAX_SIZE = 100;

    private final SoftAssertions assertions = new SoftAssertions();

    @Test
    void testCargoUnitsOk() {
        PushCargoUnitsRequest pushCargoUnitsRequest = new PushCargoUnitsRequest.PushCargoUnitsRequestBuilder(
            new DateTime("2019-02-10T11:00:00+02:30"),
            new WarehouseId(172, WarehouseIdType.PARTNER),
            Arrays.asList(
                new CargoUnit.CargoUnitBuilder(
                    "DRP0001",
                    null,
                    UnitType.PALLET,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    null,
                    new DateTime("2017-09-10T17:00:00Z"),
                    new DateTime("2017-09-11T17:00:00Z")
                )
                    .build(),
                new CargoUnit.CargoUnitBuilder(
                    "BOX0001",
                    "DRP0001",
                    UnitType.BOX,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "TMU12345",
                    new DateTime("2017-09-11T07:16:01Z"),
                    new DateTime("2017-09-11T17:00:00Z")
                )
                    .build()
            )
        ).build();

        Set<ConstraintViolation<PushCargoUnitsRequest>> constraintViolations =
            VALIDATOR.validate(pushCargoUnitsRequest);

        assertions.assertThat(constraintViolations)
            .as("Asserting constraintViolations is not empty")
            .isEmpty();

        assertions.assertAll();
    }

    @Test
    void testCargoUnitsValidation() {
        PushCargoUnitsRequest pushCargoUnitsRequest = new PushCargoUnitsRequest.PushCargoUnitsRequestBuilder(
            new DateTime("2019-02-10T11:00:00+02:30"),
            new WarehouseId(100000172L, WarehouseIdType.LOGISTIC_POINT),
            Arrays.asList(
                new CargoUnit.CargoUnitBuilder(
                    null,
                    null,
                    null,
                    null,
                    null,
                    new DateTime("2017-09-10T17:00:00Z"),
                    new DateTime("2017-09-11T17:00:00Z")
                )
                    .build(),
                new CargoUnit.CargoUnitBuilder(
                    "BOX0001",
                    "DRP0001",
                    UnitType.BOX,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "TMU12345",
                    null,
                    new DateTime("2017-09-11T17:00:00Z")
                )
                    .build()
            )
        ).build();

        assertions.assertAll();
        Set<ConstraintViolation<PushCargoUnitsRequest>> constraintViolations =
            VALIDATOR.validate(pushCargoUnitsRequest);

        assertions.assertThat(constraintViolations)
            .as("Asserting constraintViolations is not empty")
            .hasSize(4);

        assertions.assertThat(constraintViolations)
            .as("Asserting constraintViolations message must be specific")
            .extracting(ConstraintViolation::getMessage)
            .contains("must not be null", Index.atIndex(0))
            .contains("must not be null", Index.atIndex(1))
            .contains("must not be null", Index.atIndex(2))
            .contains("must not be null", Index.atIndex(3));

        assertions.assertAll();
    }

    @Test
    void testTargetPartnerValidation() {
        PushCargoUnitsRequest pushCargoUnitsRequest = new PushCargoUnitsRequest.PushCargoUnitsRequestBuilder(
            new DateTime("2019-02-10T11:00:00+02:30"),
            null,
            Arrays.asList(
                new CargoUnit.CargoUnitBuilder(
                    "DRP0001",
                    null,
                    UnitType.PALLET,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    null,
                    new DateTime("2017-09-10T17:00:00Z"),
                    new DateTime("2017-09-11T17:00:00Z")
                )
                    .build(),
                new CargoUnit.CargoUnitBuilder(
                    "BOX0001",
                    "DRP0001",
                    UnitType.BOX,
                    UnitCargoType.INTERWAREHOUSE_FIT,
                    "TMU12345",
                    new DateTime("2017-09-11T07:16:01Z"),
                    new DateTime("2017-09-11T17:00:00Z")
                )
                    .build()
            )
        ).build();

        assertions.assertAll();
        Set<ConstraintViolation<PushCargoUnitsRequest>> constraintViolations =
            VALIDATOR.validate(pushCargoUnitsRequest);

        assertions.assertThat(constraintViolations)
            .as("Asserting constraintViolations is empty")
            .hasSize(0);

        assertions.assertAll();
    }

}
