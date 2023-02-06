package ru.yandex.market.mbi.api.controller.contact;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.api.client.entity.contact.PartnerReturnContactList;
import ru.yandex.market.mbi.api.client.entity.contact.ReturnContactType;
import ru.yandex.market.mbi.api.client.entity.contact.SupplierReturnContact;
import ru.yandex.market.mbi.api.config.FunctionalTest;


/**
 * Тест на управление контаками поставщиков
 *
 * @author stani on 15.02.18.
 */
@DbUnitDataSet(before = "PartnerReturnContactControllerTest.before.csv")
class PartnerReturnContactControllerTest extends FunctionalTest {

    /**
     * Проверка получения контакта партнера.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getPartnerReturnContactArgs")
    void getSupplierReturnContactTest(String name, long partnerId) {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(partnerId)
                .email("vasya@yandex.ru")
                .firstName("Василий")
                .middleName("Александрович")
                .lastName("Теркин")
                .phoneNumber("+79161002030")
                .build();
        SupplierReturnContact returnContact = mbiApiClient.getSupplierReturnContact(partnerId);
        ReflectionAssert.assertReflectionEquals(expected, returnContact);
    }

    /**
     * Проверка получения контакта поставщика - новая версия для множества контактов.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getPartnerReturnContactArgs")
    void getSuppliersReturnContactsTest(String name, long partnerId) {
        Map<ReturnContactType, SupplierReturnContact> expected = Map.of(
                ReturnContactType.PERSON, SupplierReturnContact.builder()
                        .supplierId(partnerId)
                        .email("vasya@yandex.ru")
                        .firstName("Василий")
                        .middleName("Александрович")
                        .lastName("Теркин")
                        .phoneNumber("+79161002030")
                        .type(ReturnContactType.PERSON)
                        .build(),
                ReturnContactType.FEEDBACK, SupplierReturnContact.builder()
                        .supplierId(partnerId)
                        .workSchedule("ПН-ВС 24/7")
                        .firstName("Василий")
                        .middleName("Александрович")
                        .lastName("Теркин")
                        .phoneNumber("+79161002030")
                        .type(ReturnContactType.FEEDBACK)
                        .build()
        );
        Map<ReturnContactType, SupplierReturnContact> returnContacts = mbiApiClient.getPartnerReturnContacts(partnerId)
                .stream()
                .collect(Collectors.toMap(SupplierReturnContact::getType, Function.identity()));
        ReflectionAssert.assertReflectionEquals(expected, returnContacts);
    }

    /**
     * Проверка получения контакта, несуществующего партнера
     */
    @Test
    void getNotFoundSupplierReturnContactTest() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getSupplierReturnContact(404L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    /**
     * Проверка получения контакта, несуществующего партнера - новая версия для множества контактов.
     */
    @Test
    void getNotFoundSuppliersReturnContactsTest() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getPartnerReturnContacts(404L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    /**
     * Проверка получения контакта, которого нет.
     */
    @Test
    void getEmptySupplierReturnContactTest() {
        SupplierReturnContact expected = SupplierReturnContact.emptyContact(103L);
        SupplierReturnContact returnContact = mbiApiClient.getSupplierReturnContact(103L);
        ReflectionAssert.assertReflectionEquals(expected, returnContact);
    }

    /**
     * Проверка получения контакта, которого нет - новая версия для множества контактов.
     */
    @Test
    void getEmptySuppliersReturnContactsTest() {
        SupplierReturnContact expected = SupplierReturnContact.emptyContact(103L);
        List<SupplierReturnContact> returnContacts = mbiApiClient.getPartnerReturnContacts(103L);
        ReflectionAssert.assertReflectionEquals(expected, returnContacts.stream().findFirst().orElse(null));
    }


    /**
     * Проверка получения ошибки 400, если в теле и в path partnerId  при обновлении рассходятся.
     */
    @Test
    void updateSupplierReturnContactNotMatchTest() {
        SupplierReturnContact partnerReturnContact = new SupplierReturnContact();
        partnerReturnContact.setSupplierId(102L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updateSupplierReturnContact(101L, partnerReturnContact, 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    /**
     * Проверка получения ошибки 400, если в теле и в path partnerId  при обновлении рассходятся.
     * Новая версия для множества контактов.
     */
    @Test
    void updateSuppliersReturnContactsNotMatchTest() {
        SupplierReturnContact partnerReturnContact = new SupplierReturnContact();
        partnerReturnContact.setSupplierId(102L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerReturnContacts(101L,
                        new PartnerReturnContactList(
                                Collections.singletonList(partnerReturnContact)), 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    /**
     * Проверка получения ошибки 404 при попытке найти контакты на возврат несуществующего gfhnythf.
     */
    @Test
    void updateSupplierReturnContactNotFoundTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(101L)
                .email("vasya@yandex.ru")
                .firstName("Василий")
                .lastName("Теркин")
                .phoneNumber("+79161002030")
                .build();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updateSupplierReturnContact(101L, expected, 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );

    }

    /**
     * Проверка получения ошибки 404 при попытке найти контакты на возврат несуществующего поставщика.
     * Новая версия для множества контактов.
     */
    @Test
    void updateSupplierReturnContactsNotFoundTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(101L)
                .email("vasya@yandex.ru")
                .firstName("Василий")
                .lastName("Теркин")
                .phoneNumber("+79161002030")
                .build();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerReturnContacts(101L,
                        new PartnerReturnContactList(Collections.singletonList(expected)), 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );

    }

    /**
     * Проверка добавления контакта поставщика
     * Новая версия для множества контактов.
     */
    @Test
    @DbUnitDataSet(after = "createSupplierReturnContactTest.after.csv")
    void createSupplierReturnContactTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(102L)
                .email("alexey@yandex.ru")
                .firstName("Алексей")
                .middleName("Петрович")
                .lastName("Метелкин")
                .phoneNumber("+79161005060")
                .build();
        mbiApiClient.updateSupplierReturnContact(102L, expected, 12345L);
    }

    /**
     * Проверка добавления контакта ДСБС
     * Новая версия для множества контактов.
     */
    @Test
    @DbUnitDataSet(after = "createDsbsReturnContactTest.after.csv")
    void createDsbsReturnContactTest() {
        SupplierReturnContact returnContact = SupplierReturnContact.builder()
                .supplierId(202L)
                .email("alexey@yandex.ru")
                .firstName("Алексей")
                .middleName("Петрович")
                .lastName("Метелкин")
                .phoneNumber("+79161005060")
                .build();
        mbiApiClient.updateSupplierReturnContact(202L, returnContact, 12345L);
    }

    /**
     * Проверка добавления контакта поставщика
     * Новая версия для множества контактов.
     */
    @Test
    @DbUnitDataSet(after = "deletePartnerReturnContactTest.after.csv")
    void deleteSupplierReturnContactTest() {
        mbiApiClient.deletePartnerReturnContact(100L, ReturnContactType.PERSON, 12345L);
        mbiApiClient.deletePartnerReturnContact(100L, ReturnContactType.SELF, 12345L);
        mbiApiClient.deletePartnerReturnContact(100L, ReturnContactType.FEEDBACK, 12345L);
        mbiApiClient.deletePartnerReturnContact(200L, ReturnContactType.PERSON, 12345L);
        mbiApiClient.deletePartnerReturnContact(200L, ReturnContactType.SELF, 12345L);
        mbiApiClient.deletePartnerReturnContact(200L, ReturnContactType.FEEDBACK, 12345L);
    }

    /**
     * Проверка добавления контакта поставщика
     * Новая версия для множества контактов.
     */
    @Test
    @DbUnitDataSet(after = "createSupplierReturnContactTest.after.csv")
    void createSupplierReturnContactsTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(102L)
                .email("alexey@yandex.ru")
                .firstName("Алексей")
                .middleName("Петрович")
                .lastName("Метелкин")
                .phoneNumber("+79161005060")
                .build();
        mbiApiClient.updatePartnerReturnContacts(102L,
                new PartnerReturnContactList(Collections.singletonList(expected)), 12345L);
    }


    /**
     * Проверка добавления контакта поставщика
     * Новая версия для множества контактов.
     */
    @Test
    @DbUnitDataSet(after = "createSupplierAllTypesReturnContactTest.after.csv")
    void createSupplierAllReturnContactsTypeTest() {
        Collection<SupplierReturnContact> expected = Arrays.asList(
                SupplierReturnContact.builder()
                        .supplierId(102L)
                        .email("alexey@yandex.ru")
                        .firstName("Алексей")
                        .middleName("Петрович")
                        .lastName("Метелкин")
                        .phoneNumber("+79161005060")
                        .type(ReturnContactType.PERSON)
                        .build(),
                SupplierReturnContact.builder()
                        .supplierId(102L)
                        .email("petr@yandex.ru")
                        .firstName("Петр")
                        .middleName("Алексеевич")
                        .lastName("Петелькин")
                        .phoneNumber("+79161005061")
                        .jobPosition("Супермен")
                        .address("Льва Толстого 16")
                        .type(ReturnContactType.CARRIER)
                        .build(),
                SupplierReturnContact.builder()
                        .supplierId(102L)
                        .address("Льва Толстого 16")
                        .type(ReturnContactType.POST)
                        .companyName("Чебурашка inc")
                        .build(),
                SupplierReturnContact.builder()
                        .supplierId(102L)
                        .email("rulon@aboev.ru")
                        .firstName("Рулон")
                        .middleName("Николаевич")
                        .lastName("Абоев")
                        .phoneNumber("911")
                        .jobPosition("Маляр")
                        .address("В Балашихе в новостройке")
                        .type(ReturnContactType.SELF)
                        .comments("Позвонить уточнить сколько обоев вернуть")
                        .build()
        );
        mbiApiClient.updatePartnerReturnContacts(102L, new PartnerReturnContactList(expected), 12345L);
    }

    /**
     * Проверка обновления контакта поставщика
     */
    @Test
    @DbUnitDataSet(after = "updateSupplierReturnContactTest.after.csv")
    void updateSupplierReturnContactTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(100L)
                .email("ivan@yandex.ru")
                .firstName("Иван")
                .middleName("")
                .lastName("Стелькин")
                .phoneNumber("+791820055555")
                .build();
        mbiApiClient.updateSupplierReturnContact(100L, expected, 12345L);
    }

    /**
     * Проверка обновления контакта поставщика
     * Новая версия для множества контактов.
     */
    @Test
    @DbUnitDataSet(after = "updateCarrierTypeSupplierReturnContactTest.after.csv")
    void updateCarrierTypeSupplierReturnContactsTest() {
        Collection<SupplierReturnContact> expected = Collections.singletonList(
                SupplierReturnContact.builder()
                        .supplierId(102L)
                        .email("petr@yandex.ru")
                        .firstName("Петр")
                        .middleName("Алексеевич")
                        .lastName("Петелькин")
                        .phoneNumber("+79161005061")
                        .jobPosition("Супермен")
                        .address("Льва Толстого 16")
                        .type(ReturnContactType.CARRIER)
                        .build()
        );
        mbiApiClient.updatePartnerReturnContacts(102L, new PartnerReturnContactList(expected), 12345L);

        expected = Collections.singletonList(
                SupplierReturnContact.builder()
                        .supplierId(102L)
                        .email("petr@yandex.ru")
                        .firstName("Петр")
                        .middleName("Алексеевич")
                        .lastName("Петелькин")
                        .phoneNumber("+79161005068")
                        .jobPosition("Супермен")
                        .address("Льва Толстого 18")
                        .type(ReturnContactType.CARRIER)
                        .build()
        );
        mbiApiClient.updatePartnerReturnContacts(102L, new PartnerReturnContactList(expected), 12345L);
    }

    /**
     * Проверка обновления контакта поставщика
     * Новая версия для множества контактов.
     */
    @Test
    @DbUnitDataSet(after = "updateSupplierReturnContactTest.after.csv")
    void updateSupplierReturnContactsTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(100L)
                .email("ivan@yandex.ru")
                .firstName("Иван")
                .middleName("")
                .lastName("Стелькин")
                .phoneNumber("+791820055555")
                .build();
        mbiApiClient.updatePartnerReturnContacts(100L,
                new PartnerReturnContactList(Collections.singletonList(expected)), 12345L);
    }

    /**
     * Проверка получения ошибки 400 при пропущенном обезательном поле в теле запроса
     */
    @Test
    void updateSupplierReturnContactRequiredMissedTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(100L)
                .email("ivan@yandex.ru")
                .lastName("Стелькин")
                .phoneNumber("+791820055555")
                .build();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updateSupplierReturnContact(100L, expected, 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    /**
     * Проверка получения ошибки 400 при пропущенном обезательном поле в теле запроса
     * Новая версия для множества контактов.
     */
    @Test
    void updateSupplierReturnContactsRequiredMissedTest() {
        SupplierReturnContact expected = SupplierReturnContact.builder()
                .supplierId(100L)
                .email("ivan@yandex.ru")
                .lastName("Стелькин")
                .phoneNumber("+791820055555")
                .build();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerReturnContacts(100L,
                        new PartnerReturnContactList(Collections.singletonList(expected)), 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    private static Stream<Arguments> getPartnerReturnContactArgs() {
        return Stream.of(
                Arguments.of("Получение контактов возврата поставщика", 100L),
                Arguments.of("Получение контактов возврата ДСБС", 200L)
        );
    }

}
