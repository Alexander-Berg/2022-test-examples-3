package ru.yandex.market.partner.mvc.controller.post;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты на {@link RusPostOfficesController}.
 */
class RusPostOfficesControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получение списка отделений для Москвы и области")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostOfficesController.getOfficesByRegion.before.csv")
    void getPostOfficesWithHomeAndRegion() {
        var response = FunctionalTestHelper.get(postOfficesByRegionUrl(213));
        JsonTestUtil.assertEquals(response,
                this.getClass(), "/mvc/post/RusPostControllerTest.getPostOfficesByRegion.homeAndRegion.response.json");
    }

    @Test
    @DisplayName("Получение списка отделений для региона, где ОПС есть только в области")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostOfficesController.getOfficesByRegion.before.csv")
    void getPostOfficesWithRegionOfficesOnly() {
        var response = FunctionalTestHelper.get(postOfficesByRegionUrl(65));
        JsonTestUtil.assertEquals(response,
                this.getClass(), "/mvc/post/RusPostControllerTest.getPostOfficesByRegion.onlyRegionOffices.response.json");
    }

    @Test
    @DisplayName("Проверка получения пустого списка отделений, если их нет в городе и области")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostOfficesController.getOfficesByRegion.before.csv")
    void getPostOfficesByRegionNoOffices() {
        var response = FunctionalTestHelper.get(postOfficesByRegionUrl(66));
        JsonTestUtil.assertEquals(response, "{}");
    }

    @Test
    @DisplayName("Проверка получения 404, когда регион не найден")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostOfficesController.getOfficesByRegion.before.csv")
    void getPostOfficesRegionNotFound() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(postOfficesByRegionUrl(7777))
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DisplayName("Проверка получения пустого ответа, когда у региона невозможно найти область, к которой он относится")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostOfficesController.getOfficesByRegion.before.csv")
    void getPostOfficesRegionParentNotFound() {
        var response = FunctionalTestHelper.get(postOfficesByRegionUrl(1));
        JsonTestUtil.assertEquals(response, "{}");
    }

    private String postOfficesByRegionUrl(long regionId) {
        return baseUrl + "/ruspost/post-offices/" + regionId;
    }
}
