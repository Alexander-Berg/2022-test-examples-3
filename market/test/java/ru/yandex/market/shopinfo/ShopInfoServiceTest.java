package ru.yandex.market.shopinfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.junit.Test;

import ru.yandex.market.mock.HttpResponseMockFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.shopinfo.MockHttpClientUtil.mockHttpClient;

/**
 * @author korolyov
 * 20.06.16
 */
public class ShopInfoServiceTest {
    private static final String EXPECTED_CORRECT_RESPONSE_FOR_SHOP_INFOS = "[{\"shopJurId\":\"561448\"," +
            "\"datasourceId\":\"720\",\"type\":\"1\",\"ogrn\":\"1097746730751\",\"url\":\"\"," +
            "\"name\":\"Янтарь-Фарма\",\"juridicalAddress\":\"125367, г. Москва, Волоколамское шоссе, 45\"," +
            "\"factAddress\":\"125367, г. Москва, Волоколамское шоссе, 45\",\"shopName\":\"shampoomania.ru\"," +
            "\"datasourceUrl\":\"www.shampoomania.ru\",\"createdAt\":\"2013-11-18 13:08:17.0\"," +
            "\"regnumName\":\"ОГРН\",\"contactPhone\":\"8 800 775-06-63\",\"cpaPersonId\":\"\"}," +
            "{\"shopJurId\":\"171\",\"datasourceId\":\"333\",\"type\":\"1\",\"ogrn\":\"1147746619240\"," +
            "\"url\":\"http://pleer.ru/reg_article.html\",\"name\":\"«Рассвет»\"," +
            "\"juridicalAddress\":\"115230, РФ, г. Москва," +
            " Варшавское шоссе, д. 65, корп. 2, помещение V, комната 6.\"," +
            "\"factAddress\":\"г. Москва, ул. Мастеркова, д. 4\",\"shopName\":\"www.pleer.ru\"," +
            "\"datasourceUrl\":\"Pleer.ru\",\"createdAt\":\"2003-12-18 18:47:51.0\"," +
            "\"regnumName\":\"ОГРН\",\"contactPhone\":\"+7 (495) 7750476\",\"cpaPersonId\":\"\"}]";
    private static final String EXPECTED_CORRECT_RESPONSE_FOR_SHOP_INFO = "[{\"shopJurId\":\"297\"," +
            "\"datasourceId\":\"720\",\"type\":\"1\",\"ogrn\":\"1027739244741\"," +
            "\"url\":\"http://www.ozon.ru/context/detail/id/1687308/\",\"name\":\"\\\"Интернет Решения\\\"\"," +
            "\"juridicalAddress\":\"125252, г. Москва, Чапаевский пер., д. 14\"," +
            "\"factAddress\":\"125252, г. Москва, Чапаевский пер., д. 14\",\"shopName\":\"www.ozon.ru\"," +
            "\"datasourceUrl\":\"www.ozon.domen.ru\",\"createdAt\":\"2000-12-22 14:05:49.0\",\"regnumName\":\"ОГРН\"," +
            "\"contactPhone\":\"8 (800) 775-06-06\",\"cpaPersonId\":\"\"}]";

    private static final String EXPECTED_CORRECT_RESPONSE_FOR_SHOP_NAMES = "[\n" +
            "  {\n" +
            "    \"id\": 720,\n" +
            "    \"name\": \"Pleer.ru\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"id\": 333,\n" +
            "    \"name\": \"www.ruptur.com\"\n" +
            "  }\n" +
            "]";

    private static final String EXPECTED_CORRECT_RESPONSE_FOR_SHOP_NAME = "[\n" +
            "  {\n" +
            "    \"id\": 666,\n" +
            "    \"name\": \"www.jj-connect.ru\"\n" +
            "  }\n" +
            "]";

    private static final String EXPECTED_CORRECT_RESPONSE_FOR_SHOP_RETURN_ADDRESS = "{\n" +
            "    \"value\": \"{\\\"street\\\":\\\"1-й Нагатинский проезд\\\"," +
            "\\\"fullAddress\\\":\\\"Москва, 1-й Нагатинский проезд, дом 10, строение 1, 115533\\\"," +
            "\\\"building\\\":\\\"10\\\",\\\"wing\\\":\\\"1\\\",\\\"description\\\":\\\"\\\"," +
            "\\\"region\\\":\\\"Москва\\\",\\\"index\\\":\\\"115533\\\"}\"\n" +
            "}";

    private static final long[] TEST_SHOP_IDS = new long[] {720, 333};
    private static final String[] TEST_SHOP_NAMES = new String[] {"Pleer.ru", "www.ruptur.com"};
    private static final long TEST_SHOP_ID = 666;
    private static final String TEST_SHOP_NAME = "www.jj-connect.ru";

