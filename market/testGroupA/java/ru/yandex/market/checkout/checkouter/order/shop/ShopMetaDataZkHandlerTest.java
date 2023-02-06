package ru.yandex.market.checkout.checkouter.order.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ZkHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class ShopMetaDataZkHandlerTest extends AbstractWebTestBase {

    @Autowired
    private ZkHandler serializer;

    @Test
    public void shouldDeserializeV1() {
        ShopMetaData expected = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(4)
                .withClientId(5)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
        String string = serializer.serialize(expected);
        ShopMetaData actual = serializer.deserialize(string);
        assertEquals(expected, actual);
    }
}
