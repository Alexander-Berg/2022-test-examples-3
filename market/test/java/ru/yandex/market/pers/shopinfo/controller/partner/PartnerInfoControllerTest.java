package ru.yandex.market.pers.shopinfo.controller.partner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Supplier;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.pers.shopinfo.db.EatsAndLavkaPartnersYtDao;
import ru.yandex.market.pers.shopinfo.db.mapper.EatsAndLavkaPartnerYtInfo;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;
import ru.yandex.market.pers.shopinfo.yt.YtTemplate;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Функциональные тесты на {@link ru.yandex.market.pers.shopinfo.controller.PartnerInfoController}.
 */
@DbUnitDataSet(before = "partners.csv")
public class PartnerInfoControllerTest extends FunctionalTest {

    @Autowired
    YtTemplate eatsAndLavkaPartnersYtTemplate;

    @Autowired
    EatsAndLavkaPartnersYtDao eatsAndLavkaPartnersYtDao;

    @DisplayName("GET /partnerNames. Проверяет ответ при наличии и отсутствии партнера.")
    @Test
    void testSuppliersAndShopNames() {
        ResponseEntity<String> names = getPartnerNames(774, 775, 776, 777);
        //language=json
        String expected = "" +
                "[\n" +
                "  {\n" +
                "    \"id\": 774,\n" +
                "    \"name\": \"business1774\",\n" +
                "    \"slug\": \"business1774\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 775,\n" +
                "    \"name\": null,\n" +
                "    \"slug\": null\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 776,\n" +
                "    \"name\": \"business1776\",\n" +
                "    \"slug\": \"business1776\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 777,\n" +
                "    \"name\": null,\n" +
                "    \"slug\": null\n" +
                "  }\n" +
                "]";
        Assertions.assertNotNull(names.getBody());
        JsonTestUtil.assertEquals(expected, names.getBody());
    }

