package ru.yandex.market.b2bcrm.module.contact.test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.account.AbstractDataImportTest;
import ru.yandex.market.b2bcrm.module.account.B2bAccountContactRelation;
import ru.yandex.market.b2bcrm.module.account.B2bContact;
import ru.yandex.market.b2bcrm.module.account.Business;
import ru.yandex.market.b2bcrm.module.account.ImportMbiOffset;
import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.account.Supplier;
import ru.yandex.market.b2bcrm.module.account.impl.B2bContactsImporter;
import ru.yandex.market.b2bcrm.module.utils.AccountModuleTestUtils;
import ru.yandex.market.jmf.dataimport.conf.datasource.StreamType;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.jmf.entity.test.assertions.EntityAttributeMatcher.havingAttributes;

public class B2bContactDataImportTest extends AbstractDataImportTest {

    @Inject
    B2bContactsImporter b2bContactsImporter;
    @Inject
    AccountModuleTestUtils accountModuleTestUtils;

    @Test
    public void xmlContactTest() {
        // инициализация дополнительных данных
        Shop shopDemo = bcpService.create(Shop.FQN, Map.of(
                Shop.TITLE, "Демо Магазин",
                Shop.CAMPAIGN_ID, 1000148L,
                Shop.CLIENT_ID, 1123581321L,
                Shop.CPA_BUDGET, 1000000L,
                Shop.SHOP_ID, 555L
        ));

        Supplier supplierDemo = bcpService.create(Supplier.FQN, Map.of(
                Supplier.TITLE, "Демо Магазин 2",
                Supplier.CAMPAIGN_ID, 1000149L,
                Supplier.CLIENT_ID, 1123581322L,
                Supplier.SUPPLIER_ID, 666L,
                Supplier.BUSINESS_ID, 777L
        ));

        Business businessDemo = bcpService.create(Business.FQN, Map.of(
                Business.BUSINESS_ID, 777L, Business.TITLE, "777")
        );

        B2bContact b2bContactOldLoaded = bcpService.edit(
                accountModuleTestUtils.createMbiContact("Иванов Иван Иванович", 1L, false),
                B2bContact.UPDATE_TIME, OffsetDateTime.parse("2020-10-01T01:00:16.833Z")
        );

        B2bContact b2bContactOldLoaded2 = bcpService.edit(
                accountModuleTestUtils.createMbiContact("Зернов Никита", 279L, false),
                B2bContact.UPDATE_TIME, OffsetDateTime.parse("2020-10-01T01:00:16.833Z")
        );

        B2bContact b2bContactOldManuallyCreated = bcpService.edit(
                accountModuleTestUtils.createMbiContact("Петров Пётр Петрович", 2L, true),
                B2bContact.UPDATE_TIME, OffsetDateTime.parse("2020-10-01T01:00:16.833Z")
        );

        B2bAccountContactRelation b2bAccountContactRelationOld = accountModuleTestUtils.createMbiRelation(
                shopDemo, b2bContactOldLoaded, "SHOP_ADMIN"
        );

        B2bAccountContactRelation b2bAccountContactRelationToRemove = bcpService.edit(
                accountModuleTestUtils.createMbiRelation(supplierDemo, b2bContactOldLoaded2, "SHOP_ADMIN"),
                B2bAccountContactRelation.UPDATE_TIME, OffsetDateTime.parse("2020-10-01T01:00:16.833Z")
        );

        B2bAccountContactRelation b2bBusinessContactRelationToRemove = bcpService.edit(
                accountModuleTestUtils.createMbiRelation(businessDemo, b2bContactOldLoaded2, "SHOP_SUPER_ADMIN"),
                B2bAccountContactRelation.UPDATE_TIME, OffsetDateTime.parse("2020-10-01T01:00:16.833Z")
        );

        // загрузка
        b2bContactsImporter.loadAndUpdateContacts("classpath:b2bContact.xml", null, "b2bContact.xml",
                StreamType.DEFAULT
        );

        List<Entity> list = dbService.list(Query.of(B2bContact.FQN));
        EntityCollectionAssert.assertThat(list)
                .hasSize(4)
                .anyHasAttributes(
                        B2bContact.CONTACT_ID, 279L,
                        B2bContact.TITLE, "Зернов Никитa",
                        B2bContact.CLIENT_ID, 49850L,
                        B2bContact.PHONES, hasSize(2),
                        B2bContact.PHONES, containsInAnyOrder("+7(812) 329-3617", "78123293617"),
                        B2bContact.SECOND_NAME, null,
                        B2bContact.ACCOUNT_CONTACT_RELATIONS, hasSize(1),
                        B2bContact.ACCOUNT_CONTACT_RELATIONS, contains(havingAttributes("account", shopDemo))
                )
                .anyHasAttributes(
                        B2bContact.CONTACT_ID, 479L,
                        B2bContact.TITLE, "Рудаков Николай Владимирович",
                        B2bContact.CLIENT_ID, 10899L,
                        B2bContact.PHONES, hasSize(2),
                        B2bContact.PHONES, containsInAnyOrder("+7 (926)2126020", "79262126020"),
                        B2bContact.SECOND_NAME, "Владимирович",
                        B2bContact.ACCOUNT_CONTACT_RELATIONS, hasSize(4),
                        B2bContact.ACCOUNT_CONTACT_RELATIONS, hasItems(havingAttributes("account", supplierDemo),
                                havingAttributes("account", businessDemo))
                )

                //Автоматически созданный контакт должен быть архивирован
                .anyHasAttributes(
                        B2bContact.CONTACT_ID, 1L,
                        B2bContact.ARCHIVED, true
                )

                //Созданный вручную контакт должен остаться
                .anyHasAttributes(
                        B2bContact.CONTACT_ID, 2L,
                        B2bContact.SOURCE_SYSTEM, "MANUALLY"
                );

        EntityCollectionAssert.assertThat(dbService.list(Query.of(ImportMbiOffset.FQN)))
                .anyHasAttributes(
                        ImportMbiOffset.QUEUE, "b2bContact.xml",
                        ImportMbiOffset.OFFSET, OffsetDateTime.parse("2020-10-07T01:00:16.833Z")
                );

        EntityCollectionAssert.assertThat(dbService.list(Query.of(B2bAccountContactRelation.FQN)))
                //Удалили старую связь для супплаера
                .noneHasAttributes(
                        B2bAccountContactRelation.ACCOUNT, supplierDemo,
                        B2bAccountContactRelation.CONTACT, b2bContactOldLoaded2
                )
                //Удалили старую связь для бизнеса
                .noneHasAttributes(
                        B2bAccountContactRelation.ACCOUNT, businessDemo,
                        B2bAccountContactRelation.CONTACT, b2bContactOldLoaded2
                );
    }

}
