package ru.yandex.market.partner.shops;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitRefreshMatViews;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DbUnitDataSet(before = "SearchShopsServantletTest.before.csv")
@DbUnitRefreshMatViews
class SearchShopsServantletTest extends FunctionalTest {
    private static final long NO_MANAGER = 0L;
    private static final long DATASOURCE_ID = 100000774L;
    private static final long SECOND_DATASOURCE_ID = 1001930100L;
    private static final long DATASOURCE_ID_WITHOUT_CONTACTS = 103920100L;
    private static final long DATASOURCE_ID_WITHOUT_COMMENTS = 39192910L;

    @Test
    void testSearchShops() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/searchShops?" +
                        "query=" + DATASOURCE_ID +
                        "&managerId=" + NO_MANAGER +
                        "&order=1" +
                        "&page=1" +
                        "&perpageNumber=10" +
                        "&format=json");
        JsonTestUtil.assertEquals("SearchShopsServantletDbTest.json", getClass(), response);
    }

    @Test
    void testSearchShopsNotFoundCampaignType() {
        var exception = assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(
                        baseUrl + "/searchShops?campaignType=BREWERY&query=test&format=json")
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(
                "[{\"name\":\"\",\"message\":\"invalid-param-value\",\"cause\":\"\",\"details\":\"Not found campaign " +
                        "types: [BREWERY]\",\"paramName\":\"campaignType\",\"statusCode\":null}]",
                exception.getResponseBodyAsString());
    }

    @Test
    void testQueryOverflowSearchShops() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/searchShops?" +
                        "query=" + "10016415695967008875431370122077" +
                        "&managerId=" + NO_MANAGER +
                        "&order=1" +
                        "&page=1" +
                        "&perpageNumber=10" +
                        "&format=json");
        JsonTestUtil.assertEquals("SearchShopsServantletQueryOverflowTestDb.json", getClass(), response);
    }

    @Test
    void testSearchWithoutEmailsShops() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/searchShops?" +
                        "query=" + SECOND_DATASOURCE_ID +
                        "&managerId=" + NO_MANAGER +
                        "&order=1" +
                        "&page=1" +
                        "&perpageNumber=10" +
                        "&format=json");
        JsonTestUtil.assertEquals("SearchShopsServantletWithoutEmailsTestDb.json", getClass(), response);
    }

    @Test
    void testDataSourceWithoutContactsShops() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/searchShops?" +
                        "query=" + DATASOURCE_ID_WITHOUT_CONTACTS +
                        "&managerId=" + NO_MANAGER +
                        "&order=1" +
                        "&page=1" +
                        "&perpageNumber=10" +
                        "&format=json");
        JsonTestUtil.assertEquals("SearchShopsServantletWithoutContactsTestDb.json", getClass(), response);
    }

    @Test
    void testShopWithoutCommentsDataSource() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/searchShops?" +
                        "query=" + DATASOURCE_ID_WITHOUT_COMMENTS +
                        "&managerId=" + NO_MANAGER +
                        "&order=1" +
                        "&page=1" +
                        "&perpageNumber=10" +
                        "&format=json");
        JsonTestUtil.assertEquals("SearchShopsServantletWithoutCommentsTestDb.json", getClass(), response);
    }
}
