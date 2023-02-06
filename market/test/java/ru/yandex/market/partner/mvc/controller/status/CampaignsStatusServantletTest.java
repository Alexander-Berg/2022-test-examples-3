package ru.yandex.market.partner.mvc.controller.status;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.campaign.CampaignsStatusServantlet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link CampaignsStatusServantlet}.
 */
@DbUnitDataSet(before = "CampaignsStatusServantletTest.before.csv")
class CampaignsStatusServantletTest extends FunctionalTest {

    @Test
    @DisplayName("Получение списка кампаний с их статусами фидов для передаваемого пользователя")
    void testCampaignsStatusByLinks() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(createUrl("Data", 123456));
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        assertResultEquals(response.getBody(), "expected-feed-status-list-by-link.json");
    }

    @Test
    @DisplayName("Получение списка кампаний с их статусами баланса для передаваемого пользователя")
    void testCampaignsFinanceStatusByLinks() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(createUrl("Finance", 123456));
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        assertResultEquals(response.getBody(), "expected-finance-status-list-by-link.json");
    }

    private void assertResultEquals(String actualBody, String expectedJsonResourceName) {
        JsonElement actualResult = JsonTestUtil.parseJson(actualBody).getAsJsonObject().get("result");
        JsonElement expectedResult = JsonTestUtil.parseJson(this.getClass(), expectedJsonResourceName);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    private String createUrl(String handlerSuffix, long userId) {
        return baseUrl + "/getCampaigns" + handlerSuffix + "Status?_user_id=" + userId + "&format=json";
    }
}
