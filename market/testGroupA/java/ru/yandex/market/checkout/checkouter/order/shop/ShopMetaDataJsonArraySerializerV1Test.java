package ru.yandex.market.checkout.checkouter.order.shop;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.pay.legacy.PaymentSubMethod;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.PaymentArticle;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ZkHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class ShopMetaDataJsonArraySerializerV1Test extends AbstractWebTestBase {

    @Autowired
    private ZkHandler shopMetaDataSerializer;

    @Test
    public void shouldDeserializeUnknownPaymentSubMethod() {
        ShopMetaData data = shopMetaDataSerializer.deserialize("[1,1013063,524626,2,2,\"shop123/b\",[[0," +
                "\"AllYourMoneyAreBelongToUs-123\"],[-10,\"No!Us!-456\"]]]");
        PaymentArticle[] articles = data.getArticles();
        if (articles != null && articles.length > 1) {
            assertEquals(PaymentSubMethod.UNKNOWN, articles[1].getPaymentSubMethod());
            return;
        }
        fail();
    }

    @Test
    public void shouldSerializeWithNullYaMoneyIdAndArticles() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .build();
        String string = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(string));
    }

    @Test
    public void shouldSerializeWithYaMoneyId() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1)
                .withClientId(2)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.SHOP)
                .withYaMoneyId("qazwsx")
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .build();
        String string = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(string));
    }

    @Test
    public void shouldSerializeWithYaMoneyIdAndEmptyArticles() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1013063)
                .withClientId(524626)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withYaMoneyId("shop123/b")
                .withArticles(new PaymentArticle[0])
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .build();
        String serialized = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(serialized));
    }

    @Test
    public void shouldSerializeWithVisibilityMapContacts() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1013063)
                .withClientId(524626)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withYaMoneyId("shop123/b")
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withOrderVisibilityMap(Collections.singletonMap(OrderVisibility.BUYER_EMAIL, true))
                .build();
        String serialized = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(serialized));
    }

    @Test
    public void shouldSerializeWithYaMoneyIdAndOneArticle() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1013063)
                .withClientId(524626)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withYaMoneyId("shop123/b")
                .withArticles(new PaymentArticle[]{
                        new PaymentArticle("AllYourMoneyAreBelongToUs-123", PaymentSubMethod.BANK_CARD, null),
                })
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .build();
        String string = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(string));
    }

    @Test
    public void shouldSerializeWithYaMoneyIdAndTwoArticles() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1013063)
                .withClientId(524626)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withYaMoneyId("shop123/b")
                .withArticles(new PaymentArticle[]{
                        new PaymentArticle("AllYourMoneyAreBelongToUs-123", PaymentSubMethod.BANK_CARD, null),
                        new PaymentArticle("No!Us!-456", PaymentSubMethod.YA_MONEY, "some-scid")
                })
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .build();
        String string = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(string));
    }

    @Test
    public void shouldSerializeWithOgrnAndSupplierName() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1013063)
                .withClientId(524626)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withSupplierName("shop123")
                .withOgrn("1397431111806")
                .build();
        String string = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(string));
    }

    @Test
    public void shouldSerializeWithMedicineLicense() {
        ShopMetaData data = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1013063)
                .withClientId(524626)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withMedicineLicense("medicine_license_12131415")
                .build();
        String string = shopMetaDataSerializer.serialize(data);
        assertEquals(data, shopMetaDataSerializer.deserialize(string));
    }
}
