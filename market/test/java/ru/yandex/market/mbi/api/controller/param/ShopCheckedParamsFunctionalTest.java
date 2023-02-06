package ru.yandex.market.mbi.api.controller.param;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.param.model.PushPartnerStatus;
import ru.yandex.market.mbi.api.client.entity.params.ShopParams;
import ru.yandex.market.mbi.api.client.entity.params.ShopsWithParams;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

/**
 * Функциональные тесты ручки
 * {@link ru.yandex.market.mbi.api.controller.DispatcherController#getShopCheckedParams(List) /get-shop-checked-params}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "ShopCheckedParamsFunctionalTest.before.csv")
class ShopCheckedParamsFunctionalTest extends FunctionalTest {

    @Test
    void getEmptyParams() {
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getShopCheckedParams(Collections.emptyList())
        );
    }

    @Test
    void getNonEmptyParams() {
        ShopsWithParams shopsWithParams = mbiApiClient.getShopCheckedParams(Arrays.asList(1L, 2L, 3L));
        MatcherAssert.assertThat(
                shopsWithParams.getShops(),
                Matchers.hasSize(1)
        );
        List<ShopParams> shops = shopsWithParams.getShops();
        ShopParams shopParams = shops.get(0);
        Assertions.assertEquals(shopParams.getShopId(), 1L);
        Map<ParamType, ParamValue> params = shopParams.getParams();
        Assertions.assertEquals(params.size(), 3);
        Assertions.assertEquals(
                ParamCheckStatus.NEW.name(),
                params.get(ParamType.CPA_REGION_CHECK_STATUS).getValueAsString()
        );
        Assertions.assertEquals(
                PushPartnerStatus.REAL.name(),
                params.get(ParamType.IS_PUSH_PARTNER).getValueAsString()
        );
        Assertions.assertEquals(true, params.get(ParamType.IGNORE_STOCKS).getValueAsBoolean());
    }

    @Test
    @DisplayName("Проверка ответа ручки в xml-формате")
    void xmlResponse() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                "http://localhost:" + port + "/get-shop-checked-params?shop-id=1,2,3"
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<shops>\n" +
                        "    <shop id=\"1\">\n" +
                        "        <params>\n" +
                        "            <param type-id=\"8\" value=\"NEW\" />\n" +
                        "            <param type-id=\"123\" value=\"REAL\" />\n" +
                        "            <param type-id=\"128\" value=\"true\" />\n" +
                        "        </params>\n" +
                        "    </shop>\n" +
                        "</shops>",
                response.getBody()
        );
    }
}
