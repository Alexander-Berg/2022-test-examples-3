package ru.yandex.market.logistics.lom.converter.marketId;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.embedded.Credentials;

@DisplayName("LegalInfoConverter юнит тест")
public class LegalInfoConverterTest extends AbstractTest {

    public static final String OGRN = "ogrn";
    public static final String INN = "inn";
    public static final String ADDRESS = "address";
    public static final String NAME = "name";
    public static final String IP = "IP";
    public static final String IP_LOWER_CASE = "ip";
    public static final String IP_CYRILLIC = "ИП";
    public static final String IP_LOWER_CASE_CYRILLIC = "ип";
    public static final String INCORRECT_LEGAL_FORM = "POP";
    public static final String VALID_URL = "beru.ru";

    private LegalInfoConverter legalInfoConverter = new LegalInfoConverter();

    @DisplayName("Проверяем конвертацию юр. данных из MarketId, метод обогащения заказа")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("parameters")
    void convertToCredentials(LegalInfo legalInfo, Credentials expectedCredentials) {
        Credentials credentials = legalInfoConverter.toCredentials(legalInfo);

        assertCredentials(credentials, expectedCredentials);
    }

    @DisplayName("Проверяем конвертацию юр. данных из MarketId, метод обогащения вэйбиллов")
    @ParameterizedTest(name = "[{index}] url: {1}")
    @MethodSource("parametersWithUrls")
    void convertToCredentialsWithUrls(LegalInfo legalInfo, String url, Credentials expectedCredentials) {
        Credentials credentials = legalInfoConverter.toCredentials(legalInfo, url);

        assertCredentials(credentials, expectedCredentials);
    }

    private void assertCredentials(Credentials actual, Credentials expected) {
        softly.assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(simple(), expected()),
            Arguments.of(lowercaseLegalForm(), expected()),
            Arguments.of(cyrillicLegalForm(), expected()),
            Arguments.of(lowercaseCyrillicLegalForm(), expected()),
            Arguments.of(incorrectLegalForm(), expectedIncorrectLegalForm())
        );
    }

    static Stream<Arguments> parametersWithUrls() {
        return Stream.of(
            Arguments.of(simple(), VALID_URL, expectedWithUrl(VALID_URL)),
            Arguments.of(simple(), "", expectedWithUrl(LegalInfoConverter.URL_STUB)),
            Arguments.of(simple(), null, expectedWithUrl(LegalInfoConverter.URL_STUB))
        );
    }

    private static Credentials expected() {
        return new Credentials()
            .setOgrn(OGRN)
            .setInn(INN)
            .setAddress(ADDRESS)
            .setIncorporation(IP_CYRILLIC + " " + NAME)
            .setName(NAME)
            .setLegalForm(IP)
            .setUrl(LegalInfoConverter.DELIVERY_URL_STUB);
    }

    private static Credentials expectedIncorrectLegalForm() {
        return expected()
            .setIncorporation(NAME)
            .setLegalForm(null);
    }

    private static Credentials expectedWithUrl(String url) {
        return new Credentials()
            .setOgrn(OGRN)
            .setInn(INN)
            .setAddress(ADDRESS)
            .setIncorporation(IP_CYRILLIC + " " + NAME)
            .setName(NAME)
            .setLegalForm(IP)
            .setUrl(url);
    }

    private static LegalInfo simple() {
        return buildLegalInfo(IP);
    }

    private static LegalInfo lowercaseLegalForm() {
        return buildLegalInfo(IP_LOWER_CASE);
    }

    private static LegalInfo cyrillicLegalForm() {
        return buildLegalInfo(IP_CYRILLIC);
    }

    private static LegalInfo lowercaseCyrillicLegalForm() {
        return buildLegalInfo(IP_LOWER_CASE_CYRILLIC);
    }

    private static LegalInfo incorrectLegalForm() {
        return buildLegalInfo(INCORRECT_LEGAL_FORM);
    }

    private static LegalInfo buildLegalInfo(String legalForm) {
        return LegalInfo.newBuilder()
            .setRegistrationNumber(OGRN)
            .setInn(INN)
            .setLegalAddress(ADDRESS)
            .setLegalName(NAME)
            .setType(legalForm)
            .build();
    }
}
