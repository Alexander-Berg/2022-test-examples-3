package ru.yandex.market.mbisfintegration.converters.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbisfintegration.converters.impl.mbi.ContactConverter;
import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.Contact;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;

import static org.assertj.core.api.Assertions.assertThat;

class ContactConverterTest {
    @Mock
    EntityService entityService;

    private static Contact clone(Contact old) {
        return (Contact) old.clone();
    }

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void newEntityOnlyTest() {
        mockGetAllIds(ImportEntityType.SHOP, 1L);
        var dto = Map.of(
                "contact_id", "123",
                "phone", "1",
                "shops", Map.of("shop", List.of(Map.of("shop_id", "1", "type", "SHOP")))
        );
        assertThat(convert(dto, null))
                .hasSize(1)
                .contains(
                        new Contact()
                                .withPhone("1")
                                .withExternalIDC("123")
                                .withAccount(new Account().withShopIDC(1.0))
                                .withLastName("<Фамилия>")
                );
    }

    @Test
    @Description("Создаем только один контакт, даже когда у него несколько связей")
    void shouldCreateSingleContact() {
        mockGetAllIds(ImportEntityType.SHOP, 1L);
        mockGetAllIds(ImportEntityType.SUPPLIER, 2L);
        var dto = Map.of(
                "contact_id", "123",
                "phone", "1",
                "shops", Map.of("shop", List.of(
                        Map.of("shop_id", "1", "type", "SHOP"),
                        Map.of("shop_id", "2", "type", "SUPPLIER")
                ))
        );
        Contact expectedResult = new Contact().withPhone("1").withExternalIDC("123");
        assertThat(convert(dto, null))
                .hasSize(1)
                .containsAnyOf(
                        clone(expectedResult).withAccount(new Account().withShopIDC(1.0)).withLastName("<Фамилия>"),
                        clone(expectedResult).withAccount(new Account().withSupplierIDC(2.0)).withLastName("<Фамилия>")
                );
    }

    @Test
    void mergeDifferentEntitiesTest() {
        mockGetAllIds(ImportEntityType.SHOP, 1L);
        Account account = new Account().withShopIDC(1.0);
        List<Contact> oldContacts = List.of(new Contact().withAccount(account).withPhone("1"));
        Entity oldEntity = new Entity(123L, ImportEntityType.CONTACT, null, oldContacts);
        var dto = Map.of(
                "contact_id", "123",
                "phone", "2",
                "emails", Map.of("email", "test@mail.ru"),
                "shops", Map.of("shop", List.of(Map.of("shop_id", "1", "type", "SHOP")))
        );
        assertThat(convert(dto, oldEntity))
                .isNotSameAs(oldContacts)
                .isNotEqualTo(oldContacts)
                .hasSize(1)
                .contains(
                        new Contact()
                                .withPhone("2")
                                .withAccount(account)
                                .withLastName("<Фамилия>")
                                .withEmail("test@mail.ru")
                                .withExternalIDC("123")
                );
    }

    @Test
    @Description("Если контакт связан только с неподдерживаемым аккаунтом, не создаем ничего")
    void ignoreBusinessContactTest() {
        var dto = Map.of(
                "contact_id", "123",
                "phone", "2",
                "emails", Map.of("email", "1@mail.ru"),
                "shops", Map.of(
                        "shop", List.of(
                                Map.of("shop_id", "1", "type", "BUSINESS")
                        )
                )
        );
        assertThat(convert(dto, null)).hasSize(0);
    }

    @Disabled("Контакт можно создать без связи, в этом нет проблем")
    @Test
    void ignoreContactWithoutAccountTest() {
        mockGetAllIds(ImportEntityType.SHOP); //в бд нет подходящих шопов
        var dto = Map.of(
                "contact_id", "123",
                "phone", "2",
                "emails", Map.of("email", "1@mail.ru"),
                "shops", Map.of(
                        "shop", List.of(
                                Map.of("shop_id", "1", "type", "SHOP")
                        )
                )
        );
        assertThat(convert(dto, null)).hasSize(0);
    }

    @SuppressWarnings("unchecked")
    private List<Contact> convert(Map<String, Object> dto, Entity oldEntity) {
        var converter = new ContactConverter(entityService);
        var importConfiguration = new ImportConfiguration(Contact.class, "uri", "contact_id", ImportEntityType.CONTACT);
        return (List<Contact>) converter.convert(dto, oldEntity, importConfiguration).getData();
    }

    private void mockGetAllIds(ImportEntityType type, Long... ids) {
        Mockito.when(entityService.getAllIds(type)).thenReturn(Set.of(ids));
    }
}