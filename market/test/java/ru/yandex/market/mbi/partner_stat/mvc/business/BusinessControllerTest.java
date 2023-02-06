package ru.yandex.market.mbi.partner_stat.mvc.business;


import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;

/**
 * Тесты для {@link BusinessController}
 */
@DbUnitDataSet(before = "BusinessController/before.csv")
class BusinessControllerTest extends FunctionalTest {

    @DisplayName("Проверка получения сводки по бизнесу")
    @ParameterizedTest
    @MethodSource("testGetStubSummaryArgs")
    void testGetStubSummary(long businessId, String expectedFilepath) {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getBusinessSummaryUrl(businessId));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "BusinessController/" + expectedFilepath);
    }

    private static Stream<Arguments> testGetStubSummaryArgs() {
        return Stream.of(
                // статистика отсутствует
                Arguments.of(0, "summary.0.json"),
                // только реклама
                Arguments.of(1, "summary.1.json"),
                // продажи + прогноз
                Arguments.of(2, "summary.2.json"),
                // полная статистика
                Arguments.of(3, "summary.3.json"),
                //реклама с прогнозом
                Arguments.of(4, "summary.4.json")
        );
    }

    private String getBusinessSummaryUrl(long businessId) {
        return baseUrl() + "/businesses/" + businessId + "/statistics/common";
    }

}
