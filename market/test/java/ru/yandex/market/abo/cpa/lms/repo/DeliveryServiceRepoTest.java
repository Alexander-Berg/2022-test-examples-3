package ru.yandex.market.abo.cpa.lms.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.lms.model.DeliveryService;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 14.04.2021
 */
public class DeliveryServiceRepoTest extends EmptyTest {

    @Autowired
    private DeliveryServiceRepo repo;

    @Test
    void saveServicesTest() {
        var deliveryService = new DeliveryService(123, "Служба доставки", "ООО Возим-Недовозим",
            PartnerType.SORTING_CENTER, 136L, false, 80, true);
        repo.save(deliveryService);

        assertEquals(deliveryService, repo.findAll().get(0));
    }
}
