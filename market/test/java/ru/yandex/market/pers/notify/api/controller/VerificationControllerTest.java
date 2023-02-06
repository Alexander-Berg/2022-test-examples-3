package ru.yandex.market.pers.notify.api.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;
import ru.yandex.market.pers.notify.test.VerificationUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.test.TestUtil.stringFromFile;

/**
 * @author dinyat
 *         07/04/2017
 */
class VerificationControllerTest extends MarketUtilsMockedDbTest {
    private static final String EMAIL = "foo@bar.com";
    private static final String MODEL_ID = "5555";
    private static final String REGION_ID = "213";
    private static final String PA_ON_SALE_RESPONSE = "/data/verification/pa_on_sale.json";
    private static final String ADV_RESPONSE = "/data/verification/advertising.json";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private VerificationUtil verificationUtil;
    @Autowired
    private SubscriptionControllerInvoker subscriptionControllerInvoker;

    @Test
    void testPostActivate() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/verification/")
            .param("sk", "fdd5ccd456c4f3cc8f1675678d65eb78aefd1bf564d47c615ed755cf18d5e43c")
            .param("action", "74696d653d3134393134383638343131393126747970653d323926656d61696c3d796e64782d64696e7961744079616e6465782e7275267569643d343736333536343236")
            .param("userIp", "2a02%3A6b8%3A0%3A1495%3A%3A1%3A2")
            .param("userAgent", "Mozilla%2F5.0%20(Macintosh%3B%20Intel%20Mac%20OS%20X%2010_11_6)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F56.0.2924.87%20YaBrowser%2F17.3.1.838%20Yowser%2F2.5%20Safari%2F537.36")
        ).andDo(print())
            .andExpect(status().isOk())
        .andExpect(content().json("{\"uid\":476356426,\"email\":\"yndx-dinyat@yandex.ru\",\"type\":\"MODEL_GRADE\",\"success\":true}"));
    }

    @Test
    void testUnsubscribeAdvertising() throws Exception {
        String response = verificationUtil.unsubscribe(NotificationType.ADVERTISING, null, EMAIL);
        JSONAssert.assertEquals(stringFromFile(ADV_RESPONSE), response, JSONCompareMode.LENIENT);
    }

    @Test
    void testUnsubscribePaOnSale() throws Exception {
        long subscriptionId = subscriptionControllerInvoker.createProductSubscription(NotificationType.PA_ON_SALE,
                new Uid(1234L), EMAIL, MODEL_ID, REGION_ID, "0");
        String response = verificationUtil.unsubscribe(NotificationType.PA_ON_SALE, subscriptionId, EMAIL);
        JSONAssert.assertEquals(stringFromFile(PA_ON_SALE_RESPONSE), response, JSONCompareMode.LENIENT);
    }


    @Test
    void testConfirmPaOnSale() throws Exception {
        long subscriptionId = subscriptionControllerInvoker.createProductSubscription(NotificationType.PA_ON_SALE,
                new YandexUid("sdfdsfds"), EMAIL, MODEL_ID, REGION_ID, "0");
        String response = verificationUtil.confirmSubscription(subscriptionId);
        JSONAssert.assertEquals(stringFromFile(PA_ON_SALE_RESPONSE), response, JSONCompareMode.LENIENT);
    }

}
