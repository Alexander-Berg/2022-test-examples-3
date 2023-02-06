package ru.yandex.market.checkout.checkouter.shop;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static java.util.Collections.singletonMap;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_EMAIL;


public class ShopControllerUpdateMetaTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 19922018L;
    @Autowired
    private TestSerializationService testSerializationService;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                ShopSettingsHelper.getOldPrepayMeta(),
                ShopSettingsHelper.oldPrepayBuilder().withArticles(ShopSettingsHelper.paymentArticles()).build(),
                ShopSettingsHelper.oldPrepayBuilder().withCampaiginId(-123456L).build(),
                ShopSettingsHelper.oldPrepayBuilder().withClientId(-345678L).build(),
                ShopSettingsHelper.oldPrepayBuilder().withSandboxClass(PaymentClass.SHOP).build(),
                ShopSettingsHelper.oldPrepayBuilder().withSandboxClass(PaymentClass.OFF).build(),
                ShopSettingsHelper.oldPrepayBuilder().withSandboxClass(PaymentClass.YANDEX).build(),
                ShopSettingsHelper.oldPrepayBuilder().withProdClass(PaymentClass.SHOP).build(),
                ShopSettingsHelper.oldPrepayBuilder().withProdClass(PaymentClass.OFF).build(),
                ShopSettingsHelper.oldPrepayBuilder().withProdClass(PaymentClass.YANDEX).build(),
                ShopSettingsHelper.oldPrepayBuilder().withYaMoneyId("123456").build(),
                ShopSettingsHelper.oldPrepayBuilder().withOrderVisibilityMap(singletonMap(BUYER_EMAIL, true)).build(),
                ShopSettingsHelper.oldPrepayBuilder().withOrderVisibilityMap(singletonMap(BUYER_EMAIL, false)).build(),
                ShopSettingsHelper.oldPrepayBuilder().build(),
                ShopSettingsHelper.getDefaultMeta()
        )
                .map(smd -> new Object[]{smd})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testUpdateMeta(ShopMetaData shopMetaData) throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(testSerializationService.serializeCheckouterObject(shopMetaData)))
                .andExpect(status().isOk());
    }
}
