package ru.yandex.market.partner.mvc.controller.survey.functional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorListMatchesInAnyOrder;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorMatches;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

@DbUnitDataSet(
        before = "SurveyControllerFunctionalTest.before.csv"
)
class SurveyControllerFunctionalTest extends FunctionalTest {

    private static final int DEFAULT_ADMIN_UID = 67282295;
    private static final int SHOP_ADMIN_UID = 6000001;
    private static final int SHOP_TECHNICAL_UID = 6000002;
    private static final int SHOP_OPERATOR_UID = 6000003;
    private static final int SHOP_ADMIN_TECHNICAL_UID = 6000004;
    private static final long CAMPAIGN_ID = 10774L;

    /**
     * Кейс проверяет базовый сценарий получения опросов для магазина:
     * <ul>
     * <li>опросы выбираются согласно заданной сегментации</li>
     * <li>опросы показываются в порядке, согласно {@code order_num}</li>
     * <li>к ссылке на опрос добавляется query параметр с хешированным id магазина</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testGetSurveysForShop.before.csv"
    )
    void testGetSurveysForShop() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, DEFAULT_ADMIN_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testGetSurveysForShop.json");
    }

    /**
     * Тест проверяет выдачу для магазина, у которого нет опросов.
     */
    @Test
    void testNoSurveysForShop() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, DEFAULT_ADMIN_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testNoSurveysForShop.json");
    }

    /**
     * Тест проверят, что пользователь с ролью {@link InnerRole#SHOP_ADMIN} получает только опросы, предназначенные ему.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testSurveysForSpecificRole.before.csv"
    )
    void testSurveysForAdminRole() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, SHOP_ADMIN_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testSurveysForAdminRole.json");
    }

    /**
     * Тест проверят, что пользователь с ролью {@link InnerRole#SHOP_TECHNICAL} получает только опросы,
     * предназначенные ему.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testSurveysForSpecificRole.before.csv"
    )
    void testSurveysForTechnicalRole() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, SHOP_TECHNICAL_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testSurveysForTechnicalRole.json");
    }

    /**
     * Тест проверят, что пользователь с ролью {@link InnerRole#SHOP_OPERATOR} получает только опросы,
     * предназначенные ему.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testSurveysForSpecificRole.before.csv"
    )
    void testSurveysForOperatorRole() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, SHOP_OPERATOR_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testSurveysForOperatorRole.json");
    }

    /**
     * Тест проверят, что пользователь с ролями {@link InnerRole#SHOP_ADMIN} и {@link InnerRole#SHOP_TECHNICAL}
     * получает только опросы, предназначенные ему.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testSurveysForSpecificRole.before.csv"
    )
    void testSurveysForMultiRole() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, SHOP_ADMIN_TECHNICAL_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testSurveysForMultiRole.json");
    }

    /**
     * Тест проверяет, что в случае переопределения URL он выводится вместо URL опроса по умолчанию и к нему не
     * добавляется хеш id магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testOverriddenUrl.before.csv"
    )
    void testOverriddenUrl() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, DEFAULT_ADMIN_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testOverriddenUrl.json");
    }

    /**
     * Тест проверят, что в случае выставления {@code append_hash=false} в настройках опроса ссылке на опрос
     * не добавляется хеш id магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.appendHashFalse.before.csv"
    )
    void testAppendHashFalse() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, DEFAULT_ADMIN_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testAppendHashFalse.json");
    }

    /**
     * Тест проверяет, что пройденные опросы не возвращаются в ответе ручки.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testPassedSurveyIsNotReturned.before.csv"
    )
    void testPassedSurveyIsNotReturned() {
        ResponseEntity<String> response = getSurveysResponse(CAMPAIGN_ID, DEFAULT_ADMIN_UID);
        JsonTestUtil.assertEquals(response, getClass(), "expected/testPassedSurveyIsNotReturned.json");
    }

    /**
     * Тест проверяет, что ручка упешно помечает опрос как пройденный.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testMarkSurveyAsPassed.before.csv",
            after = "SurveyControllerFunctionalTest.testMarkSurveyAsPassed.after.csv"
    )
    void testMarkSurveyAsPassed() {
        ResponseEntity<String> response = markSurveyAsPassed(CAMPAIGN_ID, "test-survey1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Тест проверяет, что в случае, если опрос пытаются пройти 2 раза для одного магазина -
     * выпадает ошибка ожидаемого для фронта формата.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testDuplicateMarkSurveyAsPassed.csv",
            after = "SurveyControllerFunctionalTest.testDuplicateMarkSurveyAsPassed.csv"
    )
    void testDuplicateMarkSurveyAsPassed() {
        try {
            markSurveyAsPassed(CAMPAIGN_ID, "test-survey1");
            fail("Expected http code is not returned");
        } catch (HttpClientErrorException e) {
            assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
            assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "surveyId", "ALREADY_EXISTS")));
        }
    }

    /**
     * Тест проверяет, что для опроса с неизвестным {@code surveyId} ручка возвращает 200.
     * Это защита от ошибок на случай, если мы удалили опрос, но у пользователя на странице он еще доступен.
     */
    @Test
    @DbUnitDataSet(
            before = "SurveyControllerFunctionalTest.testMarkSurveyAsPassed.before.csv",
            after = "SurveyControllerFunctionalTest.testMarkSurveyAsPassed.before.csv"
    )
    void testMarkUnknownSurveyAsPassed() {
        ResponseEntity<String> response = markSurveyAsPassed(CAMPAIGN_ID, "unknown-survey1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private ResponseEntity<String> markSurveyAsPassed(long campaignId, String surveyId) {
        String url = baseUrl + "/surveys/" + surveyId + "/passed?id=" + campaignId + "&_user_id=" + DEFAULT_ADMIN_UID;
        return FunctionalTestHelper.post(url);
    }

    private ResponseEntity<String> getSurveysResponse(long campaignId, long uid) {
        String url = baseUrl + "/surveys?id=" + campaignId + "&_user_id=" + uid;
        return FunctionalTestHelper.get(url);
    }
}
