package ru.yandex.market.partner.mvc.controller.feed.supplier;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mbi.util.io.MbiFiles;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.partner.util.MbiArgumentMatchers;

@ParametersAreNonnullByDefault
class AbstractSupplierFeedControllerFunctionalTest extends FunctionalTest {
    static final long CAMPAIGN_ID = 10774L;
    static final String CHECK_FEED_PBUF_URL = "http://idx.ru/check_feed.response.pbuf.sn";
    static final String COMMIT_PBUF_URL = "http://idx.ru/commit.response.pbuf.sn";

    @Autowired
    FeedFileStorage feedFileStorage;

    @Autowired
    HttpClient indexerApiClientHttpClient;

    @Autowired
    @Qualifier("unitedSupplierXlsHelper")
    SupplierXlsHelper unitedSupplierXlsHelper;

    static long extractUploadDateTime(ResponseEntity<String> response, String... container) throws IOException {
        return extractUpdateTime(response, "uploadDateTime", container);
    }

    static long extractUpdateTime(ResponseEntity<String> response, String longNodeName, String... container) throws IOException {
        ObjectNode parsedResponse = new ObjectMapper().readValue(response.getBody(), ObjectNode.class);
        ObjectNode resultNode = (ObjectNode) parsedResponse.get("result");
        ObjectNode uploadNode = resultNode;
        for (int i = 0; i < container.length; i++) {
            uploadNode = (ObjectNode) uploadNode.get(container[i]);
        }
        NumericNode uploadDateTimeNode = (NumericNode) uploadNode.get(longNodeName);
        return uploadDateTimeNode.asLong();
    }

