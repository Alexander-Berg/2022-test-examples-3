package ru.yandex.market.partner.mvc.controller.feed.supplier;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.mvc.controller.feed.SupplierFeedController;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты на {@link SupplierFeedController}.
 */
@DbUnitDataSet(before = "SupplierFeedControllerFunctionalTest.csv")
//TODO Поправить тесты в рамках https://st.yandex-team.ru/MBI-56451, сломавшиеся вследствие удаления pull валидации
class SupplierFeedControllerFunctionalTest extends AbstractSupplierFeedControllerFunctionalTest {
    private static final long CAMPAIGN_ID = 10774L;
    @Autowired
    @Qualifier("baseUrl")
    protected String baseUrl;
    @Autowired
    FeedFileStorage feedFileStorage;
    @Autowired
    HttpClient indexerApiClientHttpClient;

    @Autowired
    private TestEnvironmentService environmentService;

    //TODO: при выкашивании флажка чтения из логброкера тесты упадут.
    // Их надо оставить, но достаточно будет проверить, что валидное задание отправили в ЛБ

    static Stream<Arguments> args() {
        return Stream.of(
                //dropship and flag=true
                Arguments.of(311, 10311, true, true),
                //clickandcollect and flag=true
                Arguments.of(312, 10312, true, false),
                //clickandcollect and flag=false
                Arguments.of(313, 10313, false, false),
                //dropship and flag=false
                Arguments.of(314, 10314, false, false),
                //not dropship and not clickandcollect and flag=true
                Arguments.of(315, 10315, true, false)
        );
    }

    @BeforeEach
    void resetMocks() throws IOException {
        Mockito.reset(indexerApiClientHttpClient, feedFileStorage);
        Mockito.when(feedFileStorage.upload(Mockito.any(), Mockito.anyLong()))
                .thenReturn(new StoreInfo(530944, "http://mds.url/"));
        environmentService.setEnvironmentType(EnvironmentType.TESTING);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("environment");
    }

    @Test
    @DbUnitDataSet(before = "SupplierFeedControllerFunctionalTest.testGetFeedMultiFF.before.csv")
    void testGetFeedMultiFF() {
        ResponseEntity<String> response = FunctionalTestHelper.get(latestFeedUrl(CAMPAIGN_ID));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result",
                "{\n" +
                        "  \"feed\": {\n" +
                        "    \"resource\": {\n" +
                        "      \"url\": \"http://myshop.ru/myfeed.yml\",\n" +
                        "      \"credentials\": {\n" +
                        "        \"login\": \"mylogin\",\n" +
                        "        \"password\": \"mypassword\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"id\": 101,\n" +
                        "    \"partnerId\": 774,\n" +
                        "    \"indexerFeedIds\": [\n" +
                        "      102,\n" +
                        "      103\n" +
                        "    ],\n" +
                        "    \"isDefault\": false,\n" +
                        "    \"feedType\":\"ASSORTMENT_WITH_PRICES\"\n" +
                        "  }\n" +
                        "}")));
    }

    @Test
    @DbUnitDataSet(before = "SupplierFeedControllerFunctionalTest.testGetFeedMultiFF.before.csv")
    void testGetDefaultFeed() {
        ResponseEntity<String> response = FunctionalTestHelper.get(latestFeedUrl(10666));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(
                MbiMatchers.jsonPropertyEquals("result",
                        "{\"feed\":{\"id\":1066,\"partnerId\":666,\"isDefault\":true,\"indexerFeedIds\":[104]," +
                                "\"feedType\":\"ASSORTMENT_WITH_PRICES\"}}")));
    }

    @Test
    @DbUnitDataSet(before = "SupplierFeedControllerFunctionalTest.feedInfo.csv")
    void getFeedInfoComplete() {
        ResponseEntity<String> response = FunctionalTestHelper.get(latestFeedUrl(555L));
        JsonTestUtil.assertEquals(response, "{\n" +
                "  \"feed\": {\n" +
                "    \"resource\": {\n" +
                "      \"url\": \"http://url.local\",\n" +
                "      \"credentials\": {\n" +
                "        \"login\": \"login\",\n" +
                "        \"password\": \"pass\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": 100,\n" +
                "    \"partnerId\": 23456,\n" +
                "    \"indexerFeedIds\": [],\n" +
                "    \"pushFeedInfo\": {\n" +
                "      \"complete\": false,\n" +
                "      \"updateTime\": 1525176000000\n" +
                "    },\n" +
                "    \"isDefault\": false," +
                "    \"feedType\":\"ASSORTMENT_WITH_PRICES\"" +
                "  }\n" +
                "}");
    }

    @Test
    @DbUnitDataSet(before = "SupplierFeedControllerFunctionalTest.feedInfo.csv")
    void getFeedInfoCompleteOld() {
        ResponseEntity<String> response = FunctionalTestHelper.get(feedUrl(555L));
        JsonTestUtil.assertEquals(response, "{\n" +
                "  \"feed\": {\n" +
                "    \"resource\": {\n" +
                "      \"url\": \"http://url.local\",\n" +
                "      \"credentials\": {\n" +
                "        \"login\": \"login\",\n" +
                "        \"password\": \"pass\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"id\": 100,\n" +
                "    \"partnerId\": 23456,\n" +
                "    \"indexerFeedIds\": [],\n" +
                "    \"pushFeedInfo\": {\n" +
                "      \"complete\": false,\n" +
                "      \"updateTime\": 1525176000000\n" +
                "    },\n" +
                "   \"isDefault\": false,\n" +
                "   \"feedType\":\"ASSORTMENT_WITH_PRICES\"\n" +
                "  }," +
                "  \"assortment\":{\"isDefault\":false}\n" +
                "}");
    }
}
