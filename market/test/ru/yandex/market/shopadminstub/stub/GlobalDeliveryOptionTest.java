package ru.yandex.market.shopadminstub.stub;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

public class GlobalDeliveryOptionTest extends AbstractTestBase {
    private static final int SHOP_ID = 10217455;

    @Autowired
    private CartHelper cartHelper;
    @Autowired
    private TestableClock clock;

    @AfterEach
    public void cleanup() {
        clock.clearFixed();
    }

    /**
     * Стаб должен отдавать доставку для глобала в родной регион
     * <p>
     * Подготовка:
     * 1. Кладем в настройки домашний регион = спб
     * 2. Подкладываем ответ репорта
     * 3. Подкладываем ответ фиддиспатчера
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что есть опция доставки, которую вернул репорт
     */
    @Test
    public void testShouldNotClearDeliveryOptionsForGlobalInLocalRegion() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest(ItemProvider.buildGlobalItem());

        CartParameters cartParameters = prepareGlobalShop(cartRequest);

        cartHelper.cart(cartParameters)
                .andExpect(xpath("/cart/items/item/@delivery").string("true"))
                .andExpect(xpath("/cart/delivery-options/delivery").nodeCount(1))
                .andExpect(xpath("/cart/delivery-options/delivery/@type").string("POST"))
                .andExpect(xpath("/cart/delivery-options/delivery/@price").string("300.00"));
    }

    /**
     * Стаб должен отдавать доставку для глобала в неродной регион
     * <p>
     * Подготовка:
     * 1. Кладем в настройки домашний регион = мск
     * 2. Подкладываем ответ репорта
     * 3. Подкладываем ответ фиддиспатчера
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что есть опция доставки, которую вернул репорт
     */
    @Test
    public void testShouldNotClearDeliveryOptionsForGlobal() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest(ItemProvider.buildGlobalItem());
        cartRequest.setCurrency("USD");

        CartParameters cartParameters = prepareGlobalShop(cartRequest);

        cartHelper.cart(cartParameters)
                .andExpect(xpath("/cart/items/item/@delivery").string("true"))
                .andExpect(xpath("/cart/delivery-options/delivery").nodeCount(1))
                .andExpect(xpath("/cart/delivery-options/delivery/@type").string("POST"))
                .andExpect(xpath("/cart/delivery-options/delivery/@price").string("300.00"));
    }

    private CartParameters prepareGlobalShop(CartRequest cartRequest) {
        LocalDate today = LocalDate.now();
        LocalDateTime fakeNow = LocalDateTime.of(today, LocalTime.of(20, 15, 30));

        CartParameters cartParameters = new CartParameters(SHOP_ID, cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_GLOBAL_WARE_MD5, Collections.emptyList());
        cartParameters.setFakeNow(fakeNow);
        return cartParameters;
    }

}
