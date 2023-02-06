package ru.yandex.market.report;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.StreamUtils;

import ru.yandex.market.mock.HttpResponseMockFactory;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Offer;
import ru.yandex.market.report.model.ProductType;
import ru.yandex.market.report.request.AccessoriesRequest;
import ru.yandex.market.report.request.ProductAccessoriesRequest;
import ru.yandex.market.report.request.ReportRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author korolyov
 * 07.04.17
 */
public class ReportServiceTest {
    private static final String BAD_JSON = "{\n" +
            "\"error\": {\n" +
            "\"code\": \"EMPTY_REQUEST\",\n" +
            "\"message\": \"\"\n" +
            "}\n" +
            "}";
    private ReportService reportService = new ReportService("", 0);

    private void verifyCommonFields(Model model) {
        assertNotNull(model.getSlug());
        assertNotNull(model.getCategory());
        assertNotNull(model.getCategory().getId());
        assertNotNull(model.getCategory().getName());
        assertNotNull(model.getPictureUrl());
        assertNotNull(model.getPrices().getAveragePrice());
        assertNotNull(model.getPrices().getMinimalPrice());
        assertNotNull(model.getPrices().getMaximalPrice());
        assertNotNull(model.getPrices().getCurrency());
        assertNotNull(model.getType());
        assertNotNull(model.getOffersCount());
    }

    private void verifyOffer(Offer offer) {
        assertNotNull(offer.getCategory());
        assertNotNull(offer.getCategory().getId());
        assertNotNull(offer.getCategory().getName());
        assertNotNull(offer.getPictureUrl());
        assertNotNull(offer.getPrices().getCurrency());
        assertNotNull(offer.getPrices().getPrice());
        assertNotNull(offer.getPrices().getPriceWithoutModifications());
        assertNotNull(offer.getName());
        assertNotNull(offer.getModelId());
        assertNotNull(offer.getFeedId());
        assertNotNull(offer.getShopOfferId());
        assertNotNull(offer.getWareId());
        assertNotNull(offer.getShop().getId());
        assertNotNull(offer.getShop().getName());
        assertNotNull(offer.getShop().getQualityRating());
        assertNotNull(offer.getShop().getPriorityRegion().getId());
        assertNotNull(offer.getShop().getPriorityRegion().getName());
        assertNotNull(offer.getCpa());
        assertNotNull(offer.isOnStock());
        assertNotNull(offer.isDeliveryAvailable());
    }


    private void verifyModel(Model model) {
        verifyCommonFields(model);
        assertEquals(ProductType.MODEL, model.getType());
        assertNotNull(model.getVendor().getId());
        assertNotNull(model.getVendor().getName());
    }

    private void verifyModelAnyType(Model model) {
        verifyCommonFields(model);
        assertNotNull(model.getVendor().getId());
        assertNotNull(model.getVendor().getName());
    }


    private void verifyCluster(Model model) {
        verifyCommonFields(model);
        assertEquals(ProductType.CLUSTER, model.getType());
        assertNotNull(model.getVendor().getId());
    }

    private void verifyBook(Model model) {
        verifyCommonFields(model);
        assertEquals(ProductType.BOOK, model.getType());
        assertNull(model.getVendor());
    }

    private void verifyGroup(Model model) {
        verifyCommonFields(model);
        assertEquals(ProductType.GROUP, model.getType());
        assertNotNull(model.getVendor().getId());
        assertNotNull(model.getVendor().getName());
    }

    private void verifyModification(Model model) {
        verifyCommonFields(model);
        assertEquals(ProductType.MODIFICATION, model.getType());
        assertNotNull(model.getVendor().getId());
        assertNotNull(model.getVendor().getName());
    }

    @Test
    public void getAccessories() throws Exception {
        mockHttpClient("report_accessories.json");

        ReportRequest request = AccessoriesRequest.builder(
                Collections.singletonList("DdlMBsPvpjP72hFjKprJOQ"), Collections.singletonList(1L))
                .addRegionId(109371L)
                .request();
        List<Offer> offers = reportService.getOffersByRequest(request);
        assertFalse(offers.isEmpty());
        for (Offer offer : offers) {
            verifyOffer(offer);
        }
    }

    @Test
    public void getProductAccessories() throws Exception {
        mockHttpClient("report_product_accessoires.json");

        ReportRequest request = ProductAccessoriesRequest.builder(Collections.singletonList(1722193751L))
                .request();
        List<Model> models = reportService.getModelsByRequest(request);
        assertFalse(models.isEmpty());
        for (Model model : models) {
            verifyModelAnyType(model);
        }
    }

