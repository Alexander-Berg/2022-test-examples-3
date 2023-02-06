package ru.yandex.market.mbisfintegration.datapreparation.impl;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbisfintegration.converters.Converter;
import ru.yandex.market.mbisfintegration.converters.impl.mbi.ShopConverter;
import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.dao.QueueService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.entity.QueueElement;
import ru.yandex.market.mbisfintegration.entity.QueueElementStatus;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.SObject;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;

class AccountPreparationServiceTest {

    final static SObject salesforceEntity1 = new Account()
            .withShopIDC(1d)
            .withAgencyC("agency1")
            .withDistributionSchemeC("DBS");

    final static SObject salesforceEntity2 = new Account()
            .withShopIDC(1d)
            .withAgencyC("agency2")
            .withDistributionSchemeC("DBS");

    final Entity testEntity11 = new Entity(1L, ImportEntityType.SHOP, "1", salesforceEntity1);

    final Entity testEntity12 = new Entity(1L, ImportEntityType.SHOP, "1", salesforceEntity1);

    final Entity testEntity2 = new Entity(1L, ImportEntityType.SHOP, "1", salesforceEntity2);

    final QueueElement pending = new QueueElement(10L, 1L, ImportEntityType.SHOP, QueueElementStatus.PENDING,
            "hostFqdn", null, null);

    final QueueElement sent = new QueueElement(10L, 1L, ImportEntityType.SHOP, QueueElementStatus.SENT,
            "hostFqdn", null, null);

    final QueueElement success = new QueueElement(10L, 1L, ImportEntityType.SHOP, QueueElementStatus.SUCCESS,
            "hostFqdn", null, null);

    final QueueElement failure = new QueueElement(10L, 1L, ImportEntityType.SHOP, QueueElementStatus.FAILURE,
            "hostFqdn", null, null);

    @Mock
    EntityService entityService;

    @Mock
    QueueService queueService;

    AccountPreparationService dataPreparationService;

    Converter converter;

    ImportConfiguration importConfiguration;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        dataPreparationService = new AccountPreparationService(entityService, queueService);

        converter = Mockito.mock(ShopConverter.class);
        importConfiguration = new ImportConfiguration(Account.class, "entity1", "shop_id", ImportEntityType.SHOP);
    }

    @Test
    void prepare_dataEqualsPreviousSentWasSuccessful() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity11);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity12);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(success);

        dataPreparationService.prepare(Map.of("shop_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.any());
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_dataEqualsPreviousSentFailed() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity11);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity12);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(failure);

        dataPreparationService.prepare(Map.of("shop_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.any());
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP));
        Mockito.verify(queueService, Mockito.times(1))
                .add(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP));
        Mockito.verify(entityService, Mockito.times(1))
                .update(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.eq(salesforceEntity1));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_updateQueue() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity11);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity2);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(pending);

        dataPreparationService.prepare(Map.of("shop_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.any());
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP));
        Mockito.verify(queueService, Mockito.times(1))
                .updateElement(Mockito.eq(10L));
        Mockito.verify(entityService, Mockito.times(1))
                .update(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.eq(salesforceEntity2));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_addQueue() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity11);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity2);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(null);

        dataPreparationService.prepare(Map.of("shop_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.any());
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP));
        Mockito.verify(queueService, Mockito.times(1))
                .add(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP));
        Mockito.verify(entityService, Mockito.times(1))
                .update(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.eq(salesforceEntity2));

        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @Test
    void prepare_skipIfCurrentIsStillSent() {
        Mockito.when(entityService.find(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(testEntity11);
        Mockito.when(converter.convert(Mockito.anyMap(), Mockito.isNotNull(), Mockito.eq(importConfiguration)))
                .thenReturn(testEntity12);
        Mockito.when(queueService.findLast(Mockito.any(), Mockito.any()))
                .thenReturn(sent);

        dataPreparationService.prepare(Map.of("shop_id", "1"), importConfiguration, converter);

        Mockito.verify(entityService, Mockito.times(1))
                .find(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP), Mockito.any());
        Mockito.verify(queueService, Mockito.times(1))
                .findLast(Mockito.eq(1L), Mockito.eq(ImportEntityType.SHOP));
        Mockito.verifyNoMoreInteractions(entityService, queueService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADV", "FBS"})
    @NullSource
    void shouldFilterNewShopWithUnsupportedScheme(String scheme) {
        var entity = new Entity(1L, ImportEntityType.SHOP, null, new Account().withDistributionSchemeC(scheme));
        Assertions.assertTrue(dataPreparationService.shouldBeFiltered(entity, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADV", "FBS"})
    @NullSource
    void shouldNotFilterExistedShopWithUnsupportedScheme(String scheme) {
        var oldEntity = new Entity(1L, ImportEntityType.SHOP, null, new Account().withDistributionSchemeC("DBS"));
        var entity = new Entity(1L, ImportEntityType.SHOP, null, new Account().withDistributionSchemeC(scheme));
        Assertions.assertFalse(dataPreparationService.shouldBeFiltered(entity, oldEntity));
    }

    @Test
    void prepare_doNotUpdateOwnerStaffLoginCForExistingEntities() {
        Entity entity = new Entity(1L, ImportEntityType.SHOP, "id", new Account().withOwnerStaffLoginC("value"));
        dataPreparationService.customizeNewEntity(entity);
        Assertions.assertNull(((Account) entity.getData()).getOwnerStaffLoginC());
    }

    @Test
    void prepare_updateOwnerStaffLoginCForNewEntities() {
        Entity entity = new Entity(1L, ImportEntityType.SHOP, null, new Account().withOwnerStaffLoginC("value"));
        dataPreparationService.customizeNewEntity(entity);
        Assertions.assertEquals("value", ((Account) entity.getData()).getOwnerStaffLoginC());
    }
}