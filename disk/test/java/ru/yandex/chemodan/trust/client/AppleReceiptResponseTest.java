package ru.yandex.chemodan.trust.client;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.trust.client.responses.AppleReceiptResponse;

public class AppleReceiptResponseTest {
    @Test
    public void deserializeTest() throws IOException {
        ObjectMapper mapper = TrustClient.buildObjectMapper();
        AppleReceiptResponse appleReceiptResponse =
                mapper.readValue(AppleReceiptResponseTest.class.getResourceAsStream("AppleRawRecieptExample.json"),
                AppleReceiptResponse.class);
        ListF<AppleReceiptResponse.InAppPurchase> inapps =
                appleReceiptResponse.getResult().getReceiptInfo().getReceipt().getInAppPurchases();
        Assert.assertEquals(1, inapps.length());
        AppleReceiptResponse.InAppPurchase inapp = inapps.get(0);
        Assert.assertEquals(false, inapp.getIsInIntroOfferPeriod());
        Assert.assertEquals("1000000567558393", inapp.getOriginalTransactionId());
        Assert.assertEquals("1tb_1m_apple_appstore_2019", inapp.getProductId());
    }
}
