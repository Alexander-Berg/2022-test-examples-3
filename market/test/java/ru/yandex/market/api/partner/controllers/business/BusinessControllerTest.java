package ru.yandex.market.api.partner.controllers.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.util.MbiAsserts;

/**
 * Функциональные тесты для {@link BusinessController}.
 */
public class BusinessControllerTest extends FunctionalTest {

    @DisplayName("Получение списка бизнесов.")
    @Test
    @DbUnitDataSet(before = "BusinessControllerTest.getBusinessesByUid.before.csv")
    void getBusinessesByUid() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(urlBasePrefix + "/businesses",
                HttpMethod.GET, Format.JSON);
        String expected = StringTestUtil.getString(getClass(), "expectedBusinesses.json");
        MbiAsserts.assertJsonEquals(expected, response.getBody());
        response = FunctionalTestHelper.makeRequest(urlBasePrefix + "/businesses",
                HttpMethod.GET, Format.XML);
        expected = StringTestUtil.getString(getClass(), "expectedBusinesses.xml");
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }
}