    static HttpResponse createHttpOkResponse(ContentType contentType, String body) {
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1, 1), 200, "OK"));
        response.setEntity(new StringEntity(body, contentType));
        return response;
    }

    static HttpResponse createHttpOkResponse(ContentType contentType, byte[] body) {
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(new HttpVersion(1, 1), 200, "OK"));
        response.setEntity(new ByteArrayEntity(body, contentType));
        return response;
    }

    @BeforeEach
    public void initMocks() throws IOException {
        Mockito.when(feedFileStorage.upload(Mockito.any(), Mockito.anyLong()))
                .thenReturn(new StoreInfo(530944, "http://mds.url/"));
    }

    long createSuccessfulAssortmentValidation() throws IOException, URISyntaxException {
        String checkResponse =
                MbiFiles.readText(
                        () -> this.getClass()
                                .getResourceAsStream(
                                        "SupplierAssortmentControllerFunctionalTest.check_feed.response.txt"),
                        StandardCharsets.UTF_8);
        byte[] checkResponsePbuf = MbiFiles.readBytes(() -> this.getClass()
                .getResourceAsStream("SupplierAssortmentControllerFunctionalTest.check_feed.response.pbuf.sn"));
        String expectedCheckUrl =
                "http://active.idxapi.tst.vs.market.yandex.net:29334" +
                        "/v1/check/feed?" +
                        "url=http%3A%2F%2Fmds.url%2F&" +
                        "supplier_id=774&" +
                        "is_online=true&" +
                        "is_offline=true&" +
                        "format=pbsn&" +
                        "check_prices=false&" +
                        "check_mapping_data=true&" +
                        "check_msku_exists=true";
        Mockito.when(indexerApiClientHttpClient.execute(MbiArgumentMatchers.httpGets(expectedCheckUrl)))
                .thenReturn(createHttpOkResponse(ContentType.TEXT_PLAIN, checkResponse));
        Mockito.when(indexerApiClientHttpClient.execute(MbiArgumentMatchers.httpGets(CHECK_FEED_PBUF_URL)))
                .thenReturn(createHttpOkResponse(ContentType.APPLICATION_OCTET_STREAM, checkResponsePbuf));
        ClassPathResource uploadedResource = new ClassPathResource("supplier/feed/Stock_xls-sku.xls");
        Mockito.when(feedFileStorage.open(Mockito.any())).thenAnswer(invocation -> uploadedResource.getInputStream());

        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("upload", uploadedResource);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<?> request = new HttpEntity<>(multipart, headers);
        ResponseEntity<String> response =
                FunctionalTestHelper.post(assortmentValidationsUrl(CAMPAIGN_ID), request);

        Mockito.verify(indexerApiClientHttpClient).execute(MbiArgumentMatchers.httpGets(expectedCheckUrl));
        Mockito.verify(indexerApiClientHttpClient).execute(MbiArgumentMatchers.httpGets(CHECK_FEED_PBUF_URL));

        long uploadDateTime = extractUploadDateTime(response, "feed", "upload");
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "" +
                "{" +
                "    \"id\":1," +
                "    \"campaignId\":10774," +
                "    \"feed\": {" +
                "        \"upload\":{" +
                "            \"fileName\":\"Stock_xls-sku.xls\"," +
                "            \"fileSize\":509440," +
                "            \"uploadDateTime\":" + uploadDateTime + "" +
                "        }" +
                "    }," +
                "    \"status\":\"PROCESSING\"" +
                "}")));

        response = FunctionalTestHelper.get(validationUrl(CAMPAIGN_ID, 1));
        long enrichedFileUploadTimestamp = extractUploadDateTime(response, "result", "enrichedFile");
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "" +
                "{" +
                "    \"id\":1," +
                "    \"campaignId\":10774," +
                "    \"feed\": {" +
                "        \"upload\":{" +
                "            \"fileName\":\"Stock_xls-sku.xls\"," +
                "            \"fileSize\":509440," +
                "            \"uploadDateTime\":" + uploadDateTime + "" +
                "        }" +
                "    }," +
                "    \"result\":{" +
                "        \"totalOffers\":13," +
                "        \"processedOffers\":13," +
                "        \"declinedOffers\":0," +
                "        \"newOffers\":13," +
                "        \"existingOffers\": {\n" +
                "            \"acceptedOffers\": 0,\n" +
                "            \"moderationOffers\": 0,\n" +
                "            \"rejectedOffers\": 0\n" +
                "        }," +
                "        \"enrichedFile\":{" +
                "            \"fileName\":\"Stock_xls-sku.xls\"," +
                "            \"fileSize\":496640," +
                "            \"uploadDateTime\":" + enrichedFileUploadTimestamp +
                "        }" +
                "    }," +
                "    \"status\":\"OK\"" +
                "}")));
        return 1;
    }

    String supplierUrl(long campaignId) {
        return String.format("%s/suppliers/%d", baseUrl, campaignId);
    }

    String campaignUrl(long campaignId) {
        return String.format("%s/campaigns/%d", baseUrl, campaignId);
    }

    String assortmentUrl(long campaignId) {
        return String.format("%s/assortment", campaignUrl(campaignId));
    }

    String assortmentDownloadUrl(long campaignId) {
        return String.format("%s/assortment/download", supplierUrl(campaignId));
    }

    String feedUrl(long campaignId) {
        return String.format("%s/feed", supplierUrl(campaignId));
    }

    String latestFeedUrl(long campaignId) {
        return String.format("%s/latest", feedUrl(campaignId));
    }

    String latestStocksFeedUrl(long campaignId) {
        return String.format("%s/stocks/latest", feedUrl(campaignId));
    }

    String downloadStocksFeedUrl(long campaignId) {
        return String.format("%s/stocks/download", feedUrl(campaignId));
    }

    String commitsUrl(long campaignId) {
        return String.format("%s/commits", assortmentUrl(campaignId));
    }

    String commitUrl(long campaignId, long commitId) {
        return String.format("%s/%d", commitsUrl(campaignId), commitId);
    }

    String assortmentValidationsUrl(long campaignId) {
        return String.format("%s/validations", assortmentUrl(campaignId));
    }

    String assortmentResourceValidationsUrl(long campaignId) {
        return String.format("%s/validations/resources", assortmentUrl(campaignId));
    }

    String validationUrl(long campaignId, long validationId) {
        return String.format("%s/validations/%d", assortmentUrl(campaignId), validationId);
    }

    String feedUpdatesUrl(long campaignId) {
        return String.format("%s/updates", feedUrl(campaignId));
    }

    String patchFeedUpdatesUrl(long campaignId) {
        return String.format("%s/patch/updates", feedUrl(campaignId));
    }

    String feedUploadValidationsUrl(long campaignId) {
        return String.format("%s/validations/uploads", feedUrl(campaignId));
    }

    String feedReourceValidationsUrl(long campaignId) {
        return String.format("%s/validations/resources", feedUrl(campaignId));
    }

    String feedSuggestionsUrl(long campaignId) {
        return String.format("%s/suggests", feedUrl(campaignId));
    }

    String feedSuggestionUrl(long campaignId, long suggestionId) {
        return String.format("%s/suggests/%d", feedUrl(campaignId), suggestionId);
    }
}
