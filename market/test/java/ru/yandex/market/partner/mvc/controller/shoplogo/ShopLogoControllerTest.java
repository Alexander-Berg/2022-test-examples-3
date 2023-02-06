package ru.yandex.market.partner.mvc.controller.shoplogo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.avatars.AvatarsClient;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Функциональные тесты на {@link ShopLogoController}.
 *
 * @author au-rikka
 */
@DbUnitDataSet(before = "ShopLogo.before.csv")
class ShopLogoControllerTest extends FunctionalTest {
    private static final long SHOP_LOGO_CAMPAIGN_ID = 1111L;
    private static final long SHOP_LOGO_WITH_RETINA_CAMPAIGN_ID = 2222L;
    private static final long CAMPAIGN_WITH_NO_LOGO_ID = 3333L;

    private static final String VALID_SHOP_LOGO_RESPONSE = "shop_logo_info.json";
    private static final String VALID_SHOP_LOGO_RETINA_RESPONSE = "shop_logo_retina_info.json";

    private static final String DELETE_URL = "test.ru/delete";
    @Autowired
    private AvatarsClient avatarsClient;

    private String shopLogoUrl(long campaignId) {
        return baseUrl + String.format("/campaign/%d/logo", campaignId);
    }

    private void deleteShopLogo(long campaignId) {
        final String url = shopLogoUrl(campaignId) + "/delete";
        FunctionalTestHelper.delete(url);
    }

    private void getShopLogo(long campaignId, String jsonFileName) {
        final String url = shopLogoUrl(campaignId) + "/info";
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        final String jsonFilePath = String.format("/mvc/shoplogo/%s", jsonFileName);
        JsonTestUtil.assertEquals(response, this.getClass(), jsonFilePath);
    }


    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/info}.
     * Получение информации для магазина (без ретина версии).
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void testGetShopLogoInfo() {
        getShopLogo(SHOP_LOGO_CAMPAIGN_ID, VALID_SHOP_LOGO_RESPONSE);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/info}.
     * Получение информации для магазина (с ретина версией).
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void testGetShopLogoInfoRetina() {
        getShopLogo(SHOP_LOGO_WITH_RETINA_CAMPAIGN_ID, VALID_SHOP_LOGO_RETINA_RESPONSE);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/info}.
     * Получение информации для магазина без данных о логотипе.
     * Должен вернуться пустой результат
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void testGetShopLogoNoLogo() {
        ResponseEntity<String> response = FunctionalTestHelper.get(shopLogoUrl(CAMPAIGN_WITH_NO_LOGO_ID) + "/info");
        Assertions.assertNull(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject().get("result"));
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/delete}.
     * Удаление информации о логотипе (с ретина версией).
     * Должна удалиться информация об обеих версиях логотипа.
     */
    @Test
    @DbUnitDataSet(before = "ShopLogo.delete.before.csv", after = "ShopLogo.delete.after.csv")
    void testDeleteShopLogo() {
        doNothing().when(avatarsClient).deleteImage(eq(DELETE_URL));
        deleteShopLogo(SHOP_LOGO_WITH_RETINA_CAMPAIGN_ID);
        verifyNoMoreInteractions(avatarsClient);
    }

    /**
     * Тест для ручки {@code /campaign/{campaignId}/logo/delete}.
     * При удаление информации о логотипе для магазина без данных о логотипе не должна бросаться ошибка.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogo.before.csv")
    void testDeleteShopLogoWithNoLogo() {
        deleteShopLogo(CAMPAIGN_WITH_NO_LOGO_ID);
        verifyNoMoreInteractions(avatarsClient);
    }
}
