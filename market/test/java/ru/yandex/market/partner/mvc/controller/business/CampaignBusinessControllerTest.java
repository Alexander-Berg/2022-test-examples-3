package ru.yandex.market.partner.mvc.controller.business;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * Тесты для контроллера {@link CampaignBusinessController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessControllerTest.before.csv")
public class CampaignBusinessControllerTest extends FunctionalTest {
    /**
     * Тестирует ручку /campaigns/businesses/id.
     */
    @ParameterizedTest
    @MethodSource("testBusinessArgs")
    void testBusiness(long campaignId, String responseJson) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "campaigns/businesses/id?campaignId=" + campaignId);
        assertEquals(response, responseJson);
    }

    private static Stream<Arguments> testBusinessArgs() {
        return Stream.of(
                Arguments.of(999, "{}"),                    // нет кампании с таким ид
                Arguments.of(210, "{businessId: 100}"),     // у кампании магазина есть бизнес
                Arguments.of(220, "{businessId: 101}")      // у кампании поставщика есть бизнес
        );
    }
}
