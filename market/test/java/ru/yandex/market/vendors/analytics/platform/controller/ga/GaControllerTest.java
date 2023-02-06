package ru.yandex.market.vendors.analytics.platform.controller.ga;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.service.ga.GaProfileService;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.utils.GaTestUtils.profile;
import static ru.yandex.market.vendors.analytics.core.utils.GaTestUtils.profiles;

/**
 * @author antipov93.
 */
public class GaControllerTest extends FunctionalTest {

    @MockBean
    private GaProfileService gaProfileService;

    @Test
    @DbUnitDataSet(before = "GaControllerTest.before.csv")
    @DisplayName("Загрузить профили пользователя")
    void loadProfiles() {
        long uid = 1;
        when(gaProfileService.loadProfiles(eq(uid)))
                .thenReturn(profiles(
                        profile("0", "0", "UA-0-0", "Not ecom", "http://yandex.ru", false),
                        profile("1", "1", "UA-1-1", "Основной", "http://yandex.ru", true),
                        profile("2", "1", "UA-1-1", "Запасной", "http://yandex.ru", true),
                        profile("3", "2", "UA-2-3", "Другой", "http://ozon.ru", true)
                ));
        var expected = loadFromFile("GaControllerTest.loadProfiles.response.json");
        var actual = getProfiles(uid);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("У пользователя нет ни одного профиля")
    void hasNoProfiles() {
        long uid = 1;
        when(gaProfileService.loadProfiles(eq(uid)))
                .thenReturn(profiles());
        JsonAssert.assertJsonEquals("[]", getProfiles(uid));
    }

    private String getProfiles(long uid) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("ga", "profiles")
                .queryParam("uid", uid)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }
}