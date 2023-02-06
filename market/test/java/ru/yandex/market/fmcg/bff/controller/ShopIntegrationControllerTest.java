package ru.yandex.market.fmcg.bff.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import ru.yandex.market.fmcg.bff.controller.dto.ShopOutlet;
import ru.yandex.market.fmcg.bff.region.RegionService;
import ru.yandex.market.fmcg.bff.region.model.Coords;
import ru.yandex.market.fmcg.bff.region.model.GeobaseRegion;
import ru.yandex.market.fmcg.bff.shops.basket.ShopIntegrationParamsUpdateService;
import ru.yandex.market.fmcg.bff.test.FmcgBffTest;
import ru.yandex.market.fmcg.client.backend.FmcgBackClient;
import ru.yandex.market.fmcg.core.search.index.IndexService;
import ru.yandex.market.fmcg.core.search.index.model.Price;
import ru.yandex.market.fmcg.main.model.CartMethod;
import ru.yandex.market.fmcg.main.model.dto.BasketShopIntegrationParamsDto;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static ru.yandex.market.fmcg.bff.test.TestUtil.loadResourceAsBytes;
import static ru.yandex.market.fmcg.bff.test.TestUtil.loadResourceAsString;

/**
 * @author semin-serg
 */
class ShopIntegrationControllerTest extends FmcgBffTest {

    private static final long SHOP_ID = 12354;
    private static final String PARTNER_CART_URL = "https://example.com:8080/basket";
    private static final String RESULT_BASKET_URL_FROM_PARTNER = "https://example.com/basket?id=v6ruxglny7qqykzn";
    private static final String ULYBKA_RADUGI_CART_URL = "https://eshop.api.prod.tdera.ru/v1/basket/page";
    private static final String RESULT_BASKET_URL_ULYBKA_RADUGI = "https://www.r-ulybka.ru/personal/basket/?id=1234";
    private static final Set<Long> MSKUS = unmodifiableSet(new HashSet<>(Arrays.asList(5035L, 810L, 1118L)));
    private static final String OFFER_ID_PREFIX = "offerId-";

    @Value("${market.fmcg.bff.partner.azbuka_vkusa.shop_id}")
    long azbukaVkusaShopId;
    @Value("${market.fmcg.bff.partner.ulybka_radugi.shop_id}")
    long ulybkaRadugiShopId;
    @Value("${market.fmcg.bff.partner.ulybka_radugi.authorization_token}")
    String ulybkaRadugiToken;

    @Autowired
    FmcgBackClient fmcgBackClient;

    @Autowired
    IndexService indexService;

    @Autowired
    RegionService regionService;

    @Autowired
    ShopIntegrationParamsUpdateService shopIntegrationParamsUpdateService;

    @Autowired
    MockRestServiceServer shopIntegrationMockServer;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AtomicReference<Map<Long, BasketShopIntegrationParamsDto>> basketShopIntegrationParamsRef;

    private Map<Long, BasketShopIntegrationParamsDto> originalIntegrationParams;

    @BeforeEach
    void backupOriginalIntegrationParams() {
        originalIntegrationParams = basketShopIntegrationParamsRef.get();
    }

    @BeforeEach
    void setupIndexService() {
        setupIndexService(SHOP_ID);
    }

    @AfterEach
    void restoreOriginalIntegrationParams() {
        basketShopIntegrationParamsRef.set(originalIntegrationParams);
    }

    void loadIntegrationParams() {
        loadIntegrationParams(CartMethod.POST);
    }

    void loadIntegrationParams(CartMethod cartMethod) {
        when(fmcgBackClient.getShopsIntegrationParams()).thenAnswer((invocation -> generateShopIntegrationParams(
            cartMethod)));
        shopIntegrationParamsUpdateService.update();
    }

    void setupIndexService(long shopId) {
        reset(indexService);
        ShopOutlet shopOutlet = new ShopOutlet(shopId, null);
        when(indexService.getPrices(argThat(request -> MSKUS.equals(new HashSet<>(request.getMskuIds())) &&
            singletonList(shopOutlet).equals(request.getShopOutlets())))).thenAnswer(inv -> createPrices(shopOutlet));
    }

    List<Price> createPrices(ShopOutlet shopOutlet) {
        return MSKUS.stream().map(msku -> new Price(msku, shopOutlet, 1L, null, null, OFFER_ID_PREFIX + msku))
            .collect(Collectors.toList());
    }

    List<BasketShopIntegrationParamsDto> generateShopIntegrationParams(CartMethod cartMethod) {
        return Arrays.asList(
            new BasketShopIntegrationParamsDto(1, CartMethod.GET, "https://supershop.ru/api/basket"),
            new BasketShopIntegrationParamsDto(SHOP_ID, cartMethod, PARTNER_CART_URL),
            new BasketShopIntegrationParamsDto(3, CartMethod.GET, "https://somelocalshop.ru/order"),
            new BasketShopIntegrationParamsDto(azbukaVkusaShopId, CartMethod.GET,
                "https://av.ru/recipe/api/order-by-goods"),
            new BasketShopIntegrationParamsDto(ulybkaRadugiShopId, CartMethod.POST, ULYBKA_RADUGI_CART_URL)
        );
    }

    void setupPartnerMockServerExpectPost(ResponseCreator responseCreator) {
        setupPartnerMockServerExpectPost(responseCreator, 1);
    }

