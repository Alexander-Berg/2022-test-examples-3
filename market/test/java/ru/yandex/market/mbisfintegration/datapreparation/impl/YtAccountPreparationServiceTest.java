package ru.yandex.market.mbisfintegration.datapreparation.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.dao.QueueService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.SObject;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 26.02.2022
 */
public class YtAccountPreparationServiceTest {

    @Mock
    EntityService entityService;

    @Mock
    QueueService queueService;

    YtAccountPreparationService dataPreparationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        dataPreparationService = new YtAccountPreparationService(entityService, queueService);

    }

    @Test
    void shouldFilterWithoutSupplierId(){
        SObject data = new Account().withActiveC(true).withGMVC(1.0d);
        Entity entity = new Entity(1L, ImportEntityType.SUPPLIER, "salesforceId1", data);
        Assertions.assertTrue(dataPreparationService.shouldBeFiltered(entity, null));
    }

    @Test
    void shouldNotFilterWithSupplierId(){
        SObject data = new Account().withActiveC(true).withGMVC(1.0d).withSupplierIDC(123.0);
        Entity entity = new Entity(1L, ImportEntityType.SUPPLIER, "salesforceId1", data);
        Assertions.assertFalse(dataPreparationService.shouldBeFiltered(entity, null));
    }

    @Test
    void shouldFilterWithoutShopId(){
        SObject data = new Account().withActiveC(true).withGMVC(1.0d);
        Entity entity = new Entity(1L, ImportEntityType.SHOP, "salesforceId1", data);
        Assertions.assertTrue(dataPreparationService.shouldBeFiltered(entity, null));
    }

    @Test
    void shouldNotFilterWithShopId(){
        SObject data = new Account().withActiveC(true).withGMVC(1.0d).withShopIDC(123.0);
        Entity entity = new Entity(1L, ImportEntityType.SHOP, "salesforceId1", data);
        Assertions.assertFalse(dataPreparationService.shouldBeFiltered(entity, null));
    }


}
