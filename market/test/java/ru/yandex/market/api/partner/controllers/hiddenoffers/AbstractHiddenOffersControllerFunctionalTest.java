package ru.yandex.market.api.partner.controllers.hiddenoffers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.http.HttpDeleteRequest;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerServiceStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "hiddenOffersEnv.before.csv")
class AbstractHiddenOffersControllerFunctionalTest extends FunctionalTest {

    static final String USER_ID = "67282295";

    static final String SEND_TO_LOGBROKER = "market.quick.partner-api.send.to.logbroker";

    @Autowired
    private UltraControllerServiceStub ultraControllerClient;


    private static String performRequest(HttpUriRequest request, int httpCode, Format format, String uid) throws IOException {
        request.setHeader("X-AuthorizationService", "Mock");
        request.setHeader("Cookie", String.format("yandexuid = %s;", uid));
        request.setHeader(HttpHeaders.CONTENT_TYPE, format.getContentType().toString());
        HttpResponse response = HttpClients.createDefault().execute(request);
        byte[] responseBytes = ByteStreams.toByteArray(response.getEntity().getContent());
        assertEquals(httpCode, response.getStatusLine().getStatusCode());
        Header[] mimeTypeHeaders = response.getHeaders(HttpHeaders.CONTENT_TYPE);
        assertEquals(mimeTypeHeaders.length, 1);
        String mimeType = mimeTypeHeaders[0].getValue();
        assertTrue(mimeType.startsWith(format.getContentType().toString()));
        return new String(responseBytes, StandardCharsets.UTF_8);
    }

    String performGet(long campaignId,
                      Format format,
                      int httpCode,
                      Multimap<String, Object> params) throws IOException {
        final StringBuilder url = new StringBuilder(getUrl(campaignId, format));
        fillParams(url, params);
        HttpGet request = new HttpGet(url.toString());
        return performRequest(request, httpCode, format, USER_ID);
    }

    private void fillParams(StringBuilder url, Multimap<String, Object> params) {
        if (!params.isEmpty()) {
            boolean firstParam = true;
            for (Map.Entry<String, Object> entry : params.entries()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                if (firstParam) {
                    url.append("?");
                } else {
                    url.append("&");
                }
                url.append(key).append("=").append(value);
                firstParam = false;
            }
        }
    }

    String performPost(long campaignId, String content, int httpCode, Format format) throws IOException {
        HttpPost request = new HttpPost(getUrl(campaignId, format));
        request.setEntity(new StringEntity(content));
        return performRequest(request, httpCode, format, USER_ID);
    }

    String performPost(long campaignId, String content, int httpCode, Format format, String uid) throws IOException {
        HttpPost request = new HttpPost(getUrl(campaignId, format));
        request.setEntity(new StringEntity(content));
        return performRequest(request, httpCode, format, uid);
    }

    String performDelete(long campaignId, String content, int httpCode, Format format) throws IOException {
        HttpDeleteRequest request = new HttpDeleteRequest(getUrl(campaignId, format));
        request.setEntity(new StringEntity(content));
        return performRequest(request, httpCode, format, USER_ID);
    }

    protected String getJsonUrl(long campaignId, Multimap<String, Object> params) {
        StringBuilder url = new StringBuilder(getUrl(campaignId, Format.JSON));
        fillParams(url, params);
        return url.toString();
    }

    protected String getUrl(long campaignId, Format format) {
        return urlBasePrefix + "/campaigns/" + campaignId + "/hidden-offers." + format.formatName();
    }

    protected void mockUltraControllerClient() {
        when(ultraControllerClient.getShopSKU(any())).thenAnswer(a -> {
            UltraController.ShopSKURequest request = a.getArgument(0);
            UltraController.SKUMappingResponse.Builder builder = UltraController.SKUMappingResponse.newBuilder();
            for (Long marketSku : request.getMarketSkuIdList()) {
                builder.addSkuMapping(
                        UltraController.SKUMappingResponse.SKUMapping.newBuilder()
                                .addShopSku(marketSku.toString())
                                .setMarketSkuId(marketSku)
                                .build());
            }
            return builder.build();
        });
    }

    @Nonnull
    protected String fileToString(@Nonnull String testFolder, @Nonnull String test) {
        return StringTestUtil.getString(this.getClass(),
                "HiddenOffersController/" + testFolder + "/json/" + test + ".json");
    }
}