    void setupPartnerMockServerExpectPost(ResponseCreator responseCreator, int expectedCount) {
        shopIntegrationMockServer
            .expect(ExpectedCount.times(expectedCount), requestTo(PARTNER_CART_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/json;charset=UTF-8"))
            .andExpect(MockRestRequestMatchers.content().json(loadResourceAsString("ExternalBasketUrlRequest.json")))
            .andRespond(responseCreator);
    }

    void setupPartnerMockServerExpectPostReturnsRedirect() {
        setupPartnerMockServerExpectPost(withStatus(HttpStatus.FOUND).headers(createHeadersWithRedirect(
            RESULT_BASKET_URL_FROM_PARTNER)));
    }

    HttpHeaders createHeadersWithRedirect(String redirectUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        return headers;
    }

    void setupPartnerMockServerExpectPostReturnsBadRequest() {
        setupPartnerMockServerExpectPost(withStatus(HttpStatus.BAD_REQUEST));
    }

    void setupPartnerMockServerExpectPostReturnsInternalServerError(int expectedCount) {
        setupPartnerMockServerExpectPost(withStatus(HttpStatus.INTERNAL_SERVER_ERROR), expectedCount);
    }

    void setupPartnerMockServerExpectNoRequests() {
        shopIntegrationMockServer.expect(ExpectedCount.never(), anything());
    }

    void makeRequestToBff(String requestBodyFileName, int expectedStatus,
                          String expectedResponseFileName) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/apiv1/shops/basketUrl")
            .header("User-Region", 2)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(loadResourceAsBytes(requestBodyFileName)))
            .andDo(print())
            .andExpect(status().is(expectedStatus))
            .andExpect(content().json(loadResourceAsString(expectedResponseFileName)));
    }

    @Test
    void generalPostTest() throws Exception {
        loadIntegrationParams(CartMethod.POST);
        setupPartnerMockServerExpectPostReturnsRedirect();

        makeRequestToBff("BasketUrlRequest.json", HttpStatus.OK.value(), "BasketUrlResponse1.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void ulybkaRadugiTest() throws Exception {
        //Санкт-Петербург
        final int regionId = 2;
        loadIntegrationParams();
        setupIndexService(ulybkaRadugiShopId);
        shopIntegrationMockServer
            .expect(requestTo(ULYBKA_RADUGI_CART_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic " + ulybkaRadugiToken))
            .andExpect(header("Content-Type", "application/json;charset=UTF-8"))
            .andExpect(MockRestRequestMatchers.content().json(loadResourceAsString(
                "ExternalBasketUrlRequestUlybkaRadugi.json")))
            .andRespond(withStatus(HttpStatus.FOUND).headers(createHeadersWithRedirect(
                RESULT_BASKET_URL_ULYBKA_RADUGI)));

        when(regionService.getFmcgRegion(eq(regionId))).thenReturn(Optional.of(
            new GeobaseRegion(2, "Санкт-Петербург", new Coords(0d, 0d), "", 11)
        ));

        mockMvc.perform(MockMvcRequestBuilders.post("/apiv1/shops/basketUrl")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .header("User-Region", String.valueOf(regionId))
            .content(loadResourceAsBytes("BasketUrlRequestUlybkaRadugi.json")))
            .andDo(print())
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().json(loadResourceAsString("BasketUrlResponseUlybkaRadugi.json")));

        shopIntegrationMockServer.verify();
    }

    @Test
    void generalGetTest() throws Exception {
        loadIntegrationParams(CartMethod.GET);
        setupPartnerMockServerExpectNoRequests();

        makeRequestToBff("BasketUrlRequest.json", HttpStatus.OK.value(), "BasketUrlResponse2.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void azbukaVkusaTest() throws Exception {
        loadIntegrationParams();
        setupIndexService(azbukaVkusaShopId);
        setupPartnerMockServerExpectNoRequests();

        makeRequestToBff("BasketUrlRequestAzbukaVkusa.json", HttpStatus.OK.value(),
            "BasketUrlResponseAzbukaVkusa.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void nonExistentShop() throws Exception {
        loadIntegrationParams();
        setupPartnerMockServerExpectNoRequests();

        makeRequestToBff("BasketUrlRequestNonExistentShop.json", HttpStatus.BAD_REQUEST.value(),
            "BasketUrlResponseForNonExistentShop.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void integrationParamsWereNotReceived() throws Exception {
        setupPartnerMockServerExpectNoRequests();

        makeRequestToBff("BasketUrlRequest.json", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "BasketUrlResponseWhenInternalServerError.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void partnerServiceReturnsBadRequest() throws Exception {
        loadIntegrationParams(CartMethod.POST);
        setupPartnerMockServerExpectPostReturnsBadRequest();

        makeRequestToBff("BasketUrlRequest.json", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "BasketUrlResponseWhenInternalServerError.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void partnerServiceReturnsInternalServerErrorOnce() throws Exception {
        loadIntegrationParams(CartMethod.POST);
        setupPartnerMockServerExpectPostReturnsInternalServerError(1);
        setupPartnerMockServerExpectPostReturnsRedirect();

        makeRequestToBff("BasketUrlRequest.json", HttpStatus.OK.value(), "BasketUrlResponse1.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void partnerServiceReturnsInternalServerTooManyTimes() throws Exception {
        loadIntegrationParams(CartMethod.POST);
        setupPartnerMockServerExpectPostReturnsInternalServerError(4);

        makeRequestToBff("BasketUrlRequest.json", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "BasketUrlResponseWhenInternalServerError.json");

        shopIntegrationMockServer.verify();
    }

    @Test
    void incorrectRequestFromBffClient() throws Exception {
        loadIntegrationParams();
        setupPartnerMockServerExpectNoRequests();

        makeRequestToBff("BasketUrlRequestIncorrect.json", HttpStatus.BAD_REQUEST.value(),
            "BasketUrlResponseForIncorrectRequest.json");

        shopIntegrationMockServer.verify();
    }

}
