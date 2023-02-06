package ru.yandex.market.mbisfintegration.importer.mbi;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbisfintegration.converters.impl.mbi.ContactRelationsConverter;
import ru.yandex.market.mbisfintegration.datapreparation.impl.MultipleRecordsPreparationService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.AccountContactRelation;
import ru.yandex.market.mbisfintegration.generated.sf.model.Contact;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.salesforce.query.SfQueryService;

import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.CONTACT;
import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.CONTACT_RELATIONS;
import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.SHOP;
import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.SUPPLIER;

class ContactRelationsImportTest extends AbstractMbiImportTest {

    private static final Long SUPPLIER_ID = 123L;
    private static final Long SHOP_ID = 456L;
    private static final Long CONTACT_ID = 789L;
    private static final Account SUPPLIER_DATA = new Account().withSupplierIDC(SUPPLIER_ID.doubleValue());
    private static final Account SHOP_DATA = new Account().withShopIDC(SHOP_ID.doubleValue());
    private static final Contact CONTACT_DATA = new Contact().withExternalIDC(CONTACT_ID.toString());

    @Autowired
    SfQueryService sfQueryService;

    @Autowired
    MultipleRecordsPreparationService dataPreparationService;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        entityService.add(new Entity(SUPPLIER_ID, SUPPLIER, null, SUPPLIER_DATA));
        entityService.add(new Entity(SHOP_ID, SHOP, null, SHOP_DATA));
        entityService.add(new Entity(CONTACT_ID, CONTACT, null, List.of(CONTACT_DATA)));
        converter = new ContactRelationsConverter(entityService);
        entityClass = AccountContactRelation.class;
        entityType = CONTACT_RELATIONS;
        importConfig = new ImportConfiguration(entityClass, "uri", "contact_id", entityType);
    }

    @Test
    void testImportRelationsWithMerge() {
        var existedSupplierRelation = new AccountContactRelation()
                .withContact(CONTACT_DATA)
                .withAccount(SUPPLIER_DATA)
                .withExternalIDC("789_Supplier_123")
                .withMakedForDeletionC(false);
        entityService.add(new Entity(CONTACT_ID, CONTACT_RELATIONS, null, List.of(existedSupplierRelation)));
        doImport(dataPreparationService, "classpath:/import.sources/contact_crm_export.xml");
        Assertions.assertThat(this.<AccountContactRelation>findEntityDataList(CONTACT_ID))
                .hasSize(2)
                .contains(
                        existedSupplierRelation.withMakedForDeletionC(true),
                        new AccountContactRelation()
                                .withContact(CONTACT_DATA)
                                .withAccount(SHOP_DATA)
                                .withExternalIDC("789_Shop_456")
                                .withMakedForDeletionC(false)
                );
    }
}
