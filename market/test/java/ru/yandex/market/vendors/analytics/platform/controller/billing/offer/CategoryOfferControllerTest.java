package ru.yandex.market.vendors.analytics.platform.controller.billing.offer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "CategoryOfferControllerTest.before.csv")
class CategoryOfferControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Дерево категорий")
    void tree() {
        var response = treeRequest();
        var expected = loadFromFile("CategoryOfferControllerTest.tree.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Поиск категорий по русскому названию")
    void russianSearch() {
        var response = searchRequest("оБилЬны");
        var expected = loadFromFile("CategoryOfferControllerTest.russianSearch.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Поиск категорий по английскому названию")
    void englishSearch() {
        var response = searchRequest("troLLeR");
        var expected = loadFromFile("CategoryOfferControllerTest.englishSearch.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Поиск категорий: несколько результатов")
    void searchManyResults() {
        var response = searchRequest("оЛ");
        var expected = loadFromFile("CategoryOfferControllerTest.searchManyResults.response.json");
        assertJsonEquals(expected, response);
    }

    private String treeRequest() {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("offer", "category", "tree")
                .build().toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }


    private String searchRequest(String pattern) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("offer", "category", "search")
                .queryParam("pattern", pattern)
                .build().toUriString();

        return FunctionalTestHelper.get(url).getBody();
    }
}