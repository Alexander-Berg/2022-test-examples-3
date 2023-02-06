package ru.yandex.market.partner.mvc.controller.partner.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link AvailableMarketplaceProgramController}
 */
@DbUnitDataSet(before = "AvailableMarketplaceProgramControllerTest.before.csv")
public class AvailableMarketplaceProgramControllerTest extends FunctionalTest {

    private static final String methodUrl = "/partner/marketplace/programs/available?campaignId={id}";

    @Test
    public void testGetBothPrograms() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + methodUrl,
                555L
        );
        JsonTestUtil.assertEquals(response, getClass(), "testGetBothPrograms.json");
    }

    @Test
    public void testGetDropshipProgram() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + methodUrl,
                556L
        );
        JsonTestUtil.assertEquals(response, getClass(), "testGetDropshipProgram.json");
    }

    @Test
    public void testGetNoProgram() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + methodUrl,
                557L
        );
        JsonTestUtil.assertEquals(response, getClass(), "testGetNoProgram.json");
    }
}
