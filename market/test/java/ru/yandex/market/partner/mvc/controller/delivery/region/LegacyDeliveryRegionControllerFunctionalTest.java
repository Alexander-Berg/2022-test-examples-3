package ru.yandex.market.partner.mvc.controller.delivery.region;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link LegacyDeliveryRegionController}.
 */
class LegacyDeliveryRegionControllerFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Тест для ручки /getRegionChildren. Получение следующего уровня дочерних регионов для текущего " +
            "региона")
    @DbUnitDataSet(before = "csv/LegacyDeliveryRegionController_Groups.before.csv")
    void testGetRegionChildren() {
        List<Long> ids = List.of(2L, 3L);
        ResponseEntity<String> response = FunctionalTestHelper.get(urlGetRegionChildren(ids));
        JsonTestUtil.assertEquals(response, this.getClass(), "LegacyDeliveryRegionController_getRegionChildren.json");
    }

    @Test
    @DisplayName("Проверка формата ошибок ручки /getRegionChildren")
    void testGetRegionChildrenErrors() {
        // invalid parameters
        ResponseEntity<String> responseEmptyRgId = FunctionalTestHelper.get(urlGetRegionChildren(
                "10,20,,30"));
        JsonTestUtil.assertResponseErrorMessage(
                StringTestUtil.getString(this.getClass(),
                        "LegacyDeliveryRegionController_getRegionChildren_wrongParam.json"),
                responseEmptyRgId.getBody());

        // empty region id
        ResponseEntity<String> responseGroupNF = FunctionalTestHelper.get(urlGetRegionChildren(List.of(-1L)));
        JsonTestUtil.assertResponseErrorMessage(
                StringTestUtil.getString(this.getClass(), "LegacyDeliveryRegionController_deleteGroup_emptyRgId.json"),
                responseGroupNF.getBody());
    }

    private String urlGetRegionChildren(List<Long> ids) {
        String regionIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return urlGetRegionChildren(regionIds);
    }

    private String urlGetRegionChildren(String ids) {
        return String.format("%s/getRegionChildren?regionId=%s&campaign_id=0&format=json", baseUrl, ids);
    }

}
