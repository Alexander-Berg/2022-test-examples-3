package ru.yandex.market.partner.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link ClientOverdraftServantlet}.
 *
 * @author zoom
 */
@DbUnitDataSet(before = "ClientOverdraftServantletTest.before.csv")
class ClientOverdraftServantletTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 10600L;

    @Test
    @DisplayName("Возвращаем овердрафт для клиента")
    @DbUnitDataSet(before = {
            "ClientOverdraftServantletTest.nettingStatus.before.csv",
            "ClientOverdraftServantletTest.shouldReturnExistingOverdraft.before.csv"
    })
    void shouldReturnExistingOverdraft() {
        ResponseEntity<String> response = getResponse(CAMPAIGN_ID);
        JsonTestUtil.assertEquals(response, getClass(), "shouldReturnExistingOverdraft.json");
    }

    @Test
    @DisplayName("Для субклиента ничего не возвращаем")
    @DbUnitDataSet(before = {
            "ClientOverdraftServantletTest.nettingStatus.before.csv",
            "ClientOverdraftServantletTest.skipSubliclients.before.csv",
            "ClientOverdraftServantletTest.shouldReturnExistingOverdraft.before.csv"
    })
    void skipSubclients() {
        ResponseEntity<String> response = getResponse(CAMPAIGN_ID);
        JsonTestUtil.assertEquals(response, getClass(), "empty.json");
    }

    @Test
    @DisplayName("Нет предоплатного клиента. Пустой ответ")
    void shouldReturnErrorWhenClientNotFound() {
        ResponseEntity<String> response = getResponse(CAMPAIGN_ID);
        JsonTestUtil.assertEquals(response, getClass(), "empty.json");
    }

    @Test
    @DisplayName("Нет овердрафта для клиента. Возвращем пустой список")
    @DbUnitDataSet(before = "ClientOverdraftServantletTest.nettingStatus.before.csv")
    void shouldReturnWhenClientOverdraftNotFound() {
        ResponseEntity<String> response = getResponse(CAMPAIGN_ID);
        JsonTestUtil.assertEquals(response, getClass(), "empty.json");
    }

    private ResponseEntity<String> getResponse(long id) {
        return FunctionalTestHelper.get(baseUrl + "clientOverdraft?_user_id=123&id=" + id + "&format=json");
    }
}
