package ru.yandex.market.partner.mvc.controller.business.metrika;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
public class BusinessMetricaControllerGrantTest extends FunctionalTest {
    @Test
    @DbUnitDataSet(before = "csv/BusinessMetrikaControllerTest.addGrantsTest.before.csv",
            after = "csv/BusinessMetrikaControllerTest.addGrantsTest.after.csv")
    void addGrantsTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/metrika/grants/add")
                .queryParam("login", "new_login")
                .queryParam("counter_ids", 1, 2, 3, 4, 5)
                .build()
                .toString();
        FunctionalTestHelper.post(url);
    }
}
