package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.personal.converter.AddressConverter;
import ru.yandex.market.logistics.personal.model.Address;
import ru.yandex.market.personal.PersonalClient;
import ru.yandex.market.personal.enums.PersonalDataType;
import ru.yandex.market.personal.model.PersonalError;
import ru.yandex.market.personal.model.PersonalResponseItem;
import ru.yandex.market.personal.model.PersonalStoreRequestItem;
import ru.yandex.market.personal.model.PersonalStoreResponseItem;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Тест сервис получения и обработки персональных данных")
class PersonalDataServiceTest extends AbstractTest {
    private final FeatureProperties properties = new FeatureProperties();
    private final AddressConverter addressConverter = new AddressConverter();
    private final Appender traceAppender = mock(Appender.class);

    @Mock
    private PersonalClient personalClient;
    private PersonalDataService personalDataService;

    @BeforeEach
    public void setUp() {
        Logger logger = (((Logger) LoggerFactory.getLogger(PersonalDataService.class)));
        logger.addAppender(traceAppender);
        logger.setLevel(Level.DEBUG);
        properties.setConvertCheckoutPersonalAddressToLogisticsPersonalAddress(false);
        personalDataService = new PersonalDataService(properties, personalClient, addressConverter);
    }

    @Test
    @DisplayName("Получение: Клиент ничего не возвращает")
    void convertToLogisticsAddressStringEmptyResponse() {
        properties.setConvertCheckoutPersonalAddressToLogisticsPersonalAddress(true);
        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("test");

        verify(traceAppender, Mockito.times(2)).doAppend(argumentCaptor.capture());

        assertLogMessage(argumentCaptor, 0, "Personal: converting personalAddressId={}");
        assertLogMessage(argumentCaptor, 1, "Personal: receiving address by personalAddressId={}");
    }

    @Test
    @DisplayName("Получение: Клиент возвращает не тип Address")
    void convertToLogisticsAddressStringResponseWithOtherType() {
        properties.setConvertCheckoutPersonalAddressToLogisticsPersonalAddress(true);
        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        when(personalClient.multiTypesRetrieve(anyList())).thenReturn(
            List.of(new PersonalResponseItem("test", PersonalDataType.EMAIL, "email"))
        );

        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("test");

        verify(traceAppender, Mockito.times(3)).doAppend(argumentCaptor.capture());

        assertLogMessage(argumentCaptor, 0, "Personal: converting personalAddressId={}");
        assertLogMessage(argumentCaptor, 1, "Personal: receiving address by personalAddressId={}");
        assertLogMessage(
            argumentCaptor,
            2,
            "Personal: PersonalClient response item is not PersonalDataType.ADDRESS. personalAddressId={}"
        );
    }

    @Test
    @DisplayName("Получение: Клиент возвращает не чекаутерный адрес")
    void convertToLogisticsAddressStringResponseWithNotCheckoutAddress() {
        properties.setConvertCheckoutPersonalAddressToLogisticsPersonalAddress(true);
        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        when(personalClient.multiTypesRetrieve(anyList())).thenReturn(
            List.of(
                new PersonalResponseItem(
                    "test",
                    PersonalDataType.ADDRESS,
                    Address.builder().metro("smolenskaya").build())
            )
        );

        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("test");

        verify(traceAppender, Mockito.times(3)).doAppend(argumentCaptor.capture());

        assertLogMessage(argumentCaptor, 0, "Personal: converting personalAddressId={}");
        assertLogMessage(argumentCaptor, 1, "Personal: receiving address by personalAddressId={}");
        assertLogMessage(
            argumentCaptor,
            2,
            "Personal: PersonalClient response item is not checkout address. personalAddressId={}"
        );
    }

    @Test
    @DisplayName("Сохранение: Клиент кидает исключение")
    void convertToLogisticsAddressStringWithFailToSave() {
        ArgumentCaptor<Appender> argumentCaptor = prepareGoodResponseWithCaptor();
        when(personalClient.multiTypesStore(anyList())).thenThrow(new RuntimeException("test"));

        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> personalDataService.convertToLogisticsAddressId("test"),
            "Expected RuntimeException() to throw, but it didn't"
        );

        softly.assertThat(thrown.getMessage()).isEqualTo("test");

        verify(traceAppender, Mockito.times(5)).doAppend(argumentCaptor.capture());

