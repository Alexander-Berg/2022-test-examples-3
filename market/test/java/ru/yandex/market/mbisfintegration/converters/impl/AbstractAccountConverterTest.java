package ru.yandex.market.mbisfintegration.converters.impl;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.RecordType;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;
import ru.yandex.market.mbisfintegration.salesforce.AccountType;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 11.02.2022
 */
public class AbstractAccountConverterTest {

    AbstractAccountConverter converter;
    ImportConfiguration importConfiguration;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);

        converter = new AbstractAccountConverter() {
            @Override
            protected AccountType getAccountType() {
                return AccountType.SHOP;
            }
        };
        importConfiguration = new ImportConfiguration(Account.class, "uri", "business_id", ImportEntityType.SHOP);
    }

    @Test
    void newEntityOnlyTest() {
        Entity entity = converter.convert(Map.of("business_id", 123), null, importConfiguration);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("123", ((Account) entity.getData()).getBusinessIDC());
        Assertions.assertEquals("Shop", ((Account) entity.getData()).getRecordType().getName());
    }

    @Test
    void mergeDifferentEntitiesTest() {
        Account account = new Account();
        account.setActiveC(true);
        account.setAgencyC("OldAgency");
        account.setWebsite("websiteold");
        Entity oldEntity = new Entity(123L, ImportEntityType.SHOP, "salesforceId", account);

        Entity newEntity = converter.convert(
                Map.of("business_id", 123, "domain", "websitenew", "campaign_id", 123465),
                oldEntity,
                importConfiguration);

        Assertions.assertNotNull(newEntity);

        Account newAccount = (Account) newEntity.getData();

        Assertions.assertNotSame(account, newAccount);
        Assertions.assertNotEquals(account, newAccount);
        Assertions.assertEquals(true, newAccount.isActiveC());
        Assertions.assertEquals("OldAgency", newAccount.getAgencyC());
        Assertions.assertEquals("websitenew", newAccount.getWebsite());
        Assertions.assertEquals("123465", newAccount.getCompanyIDC());
        Assertions.assertEquals("Shop", newAccount.getRecordType().getName());
    }

    @Test
    void mergeEqualEntitiesTest() {
        Account account = new Account();
        account.setBusinessIDC("123");
        account.setActiveC(true);
        account.setAgencyC("OldAgency");
        account.setWebsite("websiteold");
        account.setCompanyIDC("123465");
        account.setRecordType(new RecordType().withName("Shop"));

        Entity oldEntity = new Entity(123L, ImportEntityType.SHOP, "salesforceId", account);

        Entity newEntity = converter.convert(
                Map.of("business_id", 123, "domain", "websiteold", "campaign_id", 123465),
                oldEntity,
                importConfiguration);

        Assertions.assertNotNull(newEntity);

        Account newAccount = (Account) newEntity.getData();

        Assertions.assertNotSame(account, newAccount);
        Assertions.assertEquals(account, newAccount);
    }
}
