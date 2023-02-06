package ru.yandex.market.partner.mvc.controller.region;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты для {@link RegionController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RegionControllerFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Получение родительских регионов: Пустой ответ по пустым входным данным")
    void testFindParentsEmpty() throws Exception {
        checkBadRequest(
                () -> FunctionalTestHelper.get(getFindParentsUrl(Collections.emptyList())),
                "json/parent_regions_missing_param.json"
        );
    }

    @Test
    @DisplayName("Получение родительских регионов: Полный список родителей")
    @DbUnitDataSet(before = "csv/RegionController.regionParents.csv")
    void testFindParents() {
        final List<Long> regionIds = ImmutableList.of(65L, 65L, 11316L, 66L);
        final ResponseEntity<String> response = FunctionalTestHelper.get(getFindParentsUrl(regionIds));

        JsonTestUtil.assertEquals(response, getClass(), "json/parent_regions.json");
    }

    @Test
    @DisplayName("Получение таймзоны: Несуществующий регион")
    void testInvalidRegion() throws Exception {
        checkBadRequest(
                () -> FunctionalTestHelper.get(getRegionTimezoneUrl(2222L)),
                "json/invalid_region.json"
        );
    }

    @Test
    @DisplayName("Получение таймзоны: Регион без таймзоны")
    @DbUnitDataSet(before = "csv/RegionController.regionWithoutTimezone.csv")
    void testRegionWithoutTimezone() throws Exception {
        checkBadRequest(
                () -> FunctionalTestHelper.get(getRegionTimezoneUrl(1L)),
                "json/region_without_timezone.json"
        );
    }

    @Test
    @DisplayName("Получение таймзоны: успех")
    @DbUnitDataSet(before = "csv/RegionController.regionTimezone.csv")
    void testRegionTimezone() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRegionTimezoneUrl(1L));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/region_timezone.json");
    }

    private void checkBadRequest(final Executable executable, final String jsonFile) throws Exception {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                executable
        );

        MatcherAssert.assertThat(
                httpClientErrorException,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyEquals(
                                        "errors",
                                        IOUtils.toString(getClass().getResource(jsonFile), StandardCharsets.UTF_8)
                                )
                        )
                )
        );
    }

    private String getRegionTimezoneUrl(final Long regionId) {
        return String.format("%s/region/%d/timezone", baseUrl, regionId);
    }

    private String getFindParentsUrl(final List<Long> regionIds) {
        return String.format("%s/region/parents?regionIds=%s", baseUrl, StringUtils.join(regionIds, ","));
    }

}
