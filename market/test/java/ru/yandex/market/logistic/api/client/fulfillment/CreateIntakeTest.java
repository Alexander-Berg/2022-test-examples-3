package ru.yandex.market.logistic.api.client.fulfillment;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.Intake.IntakeBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateIntakeResponse;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotNullErrorMessage;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createResourceId;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createWarehouse;

class CreateIntakeTest extends CommonServiceClientTest {
    @Test
    void createIntakeSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_intake", PARTNER_URL);
        CreateIntakeResponse response =
            fulfillmentClient.createIntake(createIntakeBuilder().build(), getPartnerProperties());
        assertEquals(
            new CreateIntakeResponse.CreateIntakeResponseBuilder(createIntakeId()).setIntakeNum("10001").build(),
            response,
            "Должен вернуть корректный ответ CreateIntakeResponse"
        );
    }

    @Test
    void createIntakeWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_create_intake",
            "ff_create_intake_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.createIntake(createIntakeBuilder().build(), getPartnerProperties())
        );
    }

    @Test
    void validateRequest() {
        Stream.of(
            Pair.<String, Function<IntakeBuilder, IntakeBuilder>>of("intakeId", intake -> intake.setIntakeId(null)),
            Pair.<String, Function<IntakeBuilder, IntakeBuilder>>of("warehouse", intake -> intake.setWarehouse(null)),
            Pair.<String, Function<IntakeBuilder, IntakeBuilder>>of("time", intake -> intake.setTime(null))
        )
            .forEach(pair -> validateField(pair.getLeft(), pair.getRight()));
    }

    private void validateField(String propertyPath, Function<IntakeBuilder, IntakeBuilder> intakeModifier) {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createIntake(
                intakeModifier.apply(createIntakeBuilder()).build(),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotNullErrorMessage(propertyPath));
    }

    private IntakeBuilder createIntakeBuilder() {
        return new IntakeBuilder(createIntakeId(),
            createWarehouse(),
            DateTimeInterval.fromFormattedValue("2019-08-15T10:00:00+07:00/2019-08-15T19:00:00+07:00"))
            .setVolume(new BigDecimal("3.14"))
            .setWeight(new BigDecimal("2.71"));
    }

    private ResourceId createIntakeId() {
        return createResourceId("yandex-id-1", "fulfillment-id-1");
    }
}
