package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.RegisterType;
import ru.yandex.market.logistic.api.model.fulfillment.RegisterUnit;
import ru.yandex.market.logistic.api.model.fulfillment.RegisterUnitType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.TransportationRegister;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutRegisterResponse;
import ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotEmptyErrorMessage;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotNullErrorMessage;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getPositiveErrorMessage;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createRegisterUnitBuilder;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createResourceId;

class PutRegisterTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "5927638";
    private static final String PARTNER_ID = "EXT101811250";
    private static final String FULFILLMENT_ID = "EXT101811250";

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    void testPutRegisterSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_put_register", PARTNER_URL);

        PutRegisterResponse response = fulfillmentClient.putRegister(
            DtoFactory.createTransportationRegister(),
            getPartnerProperties()
        );

        assertEquals(getExpectedResponse(), response, "Asserting the response is correct");
    }

    @Test
    void testPutRegisterWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_put_register",
            "ff_put_register_with_errors",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.putRegister(
                DtoFactory.createTransportationRegister(),
                getPartnerProperties()
            )
        );

    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
            Arguments.of(null, getNotNullErrorMessage("register"), "Реестр null"),
            Arguments.of(
                new TransportationRegister.TransportationRegisterBuilder(
                    null,
                    RegisterType.YANDEX,
                    ImmutableList.of(createRegisterUnitBuilder("root-register-unit").build())
                )
                    .build(),
                getNotNullErrorMessage("register.transportationRequestId"),
                "Идентификатор реестра null"
            ),
            Arguments.of(
                new TransportationRegister.TransportationRegisterBuilder(
                    createResourceId("transportation-register-yandex-1", "transportation-fulfillment-1"),
                    null,
                    ImmutableList.of(createRegisterUnitBuilder("root-register-unit").build())
                )
                    .build(),
                getNotNullErrorMessage("register.registerType"),
                "Тип реестра null"
            ),
            Arguments.of(
                new TransportationRegister.TransportationRegisterBuilder(
                    createResourceId("transportation-register-yandex-1", "transportation-fulfillment-1"),
                    RegisterType.YANDEX,
                    null
                )
                    .build(),
                getNotEmptyErrorMessage("register.registerUnits"),
                "Список грузомест реестра null"
            ),
            Arguments.of(
                new TransportationRegister.TransportationRegisterBuilder(
                    createResourceId("transportation-register-yandex-1", "transportation-fulfillment-1"),
                    RegisterType.YANDEX,
                    ImmutableList.of()
                )
                    .build(),
                getNotEmptyErrorMessage("register.registerUnits"),
                "Список грузомест реестра пуст"
            ),
            Arguments.of(
                new TransportationRegister.TransportationRegisterBuilder(
                    createResourceId("transportation-register-yandex-1", "transportation-fulfillment-1"),
                    RegisterType.YANDEX,
                    ImmutableList.of(new RegisterUnit.RegisterUnitBuilder(null).build())
                )
                    .build(),
                getNotNullErrorMessage("register.registerUnits[0].registerUnitType"),
                "Тип грузоместа null"
            ),
            Arguments.of(
                new TransportationRegister.TransportationRegisterBuilder(
                    createResourceId("transportation-register-yandex-1", "transportation-fulfillment-1"),
                    RegisterType.YANDEX,
                    ImmutableList.of(
                        new RegisterUnit.RegisterUnitBuilder(RegisterUnitType.BOX)
                            .setChildRegisterUnits(
                                ImmutableList.of(new RegisterUnit.RegisterUnitBuilder(null).build())
                            )
                            .build()
                    )
                )
                    .build(),
                getNotNullErrorMessage("register.registerUnits[0].childRegisterUnits[0].registerUnitType"),
                "Тип вложенного грузоместа null"
            ),
            Arguments.of(
                new TransportationRegister.TransportationRegisterBuilder(
                    createResourceId("transportation-register-yandex-1", "transportation-fulfillment-1"),
                    RegisterType.YANDEX,
                    ImmutableList.of(createRegisterUnitBuilder("root-register-unit").setAmount(-1).build())
                )
                    .build(),
                getPositiveErrorMessage("register.registerUnits[0].amount"),
                "Количество товаров в грузоместе отрицательно"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidRequests")
    void testPutRegisterValidationFailed(
        TransportationRegister transportationRegister,
        String errorMessage,
        String displayName
    ) {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.putRegister(transportationRegister, getPartnerProperties())
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(errorMessage);
    }

    private PutRegisterResponse getExpectedResponse() {
        return new PutRegisterResponse(
            new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setPartnerId(PARTNER_ID)
                .setFulfillmentId(FULFILLMENT_ID)
                .build());
    }
}
