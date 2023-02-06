package ru.yandex.market.abo.core.resupply.registry;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.lms.model.DeliveryService;
import ru.yandex.market.abo.cpa.lms.repo.DeliveryServiceRepo;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ResupplyRegistryTest extends EmptyTest {

    @Autowired
    RegistryRepo registryRepo;
    @Autowired
    RegistryItemRepo registryItemRepo;
    @Autowired
    DeliveryServiceRepo deliveryServiceRepo;

    @Test
    public void dao() {
        DeliveryService deliveryService = new DeliveryService(1L, "name", "title",
            PartnerType.SORTING_CENTER, 136L, false, 80, true);
        deliveryServiceRepo.save(deliveryService);
        flushAndClear();

        Registry r = new Registry();
        r.setDeliveryService(deliveryService);
        RegistryItem item = new RegistryItem();
        item.setRegistry(r);
        item.setOrderId("1");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{"one", "two"});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{"one"});
        item.setBoxCount(2);

        registryRepo.save(r);
        registryItemRepo.save(item);
        flushAndClear();

        Registry r2 = new Registry();
        RegistryItem i2 = new RegistryItem();
        i2.setRegistry(r2);
        i2.setOrderId("1234");
        i2.setTrackCode("1234");
        i2.setBoxesArray(new String[]{"blah"});
        i2.setBoxCount(3);
        registryRepo.save(r2);
        registryItemRepo.save(i2);
        flushAndClear();

        List<RegistryItem> list = registryItemRepo.searchUnpaid(r.getId(), "one", "one");
        assertEquals(1, list.size());
        assertNotNull(list.get(0).getRegistry());
        assertEquals(deliveryService, list.get(0).getRegistry().getDeliveryService());

        list = registryItemRepo.searchUnpaid(r2.getId(), "blah", "blah");
        assertEquals(1, list.size());
    }

}