    @Test
    public void getModelByIdTest() throws Exception {
        mockHttpClient("report_model.json");
        Optional<Model> optional = reportService.getModelById(0L);
        assertTrue(optional.isPresent());
        verifyModel(optional.get());
    }

    @Test
    public void getModelsByIdsWithTwoEqualModelTest() throws Exception {
        mockHttpClient("report_two_same_models.json");
        Map<Long, Model> models = reportService.getModelsByIds(Arrays.asList(1L, 2L));
        assertEquals(1, models.size());
        Assert.assertNotNull(models.get(13909442L));
    }

    @Test
    public void getModelsByIdsTest() throws Exception {
        mockHttpClient("report_models.json");
        Map<Long, Model> models = reportService.getModelsByIds(Arrays.asList(1718783441L, 1722876098L));
        assertEquals(2, models.size());
        verifyCluster(models.get(1718783441L));
        verifyCluster(models.get(1722876098L));
    }

    @Test
    public void getClusterByIdTest() throws Exception {
        mockHttpClient("report_cluster.json");
        Optional<Model> optional = reportService.getModelById(0L);
        assertTrue(optional.isPresent());
        verifyCluster(optional.get());
    }

    @Test
    public void getBookByIdTest() throws Exception {
        mockHttpClient("report_book.json");
        Optional<Model> optional = reportService.getModelById(0L);
        assertTrue(optional.isPresent());
        verifyBook(optional.get());
    }

    @Test
    public void getModificationByIdTest() throws Exception {
        mockHttpClient("report_modification.json");
        Optional<Model> optional = reportService.getModelById(0L);
        assertTrue(optional.isPresent());
        verifyModification(optional.get());
    }

    @Test
    public void getGroupByIdTest() throws Exception {
        mockHttpClient("report_group.json");
        Optional<Model> optional = reportService.getModelById(0L);
        assertTrue(optional.isPresent());
        verifyGroup(optional.get());
    }

    @Test
    public void badJsonTest() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock("bad json", 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
        Optional<Model> optional = reportService.getModelById(0L);
        optional.ifPresent(model -> fail("Optional is not empty"));
    }

