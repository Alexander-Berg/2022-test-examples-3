package ru.yandex.market.mbisfintegration.dao.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbisfintegration.MbiSfAbstractJdbcRecipeTest;
import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.entity.QueueElementStatus;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.Contact;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;


class EntityServiceImplTest extends MbiSfAbstractJdbcRecipeTest {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("salesForceSerializer")
    ObjectMapper objectMapper;

    EntityService service;

    @BeforeEach
    void setup() {
        service = new EntityServiceImpl(jdbcTemplate, objectMapper);
    }

    @Test
    void basicOperations_account() {
        String salesForceId = "2";
        Account data = getAccount(salesForceId);
        Instant now = Instant.now();
        Entity entity = new Entity(1L, ImportEntityType.SHOP, salesForceId, data, now, now);
        service.add(entity);
        Entity found = service.find(1L, ImportEntityType.SHOP, data.getClass());
        Assertions.assertEquals(1L, found.getEntityId());
        Assertions.assertEquals(ImportEntityType.SHOP, found.getImportEntityType());
        Assertions.assertEquals(salesForceId, found.getSalesforceId());
        Assertions.assertEquals("DBS", ((Account) found.getData()).getDistributionSchemeC());
        Assertions.assertEquals("name", ((Account) found.getData()).getName());
        Assertions.assertTrue(((Account) found.getData()).isModelIsSelectedC());
        Assertions.assertFalse(((Account) found.getData()).isCatalogIsUploadedC());
        Assertions.assertEquals(now, found.getCreatedAt());
        Assertions.assertEquals(now, found.getUpdatedAt());

        data.setDistributionSchemeC("FBY");
        service.update(1L, ImportEntityType.SHOP, data);
        Entity foundUpdated = service.find(1L, ImportEntityType.SHOP, data.getClass());
        Assertions.assertEquals("FBY", ((Account) foundUpdated.getData()).getDistributionSchemeC());
    }

    @Test
    void basicOperations_collections() {
        List<Contact> data = List.of(
                new Contact()
                        .withFirstName("firstName1")
                        .withLastName("lastName1")
                        .withEmail("email1@mail.ru")
                        .withPhone("1")
                        .withAccountId("1"),
                new Contact()
                        .withFirstName("firstName2")
                        .withLastName("lastName2")
                        .withEmail("email2@mail.ru")
                        .withPhone("2")
                        .withAccountId("2")
        );
        Instant now = Instant.now();
        Entity entity = new Entity(1L, ImportEntityType.CONTACT, null, data, now, now);
        service.add(entity);
        Entity foundEntity = service.find(1L, ImportEntityType.CONTACT, Contact.class, List.class);
        Assertions.assertEquals(1L, foundEntity.getEntityId());
        Assertions.assertEquals(ImportEntityType.CONTACT, foundEntity.getImportEntityType());
        Assertions.assertNull(foundEntity.getSalesforceId());
        List<Contact> foundEntityData = (List<Contact>) foundEntity.getData();
        Assertions.assertEquals(2, foundEntityData.size());
        Assertions.assertTrue(foundEntityData.stream().map(Contact::getEmail).anyMatch("email1@mail.ru"::equals));
        Assertions.assertTrue(foundEntityData.stream().map(Contact::getEmail).anyMatch("email2@mail.ru"::equals));

        data.get(0).setPhone("11");
        service.update(1L, ImportEntityType.CONTACT, data);
        Entity foundUpdated = service.find(1L, ImportEntityType.CONTACT, Contact.class, List.class);
        Assertions.assertTrue(((List<Contact>) foundUpdated.getData()).stream().map(Contact::getPhone).anyMatch("11"::equals));
    }

    @Test
    void findAllPending() {
        Instant now = Instant.now();
        for (int i = 0; i < 10; i++) {
            service.add(new Entity(((long) i), ImportEntityType.SHOP, String.valueOf(i),
                    getAccount(String.valueOf(i)), now, now));
            jdbcTemplate.update(
                    "insert into queue (entity_id, entity_type, status, created_at)" +
                            " values (:id, :entity_type, :status, :created_at)",
                    new MapSqlParameterSource()
                            .addValue("id", i)
                            .addValue("entity_type", ImportEntityType.SHOP.name())
                            .addValue("status", i % 2 == 0 ? QueueElementStatus.PENDING.name() :
                                    QueueElementStatus.SENT.name())
                            .addValue("created_at", Timestamp.from(Instant.now()))
            );
        }
        Collection<Entity> queued = service.findQueued(ImportEntityType.SHOP, Account.class);
        Assertions.assertEquals(5, queued.size());
    }

    private Account getAccount(String id) {
        Account data = new Account();
        data.setId(id);
        data.setDistributionSchemeC("DBS");
        data.setWebsite("www.ya.ru");
        data.setBusinessIDC("yandex");
        data.setCompanyIDC("campaign_id");
        data.setDescription("description");
        data.setSupplierIDC((double) 3);
        data.setClientIDC((double) 4);
        data.setName("name");
        data.setExpressC(false);
        data.setModelIsSelectedC(true);
        data.setLEIsFilledC(false);
        data.setCatalogIsUploadedC(false);
        data.setReleasedC(false);
        return data;
    }
}