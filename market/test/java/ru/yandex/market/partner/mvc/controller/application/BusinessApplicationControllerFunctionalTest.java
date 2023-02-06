package ru.yandex.market.partner.mvc.controller.application;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты для {@link BusinessApplicationController}.
 */
@DbUnitDataSet(before = "data/BusinessApplicationControllerFunctionalTest.csv")
public class BusinessApplicationControllerFunctionalTest extends FunctionalTest {
    static Stream<Arguments> testGetBusinessApplicationsData() {
        return Stream.of(
                Arguments.of("Вернулись все заявления, доступные владельцу бизнеса",
                        10L,
                        "{\n" +
                                "  \"applications\": [\n" +
                                "    {\n" +
                                "      \"requestId\":2,\n" +
                                "      \"contractId\": \"469743/20\",\n" +
                                "      \"jurName\": \"orgName2\",\n" +
                                "      \"organizationType\": \"2\",\n" +
                                "      \"partners\": [\n" +
                                "        {\n" +
                                "          \"partnerId\": 200,\n" +
                                "          \"name\": \"магазин\"\n" +
                                "        },\n" +
                                "        {\n" +
                                "          \"partnerId\": 300,\n" +
                                "          \"name\": \"магазин2\"\n" +
                                "        },\n" +
                                "        {\n" +
                                "          \"partnerId\": 301,\n" +
                                "          \"name\": \"магазин301\"\n" +
                                "        }\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}"
                ),
                Arguments.of("Вернулись все заявления, доступные администратору магазина",
                        20L,
                        "{\n" +
                                "  \"applications\": [\n" +
                                "    {\n" +
                                "      \"requestId\":2,\n" +
                                "      \"contractId\": \"469743/20\",\n" +
                                "      \"jurName\": \"orgName2\",\n" +
                                "      \"organizationType\": \"2\",\n" +
                                "      \"partners\": [\n" +
                                "        {\n" +
                                "          \"partnerId\": 200,\n" +
                                "          \"name\": \"магазин\"\n" +
                                "        }\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}"
                ),
                Arguments.of("Вернулись все заявления, доступные агентству",
                        30L,
                        "{\n" +
                                "  \"applications\": [\n" +
                                "    {\n" +
                                "      \"requestId\":2,\n" +
                                "      \"contractId\": \"469743/20\",\n" +
                                "      \"jurName\": \"orgName2\",\n" +
                                "      \"organizationType\": \"2\",\n" +
                                "      \"partners\": [\n" +
                                "        {\n" +
                                "          \"partnerId\": 301,\n" +
                                "          \"name\": \"магазин301\"\n" +
                                "        }\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}"
                ));
    }

    static Stream<Arguments> testSelfEmployedGetBusinessApplicationsData() {
        return Stream.of(
                Arguments.of(
                        "Вернулись все не самозанятые заявления",
                        10L,
                        false,
                        //language=json
                        "" +
                                "{\n" +
                                "  \"applications\": [\n" +
                                "    {\n" +
                                "      \"requestId\": 16,\n" +
                                "      \"contractId\": \"12345/20\",\n" +
                                "      \"jurName\": \"orgName16\",\n" +
                                "      \"organizationType\":\"2\"," +
                                "      \"partners\": [\n" +
                                "        {\n" +
                                "          \"partnerId\": 600,\n" +
                                "          \"name\": \"поставщик\"\n" +
                                "        }\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}"
                ),
                Arguments.of(
                        "Вернулись все самозанятые заявления",
                        10L,
                        true,
                        //language=json
                        "" +
                                "{\n" +
                                "  \"applications\": [\n" +
                                "    {\n" +
                                "      \"requestId\": 15,\n" +
                                "      \"contractId\": \"12908/20\",\n" +
                                "      \"jurName\": \"orgName15\",\n" +
                                "      \"organizationType\":\"9\"," +
                                "      \"partners\": [\n" +
                                "        {\n" +
                                "          \"partnerId\": 500,\n" +
                                "          \"name\": \"поставщик\"\n" +
                                "        }\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}"
                )
        );
    }

    static Stream<Arguments> testB2BApplicationsData() {
        return Stream.of(
                Arguments.of(
                        "Есть подходящая заявка",
                        10,
                        3000,
                        //language=json
                        "" +
                                "{\n" +
                                "  \"applications\": [\n" +
                                "    {\n" +
                                "      \"requestId\": 16,\n" +
                                "      \"contractId\": \"12345/20\",\n" +
                                "      \"jurName\": \"orgName16\",\n" +
                                "      \"organizationType\": \"2\",\n" +
                                "      \"partners\": [\n" +
                                "        {\n" +
                                "          \"partnerId\": 600,\n" +
                                "          \"name\": \"поставщик\"\n" +
                                "        }\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}"
                ),
                Arguments.of(
                        "Нет подходящих заявок",
                        10,
                        2000,
                        //language=json
                        "{\"applications\":[]}"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGetBusinessApplicationsData")
    void testGetBusinessApplications(String testName, long uid, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/businesses/" + 2000 + "/applications?euid=" + uid);
        JsonTestUtil.assertEquals(response, expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testSelfEmployedGetBusinessApplicationsData")
    void testSelfEmployedGetBusinessApplications(String testName, long uid, Boolean isSelfEmployed, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/businesses/" + 3000 + "/applications?euid=" + uid + "&is_self_employed=" + isSelfEmployed);
        JsonTestUtil.assertEquals(response, expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testB2BApplicationsData")
    void testB2BSellerApplications(String testName, long uid, long businessId, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/businesses/" + businessId + "/applications?euid=" + uid + "&is_b2b_seller=true");
        JsonTestUtil.assertEquals(response, expected);
    }
}
