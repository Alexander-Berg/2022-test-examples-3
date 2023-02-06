package ru.yandex.market.deliverycalculator.searchengine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.xml.bind.JAXBContext;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;
import ru.yandex.market.deliverycalculator.searchengine.task.ImportNewGenerationsTask;
import ru.yandex.market.deliverycalculator.storage.model.FeedSourceType;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.FeedGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.RegularCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorMetaStorageService;
import ru.yandex.market.deliverycalculator.workflow.FeedSource;
import ru.yandex.market.deliverycalculator.workflow.regularcourier.IndexedCategoryInfo;
import ru.yandex.market.deliverycalculator.workflow.regularcourier.IndexedCategoryInfoCollection;
import ru.yandex.market.deliverycalculator.workflow.regularcourier.MatrixInfo;
import ru.yandex.market.deliverycalculator.workflow.regularcourier.TariffInfo;
import ru.yandex.market.deliverycalculator.workflow.util.FeedParserWorkflowUtils;
import ru.yandex.market.deliverycalculator.workflow.util.XmlUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchEngineApplicationTest extends FunctionalTest {
    private static final JAXBContext JAXB_CONTEXT = XmlUtils.jaxbContext(
            TariffInfo.class, MatrixInfo.class, IndexedCategoryInfo.class, IndexedCategoryInfoCollection.class
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliveryCalculatorMetaStorageService metaStorageService;

    @Autowired
    private ImportNewGenerationsTask importNewGenerationsTask;

    @Test
    void feedOffersJsonApiTest() {
        long shopId = 774;
        long feedId = 1069;
        long generationId = 10;
        long bucketId = 1000;
        String requestId = "testReq";

        String currency = "RUR";
        setTestData(shopId, feedId, generationId, bucketId, currency);
        importNewGenerationsTask.run();

        String request =
                "{\n" +
                        "  \"requestId\":\"" + requestId + "\",\n" +
                        "  \"feedId\":" + feedId + ",\n" +
                        "  \"generation_id\":" + generationId + ",\n" +
                        "  \"offers\":[\n" +
                        "    {\n" +
                        "      \"offerId\":123,\n" +
                        "      \"categories\":[1734],\n" +
                        "      \"programType\":[\"REGULAR_PROGRAM\"],\n" +
                        "      \"priceMap\":[{\"currency\":\"" + currency + "\", \"value\": 525}]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
        String response = new String(sendFeedOffersRequest(request.getBytes(StandardCharsets.UTF_8),
                MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE), StandardCharsets.UTF_8);
        String expected =
                "{\n" +
                        "  \"requestId\": \"" + requestId + "\",\n" +
                        "  \"responseCode\": 200,\n" +
                        "  \"generationId\": \"10\",\n" +
                        "  \"offers\": [{\n" +
                        "    \"deliveryOptBucketIds\": [\"1000\"]\n" +
                        "  }]\n" +
                        "}";
        Assertions.assertEquals(expected, response);
    }

    @Test
    void feedOffersDefaultApiTest() {
        long shopId = 775;
        long feedId = 1070;
        long generationId = 11;
        long bucketId = 1001;
        String requestId = "testRequest2";
        String currency = "USD";
        setTestData(shopId, feedId, generationId, bucketId, currency);
        importNewGenerationsTask.run();

        DeliveryCalcProtos.FeedOffersReq.Builder request = DeliveryCalcProtos.FeedOffersReq.newBuilder();
        request.setGenerationId(generationId);
        request.setFeedId(feedId);
        request.setRequestId(requestId);
        DeliveryCalcProtos.Offer.Builder offer = DeliveryCalcProtos.Offer.newBuilder();
        offer.setOfferId("456");
        offer.addProgramType(DeliveryCalcProtos.ProgramType.REGULAR_PROGRAM);
        offer.addPriceMap(DeliveryCalcProtos.OfferPrice.newBuilder().setCurrency(currency).setValue(342));
        request.addOffers(offer);

        byte[] response = sendFeedOffersRequest(getPbSn(request.build()), null, null);
        DeliveryCalcProtos.FeedOffersResp feedOffersResp = getFeedOffersResp(response);

        DeliveryCalcProtos.FeedOffersResp.Builder expected = DeliveryCalcProtos.FeedOffersResp.newBuilder();
        expected.setRequestId(requestId);
        expected.setResponseCode(200);
        expected.setGenerationId(generationId);
        expected.addOffers(DeliveryCalcProtos.OffersDelivery.newBuilder().addDeliveryOptBucketIds(bucketId));

        Assertions.assertEquals(expected.build(), feedOffersResp);
    }

    private static byte[] getPbSn(DeliveryCalcProtos.FeedOffersReq request) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PbSnUtils.writePbSnMessage("DCOR", request, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DeliveryCalcProtos.FeedOffersResp getFeedOffersResp(byte[] body) {
        try {
            return PbSnUtils.readPbSnMessage("DCOA", DeliveryCalcProtos.FeedOffersResp.parser(),
                    new ByteArrayInputStream(body));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTestData(long shopId, long feedId, long generationId, long bucketId, String currency) {
        RegularCourierGeneration regularCourierGeneration = new RegularCourierGeneration();
        regularCourierGeneration.setShopId(shopId);
        regularCourierGeneration.setBucketsUrl("testURL");
        regularCourierGeneration.setTariffInfo(getRegularCourierGenerationTariffInfo(bucketId, currency));
        FeedGeneration feedGeneration = new FeedGeneration();
        feedGeneration.setFeedId(feedId);
        feedGeneration.setSourceInfo(getShopFeedSourceInfo(shopId, 213));
        Generation generation = new Generation(generationId, generationId);
        generation.setRegularCourierGenerations(Collections.singleton(regularCourierGeneration));
        generation.setFeedGenerations(Collections.singleton(feedGeneration));
        metaStorageService.addGeneration(generation);
    }

    private static String getRegularCourierGenerationTariffInfo(long bucketId, String currency) {
        TariffInfo tariff = new TariffInfo();
        MatrixInfo matrix = new MatrixInfo();
        matrix.setBucketIds(new long[]{bucketId});
        matrix.setCurrency(currency);
        tariff.setMatrices(new MatrixInfo[]{matrix});
        return XmlUtils.serialize(JAXB_CONTEXT, tariff);
    }

    private byte[] sendFeedOffersRequest(byte[] body, String contentType, String accept) {
        try {
            MockHttpServletRequestBuilder request = post("/feedOffers").content(body);
            if (contentType != null) {
                request.contentType(contentType);
            }
            if (accept != null) {
                request.header(HttpHeaders.ACCEPT, accept);
            }
            return mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getShopFeedSourceInfo(long shopId, int regionId) {
        FeedSource feedSource = new FeedSource();
        feedSource.setType(FeedSourceType.SHOP);
        feedSource.setShopId(shopId);
        feedSource.setRegionId(regionId);
        return XmlUtils.serialize(FeedParserWorkflowUtils.FEED_SOURCE_JAXB_CONTEXT, feedSource);
    }

}
