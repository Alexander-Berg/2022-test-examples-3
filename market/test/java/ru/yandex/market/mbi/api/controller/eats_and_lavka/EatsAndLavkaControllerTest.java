package ru.yandex.market.mbi.api.controller.eats_and_lavka;

import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author alexander-tr@yandex-team.ru
 * @since 20.04.2021
 */
@DbUnitDataSet(
        before = "EatsAndLavkaControllerTest.csv"
)
public class EatsAndLavkaControllerTest extends FunctionalTest {
    @Autowired
    private PassportService passportService;

    private String url;

    @Autowired
    private TestableClock clock;

    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @BeforeEach
    public void setup() {
        url = "http://localhost:" + port + "/eats-and-lavka/get-or-create/market-credentials";
        when(passportService.getUserInfo(anyLong())).thenAnswer(i ->
                new UserInfo(
                        i.<Long>getArgument(0),
                        "testUser",
                        null,
                        "testUser"
                )
        );
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    public void testWithTwoBusinessesForOneServiceIdCaseHandling() throws JsonProcessingException {
        {
            String request = //language=json
                    "{" +
                        "\"service_id\": \"lavka:kek\"," +
                        "\"eats_and_lavka_id\": \"SomeShop\"," +
                        "\"uid\": 100502" +
                    "}";
            String expectedResponse = //language=json
                    "{" +
                        "\"market_feed_id\": 124," +
                        "\"partner_id\": 654," +
                        "\"business_id\": 900" +
                    "}";
            doPostEmptyVatRegion(request, expectedResponse);
        }

        {
            String request = //language=json
                    "{" +
                            "\"service_id\": \"lavka:kek\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100502" +
                            "}";
            String expectedResponse = //language=json
                    "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 1," +
                            "\"business_id\": 900" +
                            "}";
            doPostEmptyVatRegion(request, expectedResponse);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource
    void testRequest(String request, String expectedResponse, String testName) {
        doPostEmptyVatRegion(request, expectedResponse);
    }

    private static Stream<Arguments> testRequest() {
        return Stream.of(
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:ru\"," +
                            "\"eats_and_lavka_id\": \"SomeShop\"," +
                            "\"uid\": 100500" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 123," +
                            "\"partner_id\": 456," +
                            "\"business_id\": 789" +
                        "}",
                        "testWithGettingExistingCredentialsOldUid"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:ru\"," +
                            "\"eats_and_lavka_id\": \"SomeShop\"," +
                            "\"uid\": 100501" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 123," +
                            "\"partner_id\": 456," +
                            "\"business_id\": 789" +
                        "}",
                        "testWithGettingExistingCredentialsNewUid"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:ru\"," +
                            "\"eats_and_lavka_id\": \"SomeShop\"," +
                            "\"uid\": 100502" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 123," +
                            "\"partner_id\": 456," +
                            "\"business_id\": 789" +
                        "}",
                        "testWithGettingExistingCredentialsUidWithContact"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:il\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100500" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 2," +
                            "\"business_id\": 1" +
                        "}",
                        "testWithGettingJustCreatedCredentialsOldUid"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:il\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100501" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 2," +
                            "\"business_id\": 1" +
                        "}",
                        "testWithGettingJustCreatedCredentialsNewUid"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:il\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100502" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 2," +
                            "\"business_id\": 1" +
                        "}",
                        "testWithGettingJustCreatedCredentialsUidWithContact"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:il\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100500" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 2," +
                            "\"business_id\": 1" +
                        "}",
                        "testWithGettingRecentlyCreatedCredentialsOldUid"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:il\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100501" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 2," +
                            "\"business_id\": 1" +
                        "}",
                        "testWithGettingRecentlyCreatedCredentialsNewUid"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:il\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100502" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 2," +
                            "\"business_id\": 1" +
                        "}",
                        "testWithGettingRecentlyCreatedCredentialsUidWithContact"
                ),
                Arguments.of(//language=json
                       "{" +
                            "\"service_id\": \"lavka:ru\"," +
                            "\"eats_and_lavka_id\": \"SomeShop\"," +
                            "\"uid\": 100500," +
                            "\"business_name\": \"Пятерочка\"," +
                            "\"shop_name\": \"Пятерочка №112234\"," +
                            "\"region\": 230," +
                            "\"tax_system\": 0," +
                            "\"vat\": 4" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 123," +
                            "\"partner_id\": 456," +
                            "\"business_id\": 789" +
                        "}",
                        "testWithRegionAndVatAndTax"
                ),
                Arguments.of(//language=json
                        "{" +
                            "\"service_id\": \"lavka:il\"," +
                            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                            "\"uid\": 100502," +
                            "\"business_name\": \"FixPrice\"," +
                            "\"shop_name\": \"FixPrice №98743\"," +
                            "\"region\": 235," +
                            "\"tax_system\": 0," +
                            "\"vat\": 4" +
                        "}",//language=json
                        "{" +
                            "\"market_feed_id\": 1," +
                            "\"partner_id\": 2," +
                            "\"business_id\": 1" +
                        "}",
                        "testWithRegionAndVatAndTaxNewUid"
                )

        );
    }



    @Test
    @DisplayName("Проверка сохраняемых данных - нет в бд, неполные из ручки")
    @DbUnitDataSet(after = "after/EatsAndLavkaControllerTest1.after.csv")
    void checkSavingData1() {
        String request = //language=json
        "{" +
            "\"service_id\": \"lavka:il\"," +
            "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
            "\"uid\": 100502" +
        "}";
        String response =//language=json
        "{" +
            "\"market_feed_id\": 1," +
            "\"partner_id\": 2," +
            "\"business_id\": 1" +
        "}";
        doPostEmptyVatRegion(request, response);
    }

    @Test
    @DisplayName("Проверка сохраняемых данных - нет в бд, из ручки получаем налог, страну и имя магазина")
    @DbUnitDataSet(after = "after/EatsAndLavkaControllerTest2.after.csv")
    void checkSavingData2() {
        String request = //language=json
                "{" +
                    "\"service_id\": \"lavka:il\"," +
                    "\"eats_and_lavka_id\": \"SomeOtherShop\"," +
                    "\"uid\": 100502," +
                    "\"business_name\": \"FixPrice\"," +
                    "\"shop_name\": \"FixPrice №98743\"," +
                    "\"region\": 235," +
                    "\"tax_system\": 0," +
                    "\"vat\": 4" +
                "}";
        String response =//language=json
                "{" +
                    "\"market_feed_id\": 1," +
                    "\"partner_id\": 2," +
                    "\"business_id\": 1" +
                "}";
        doPostEmptyVatRegion(request, response);
    }

    @Test
    @DisplayName("Проверка сохраняемых данных - неполные в бд, неполные из ручки")
    @DbUnitDataSet(after = "after/EatsAndLavkaControllerTest3.after.csv")
    void checkSavingData3() {
        String request = //language=json
                "{" +
                        "\"service_id\": \"lavka:kek\"," +
                        "\"eats_and_lavka_id\": \"SomeShop\"," +
                        "\"uid\": 100502" +
                        "}";
        String response = //language=json
                "{" +
                        "\"market_feed_id\": 124," +
                        "\"partner_id\": 654," +
                        "\"business_id\": 900" +
                        "}";
        doPostEmptyVatRegion(request, response);
    }

    @Test
    @DisplayName("Проверка сохраняемых данных - неполные в бд, из ручки получаем налог, страну и имя магазина")
    @DbUnitDataSet(after = "after/EatsAndLavkaControllerTest4.after.csv")
    void checkSavingData4() {
        String request = //language=json
                "{" +
                    "\"service_id\": \"lavka:kek\"," +
                    "\"eats_and_lavka_id\": \"SomeShop\"," +
                    "\"uid\": 100502," +
                    "\"business_name\": \"FixPrice\"," +
                    "\"shop_name\": \"FixPrice №98743\"," +
                    "\"region\": 235," +
                    "\"tax_system\": 0," +
                    "\"vat\": 4" +
                "}";
        String response =//language=json
                "{" +
                    "\"market_feed_id\": 124," +
                    "\"partner_id\": 654," +
                    "\"business_id\": 900" +
                "}";
        doPostEmptyVatRegion(request, response);
    }

    @Test
    @DisplayName("Проверка сохраняемых данных - в бд все данные, в ручке все данные")
    @DbUnitDataSet(after = "after/EatsAndLavkaControllerTest5.after.csv")
    void checkSavingData5() {
        String request = //language=json
                "{" +
                    "\"service_id\": \"lavka:zozh\"," +
                    "\"eats_and_lavka_id\": \"ZozhShop\"," +
                    "\"uid\": 100502," +
                    "\"business_name\": \"ЗОЖ-типание\"," +
                    "\"shop_name\": \"ЗОЖ-тип №213\"," +
                    "\"region\": 228," +
                    "\"tax_system\": 2," +
                    "\"vat\": 6" +
                "}";
        String response =//language=json
                "{" +
                    "\"market_feed_id\": 130," +
                    "\"partner_id\": 1053," +
                    "\"business_id\": 902" +
                "}";
        doPostEmptyVatRegion(request, response);
    }

    @Test
    @DisplayName("Проверка сохраняемых данных - в бд все данные, неполные из ручки")
    @DbUnitDataSet(after = "EatsAndLavkaControllerTest.csv")
    void checkSavingData6() {
        String request = //language=json
                "{" +
                    "\"service_id\": \"lavka:zozh\"," +
                    "\"eats_and_lavka_id\": \"ZozhShop\"," +
                    "\"uid\": 100502" +
                "}";
        String response =//language=json
                "{" +
                    "\"market_feed_id\": 130," +
                    "\"partner_id\": 1053," +
                    "\"business_id\": 902" +
                "}";
        doPostEmptyVatRegion(request, response);
    }

    private void doPostEmptyVatRegion(String request, String expectedResponse) {
        String response = FunctionalTestHelper.post(url, request).getBody();
        MbiAsserts.assertJsonEquals(response, expectedResponse);
        verify(marketIdGrpcService, never()).updateContactAccesses(any());
    }
}
