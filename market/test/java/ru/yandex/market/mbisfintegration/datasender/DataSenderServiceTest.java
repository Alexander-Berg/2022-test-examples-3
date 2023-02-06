package ru.yandex.market.mbisfintegration.datasender;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sforce.async.JobInfo;
import com.sforce.async.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.dao.QueueService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.entity.QueueElementStatus;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.Contact;
import ru.yandex.market.mbisfintegration.generated.sf.model.SObject;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;
import ru.yandex.market.mbisfintegration.salesforce.SObjectType;
import ru.yandex.market.mbisfintegration.salesforce.bulk.BulkSfService;
import ru.yandex.market.mbisfintegration.salesforce.bulk.JobResult;
import ru.yandex.monlib.metrics.primitives.Counter;
import ru.yandex.monlib.metrics.registry.MetricRegistry;

class DataSenderServiceTest {
    @Mock
    BulkSfService bulkSfService;
    @Mock
    EntityService entityService;
    @Mock
    QueueService queueService;
    @Mock
    MetricRegistry metricRegistry;
    @Mock
    PlatformTransactionManager platformTransactionManager;

    @Captor
    ArgumentCaptor<List<? extends SObject>> entitiesCaptor;

    @Captor
    ArgumentCaptor<? extends SObject> entityCaptor;

    @Captor
    ArgumentCaptor<Collection<Long>> entityIdsCaptor;

    @Captor
    ArgumentCaptor<String> salesforceIdCaptor;

    TransactionTemplate transactionTemplate;

    DataSenderService dataSenderService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(metricRegistry.counter(Mockito.anyString()))
                .thenReturn(new Counter());