    @Test
    public void errorFromReportTest() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock("", 500);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
        Optional<Model> optional = reportService.getModelById(0L);
        assertFalse(optional.isPresent());
    }

    @Test
    public void getModelsForCategoryIdOrderedByPriceTest() throws Exception {
        mockHttpClient("report_category.json");
        List<Model> models = reportService.getModelsForCategoryIdOrderedByPrice(0L);
        assertFalse(models.isEmpty());
    }

    @Test
    public void getModelsPrimeTest() throws Exception {
        mockHttpClient("report_category.json");
        List<Model> models = reportService.getModelsPrime(0L, 10);
        assertFalse(models.isEmpty());
        assertEquals("Apple Watch Hermes 38mm with Double Tour", models.get(0).getName());
        assertEquals("//mdata.yandex.net/i?path=b0806064331_img_id3390338478544408005.jpeg",
                models.get(0).getPictureUrl());
    }

    @Test
    public void getCategoryPicturesWithModelsTest() throws Exception {
        mockHttpClient("report_category.json");

        Map<Long, String> result = reportService.getMainPictureByHid(Arrays.asList(1L, 2L));
        assertEquals(2, result.size());
        assertEquals("//mdata.yandex.net/i?path=b0806064331_img_id3390338478544408005.jpeg",
                result.get(1L));
        assertEquals("//mdata.yandex.net/i?path=b0806064331_img_id3390338478544408005.jpeg",
                result.get(2L));
    }

    @Test
    public void getCategoryPicturesWithOffersTest() throws Exception {
        mockHttpClient("report_offerinfo.json");

        Map<Long, String> result = reportService.getMainPictureByHid(Arrays.asList(3L, 5L, 6L));
        assertEquals(3, result.size());
        assertEquals("//0.cs-ellpic01gt.yandex.ru/market_p0_2kBtJIwspgZlkAlaB5A_1x1.jpg", result.get(3L));
        assertEquals("//0.cs-ellpic01gt.yandex.ru/market_p0_2kBtJIwspgZlkAlaB5A_1x1.jpg", result.get(5L));
        assertEquals("//0.cs-ellpic01gt.yandex.ru/market_p0_2kBtJIwspgZlkAlaB5A_1x1.jpg", result.get(6L));
    }

    @Test
    public void getModelsForCategoryIdOrderedByPriceBadJsonTest() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(BAD_JSON, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
        List<Model> models = reportService.getModelsForCategoryIdOrderedByPrice(0L);
        assertTrue(models.isEmpty());
    }

    @Test
    public void getModelAnalogs() throws Exception {
        mockHttpClient("report_analogs.json");
        List<Model> models = reportService.getProductAnalogs(0L, 30L, 0L, null, null);
        assertEquals(30, models.size());
        assertEquals("Sony PlayStation 4 500 ГБ", models.get(0).getName());
    }

    @Test
    public void getModelAnalogsReturnsEmptyListIfNoneFound() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(BAD_JSON, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
        List<Model> models = reportService.getProductAnalogs(0L, 30L, 0L, null, null);
        assertTrue(models.isEmpty());
    }

    @Test
    public void getOfferTest() throws Exception {
        mockHttpClient("report_offerinfo.json");
        Optional<Offer> optional = reportService.getOffer("DdlMBsPvpjP72hFjKprJOQ", 109371L);
        if (optional.isPresent()) {
            verifyOffer(optional.get());
        } else {
            fail("Optional is empty");
        }
    }

    @Test
    public void getOfferBadRequestTest() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(BAD_JSON, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
        Optional<Offer> optional = reportService.getOffer("DdlMBsPvpjP72hFjKprJOQ", 109371L);
        assertFalse(optional.isPresent());
    }

    @Test
    public void getOffersTest() throws Exception {
        mockHttpClient("report_productoffers.json");
        List<Offer> offers = reportService.getOffersByModel(5, "RUR", 1721921261, 5, 10);
        assertEquals(10, offers.size());
        offers.forEach(this::verifyOffer);
    }

    @Test
    public void getOffersBadRequestTest() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(BAD_JSON, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
        List<Offer> offers = reportService.getOffersByModel(5, "RUR", 1721921261, 5, 10);
        assertTrue(offers.isEmpty());
    }

    @Test
    public void getPictureUrlTest() throws Exception {
        mockHttpClient("report_offerinfo.json");
        Optional<Offer> optional = reportService.getOffer("DdlMBsPvpjP72hFjKprJOQ", 109371L);
        assertEquals("//0.cs-ellpic01gt.yandex.ru/market_p0_2kBtJIwspgZlkAlaB5A_1x1.jpg",
                optional.get().getPictureUrl());
    }

    @Test
    public void getPictureThumbsUrlTest() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        String content = StreamUtils.copyToString(getClass().getClassLoader()
                .getResourceAsStream("report_offerinfo.json"), Charset.forName("UTF-8"));
        content = content.replace("\"original\": {\n" +
                "              \"containerWidth\": 1000,\n" +
                "              \"containerHeight\": 1000,\n" +
                "              \"url\": \"//0.cs-ellpic01gt.yandex.ru/market_p0_2kBtJIwspgZlkAlaB5A_1x1.jpg\",\n" +
                "              \"width\": 1000,\n" +
                "              \"height\": 1000\n" +
                "            },", "");
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(content, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
        Optional<Offer> optional = reportService.getOffer("DdlMBsPvpjP72hFjKprJOQ", 109371L);
        assertEquals("//0.cs-ellpic01gt.yandex.ru/market_p0_2kBtJIwspgZlkAlaB5A_900x1200.jpg",
                optional.get().getPictureUrl());
    }

    @Test
    public void checkExtraHeadersTest() throws Exception {
        Map<String, String> extraHeaders = Collections.singletonMap("resource-meta",
            "{\"scenario\":\"AddUgcContent\",\"client\":\"market_reviews\",\"pageId\":\"model\"}");

        HttpClient httpClient = mock(HttpClient.class);
        String content = StreamUtils.copyToString(getClass()
            .getClassLoader().getResourceAsStream("report_model.json"), Charset.forName("UTF-8"));
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(content, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        ReportService report = new ReportService("", 0, httpClient);

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);

        // no headers
        report.getModelById(0L);
        verify(httpClient).execute(requestCaptor.capture());

        assertEquals(0, requestCaptor.getValue().getAllHeaders().length);

        // with header
        report.setExtraHeaders(extraHeaders);
        report.getModelById(0L);
        verify(httpClient, times(2)).execute(requestCaptor.capture());

        assertEquals(extraHeaders.get("resource-meta"),
            requestCaptor.getValue().getHeaders("resource-meta")[0].getValue());
    }

    private void mockHttpClient(String responseFileName) throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        String content = StreamUtils.copyToString(getClass()
                .getClassLoader().getResourceAsStream(responseFileName), Charset.forName("UTF-8"));
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(content, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        reportService.setHttpClient(httpClient);
    }
}