    @DisplayName(
            "GET /partnerNames. Проверяет ответ с параметрами eatAndLavkaIds."
    )
    @DbUnitDataSet(before = "env-1.csv")
    @ParameterizedTest
    @MethodSource
    void testNamesByEatsAndLavkaIds(Boolean ytFlag, String expected) throws JSONException {
        eatsAndLavkaPartnersYtDao.initYtAccess();
        eatsAndLavkaPartnersYtDao.ytAccessEnable = Mockito.mock(Supplier.class);
        Mockito.when(eatsAndLavkaPartnersYtDao.ytAccessEnable.get()).thenReturn(ytFlag);
        Mockito.when(eatsAndLavkaPartnersYtTemplate.getFromYt(any())).thenReturn(getEatsAndLavkaPartners());
        ResponseEntity<String> response = getPartnerNames( 786, 787);
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private static Stream<Arguments> testNamesByEatsAndLavkaIds() {
        return Stream.of(
                Arguments.of(true,
                        //language=json
                        "[\n" +
                        "  {\n" +
                        "    \"id\": 786,\n" +
                        "    \"name\": null,\n" +
                        "    \"slug\": null\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"id\": 787,\n" +
                        "    \"name\": \"Лента\",\n" +
                        "    \"slug\": \"lenta\"\n" +
                        "  }\n" +
                        "]"),
                Arguments.of(false,
                        //language=json
                        "[{\"id\":786,\"name\":null,\"slug\":null},{\"id\":787,\"name\":null,\"slug\":null}]")
        );
    }

    @DisplayName("GET /partnerNames. Проверяет ответ при отсутсвии partner-id")
    @Test
    void testNoPartnerIds() {
        HttpServerErrorException exception = Assertions.assertThrows(
                HttpServerErrorException.class,
                this::getPartnerNames
        );
        JsonTestUtil.assertEquals(
                "{\"message\":\"wrong-partner-id\"}",
                exception.getResponseBodyAsString()
        );
    }

    private ResponseEntity<String> getPartnerNames(long... partnerIds) {
        return FunctionalTestHelper.get(
                urlBasePrefix + "/partnerNames?partner-id={partnerIds}",
                paramString(partnerIds)
        );
    }

    private static String paramString(long[] shopIds) {
        return Arrays.stream(shopIds)
                .boxed()
                .map(id -> Long.toString(id))
                .collect(Collectors.joining(","));
    }

    private static String paramString(String[] shopIds) {
        return String.join(",", shopIds);
    }

    @DisplayName(
            "GET /partnerInfo. Проверяет работу partnerInfo с параметрами shopJurId."
    )
    @Test
    void partnerInfoJur() throws JSONException {
        ResponseEntity<String> response = getPartnerInfoJur(urlBasePrefix, "1774", "774");
        String expected = getString(getClass(), "shopInfo774.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }


    @DisplayName(
            "GET /partnerInfo. Проверяет работу partnerInfo с параметрами shopIds."
    )
    @Test
    void partnerInfoShopIds() throws JSONException {
        ResponseEntity<String> response = getPartnerInfoByIds(urlBasePrefix, "774");
        String expected = getString(getClass(), "shopInfo774.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

        response = getPartnerInfoByIds(urlBasePrefix, "777");
        Assertions.assertEquals("[]", response.getBody());
    }

    @DisplayName(
            "GET /partnerInfo. Проверяет работу partnerInfo с параметрами shopIds (dbs)."
    )
    @Test
    void partnerInfoShopIdsDbs() throws JSONException {
        ResponseEntity<String> response = getPartnerInfoByIds(urlBasePrefix, "778");
        String expected = getString(getClass(), "shopInfo778.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

        response = getPartnerInfoByIds(urlBasePrefix, "777");
        Assertions.assertEquals("[]", response.getBody());
    }

    @DisplayName(
            "GET /partnerInfo. Проверяет работу partnerInfo с параметрами supplierIds."
    )
    @Test
    void partnerInfoSupplierIds() throws JSONException {
        ResponseEntity<String> response = getPartnerInfoByIds(urlBasePrefix, "776");
        String expected = getString(getClass(), "supplierInfo776.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @DisplayName(
            "GET /partnerInfo. Проверяет работу partnerInfo с параметрами eatAndLavkaIds."
    )
    @ParameterizedTest
    @MethodSource
    void partnerInfoEatsAndLavkaIds(Boolean ytFlag, String fileName) throws JSONException {
        eatsAndLavkaPartnersYtDao.initYtAccess();
        eatsAndLavkaPartnersYtDao.ytAccessEnable = Mockito.mock(Supplier.class);
        Mockito.when(eatsAndLavkaPartnersYtDao.ytAccessEnable.get()).thenReturn(ytFlag);
        Mockito.when(eatsAndLavkaPartnersYtTemplate.getFromYt(any())).thenReturn(getEatsAndLavkaPartners());
        ResponseEntity<String> response = getPartnerInfoByIds(urlBasePrefix, "786", "787");
        String expected = fileName.isEmpty() ? "[]" : getString(getClass(), fileName);
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private static Stream<Arguments> partnerInfoEatsAndLavkaIds() {
        return Stream.of(
                Arguments.of(true, "eatsAndLavkaInfo.json"),
                Arguments.of(false, "")
        );
    }

    @DisplayName(
            "GET /partnerInfo. Проверяет работу partnerInfo с параметрами shopJurId, shopIds и supplierIds"
    )
    @Test
    void partnerInfoFull() throws JSONException {
        ResponseEntity<String> response = getPartnerInfoFull(urlBasePrefix, "1774", "774", "776");
        String expected = getString(getClass(), "shopAndSupplierInfo.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }


    private static ResponseEntity<String> getPartnerInfoFull(String urlBasePrefix, String shopJurId, String... partnerIds) {
        String url = urlBasePrefix + "/partnerInfo?shop-jur-id={shopJurId}&partner-id={partnerIds}";
        return FunctionalTestHelper.get(url, shopJurId, paramString(partnerIds));
    }

    private static ResponseEntity<String> getPartnerInfoByIds(String urlBasePrefix, String... partnerIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/partnerInfo?partner-id={shopIds}", paramString(partnerIds));
    }

    private static ResponseEntity<String> getPartnerInfoJur(String urlBasePrefix, String shopJurId, String... partnerIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/partnerInfo?partner-id={partnerIds}&shop-jur-id={shopJurId}",
                partnerIds, shopJurId);
    }

    private List<EatsAndLavkaPartnerYtInfo> getEatsAndLavkaPartners() {
        return List.of(
                EatsAndLavkaPartnerYtInfo.builder().partnerId(786L).build(),
                EatsAndLavkaPartnerYtInfo.builder().partnerId(787L).inn("787004178702")
                        .address("Россия, Москва, Большая Черёмушкинская улица, 1")
                        .legalAddress("Санкт-Петербург, ул Савушкина, д 112, литер б")
                        .businessName("Лента")
                        .createdAt("2022-02-08T17:48:08.681213")
                        .ogrn("1037832048605")
                        .phone("84950008765")
                        .schedule(
                                "[{\"from\": \"09:00\", \"weekday\": \"monday\", \"duration\": " +
                                        "645}, {\"from\": \"09:00\", \"weekday\": \"tuesday\", " +
                                        "\"duration\": 645}, {\"from\": \"09:00\", \"weekday\": " +
                                        "\"wednesday\", \"duration\": 645}, {\"from\": \"09:00\", " +
                                        "\"weekday\": \"thursday\", \"duration\": 645}, {\"from\": " +
                                        "\"09:00\", \"weekday\": \"friday\", \"duration\": 645}, " +
                                        "{\"from\": \"09:00\", \"weekday\": \"saturday\", " +
                                        "\"duration\": 645}, {\"from\": \"09:00\", \"weekday\": " +
                                        "\"sunday\", \"duration\": 645}]"
                        )
                        .shopName("ЛЕНТА")
                        .build()
        );
    }
}
