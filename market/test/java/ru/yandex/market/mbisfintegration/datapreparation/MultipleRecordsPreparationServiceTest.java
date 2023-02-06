package ru.yandex.market.mbisfintegration.datapreparation;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbisfintegration.converters.Converter;
import ru.yandex.market.mbisfintegration.converters.impl.mbi.ContactConverter;
import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.dao.QueueService;
import ru.yandex.market.mbisfintegration.datapreparation.impl.MultipleRecordsPreparationService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.entity.QueueElement;
import ru.yandex.market.mbisfintegration.entity.QueueElementStatus;
import ru.yandex.market.mbisfintegration.generated.sf.model.Contact;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;

class MultipleRecordsPreparationServiceTest {
    @Mock
    EntityService entityService;

    @Mock
    QueueService queueService;

    MultipleRecordsPreparationService dataPreparationService;

    final List<Contact> data1 = List.of(
            new Contact().withAccountId("1").withPhone("1"), new Contact().withAccountId("1").withPhone("2")
    );
    final List<Contact> data2 = List.of(
            new Contact().withAccountId("1").withPhone("2"), new Contact().withAccountId("1").withPhone("2")
    );
    final Entity testEntity1 = new Entity(1L, ImportEntityType.CONTACT, "1", data1);
    final Entity testEntity2 = new Entity(1L, ImportEntityType.CONTACT, "1", data2);
    final Entity emptyEntity = new Entity(2L, ImportEntityType.CONTACT, null, List.of());

    final QueueElement pending = new QueueElement(10L, 1L, ImportEntityType.CONTACT, QueueElementStatus.PENDING,
            "hostFqdn", null, null);

    final QueueElement success = new QueueElement(10L, 1L, ImportEntityType.CONTACT, QueueElementStatus.SUCCESS,
            "hostFqdn", null, null);

    final QueueElement failure = new QueueElement(10L, 1L, ImportEntityType.CONTACT, QueueElementStatus.FAILURE,
            "hostFqdn", null, null);

    Converter converter = Mockito.mock(ContactConverter.class);
    ImportConfiguration importConfiguration = new ImportConfiguration(Contact.class, "entity1", "contact_id",
            ImportEntityType.CONTACT);

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        dataPreparationService = new MultipleRecordsPreparationService(entityService, queueService);
    }

    @Test
    void prepare_dataEqualsPreviousSentWasSuccefull() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity1);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity1);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(success);

        dataPreparationService.prepare(Map.of("contact_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(Contact.class),
                        Mockito.eq(List.class));
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_dataEqualsPreviousSentFailed() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity1);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity1);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(failure);

        dataPreparationService.prepare(Map.of("contact_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(Contact.class),
                        Mockito.eq(List.class));
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT));
        Mockito.verify(queueService, Mockito.times(1))
                .add(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT));
        Mockito.verify(entityService, Mockito.times(1))
                .update(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(data1));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_updateQueue() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity1);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity2);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(pending);

        dataPreparationService.prepare(Map.of("contact_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(Contact.class),
                        Mockito.eq(List.class));
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT));
        Mockito.verify(queueService, Mockito.times(1))
                .updateElement(Mockito.eq(10L));
        Mockito.verify(entityService, Mockito.times(1))
                .update(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(data2));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_addQueue() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity1);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity2);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(null);

        dataPreparationService.prepare(Map.of("contact_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(Contact.class),
                        Mockito.eq(List.class));
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT));
        Mockito.verify(queueService, Mockito.times(1))
                .add(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT));
        Mockito.verify(entityService, Mockito.times(1))
                .update(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(data2));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_ignoreAllContacts() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(null);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNull(), Mockito.eq(importConfiguration)))
                .thenReturn(emptyEntity);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(null);

        dataPreparationService.prepare(Map.of("contact_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT), Mockito.eq(Contact.class),
                        Mockito.eq(List.class));
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.CONTACT));
        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }
}