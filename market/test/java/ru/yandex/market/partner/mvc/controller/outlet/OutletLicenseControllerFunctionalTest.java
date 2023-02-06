package ru.yandex.market.partner.mvc.controller.outlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link OutletLicenseController}.
 */
class OutletLicenseControllerFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Нет данных о партнере или лицензиях")
    void testEmptyData() {
        final ResponseEntity<String> response = sendRequest(Collections.emptySet());
        JsonTestUtil.assertEquals(response, "[]");
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseControllerFunctionalTest.before.csv")
    @DisplayName("Проверить что вернулись все лицензии во всех статусах для указанного партнера")
    void testAllData() {
        final ResponseEntity<String> response = sendRequest(Collections.emptySet());
        JsonTestUtil.assertEquals(response, getClass(), "testAllData.json");
    }

    @Test
    @DbUnitDataSet(before = "OutletLicenseControllerFunctionalTest.before.csv")
    @DisplayName("Проверить что вернутся только плохие лицензии для указанного партнера")
    void testFilteredData() {
        final Collection<ParamCheckStatus> badStatuses = Arrays.asList(
                ParamCheckStatus.FAIL,
                ParamCheckStatus.FAIL_MANUAL,
                ParamCheckStatus.REVOKE
        );
        final ResponseEntity<String> response = sendRequest(badStatuses);
        JsonTestUtil.assertEquals(response, getClass(), "testFilteredData.json");
    }

    private ResponseEntity<String> sendRequest(final Collection<ParamCheckStatus> statuses) {
        final String url = getRequestUrl(statuses);
        return FunctionalTestHelper.get(url);
    }

    private String getRequestUrl(Collection<ParamCheckStatus> statuses) {
        final String url = baseUrl + "/outlets/licenses?datasourceId=103&_user_id=123&statuses=";
        if (CollectionUtils.isEmpty(statuses)) {
            return url;
        }
        return url + statuses.stream()
                .map(ParamCheckStatus::name)
                .collect(Collectors.joining(","));
    }

}
