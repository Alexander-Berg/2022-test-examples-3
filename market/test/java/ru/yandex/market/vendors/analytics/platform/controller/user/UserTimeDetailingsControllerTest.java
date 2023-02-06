package ru.yandex.market.vendors.analytics.platform.controller.user;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils;

import static ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing.MONTH;
import static ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing.QUARTER;
import static ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing.YEAR;

/**
 * @author antipov93
 */
@DbUnitDataSet(before = "UserTimeDetailingsControllerTest.before.csv")
public class UserTimeDetailingsControllerTest extends FunctionalTest {


    private static Stream<Arguments> availableTimeDetailingsArguments() {
        return Stream.of(
                Arguments.of(1L, 91491L, EnumSet.allOf(TimeDetailing.class)),
                Arguments.of(1L, 91492L, EnumSet.noneOf(TimeDetailing.class)),
                Arguments.of(1001L, 91492L, EnumSet.allOf(TimeDetailing.class)),
                Arguments.of(1001L, 91493L, EnumSet.of(MONTH, QUARTER, YEAR)),
                Arguments.of(1001L, 91494L, EnumSet.of(MONTH, QUARTER, YEAR))
        );
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("availableTimeDetailingsArguments")
    @DisplayName("Доступные пользователю детализации по времени")
    void availableTimeDetailings(long userId, long hid, Set<TimeDetailing> expectedTimeDetailings) {
        String actual = getAvailableTimeDetailings(userId, hid);
        String expected = expectedResponse(expectedTimeDetailings);
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    private String getAvailableTimeDetailings(long userId, long hid) {
        String categoriesUrl = timeDetailingsUrl(userId, hid);
        return FunctionalTestHelper.get(categoriesUrl).getBody();
    }

    private String timeDetailingsUrl(long userId, long hid) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/category/{hid}/timeDetailings")
                .buildAndExpand(userId, hid)
                .toUriString();
    }

    private static String expectedResponse(Collection<TimeDetailing> timeDetailings) {
        return StreamEx.of(timeDetailings).map(Objects::toString)
                .map(s -> "\"" + s + "\"")
                .joining(",", "[", "]");
    }

}
