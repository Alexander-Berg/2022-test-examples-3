package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.Validator;
import javax.validation.groups.Default;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.config.properties.FeatureProperties;
import ru.yandex.market.logistics.logistics4shops.logging.LoggingCode;
import ru.yandex.market.logistics.logistics4shops.logging.code.PersonalIntegrationCode;
import ru.yandex.market.logistics.logistics4shops.queue.payload.createorder.LomCreateOrderPayload;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecordFormat.ExceptionPayload;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.RouteOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.lom.model.validation.OrderWithRouteChecks;
import ru.yandex.market.logistics.personal.model.Address;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.personal.PersonalClient;
import ru.yandex.market.personal.enums.PersonalDataType;
import ru.yandex.market.personal.model.PersonalError;
import ru.yandex.market.personal.model.PersonalResponseItem;
import ru.yandex.market.personal.model.PersonalStoreRequestItem;
import ru.yandex.market.personal.model.PersonalStoreResponseItem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@DisplayName("Создание заказа в LOM")
class LomCreateOrderProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private LomCreateOrderProcessor processor;
    @Autowired
    private Validator validator;
    @Autowired
    private LomClient lomClient;
    @Autowired
    private PersonalClient personalClient;
    @Captor
    private ArgumentCaptor<RouteOrderRequestDto> orderCaptor;
    @Captor
    private ArgumentCaptor<List<PersonalStoreRequestItem>> personalCaptor;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient, personalClient);
    }

    @Test
    @DisplayName("Успешная обработка")
    void successProcessing() {
        processor.execute(defaultPayloadBuilder().build());

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(getExpectedValue());

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Успешная обработка - можно удалять товары из заказа")
    void successProcessingWithItemsRemovalEnabled() {
        processor.execute(
            defaultPayloadBuilder()
                .maxAbsentItemsPricePercent(BigDecimal.valueOf(99.9))
                .items(List.of(defaultItemBuilder().removableIfAbsent(Boolean.TRUE).build()))
                .build()
        );

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                getExpectedValue()
                    .setMaxAbsentItemsPricePercent(BigDecimal.valueOf(99.9))
                    .setItems(List.of(getExpectedItemBuilder().removableIfAbsent(Boolean.TRUE).build()))
            );

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Успешная обработка - данные об адресе достаются из геобазы")
    void successProcessingNoAddressInfo() {
        processor.execute(
            defaultPayloadBuilder()
                .recipient(
                    defaultRecipientBuilder()
                        .address(defaultAddressBuilder().country(null).region(null).locality(null).build())
                        .build()
                )
            .build());

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(getExpectedValue(
                getAddress("Россия", "Москва", "Москва"),
                "lastName"
            ));

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Успешная обработка - заказ уже есть в LOM")
    void successProcessingLomConflict() {
        when(lomClient.createOrder(any(RouteOrderRequestDto.class), eq(true)))
            .thenThrow(new HttpTemplateException(409, "Order already exists"));
        processor.execute(defaultPayloadBuilder().build());

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(getExpectedValue());

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Успешная обработка с указанием Personal адреса — функциональность не включена")
    void successWithDisabledPersonal() {
        setupFeature(FeatureProperties::isPersonalAddressConversionEnabled, false);

        processor.execute(getDefaultPayloadWithPersonalAddress());

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(getExpectedValue());

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Успешная обработка с указанием Personal адреса")
    void successWithPersonalAddress() {
        when(personalClient.multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS))))
            .thenReturn(List.of(new PersonalResponseItem(
                "external-personal-id",
                PersonalDataType.ADDRESS,
                externalPersonalAddress("country", "region", "locality")
            )));
        when(personalClient.multiTypesStore(any()))
            .thenAnswer(invocation -> List.of(new PersonalStoreResponseItem(
                "logistics-address-id",
                PersonalDataType.ADDRESS,
                invocation.<List<PersonalStoreRequestItem>>getArgument(0).get(0).getValue(),
                null,
                null
            )));

        processor.execute(getDefaultPayloadWithPersonalAddress());

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(getExpectedValue(getAddress(), "lastName", "logistics-address-id"));

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();

        verify(personalClient).multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS)));
        verify(personalClient).multiTypesStore(personalCaptor.capture());

        softly.assertThat(personalCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(List.of(new PersonalStoreRequestItem(
                PersonalDataType.ADDRESS,
                logisticPersonalAddress("country", "region", "locality")
            )));
        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.info(
                    "Successfully replaced external value external-personal-id with logistic logistics-address-id"
                )
                .setLoggingCode(PersonalIntegrationCode.OK)
                .setExtra(Map.of(
                    "externalPersonalAddressId", "external-personal-id",
                    "personalAddressId", "logistics-address-id"
                ))
        ));
    }

    @Test
    @DisplayName("Успешная обработка с указанием Personal адреса — обогащение адреса")
    void successWithPersonalAddressEnrich() {
        when(personalClient.multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS))))
            .thenReturn(List.of(new PersonalResponseItem(
                "external-personal-id",
                PersonalDataType.ADDRESS,
                externalPersonalAddress(null, " ", "")
            )));
        when(personalClient.multiTypesStore(any()))
            .thenAnswer(invocation -> List.of(new PersonalStoreResponseItem(
                "logistics-address-id",
                PersonalDataType.ADDRESS,
                invocation.<List<PersonalStoreRequestItem>>getArgument(0).get(0).getValue(),
                null,
                null
            )));

        processor.execute(getDefaultPayloadWithPersonalAddress());

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(getExpectedValue(getAddress(), "lastName", "logistics-address-id"));

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();

        verify(personalClient).multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS)));
        verify(personalClient).multiTypesStore(personalCaptor.capture());

        softly.assertThat(personalCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(List.of(new PersonalStoreRequestItem(
                PersonalDataType.ADDRESS,
                logisticPersonalAddress("Россия", "Москва", "Москва")
            )));
        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.info(
                "Successfully replaced external value external-personal-id with logistic logistics-address-id"
            )
                .setLoggingCode(PersonalIntegrationCode.OK)
                .setExtra(Map.of(
                    "externalPersonalAddressId", "external-personal-id",
                    "personalAddressId", "logistics-address-id"
                ))
        ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Неудачный поход в Personal на получении адреса")
    void failWithBadRetrieveRequest(
        @SuppressWarnings("unused") String displayName,
        List<PersonalResponseItem> storeResponse,
        String errorMessage,
        LoggingCode<?> loggingCode
    ) {
        when(personalClient.multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS))))
            .thenReturn(storeResponse);

        softly.assertThatThrownBy(() -> processor.execute(getDefaultPayloadWithPersonalAddress()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(errorMessage);

        verify(personalClient).multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS)));

        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.error(errorMessage)
                .setLoggingCode(loggingCode)
                .setExtra(Map.of("externalPersonalAddressId", "external-personal-id"))
        ));
    }

    @Nonnull
    private static Stream<Arguments> failWithBadRetrieveRequest() {
        return Stream.of(
            Arguments.of(
                "Ничего не нашлось",
                List.of(),
                "Wrong number of records found: []",
                PersonalIntegrationCode.RETRIEVE_WRONG_RESULT_SIZE
            ),
            Arguments.of(
                "Нашлось больше одной записи",
                List.of(
                    new PersonalResponseItem("id1", PersonalDataType.PHONE, null),
                    new PersonalResponseItem("id2", PersonalDataType.EMAIL, null)
                ),
                "Wrong number of records found: [(id1,PHONE), (id2,EMAIL)]",
                PersonalIntegrationCode.RETRIEVE_WRONG_RESULT_SIZE
            ),
            Arguments.of(
                "Нашлась запись некорректного типа",
                List.of(new PersonalResponseItem("external-personal-id", PersonalDataType.PHONE, null)),
                "Bad personal data type returned: PHONE",
                PersonalIntegrationCode.RETRIEVE_BAD_DATA_TYPE
            ),
            Arguments.of(
                "Пустая запись",
                List.of(new PersonalResponseItem("external-personal-id", PersonalDataType.ADDRESS, null)),
                "Empty data in correct Personal response",
                PersonalIntegrationCode.RETRIEVE_EMPTY_DATA
            )
        );
    }

    @Test
    @DisplayName("Исключение при походе в Personal на получении адреса")
    void failWithExceptionalRetrieveRequest() {
        when(personalClient.multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS))))
            .thenThrow(new HttpTemplateException(500, "exception!"));

        softly.assertThatThrownBy(() -> processor.execute(getDefaultPayloadWithPersonalAddress()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Uncaught error on retrieving address from Personal: "
                + "Http request exception: status <500>, response body <exception!>."
            );

        verify(personalClient).multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS)));

        assertLogs()
            .anyMatch(logEqualsTo(
                TskvLogRecord.error(
                    "Uncaught error on retrieving address from Personal: "
                    + "Http request exception: status <500>, response body <exception!>."
                )
                    .setLoggingCode(PersonalIntegrationCode.RETRIEVE_OTHER_ERROR)
                    .setExtra(Map.of("externalPersonalAddressId", "external-personal-id"))
            ))
            .anyMatch(logEqualsTo(
                TskvLogRecord.exception(ExceptionPayload.of(
                    "Uncaught error on retrieving address from Personal",
                    "HttpTemplateException: Http request exception: status <500>, response body <exception!>."
                ))
                    .setCode("ru.yandex.market.logistics.util.client.exception.HttpTemplateException")
            ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Неудачный поход в Personal при сохранении логистического адреса")
    void failWithBadStoreRequest(
        @SuppressWarnings("unused") String displayName,
        List<PersonalStoreResponseItem> response,
        String errorMessage,
        LoggingCode<?> loggingCode
    ) {
        when(personalClient.multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS))))
            .thenReturn(List.of(new PersonalResponseItem(
                "external-personal-id",
                PersonalDataType.ADDRESS,
                externalPersonalAddress("country", "region", "locality")
            )));
        when(personalClient.multiTypesStore(any())).thenReturn(response);

        softly.assertThatThrownBy(() -> processor.execute(getDefaultPayloadWithPersonalAddress()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(errorMessage);

        verify(personalClient).multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS)));
        verify(personalClient).multiTypesStore(personalCaptor.capture());
        softly.assertThat(personalCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(List.of(new PersonalStoreRequestItem(
                PersonalDataType.ADDRESS,
                logisticPersonalAddress("country", "region", "locality")
            )));

        assertLogs().anyMatch(logEqualsTo(
            TskvLogRecord.error(errorMessage)
                .setLoggingCode(loggingCode)
                .setExtra(Map.of("externalPersonalAddressId", "external-personal-id"))
        ));
    }

    @Nonnull
    private static Stream<Arguments> failWithBadStoreRequest() {
        return Stream.of(
            Arguments.of(
                "Не вернулось ничего",
                List.of(),
                "Wrong number of records returned after save: []",
                PersonalIntegrationCode.SAVE_WRONG_RESULT_SIZE
            ),
            Arguments.of(
                "Вернулось больше одной записи",
                List.of(
                    new PersonalStoreResponseItem("id1", PersonalDataType.PHONE, null, null, null),
                    new PersonalStoreResponseItem(null, PersonalDataType.EMAIL, null, null, new PersonalError("error"))
                ),
                "Wrong number of records returned after save: "
                + "[(id1,PHONE,null), (null,EMAIL,PersonalError{value='error'})]",
                PersonalIntegrationCode.SAVE_WRONG_RESULT_SIZE
            ),
            Arguments.of(
                "Вернулась ошибка в ответе",
                List.of(new PersonalStoreResponseItem(null, null, null, null, new PersonalError("you bad"))),
                "Error returned in response: you bad",
                PersonalIntegrationCode.SAVE_RESPONSE_ERROR
            ),
            Arguments.of(
                "Вернулась запись некорректного типа",
                List.of(new PersonalStoreResponseItem("personal-id", PersonalDataType.PHONE, null, null, null)),
                "Bad personal data type returned: PHONE",
                PersonalIntegrationCode.SAVE_BAD_DATA_TYPE
            ),
            Arguments.of(
                "Пустая запись",
                List.of(new PersonalStoreResponseItem(" \n\t ", PersonalDataType.ADDRESS, null, null, null)),
                "Blank personal addressId returned",
                PersonalIntegrationCode.SAVE_BLANK_RESULT
            )
        );
    }

    @Test
    @DisplayName("Исключение при походе в Personal при сохранении логистического адреса")
    void failWithExceptionalStoreRequest() {
        when(personalClient.multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS))))
            .thenReturn(List.of(new PersonalResponseItem(
                "external-personal-id",
                PersonalDataType.ADDRESS,
                externalPersonalAddress("country", "region", "locality")
            )));
        when(personalClient.multiTypesStore(anyList()))
            .thenThrow(new HttpTemplateException(500, "exception!"));

        softly.assertThatThrownBy(() -> processor.execute(getDefaultPayloadWithPersonalAddress()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Uncaught error on saving address to Personal: "
                + "Http request exception: status <500>, response body <exception!>."
            );

        verify(personalClient).multiTypesRetrieve(List.of(Pair.of("external-personal-id", PersonalDataType.ADDRESS)));
        verify(personalClient).multiTypesStore(personalCaptor.capture());
        softly.assertThat(personalCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(List.of(new PersonalStoreRequestItem(
                PersonalDataType.ADDRESS,
                logisticPersonalAddress("country", "region", "locality")
            )));

        assertLogs()
            .anyMatch(logEqualsTo(
                TskvLogRecord.error(
                        "Uncaught error on saving address to Personal: "
                        + "Http request exception: status <500>, response body <exception!>."
                    )
                    .setLoggingCode(PersonalIntegrationCode.SAVE_OTHER_ERROR)
                    .setExtra(Map.of("externalPersonalAddressId", "external-personal-id"))
            ))
            .anyMatch(logEqualsTo(
                TskvLogRecord.exception(ExceptionPayload.of(
                    "Uncaught error on saving address to Personal",
                    "HttpTemplateException: Http request exception: status <500>, response body <exception!>."
                ))
                    .setCode("ru.yandex.market.logistics.util.client.exception.HttpTemplateException")
            ));
    }

    @DisplayName("Валидация входных данных")
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void validationTest(
        @SuppressWarnings("unused") String displayName,
        LomCreateOrderPayload.LomCreateOrderPayloadBuilder<?, ?> payloadBuilder,
        String interpolatedMessage,
        String propertyPath
    ) {
        softly.assertThatThrownBy(() -> processor.execute(payloadBuilder.build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("interpolatedMessage='%s'".formatted(interpolatedMessage))
            .hasMessageContaining("propertyPath=%s".formatted(propertyPath));
    }

    @Nonnull
    static Stream<Arguments> validationTest() {
        return Stream.of(
            payload(),
            recipient(),
            address(),
            items()
        )
            .flatMap(Function.identity());
    }

    @Nonnull
    static Stream<Arguments> payload() {
        return Stream.of(
            Arguments.of("no route", defaultPayloadBuilder().route(null), "must not be empty", "route"),
            Arguments.of("empty route", defaultPayloadBuilder().route(""), "must not be empty", "route"),
            Arguments.of("no orderId", defaultPayloadBuilder().orderId(null), "must not be null", "orderId"),
            Arguments.of("no shopId", defaultPayloadBuilder().shopId(null), "must not be null", "shopId"),
            Arguments.of("no recipient", defaultPayloadBuilder().recipient(null), "must not be null", "recipient"),
            Arguments.of("no items", defaultPayloadBuilder().items(null), "must not be empty", "items"),
            Arguments.of("no items", defaultPayloadBuilder().items(List.of()), "must not be empty", "items"),
            Arguments.of(
                "no item element",
                defaultPayloadBuilder().items(Collections.singletonList(null)),
                "must not be null",
                "items[0].<list element>"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> recipient() {
        return Stream.of(
            Quadruple.of(
                "no recipientFirstName",
                defaultRecipientBuilder().firstName(null).personalFullnameId(null),
                "at least one field must not be blank: [firstName, personalFullnameId]",
                "recipient"
            ),
            Quadruple.of(
                "blank recipientFirstName",
                defaultRecipientBuilder().firstName(" ").personalFullnameId(" "),
                "at least one field must not be blank: [firstName, personalFullnameId]",
                "recipient"),
            Quadruple.of(
                "no recipientPhone",
                defaultRecipientBuilder().phone(null).personalPhoneId(null),
                "at least one field must not be blank: [phone, personalPhoneId]",
                "recipient"
            ),
            Quadruple.of(
                "blank recipientPhone",
                defaultRecipientBuilder().phone(" ").personalPhoneId(" "),
                "at least one field must not be blank: [phone, personalPhoneId]",
                "recipient"
            ),
            Quadruple.of(
                "blank recipientEmail",
                defaultRecipientBuilder().email(" "),
                "must be a well-formed email address",
                "recipient.email"
            ),
            Quadruple.of(
                "invalid recipientEmail",
                defaultRecipientBuilder().email("email"),
                "must be a well-formed email address",
                "recipient.email"
            ),
            Quadruple.of(
                "no recipientAddress",
                defaultRecipientBuilder().address(null),
                "must not be null",
                "recipient.address"
            )
        )
            .map(
                quadruple -> Arguments.of(
                    quadruple.getFirst(),
                    defaultPayloadBuilder().recipient(quadruple.getSecond().build()),
                    quadruple.getThird(),
                    quadruple.getFourth()
                )
            );
    }

    @Nonnull
    static Stream<Arguments> address() {
        return Stream.of(
            Quadruple.of(
                "no adress geoId",
                defaultAddressBuilder().geoId(null),
                "must not be null",
                "recipient.address.geoId"
            )
        )
            .map(
                quadruple -> Arguments.of(
                    quadruple.getFirst(),
                    defaultPayloadBuilder()
                        .recipient(defaultRecipientBuilder().address(quadruple.getSecond().build()).build()),
                    quadruple.getThird(),
                    quadruple.getFourth()
                )
            );

    }

    @Nonnull
    static Stream<Arguments> items() {
        return Stream.of(
            Quadruple.of(
                "no item title",
                defaultItemBuilder().title(null),
                "must not be blank",
                "items[0].title"
            ),
            Quadruple.of(
                "blank item title",
                defaultItemBuilder().title(" "),
                "must not be blank",
                "items[0].title"
            ),
            Quadruple.of(
                "no item shopId",
                defaultItemBuilder().shopId(null),
                "must not be null",
                "items[0].shopId"
            ),
            Quadruple.of(
                "no item ssku",
                defaultItemBuilder().ssku(null),
                "must not be blank",
                "items[0].ssku"
            ),
            Quadruple.of(
                "blank item ssku",
                defaultItemBuilder().ssku(" "),
                "must not be blank",
                "items[0].ssku"
            ),
            Quadruple.of(
                "no item msku",
                defaultItemBuilder().msku(null),
                "must not be blank",
                "items[0].msku"
            ),
            Quadruple.of(
                "blank item msku",
                defaultItemBuilder().msku(" "),
                "must not be blank",
                "items[0].msku"
            ),
            Quadruple.of(
                "no item count",
                defaultItemBuilder().count(null),
                "must not be null",
                "items[0].count"
            ),
            Quadruple.of(
                "negative item count",
                defaultItemBuilder().count(0L),
                "must be greater than 0",
                "items[0].count"
            ),
            Quadruple.of(
                "no price value",
                defaultItemBuilder().price(defaultPriceBuilder().value(null).build()),
                "must not be null",
                "items[0].price.value"
            ),
            Quadruple.of(
                "zero price value",
                defaultItemBuilder().price(defaultPriceBuilder().value(-1L).build()),
                "must be greater than or equal to 0",
                "items[0].price.value"
            ),
            Quadruple.of(
                "null price isoCurrencyCode",
                defaultItemBuilder().price(defaultPriceBuilder().isoCurrencyCode(null).build()),
                "must not be null",
                "items[0].price.isoCurrencyCode"
            ),
            Quadruple.of(
                "invalid price isoCurrencyCode",
                defaultItemBuilder().price(defaultPriceBuilder().isoCurrencyCode("code").build()),
                "currency must be one of [RUR, RUB]",
                "items[0].price.isoCurrencyCode"
            ),
            Quadruple.of(
                "no weight",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().weight(null).build()),
                "must not be null",
                "items[0].dimensions.weight"
            ),
            Quadruple.of(
                "negative weight",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().weight(0).build()),
                "must be greater than 0",
                "items[0].dimensions.weight"
            ),
            Quadruple.of(
                "no length",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().length(null).build()),
                "must not be null",
                "items[0].dimensions.length"
            ),
            Quadruple.of(
                "negative length",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().length(0).build()),
                "must be greater than 0",
                "items[0].dimensions.length"
            ),
            Quadruple.of(
                "no width",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().width(null).build()),
                "must not be null",
                "items[0].dimensions.width"
            ),
            Quadruple.of(
                "negative width",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().width(0).build()),
                "must be greater than 0",
                "items[0].dimensions.width"
            ),
            Quadruple.of(
                "no height",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().height(null).build()),
                "must not be null",
                "items[0].dimensions.height"
            ),
            Quadruple.of(
                "negative height",
                defaultItemBuilder().dimensions(defaultDimensionsBuilder().height(0).build()),
                "must be greater than 0",
                "items[0].dimensions.height"
            )
        )
            .map(
                quadruple -> Arguments.of(
                    quadruple.getFirst(),
                    defaultPayloadBuilder().items(List.of(quadruple.getSecond().build())),
                    quadruple.getThird(),
                    quadruple.getFourth()
                )
            );
    }

    @Test
    @DisplayName("Успешная обработка без фамилии получателя")
    void successProcessingWithoutLastName() {
        processor.execute(
            defaultPayloadBuilder()
                .recipient(defaultRecipientBuilder().lastName(null).build())
                .build()
        );

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));
        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(getExpectedValue("-"));

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Успешная обработка без указания ВГХ товара")
    void successProcessingWithoutItemDimensions() {
        processor.execute(
            defaultPayloadBuilder()
                .items(List.of(defaultItemBuilder().dimensions(null).build()))
                .build()
        );

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));

        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        RouteOrderRequestDto expectedValue = getExpectedValue();
        expectedValue.setItems(List.of(
            expectedValue.getItems().get(0).toBuilder()
                .dimensions(null)
                .build()
        ));

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedValue);

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Успешная обработка без указания типа налогообложения товара")
    void successProcessingWithoutVatType() {
        processor.execute(
            defaultPayloadBuilder()
                .items(List.of(defaultItemBuilder().lomVatType(null).build()))
                .build()
        );

        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));

        RouteOrderRequestDto actualValue = orderCaptor.getValue();

        RouteOrderRequestDto expectedValue = getExpectedValue();
        expectedValue.setItems(List.of(
            expectedValue.getItems().get(0).toBuilder()
                .vatType(null)
                .build()
        ));

        softly.assertThat(actualValue)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedValue);

        softly.assertThat(validator.validate(actualValue, OrderWithRouteChecks.class, Default.class)).isEmpty();
    }

    @Test
    @DisplayName("Некорректный тип налогообложения товара")
    void successProcessingWithCorruptedVatType() {
        softly.assertThatThrownBy(() -> processor.execute(
            defaultPayloadBuilder()
                .items(List.of(defaultItemBuilder().vatType("INCORRECT_VAT").build()))
                .build()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No enum constant ru.yandex.market.logistics.lom.model.enums.VatType.INCORRECT_VAT");
    }

    @Test
    @DisplayName("Невалидный маршрут")
    void invalidRoute() {
        softly.assertThatThrownBy(() -> processor.execute(defaultPayloadBuilder().route("INVALID").build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Route for order 1 is unparsable");
    }

    @Nonnull
    private static LomCreateOrderPayload.LomCreateOrderPayloadBuilder<?, ?> defaultPayloadBuilder() {
        return LomCreateOrderPayload.builder()
            .orderId(1L)
            .shopId(101L)
            .route("{\"route\": {\"cost\": 0}}")
            .recipient(defaultRecipientBuilder().build())
            .items(List.of(defaultItemBuilder().build()))
            .comment("Order comment");
    }

    @Nonnull
    private static LomCreateOrderPayload.RecipientPayload.RecipientPayloadBuilder defaultRecipientBuilder() {
        return LomCreateOrderPayload.RecipientPayload.builder()
            .firstName("firstName")
            .lastName("lastName")
            .middleName("middleName")
            .email("email@email.email")
            .phone("phone")
            .personalFullnameId("personal-fullname-id")
            .personalPhoneId("personal-phone-id")
            .personalEmailId("personal-email-id")
            .address(defaultAddressBuilder().build());
    }

    @Nonnull
    private static LomCreateOrderPayload.AddressPayload.AddressPayloadBuilder defaultAddressBuilder() {
        return LomCreateOrderPayload.AddressPayload.builder()
            .country("country")
            .region("region")
            .locality("locality")
            .street("street")
            .house("house")
            .room("room")
            .zipCode("zipCode")
            .porch("porch")
            .floor("2")
            .latitude(BigDecimal.valueOf(10.1))
            .longitude(BigDecimal.valueOf(100.2))
            .geoId(213)
            .personalGpsId("personal-gps-id")
            .intercom("intercom");
    }

    @Nonnull
    private static LomCreateOrderPayload.ItemPayload.ItemPayloadBuilder defaultItemBuilder() {
        return LomCreateOrderPayload.ItemPayload.builder()
            .title("title")
            .shopId(101L)
            .ssku("ssku")
            .msku("23")
            .count(1L)
            .price(defaultPriceBuilder().build())
            .cargoTypes(List.of(300, 301))
            .dimensions(defaultDimensionsBuilder().build())
            .lomVatType(VatType.VAT_20);
    }

    @Nonnull
    private static LomCreateOrderPayload.PricePayload.PricePayloadBuilder  defaultPriceBuilder() {
        return LomCreateOrderPayload.PricePayload.builder().value(100L).isoCurrencyCode("RUB");
    }

    @Nonnull
    private static LomCreateOrderPayload.DimensionsPayload.DimensionsPayloadBuilder defaultDimensionsBuilder() {
        return LomCreateOrderPayload.DimensionsPayload.builder()
            .weight(1234)
            .length(42)
            .width(53)
            .height(64);
    }

    @Nonnull
    private LomCreateOrderPayload getDefaultPayloadWithPersonalAddress() {
        return defaultPayloadBuilder()
            .recipient(
                defaultRecipientBuilder()
                    .address(defaultAddressBuilder().personalAddressId("external-personal-id").build())
                    .build()
            )
            .build();
    }

    @Nonnull
    @SneakyThrows
    private RouteOrderRequestDto getExpectedValue(AddressDto addressDto, String lastName, String personalAddressId) {
        return (RouteOrderRequestDto) new RouteOrderRequestDto()
            .setRoute(objectMapper.readValue("{\"route\": {\"cost\": 0}}", JsonNode.class))
            .setExternalId("1")
            .setSenderId(101L)
            .setSenderName("Яндекс.Маркет")
            .setSenderUrl("pokupki.market.yandex.ru")
            .setPlatformClientId(PlatformClient.FAAS.getId())
            .setRecipient(
                RecipientDto.builder()
                    .firstName("firstName")
                    .lastName(lastName)
                    .middleName("middleName")
                    .email("email@email.email")
                    .address(addressDto)
                    .personalFullnameId("personal-fullname-id")
                    .personalEmailId("personal-email-id")
                    .personalAddressId(personalAddressId)
                    .personalGpsId("personal-gps-id")
                    .build()
            )
            .setContacts(List.of(
                OrderContactDto.builder()
                    .contactType(ContactType.RECIPIENT)
                    .firstName("firstName")
                    .lastName(lastName)
                    .middleName("middleName")
                    .phone("phone")
                    .personalFullnameId("personal-fullname-id")
                    .personalPhoneId("personal-phone-id")
                    .build()
            ))
            .setCost(
                CostDto.builder()
                    .paymentMethod(PaymentMethod.PREPAID)
                    .cashServicePercent(BigDecimal.ZERO)
                    .assessedValue(new BigDecimal("1.00"))
                    .total(new BigDecimal("1.00"))
                    .delivery(BigDecimal.ZERO)
                    .deliveryForCustomer(BigDecimal.ZERO)
                    .manualDeliveryForCustomer(BigDecimal.ZERO)
                    .isFullyPrepaid(true)
                    .services(List.of(
                        OrderServiceDto.builder()
                            .code(ShipmentOption.INSURANCE)
                            .cost(BigDecimal.ZERO)
                            .customerPay(false)
                            .build()
                    ))
                    .build()
            )
            .setItems(List.of(getExpectedItemBuilder().build()))
            .setMaxAbsentItemsPricePercent(null)
            .setComment("Order comment");
    }

    @Nonnull
    @SneakyThrows
    private RouteOrderRequestDto getExpectedValue(AddressDto addressDto, String lastName) {
        return getExpectedValue(addressDto, lastName, null);
    }

    @Nonnull
    @SneakyThrows
    private RouteOrderRequestDto getExpectedValue() {
        return getExpectedValue("lastName");
    }

    @Nonnull
    @SneakyThrows
    private RouteOrderRequestDto getExpectedValue(String lastName) {
        return getExpectedValue(getAddress(), lastName);
    }

    @Nonnull
    private AddressDto getAddress() {
        return getAddress("country", "region", "locality");
    }

    @Nonnull
    private AddressDto getAddress(String country, String region, String locality) {
        return AddressDto.builder()
            .country(country)
            .region(region)
            .locality(locality)
            .street("street")
            .house("house")
            .room("room")
            .zipCode("zipCode")
            .porch("porch")
            .floor(2)
            .latitude(BigDecimal.valueOf(10.1))
            .longitude(BigDecimal.valueOf(100.2))
            .geoId(213)
            .intercom("intercom")
            .build();
    }

    @Nonnull
    private ItemDto.ItemDtoBuilder getExpectedItemBuilder() {
        return ItemDto.builder()
            .name("title")
            .vendorId(101L)
            .article("ssku")
            .count(1)
            .price(
                MonetaryDto.builder()
                    .currency("RUB")
                    .value(new BigDecimal("1.00"))
                    .exchangeRate(BigDecimal.ONE)
                    .build()
            )
            .assessedValue(
                MonetaryDto.builder()
                    .currency("RUB")
                    .value(new BigDecimal("1.00"))
                    .exchangeRate(BigDecimal.ONE)
                    .build()
            )
            .removableIfAbsent(false)
            .cargoTypes(Set.of(CargoType.BULKY_CARGO, CargoType.BULKY_CARGO_20_KG))
            .msku(23L)
            .dimensions(
                KorobyteDto.builder()
                    .weightGross(new BigDecimal("1.234"))
                    .length(42)
                    .width(53)
                    .height(64)
                    .build()
            )
            .vatType(VatType.VAT_20);
    }

    @Nonnull
    private static Address externalPersonalAddress(String country, String region, String locality) {
        return Address.builder()
            .country(country)
            .federalDistrict("federalDistrict")
            .region(region)
            .subRegion("subRegion")
            .settlement("settlement")
            .district("district")
            .street("street")
            .house("house")
            .building("building")
            .floor(2)
            .comment("comment")
            .postcode("zipCode")
            .city(locality)
            .subway("metro")
            .km("42")
            .estate("estate")
            .block("housing")
            .entrance("porch")
            .entryPhone("intercom")
            .apartment("room")
            .build();
    }

    @Nonnull
    private static Address logisticPersonalAddress(String country, String region, String locality) {
        return Address.builder()
            .country(country)
            .federalDistrict("federalDistrict")
            .region(region)
            .locality(locality)
            .subRegion("subRegion")
            .settlement("settlement")
            .district("district")
            .street("street")
            .house("house")
            .building("building")
            .housing("housing")
            .room("room")
            .zipCode("zipCode")
            .porch("porch")
            .floor(2)
            .metro("metro")
            .geoId(213)
            .intercom("intercom")
            .comment("comment")
            .km("42")
            .estate("estate")
            .build();
    }
}