        assertLogMessage(argumentCaptor, 2, "Personal: saving address by personalAddressId={}");
    }

    @Test
    @DisplayName("Сохранение: Клиент возвращает ошибку в ответе")
    void convertToLogisticsAddressStringSaveWithError() {
        ArgumentCaptor<Appender> argumentCaptor = prepareGoodResponseWithCaptor();
        when(personalClient.multiTypesStore(anyList())).thenReturn(List.of(new PersonalStoreResponseItem(
            null,
            PersonalDataType.EMAIL,
            "email",
            "email",
            new PersonalError("ERROR")
        )));

        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("test");

        verify(traceAppender, Mockito.times(6)).doAppend(argumentCaptor.capture());

        assertLogMessage(argumentCaptor, 5, "PersonalClient response item has error={}");
    }

    @Test
    @DisplayName("Сохранение: Клиент возвращает не адрес")
    void convertToLogisticsAddressStringSaveWithBadType() {
        ArgumentCaptor<Appender> argumentCaptor = prepareGoodResponseWithCaptor();
        when(personalClient.multiTypesStore(anyList())).thenReturn(List.of(new PersonalStoreResponseItem(
            null,
            PersonalDataType.EMAIL,
            "email",
            "email",
            null
        )));

        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("test");

        verify(traceAppender, Mockito.times(6)).doAppend(argumentCaptor.capture());

        assertLogMessage(
            argumentCaptor,
            5,
            "Personal: PersonalClient store response item is not PersonalDataType.ADDRESS. "
        );
    }

    @Test
    @DisplayName("Сохранение: Клиент возвращает пустой id")
    void convertToLogisticsAddressStringSaveWithEmptyId() {
        ArgumentCaptor<Appender> argumentCaptor = prepareGoodResponseWithCaptor();
        when(personalClient.multiTypesStore(anyList())).thenReturn(List.of(new PersonalStoreResponseItem(
            null,
            PersonalDataType.ADDRESS,
            "email",
            "email",
            null
        )));

        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("test");

        verify(traceAppender, Mockito.times(6)).doAppend(argumentCaptor.capture());

        assertLogMessage(
            argumentCaptor,
            5,
            "Personal: PersonalClient store response item id is blank"
        );
    }

    @Test
    @DisplayName("Получение и Сохранение: ОК")
    void convertToLogisticsAddressOk() {
        ArgumentCaptor<Appender> argumentCaptor = prepareGoodResponseWithCaptor();
        when(personalClient.multiTypesStore(anyList())).thenReturn(List.of(new PersonalStoreResponseItem(
            "newAddressId",
            PersonalDataType.ADDRESS,
            "email",
            "email",
            null
        )));

        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("newAddressId");

        verify(traceAppender, Mockito.times(6)).doAppend(argumentCaptor.capture());

        assertLogMessage(
            argumentCaptor,
            5,
            "Personal: saved address with new id={}"
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получение и Сохранение: ОК")
    void convertToLogisticsAddressAndNormalize(
        String name,
        Address address,
        Address convertedAddress,
        Integer logsCount,
        String message
    ) {
        ArgumentCaptor<Appender> argumentCaptor = prepareGoodResponseWithCaptor(address);
        ArgumentCaptor<List<PersonalStoreRequestItem>> storeRequestCaptor = ArgumentCaptor.forClass(List.class);
        when(personalClient.multiTypesStore(anyList())).thenReturn(List.of(new PersonalStoreResponseItem(
            "newAddressId",
            PersonalDataType.ADDRESS,
            "email",
            "email",
            null
        )));

        softly.assertThat(personalDataService.convertToLogisticsAddressId("test")).isEqualTo("newAddressId");

        verify(personalClient).multiTypesStore(storeRequestCaptor.capture());

        verify(traceAppender, Mockito.times(logsCount)).doAppend(argumentCaptor.capture());

        var aha = storeRequestCaptor.getAllValues().get(0).get(0);
        softly.assertThat(aha.getValue()).isEqualTo(convertedAddress);

        assertLogMessage(argumentCaptor, 4, message);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Заполнение данными адреса")
    void convertAndEnrichToLogisticsAddressId(
        String name,
        AddressDto addressDto,
        Address address,
        Address convertedAddress
    ) {
        ArgumentCaptor<Appender> argumentCaptor = prepareGoodResponseWithCaptor(address);
        ArgumentCaptor<List<PersonalStoreRequestItem>> storeRequestCaptor = ArgumentCaptor.forClass(List.class);
        when(personalClient.multiTypesStore(anyList())).thenReturn(List.of(new PersonalStoreResponseItem(
            "newAddressId",
            PersonalDataType.ADDRESS,
            "email",
            "email",
            null
        )));

        softly.assertThat(personalDataService.convertAndEnrichToLogisticsAddressId("test", addressDto))
            .isEqualTo("newAddressId");

        verify(personalClient).multiTypesStore(storeRequestCaptor.capture());

        var aha = storeRequestCaptor.getAllValues().get(0).get(0);

        softly.assertThat(aha.getValue()).isEqualTo(convertedAddress);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Отключённая фича конвертации")
    void propsOffSource(String key, String name) {
        softly.assertThat(personalDataService.convertToLogisticsAddressId(key)).isEqualTo(key);
        verify(personalClient, Mockito.never()).multiTypesStore(anyList());
    }

    @Nonnull
    static Stream<Arguments> propsOffSource() {
        return Stream.of(
            of("test", "Нормальный ключ"),
            of("", "Пустой ключ")
        );
    }

    @Nonnull
    static Stream<Arguments> convertToLogisticsAddressAndNormalize() {
        return Stream.of(
            of(
                "Конвертируем регион и адрес при гео 213",
                Address.builder().geoId(213).region("test").subway("test").build(),
                Address.builder().geoId(213).region("Москва и Московская область").metro("test").build(),
                6,
                "Personal: normalizing address.region for locationId={}"
            ),
            of(
                "Конвертируем регион и адрес при гео 10747",
                Address.builder().geoId(10747).region("test").subway("test").build(),
                Address.builder().geoId(10747).region("Москва и Московская область").metro("test").build(),
                6,
                "Personal: normalizing address.region for locationId={}"
            ),
            of(
                "Конвертируем адрес при гео 1",
                Address.builder().geoId(1).region("test").subway("test").build(),
                Address.builder().geoId(1).region("test").metro("test").build(),
                5,
                "Personal: saved address with new id={}"
            ),
            of(
                "Конвертируем адрес при гео = NULL",
                Address.builder().region("test").subway("test").metro("test").build(),
                Address.builder().region("test").metro("test").build(),
                5,
                "Personal: saved address with new id={}"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> convertAndEnrichToLogisticsAddressId() {
        return Stream.of(
            of(
                "Добавляем поля 5 полей: geoId, country, federalDistrict, subRegion, region",
                AddressDto.builder()
                    .geoId(213)
                    .country("test")
                    .federalDistrict("test")
                    .subRegion("test")
                    .region("test")
                    .build(),
                Address.builder().subway("test").build(),
                Address.builder()
                    .country("test")
                    .federalDistrict("test")
                    .subRegion("test")
                    .geoId(213)
                    .region("Москва и Московская область")
                    .metro("test")
                    .build()
            ),
            of(
                "Не добавляем поля, потому что они уже заполнены",
                AddressDto.builder()
                    .geoId(1)
                    .country("test")
                    .federalDistrict("test")
                    .subRegion("test")
                    .region("test")
                    .build(),
                Address.builder()
                    .country("no_test")
                    .federalDistrict("no_test")
                    .subRegion("no_test")
                    .geoId(213)
                    .subway("test")
                    .build(),
                Address.builder()
                    .country("no_test")
                    .federalDistrict("no_test")
                    .subRegion("no_test")
                    .geoId(213)
                    .region("Москва и Московская область")
                    .metro("test")
                    .build()
            )
        );
    }

    void assertLogMessage(ArgumentCaptor<Appender> argumentCaptor, int index, String message) {
        softly.assertThat(
            ((LoggingEvent) argumentCaptor.getAllValues().get(index)).getMessage()
        ).contains(message);
    }

    @Nonnull
    private ArgumentCaptor<Appender> prepareGoodResponseWithCaptor() {
        return prepareGoodResponseWithCaptor(Address.builder()
            .geoId(213)
            .region("test")
            .subway("smolenskaya").build());
    }

    @Nonnull
    private ArgumentCaptor<Appender> prepareGoodResponseWithCaptor(Address address) {
        properties.setConvertCheckoutPersonalAddressToLogisticsPersonalAddress(true);
        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        when(personalClient.multiTypesRetrieve(anyList())).thenReturn(
            List.of(
                new PersonalResponseItem(
                    "test",
                    PersonalDataType.ADDRESS,
                    address
                )
            )
        );
        return argumentCaptor;
    }
}
