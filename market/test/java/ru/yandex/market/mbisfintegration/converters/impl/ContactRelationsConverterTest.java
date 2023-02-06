package ru.yandex.market.mbisfintegration.converters.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbisfintegration.converters.impl.mbi.ContactRelationsConverter;
import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.AccountContactRelation;
import ru.yandex.market.mbisfintegration.generated.sf.model.Contact;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;

import static org.assertj.core.api.Assertions.assertThat;

public class ContactRelationsConverterTest {
    public static final Long CONTACT_ID = 123L;

    @Mock
    EntityService entityService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        mockGetAllIds(ImportEntityType.CONTACT, CONTACT_ID);
    }

    @Test
    void shouldCreateSingleRelation() {
        mockGetAllIds(ImportEntityType.SHOP, 1L);
        var dto = Map.of(
                "contact_id", "123",
                "shops", Map.of("shop", List.of(Map.of("shop_id", "1", "type", "SHOP")))
        );
        assertThat(convert(dto, null))
                .hasSize(1)
                .contains(
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(1.0))
                                .withExternalIDC("123_Shop_1")
                                .withMakedForDeletionC(false)
                );
    }

    @Test
    void shouldCreateMultipleRelations() {
        mockGetAllIds(ImportEntityType.SHOP, 1L, 3L);
        mockGetAllIds(ImportEntityType.SUPPLIER, 2L);
        var dto = Map.of(
                "contact_id", CONTACT_ID.toString(),
                "shops", Map.of("shop", List.of(
                        Map.of("shop_id", "1", "type", "SHOP"),
                        Map.of("shop_id", "2", "type", "SUPPLIER"),
                        Map.of("shop_id", "3", "type", "SHOP")
                ))
        );
        assertThat(convert(dto, null))
                .hasSize(3)
                .contains(
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(1.0))
                                .withExternalIDC("123_Shop_1")
                                .withMakedForDeletionC(false),
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withSupplierIDC(2.0))
                                .withExternalIDC("123_Supplier_2")
                                .withMakedForDeletionC(false),
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(3.0))
                                .withExternalIDC("123_Shop_3")
                                .withMakedForDeletionC(false)
                );
    }

    @Test
    void shouldIgnoreUnsupportedRelations() {
        mockGetAllIds(ImportEntityType.SUPPLIER, 2L);
        mockGetAllIds(ImportEntityType.SHOP, 3L);
        var dto = Map.of(
                "contact_id", CONTACT_ID.toString(),
                "shops", Map.of("shop", List.of(
                        Map.of("shop_id", "1", "type", "BUSINESS"),
                        Map.of("shop_id", "2", "type", "SUPPLIER"),
                        Map.of("shop_id", "3", "type", "SHOP")
                ))
        );
        assertThat(convert(dto, null))
                .hasSize(2)
                .contains(
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withSupplierIDC(2.0))
                                .withExternalIDC("123_Supplier_2")
                                .withMakedForDeletionC(false),
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(3.0))
                                .withExternalIDC("123_Shop_3")
                                .withMakedForDeletionC(false)
                );
    }

    @Test
    void shouldMarkRelationsForDeletion() {
        mockGetAllIds(ImportEntityType.SHOP, 1L);
        mockGetAllIds(ImportEntityType.SUPPLIER, 2L);
        List<AccountContactRelation> oldRelations = List.of(
                new AccountContactRelation()
                        .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                        .withAccount(new Account().withShopIDC(3.0))
                        .withExternalIDC("123_Shop_3")
                        .withMakedForDeletionC(false)
        );
        Entity oldEntity = new Entity(CONTACT_ID, ImportEntityType.CONTACT_RELATIONS, null, oldRelations);
        var dto = Map.of(
                "contact_id", CONTACT_ID.toString(),
                "shops", Map.of("shop", List.of(
                        Map.of("shop_id", "1", "type", "SHOP"),
                        Map.of("shop_id", "2", "type", "SUPPLIER")
                ))
        );
        assertThat(convert(dto, oldEntity))
                .hasSize(3)
                .contains(
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(1.0))
                                .withExternalIDC("123_Shop_1")
                                .withMakedForDeletionC(false),
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withSupplierIDC(2.0))
                                .withExternalIDC("123_Supplier_2")
                                .withMakedForDeletionC(false),
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(3.0))
                                .withExternalIDC("123_Shop_3")
                                .withMakedForDeletionC(true)
                );
    }

    @Description("Проверяются удаление, игнорирование неподдерживаемых, создание новых связий в одном сценарии")
    @Test
    void mergeRelationsComplexTest() {
        mockGetAllIds(ImportEntityType.SHOP, 1L);
        mockGetAllIds(ImportEntityType.SUPPLIER, 2L);
        List<AccountContactRelation> oldRelations = List.of(
                new AccountContactRelation()
                        .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                        .withAccount(new Account().withShopIDC(3.0))
                        .withExternalIDC("123_Shop_1")
                        .withMakedForDeletionC(false),
                new AccountContactRelation()
                        .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                        .withAccount(new Account().withSupplierIDC(2.0))
                        .withExternalIDC("123_Supplier_2")
                        .withMakedForDeletionC(false),
                new AccountContactRelation()
                        .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                        .withAccount(new Account().withShopIDC(3.0))
                        .withExternalIDC("123_Shop_3")
                        .withMakedForDeletionC(false)
        );
        Entity oldEntity = new Entity(CONTACT_ID, ImportEntityType.CONTACT_RELATIONS, null, oldRelations);
        var dto = Map.of(
                "contact_id", CONTACT_ID.toString(),
                "shops", Map.of("shop", List.of(
                        Map.of("shop_id", "1", "type", "SHOP"),
                        Map.of("shop_id", "2", "type", "SUPPLIER"),
                        Map.of("shop_id", "4", "type", "BUSINESS")
                ))
        );
        assertThat(convert(dto, oldEntity))
                .hasSize(3)
                .contains(
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(1.0))
                                .withExternalIDC("123_Shop_1")
                                .withMakedForDeletionC(false),
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withSupplierIDC(2.0))
                                .withExternalIDC("123_Supplier_2")
                                .withMakedForDeletionC(false),
                        new AccountContactRelation()
                                .withContact(new Contact().withExternalIDC(CONTACT_ID.toString()))
                                .withAccount(new Account().withShopIDC(3.0))
                                .withExternalIDC("123_Shop_3")
                                .withMakedForDeletionC(true)
                );
    }


    private void mockGetAllIds(ImportEntityType type, Long... ids) {
        Mockito.when(entityService.getAllIds(type)).thenReturn(Set.of(ids));
    }

    @SuppressWarnings("unchecked")
    private List<AccountContactRelation> convert(Map<String, Object> dto, Entity oldEntity) {
        var converter = new ContactRelationsConverter(entityService);
        var importConfiguration = new ImportConfiguration(AccountContactRelation.class, "uri", "contact_id",
                ImportEntityType.CONTACT_RELATIONS);
        return (List<AccountContactRelation>) converter.convert(dto, oldEntity, importConfiguration).getData();
    }
}
