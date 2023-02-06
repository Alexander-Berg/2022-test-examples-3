package ru.yandex.market.vendors.analytics.platform.controller.user;

import java.time.Clock;
import java.time.LocalDate;

import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.DateUtils;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * @author ogonek
 */
@DbUnitDataSet(before = "UserDemoAccessControllerTest.before.csv")
public class UserDemoAccessControllerTest extends FunctionalTest {

    private static final long UID = 123L;

    @MockBean(name = "clock")
    private Clock clock;

    @Test
    @DisplayName("Получение демо-доступов пользователя")
    void getDemoAccesses() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2020, 4, 16)));
        String expected = "["
                + "  {"
                + "    \"vendorId\": 4,"
                + "    \"requestedBy\": 123,"
                + "    \"grantedDate\": \"2020-04-16\","
                + "    \"expiredDate\": \"2020-04-24\","
                + "    \"active\": true,"
                + "    \"hid\": 91491,"
                + "    \"categoryName\": \"Мобильники\""
                + "  },"
                + "  {"
                + "    \"vendorId\": 2,"
                + "    \"requestedBy\": 123,"
                + "    \"grantedDate\": \"2020-04-16\","
                + "    \"expiredDate\": \"2020-04-25\","
                + "    \"active\": true,"
                + "    \"hid\": 91122,"
                + "    \"categoryName\": \"Игровые приставки\""
                + "  }"
                + "]";
        String response = getDemoAccesses(UID, null);

        assertEquals(expected, response);
    }

    @Test
    @DisplayName("Получение демо-доступов пользователя по категории")
    void getDemoAccessesByHid() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2020, 4, 16)));
        String expected = "["
                + "  {"
                + "    \"vendorId\": 1,"
                + "    \"requestedBy\": 123,"
                + "    \"grantedDate\": \"2020-04-15\","
                + "    \"expiredDate\": \"2020-04-27\","
                + "    \"active\": true,"
                + "    \"hid\": 91122,"
                + "    \"categoryName\": \"Игровые приставки\""
                + "  }"
                + "]";
        String response = getDemoAccesses(UID, 91122L);

        assertEquals(expected, response);
    }

    private String getDemoAccesses(long userId, @Nullable Long hid) {
        var builder = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/demo");
        if (hid != null) {
            builder.queryParam("hid", hid);
        }
        String url = builder.buildAndExpand(userId).toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

}
