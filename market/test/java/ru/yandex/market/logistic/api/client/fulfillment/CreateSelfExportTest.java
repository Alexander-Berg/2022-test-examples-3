package ru.yandex.market.logistic.api.client.fulfillment;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.SelfExport;
import ru.yandex.market.logistic.api.model.fulfillment.SelfExport.SelfExportBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateSelfExportResponse;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotNullErrorMessage;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createCourier;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createResourceId;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createWarehouse;

class CreateSelfExportTest extends CommonServiceClientTest {
    @Test
    void createSelfExportSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_self_export", PARTNER_URL);
        CreateSelfExportResponse response =
            fulfillmentClient.createSelfExport(createSelfExport(), getPartnerProperties());
        assertEquals(
            new CreateSelfExportResponse(createSelfExportId(), "10001"),
            response,
            "Должен вернуть корректный ответ CreateSelfExportResponse"
        );
    }

    @Test
    void createSelfExportWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_create_self_export",
            "ff_create_self_export_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.createSelfExport(createSelfExport(), getPartnerProperties())
        );
    }

    @Test
    void validateRequest() {
        Stream.of(
            Pair.<String, Function<SelfExportBuilder, SelfExportBuilder>>of("exportId",
                selfExportBuilder -> selfExportBuilder.setExportId(null)),
            Pair.<String, Function<SelfExportBuilder, SelfExportBuilder>>of("warehouse",
                selfExportBuilder -> selfExportBuilder.setWarehouse(null)),
            Pair.<String, Function<SelfExportBuilder, SelfExportBuilder>>of("time",
                selfExportBuilder -> selfExportBuilder.setTime(null)),
            Pair.<String, Function<SelfExportBuilder, SelfExportBuilder>>of("courier",
                selfExportBuilder -> selfExportBuilder.setCourier(null))
        )
            .forEach(pair -> validateField(pair.getLeft(), pair.getRight()));
    }

    private void validateField(String propertyPath, Function<SelfExportBuilder, SelfExportBuilder> intakeModifier) {

        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createSelfExport(intakeModifier.apply(createSelfExportBuilder()).build(),
                getPartnerProperties())
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotNullErrorMessage(propertyPath));
    }

    private SelfExportBuilder createSelfExportBuilder() {
        return new SelfExportBuilder(createSelfExportId(),
            createWarehouse(),
            DateTimeInterval.fromFormattedValue("2019-08-15T10:00:00+07:00/2019-08-15T19:00:00+07:00"),
            createCourier(null))
            .setVolume(new BigDecimal("3.14"))
            .setWeight(new BigDecimal("2.71"));
    }

    private SelfExport createSelfExport() {
        return createSelfExportBuilder().build();
    }

    private ResourceId createSelfExportId() {
        return createResourceId("yandex-id-1", "fulfillment-id-1");
    }
}