        transactionTemplate = new TransactionTemplate(platformTransactionManager);
        dataSenderService = new DataSenderService(bulkSfService, entityService, queueService, transactionTemplate);
    }

    @Test
    void sendSuppliers_successfullyCreated() {
        Account data = new Account().withSupplierIDC(((double) 1));
        ImportEntityType importEntityType = ImportEntityType.SUPPLIER;
        List<Entity> entities = List.of(new Entity(1L, importEntityType, null, data));
        Mockito.when(entityService.findQueued(Mockito.eq(importEntityType), Mockito.eq(Account.class)))
                .thenReturn(entities);

        Mockito.when(
                bulkSfService.upsert(
                        Mockito.eq(SObjectType.ACCOUNT), Mockito.any(), Mockito.eq("SupplierID__c"))
        ).thenReturn(new JobResult<>(new JobInfo()));

        Result result = new Result();
        result.setCreated(true);
        result.setSuccess(true);
        String salesforceId = "1";
        result.setId(salesforceId);
        Mockito.when(bulkSfService.awaitCompletion(Mockito.any()))
                .thenReturn(Map.of(data, result));

        dataSenderService.sendData(importEntityType);

        Mockito.verify(bulkSfService, Mockito.times(1))
                .upsert(Mockito.eq(SObjectType.ACCOUNT), entitiesCaptor.capture(), Mockito.eq("SupplierID__c"));
        List<? extends SObject> capturedEntities = entitiesCaptor.getValue();
        Assertions.assertEquals(1, capturedEntities.size());
        SObject capturedEntity = capturedEntities.get(0);
        Assertions.assertTrue(capturedEntity instanceof Account);
        Assertions.assertEquals(1, ((Account) capturedEntity).getSupplierIDC());

        Mockito.verify(bulkSfService, Mockito.times(1))
                .awaitCompletion(Mockito.any());

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementsStatusFromPendingToSent(entityIdsCaptor.capture(), Mockito.eq(importEntityType),
                        Mockito.eq(QueueElementStatus.SENT));
        Collection<Long> capturedIds = entityIdsCaptor.getValue();
        Assertions.assertEquals(1, capturedIds.size());
        Assertions.assertTrue(capturedIds.stream().findFirst().isPresent());
        Long capturedId = capturedIds.stream().findFirst().get();
        Assertions.assertEquals(1, capturedId);

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementStatusFromSentTo(Mockito.eq(1L), Mockito.eq(importEntityType),
                        Mockito.eq(QueueElementStatus.SUCCESS));

        Mockito.verify(entityService, Mockito.times(1))
                .update(Mockito.eq(1L), Mockito.eq(importEntityType), salesforceIdCaptor.capture(),
                        entityCaptor.capture());
        String capturedSalesforceId = salesforceIdCaptor.getValue();
        Assertions.assertEquals(salesforceId, capturedSalesforceId);

        SObject capturedEntity2 = entityCaptor.getValue();
        Assertions.assertTrue(capturedEntity2 instanceof Account);
        Assertions.assertEquals(1, ((Account) capturedEntity2).getSupplierIDC());

        Mockito.verify(entityService, Mockito.times(1))
                .findQueued(Mockito.eq(importEntityType), Mockito.eq(Account.class));

        Mockito.verifyNoMoreInteractions(bulkSfService, entityService, queueService);
    }

    @Test
    void sendShops_successfullyUpdated() {
        Account data = new Account().withShopIDC(((double) 1));
        ImportEntityType importEntityType = ImportEntityType.SHOP;
        List<Entity> entities = List.of(new Entity(1L, importEntityType, "1", data));
        Mockito.when(entityService.findQueued(Mockito.eq(importEntityType), Mockito.eq(Account.class)))
                .thenReturn(entities);

        Mockito.when(
                bulkSfService.upsert(
                        Mockito.eq(SObjectType.ACCOUNT), Mockito.any(), Mockito.eq("ShopID__c"))
        ).thenReturn(new JobResult<>(new JobInfo()));

        Result result = new Result();
        result.setCreated(false);
        result.setSuccess(true);
        String salesforceId = "1";
        result.setId(salesforceId);
        Mockito.when(bulkSfService.awaitCompletion(Mockito.any()))
                .thenReturn(Map.of(data, result));

        dataSenderService.sendData(importEntityType);

        Mockito.verify(bulkSfService, Mockito.times(1))
                .upsert(Mockito.eq(SObjectType.ACCOUNT), entitiesCaptor.capture(), Mockito.eq("ShopID__c"));
        List<? extends SObject> capturedEntities = entitiesCaptor.getValue();
        Assertions.assertEquals(1, capturedEntities.size());
        SObject capturedEntity = capturedEntities.get(0);
        Assertions.assertTrue(capturedEntity instanceof Account);
        Assertions.assertEquals(1, ((Account) capturedEntity).getShopIDC());

        Mockito.verify(bulkSfService, Mockito.times(1))
                .awaitCompletion(Mockito.any());

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementsStatusFromPendingToSent(entityIdsCaptor.capture(), Mockito.eq(importEntityType),
                        Mockito.eq(QueueElementStatus.SENT));
        Collection<Long> capturedIds = entityIdsCaptor.getValue();
        Assertions.assertEquals(1, capturedIds.size());
        Assertions.assertTrue(capturedIds.stream().findFirst().isPresent());
        Long capturedId = capturedIds.stream().findFirst().get();
        Assertions.assertEquals(1, capturedId);

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementStatusFromSentTo(Mockito.eq(1L), Mockito.eq(importEntityType),
                        Mockito.eq(QueueElementStatus.SUCCESS));

        Mockito.verify(entityService, Mockito.times(1))
                .findQueued(Mockito.eq(importEntityType), Mockito.eq(Account.class));
        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(importEntityType), Mockito.eq(Account.class));

        Mockito.verifyNoMoreInteractions(bulkSfService, entityService, queueService);
    }

    @Test
    void sendSuppliers_unsuccessfully() {
        Account data = new Account().withSupplierIDC(((double) 1));
        ImportEntityType importEntityType = ImportEntityType.SUPPLIER;
        long entityId = 1L;
        List<Entity> entities = List.of(new Entity(entityId, importEntityType, null, data));
        Mockito.when(entityService.findQueued(Mockito.eq(importEntityType), Mockito.eq(Account.class)))
                .thenReturn(entities);

        Mockito.when(
                bulkSfService.upsert(
                        Mockito.eq(SObjectType.ACCOUNT), Mockito.any(), Mockito.eq("SupplierID__c"))
        ).thenReturn(new JobResult<>(new JobInfo()));

        Result result = new Result();
        result.setCreated(false);
        result.setSuccess(false);
        Mockito.when(bulkSfService.awaitCompletion(Mockito.any()))
                .thenReturn(Map.of(data, result));

        dataSenderService.sendData(importEntityType);

        Mockito.verify(bulkSfService, Mockito.times(1))
                .upsert(Mockito.eq(SObjectType.ACCOUNT), entitiesCaptor.capture(), Mockito.eq("SupplierID__c"));
        List<? extends SObject> capturedEntities = entitiesCaptor.getValue();
        Assertions.assertEquals(1, capturedEntities.size());
        SObject capturedEntity = capturedEntities.get(0);
        Assertions.assertTrue(capturedEntity instanceof Account);
        Assertions.assertEquals(1, ((Account) capturedEntity).getSupplierIDC());

        Mockito.verify(bulkSfService, Mockito.times(1))
                .awaitCompletion(Mockito.any());

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementsStatusFromPendingToSent(entityIdsCaptor.capture(), Mockito.eq(importEntityType),
                        Mockito.eq(QueueElementStatus.SENT));
        Collection<Long> capturedIds = entityIdsCaptor.getValue();
        Assertions.assertEquals(1, capturedIds.size());
        Assertions.assertTrue(capturedIds.stream().findFirst().isPresent());
        Long capturedId = capturedIds.stream().findFirst().get();
        Assertions.assertEquals(1, capturedId);

        Mockito.verify(queueService, Mockito.times(1))
                .markSentAsFailedAndRequeue(Mockito.eq(entityId), Mockito.eq(importEntityType));

        Mockito.verify(entityService, Mockito.times(1))
                .findQueued(Mockito.eq(importEntityType), Mockito.eq(Account.class));

        Mockito.verifyNoMoreInteractions(bulkSfService, entityService, queueService);
    }

    @Test
    void sendContacts_successfullyCreatedAndUpdated() {
        List<Contact> data = List.of(
                new Contact().withFirstName("firstname").withPhone("111").withExternalIDC("123_SHOP_1"),
                new Contact().withLastName("lastname").withEmail("222").withExternalIDC("123_SUPPLIER_2")
        );
        ImportEntityType importEntityType = ImportEntityType.CONTACT;
        long entityId = 123;
        List<Entity> entities = List.of(new Entity(entityId, importEntityType, null, data));
        Mockito.when(
                entityService.findQueued(
                        Mockito.eq(ImportEntityType.CONTACT),
                        Mockito.eq(Contact.class),
                        Mockito.eq(List.class)
                )
        ).thenReturn(entities);

        Mockito.when(
                bulkSfService.upsert(
                        Mockito.eq(SObjectType.CONTACT), Mockito.anyList(), Mockito.eq("ExternalID__c")
                )
        ).thenReturn(new JobResult<>(new JobInfo()));

        Result result1 = new Result();
        result1.setCreated(true);
        result1.setSuccess(true);
        result1.setId("id1");
        Result result2 = new Result();
        result2.setCreated(false);
        result2.setSuccess(true);
        result2.setId("id2");
        Mockito.when(bulkSfService.awaitCompletion(Mockito.any()))
                .thenReturn(Map.of(data.get(0), result1, data.get(1), result2));

        dataSenderService.sendData(importEntityType);

        Mockito.verify(bulkSfService, Mockito.times(1))
                .upsert(Mockito.eq(SObjectType.CONTACT), entitiesCaptor.capture(), Mockito.anyString());
        List<? extends SObject> capturedEntities = entitiesCaptor.getValue();
        Assertions.assertEquals(2, capturedEntities.size());
        SObject capturedEntity1 = capturedEntities.get(0);
        Assertions.assertTrue(capturedEntity1 instanceof Contact);
        Assertions.assertEquals("firstname", ((Contact) capturedEntity1).getFirstName());
        Assertions.assertEquals("111", ((Contact) capturedEntity1).getPhone());
        SObject capturedEntity2 = capturedEntities.get(1);
        Assertions.assertTrue(capturedEntity2 instanceof Contact);
        Assertions.assertEquals("lastname", ((Contact) capturedEntity2).getLastName());
        Assertions.assertEquals("222", ((Contact) capturedEntity2).getEmail());

        Mockito.verify(bulkSfService, Mockito.times(1))
                .awaitCompletion(Mockito.any());

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementsStatusFromPendingToSent(
                        entityIdsCaptor.capture(), Mockito.eq(importEntityType), Mockito.eq(QueueElementStatus.SENT)
                );
        Collection<Long> capturedIds = entityIdsCaptor.getValue();
        Assertions.assertEquals(1, capturedIds.size());
        Assertions.assertTrue(capturedIds.stream().findFirst().isPresent());
        Long capturedId = capturedIds.stream().findFirst().get();
        Assertions.assertEquals(entityId, capturedId);

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementStatusFromSentTo(Mockito.eq(entityId), Mockito.eq(importEntityType),
                        Mockito.eq(QueueElementStatus.SUCCESS));

        Mockito.verify(entityService, Mockito.times(1))
                .findQueued(Mockito.eq(importEntityType), Mockito.eq(Contact.class), Mockito.eq(List.class));

        Mockito.verifyNoMoreInteractions(bulkSfService, queueService, entityService);
    }

    @Test
    void sendContacts_unsuccessfully() {
        List<Contact> data = List.of(
                new Contact().withExternalIDC("123_SHOP_1"),
                new Contact().withExternalIDC("123_SUPPLIER_2")
        );
        ImportEntityType importEntityType = ImportEntityType.CONTACT;
        long entityId = 123;
        List<Entity> entities = List.of(new Entity(entityId, importEntityType, null, data));
        Mockito.when(
                entityService.findQueued(
                        Mockito.eq(ImportEntityType.CONTACT),
                        Mockito.eq(Contact.class),
                        Mockito.eq(List.class)
                )
        ).thenReturn(entities);

        Mockito.when(
                bulkSfService.upsert(
                        Mockito.eq(SObjectType.CONTACT), Mockito.anyList(), Mockito.eq("ExternalID__c")
                )
        ).thenReturn(new JobResult<>(new JobInfo()));

        Result result1 = new Result();
        result1.setCreated(false);
        result1.setSuccess(true);
        result1.setId("id1");
        Result result2 = new Result();
        result2.setCreated(false);
        result2.setSuccess(false);
        result2.setId("id2");
        Mockito.when(bulkSfService.awaitCompletion(Mockito.any()))
                .thenReturn(Map.of(data.get(0), result1, data.get(1), result2));

        dataSenderService.sendData(importEntityType);

        Mockito.verify(bulkSfService, Mockito.times(1))
                .upsert(Mockito.eq(SObjectType.CONTACT), entitiesCaptor.capture(), Mockito.anyString());

        Mockito.verify(bulkSfService, Mockito.times(1))
                .awaitCompletion(Mockito.any());

        Mockito.verify(queueService, Mockito.times(1))
                .updateElementsStatusFromPendingToSent(
                        entityIdsCaptor.capture(), Mockito.eq(importEntityType), Mockito.eq(QueueElementStatus.SENT)
                );
        Collection<Long> capturedIds = entityIdsCaptor.getValue();
        Assertions.assertEquals(1, capturedIds.size());
        Assertions.assertTrue(capturedIds.stream().findFirst().isPresent());
        Long capturedId = capturedIds.stream().findFirst().get();
        Assertions.assertEquals(entityId, capturedId);

        Mockito.verify(queueService, Mockito.times(1))
                .markSentAsFailedAndRequeue(Mockito.eq(entityId), Mockito.eq(importEntityType));

        Mockito.verify(entityService, Mockito.times(1))
                .findQueued(Mockito.eq(importEntityType), Mockito.eq(Contact.class), Mockito.eq(List.class));

        Mockito.verifyNoMoreInteractions(bulkSfService, queueService, entityService);
    }
}