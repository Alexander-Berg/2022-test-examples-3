package ru.yandex.market.fps.module.supplier1p.test.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class SupplierTestUtils {
    private final BcpService bcpService;

    public SupplierTestUtils(BcpService bcpService) {
        this.bcpService = bcpService;
    }

    public <T extends Supplier1p> T createSupplier() {
        return createSupplier(Map.of());
    }

    @SuppressWarnings("unchecked") // потому что если какой-то интерфейс наследует Supplier1p, то он так же будет и у
    // объекта, иначе развалимся и в самом приложении тоже
    public <T extends Supplier1p> T createSupplier(Map<String, Object> attributes) {
        Map<String, Object> randomAttributes = Map.of(
                Supplier1p.TITLE, Randoms.string(),
                Supplier1p.MBI_PARTNER_ID, Randoms.longValue(),
                Supplier1p.UID, Randoms.longValue(),
                Supplier1p.BALANCE_CLIENT_ID, Randoms.positiveIntValue(),
                Supplier1p.CLIENT_EMAIL, Randoms.email()
        );
        return (T) bcpService.create(Supplier1p.class, Maps.merge(randomAttributes, attributes));
    }
}