    private static final long NONEXISTENT_SHOP_ID = 212354L;
    private static final String EXPECTED_RESPONSE_FOR_NONEXISTENT_SHOP_NAME = String.format("[\n" +
            "  {\n" +
            "    \"id\": %d,\n" +
            "    \"name\": null\n" +
            "  }\n" +
            "]", NONEXISTENT_SHOP_ID);

    private static final long NULL_SHOP_NAME_SHOP_ID = 1254L;
    private static final String EXPECTED_RESPONSE_FOR_NULL_SHOP_NAME = String.format("[\n" +
            "  {\n" +
            "    \"id\": %d,\n" +
            "    \"name\": \"null\"\n" +
            "  }\n" +
            "]", NONEXISTENT_SHOP_ID);

    private static ShopInfoService shopInfoService = new ShopInfoService();

    @Test
    public void shopReturnAddressCorrectResponseTest() throws Exception {
        HttpResponse mockHttpResponse = HttpResponseMockFactory
                .getHttpResponseMock(EXPECTED_CORRECT_RESPONSE_FOR_SHOP_RETURN_ADDRESS, 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        ShopReturnAddress address = shopInfoService.getShopReturnAddress(TEST_SHOP_IDS[0]).orElse(null);
        assertEquals("Москва, 1-й Нагатинский проезд, дом 10, строение 1, 115533", address.getFullAddress());
    }

    @Test
    public void shopInfoCorrectResponseTest() throws Exception {
        HttpResponse mockHttpResponse = HttpResponseMockFactory
                .getHttpResponseMock(EXPECTED_CORRECT_RESPONSE_FOR_SHOP_INFO, 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        ShopInfo info = shopInfoService.getShopInfo(TEST_SHOP_IDS[0]).orElse(null);
        assertEquals(TEST_SHOP_IDS[0], Long.parseLong(info.getDatasourceId()));
    }

    @Test
    public void shopInfosCorrectResponseTest() throws Exception {
        HttpResponse mockHttpResponse = HttpResponseMockFactory
                .getHttpResponseMock(EXPECTED_CORRECT_RESPONSE_FOR_SHOP_INFOS, 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        List<ShopInfo> list = shopInfoService.getShopInfos(TEST_SHOP_IDS);
        assertEquals(2, list.size());
        assertEquals(TEST_SHOP_IDS[0], Long.parseLong(list.get(0).getDatasourceId()));
        assertEquals(TEST_SHOP_IDS[1], Long.parseLong(list.get(1).getDatasourceId()));
    }

    @Test
    public void shopNamesIncorrectResponseTest() throws IOException {
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock("bad json", 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        List<String> list = shopInfoService.getShopNames(TEST_SHOP_IDS);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shopNamesCorrectResponseTest() throws IOException {
        HttpResponse mockHttpResponse = HttpResponseMockFactory
                .getHttpResponseMock(EXPECTED_CORRECT_RESPONSE_FOR_SHOP_NAMES, 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        List<String> list = shopInfoService.getShopNames(TEST_SHOP_IDS);
        List<String> expectedResult = Arrays.asList(TEST_SHOP_NAMES);
        assertEquals(list, expectedResult);
    }

    @Test
    public void shopNamesCode500Test() throws IOException {
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock("", 500);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        List<String> list = shopInfoService.getShopNames(TEST_SHOP_IDS);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shopNameIncorrectResponseTest() throws IOException {
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock("bad json", 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        Optional<String> optional = shopInfoService.getShopName(TEST_SHOP_ID);
        assertTrue(!optional.isPresent());
    }

    @Test
    public void shopNameForNonexistentShopId() throws IOException {
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(
                EXPECTED_RESPONSE_FOR_NONEXISTENT_SHOP_NAME, 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        Optional<String> optional = shopInfoService.getShopName(NONEXISTENT_SHOP_ID);
        assertFalse(optional.isPresent());
    }

    @Test
    public void shopNameNull() throws IOException {
        HttpResponse mockHttpResponse = HttpResponseMockFactory.getHttpResponseMock(
                EXPECTED_RESPONSE_FOR_NULL_SHOP_NAME, 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        Optional<String> optional = shopInfoService.getShopName(NULL_SHOP_NAME_SHOP_ID);
        String shopName = optional.get();
        assertEquals("null", shopName);
    }

    @Test
    public void shopNameCorrectResponseTest() throws IOException {
        HttpResponse mockHttpResponse = HttpResponseMockFactory
                .getHttpResponseMock(EXPECTED_CORRECT_RESPONSE_FOR_SHOP_NAME, 200);
        shopInfoService.setHttpClient(mockHttpClient(mockHttpResponse));
        Optional<String> optional = shopInfoService.getShopName(TEST_SHOP_ID);
        assertTrue(optional.isPresent());
        assertEquals(optional.get(), TEST_SHOP_NAME);
    }

}
