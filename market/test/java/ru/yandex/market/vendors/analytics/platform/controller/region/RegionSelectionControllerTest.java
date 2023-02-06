package ru.yandex.market.vendors.analytics.platform.controller.region;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.common.language.Language;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Functional tests for {@link RegionSelectionController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "RegionSelectionControllerTest.csv")
class RegionSelectionControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получение дерева субъектов РФ")
    void getRussiaRegions() {
        var expected = loadFromFile("regions.json");
        var actual = getRegions(Language.RU);
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Получение дерева субъектов РФ на английском")
    void getRussiaRegionsEn() {
        var expected = loadFromFile("regions.en.json");
        var actual = getRegions(Language.EN);
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String regionsUrl(Language language) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/regions")
                .queryParam("language", language)
                .toUriString();
    }

    private String getRegions(Language language) {
        var url = regionsUrl(language);
        return FunctionalTestHelper.get(url).getBody();
    }
}
