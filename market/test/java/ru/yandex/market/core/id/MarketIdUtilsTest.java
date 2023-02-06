package ru.yandex.market.core.id;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactLink;
import ru.yandex.market.core.contact.model.ContactRole;
import ru.yandex.market.core.contact.model.ContactWithEmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

/**
 * Тесты для {@link MarketIdUtils}.
 *
 * @author Vadim Lyalin
 */
public class MarketIdUtilsTest {
    /**
     * Проверяет метод {@link MarketIdUtils#needUpdateMarketId(Contact, Contact)}.
     *
     * @param oldContact старая версия контакта
     * @param contact    новая версия контакта
     * @param result     ожидаемый результат
     */
    @ParameterizedTest(name = INDEX_PLACEHOLDER)
    @MethodSource("testNeedUpdateMarketIdArgs")
    void testNeedUpdateMarketId(Contact oldContact, Contact contact, boolean result) {
        assertEquals(result, MarketIdUtils.needUpdateMarketId(oldContact, contact));
    }

    /**
     * Проверяет падения метода {@link MarketIdUtils#needUpdateMarketId(Contact, Contact)}.
     *
     * @param oldContact       старая версия контакта
     * @param contact          новая версия контакта
     * @param exceptionMessage ожидаемое сообщение об ошибке
     */
    @ParameterizedTest(name = INDEX_PLACEHOLDER)
    @MethodSource("testFailNeedUpdateMarketIdArgs")
    void testFailNeedUpdateMarketId(Contact oldContact, Contact contact, String exceptionMessage) {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> MarketIdUtils.needUpdateMarketId(oldContact, contact));
        assertThat(exception.getMessage()).startsWith(exceptionMessage);
    }

    private static Stream<Arguments> testNeedUpdateMarketIdArgs() {
        List<ContactLink> oldContactLinks = List.of(
                new ContactLink(1, 1, Set.of(new ContactRole(1, InnerRole.SHOP_OPERATOR.getCode()))),
                new ContactLink(2, 2, Set.of(new ContactRole(2, InnerRole.SHOP_ADMIN.getCode()))),
                new ContactLink(3, 3, Set.of(new ContactRole(3, InnerRole.BUSINESS_ADMIN.getCode()),
                        new ContactRole(4, InnerRole.SHOP_OPERATOR.getCode())))
        );

        Contact oldContact = new Contact();
        oldContact.setLinks(Set.copyOf(oldContactLinks));

        List<ContactLink> contactLinks = new ArrayList<>(oldContactLinks);
        contactLinks.add(new ContactLink(0, 5, Set.of(new ContactRole(1, InnerRole.SHOP_OPERATOR.getCode()))));
        ContactWithEmail contact = new ContactWithEmail();
        contact.setMarketOnly(true);
        contact.setPhone("123");
        contact.setFirstName("Вася");
        contact.setEmails(Set.of(new ContactEmail(0, "ya@ya.ru", true, true)));
        contact.setPosition("директор");
        contact.setLinks(Set.copyOf(contactLinks));

        Contact delete1Contact = new Contact();
        delete1Contact.setLinks(oldContactLinks.stream().filter(cl -> cl.getId() != 1).collect(Collectors.toSet()));

        Contact delete2Contact = new Contact();
        delete2Contact.setLinks(oldContactLinks.stream().filter(cl -> cl.getId() != 2).collect(Collectors.toSet()));

        Contact update3OperatorContact = new Contact();
        update3OperatorContact.setLinks(oldContactLinks.stream()
                .map(cl -> cl.getId() == 3
                        ? new ContactLink(3, 3, Set.of(new ContactRole(3, InnerRole.SHOP_ADMIN.getCode()))) : cl)
                .collect(Collectors.toSet()));

        Contact update3AdminContact = new Contact();
        update3AdminContact.setLinks(oldContactLinks.stream()
                .map(cl -> cl.getId() == 3
                        ? new ContactLink(3, 3, Set.of(new ContactRole(4, InnerRole.SHOP_OPERATOR.getCode()))) : cl)
                .collect(Collectors.toSet()));

        return Stream.of(
                // при добавлении имени, телефона и прочих нерелевантных полей и добавлении линка с ролью оператор
                // обновление маркетИд не требуется
                Arguments.of(oldContact, contact, false),
                // при удалении линка 1 с ролью не SHOP_ADMIN обновление не требуется
                Arguments.of(oldContact, delete1Contact, false),
                // при удалении линка 2 с ролью SHOP_ADMIN обновление требуется
                Arguments.of(oldContact, delete2Contact, true),
                // при удалении из линка 3 роли SHOP_OPERATOR обновление не требуется
                Arguments.of(oldContact, update3OperatorContact, false),
                // при удалении из линка 3 роли SHOP_ADMIN обновление требуется
                Arguments.of(oldContact, update3AdminContact, true)
        );
    }

    private static Stream<Arguments> testFailNeedUpdateMarketIdArgs() {
        Contact oldNoLinkIdContact = new Contact();
        oldNoLinkIdContact.setLinks(Set.of(new ContactLink()));

        Contact oldContact = new Contact();
        oldContact.setLinks(Set.of());

        Contact contact = new Contact();
        contact.setLinks(Set.of(new ContactLink(1, 1, Set.of(new ContactRole(1, 1)))));

        return Stream.of(
                // В старом контакте линк без id
                Arguments.of(oldNoLinkIdContact, null, "Incorrect contactLinkId found for old contact:"),
                // В новом контекте есть линк, которого нет в старом
                Arguments.of(oldContact, contact, "Old contactLink not found for new contact:")
        );
    }
}
