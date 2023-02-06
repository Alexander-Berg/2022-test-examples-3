package ru.yandex.market.partner.mvc.controller.agency.checker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.mvc.controller.agency.AgencyCheckerSummaryController;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для {@link AgencyCheckerSummaryController}.
 **/
@DbUnitDataSet(before = "csv/AgencyCheckerSummaryControllerTest.before.csv")
public class AgencyCheckerSummaryControllerTest extends FunctionalTest {

    private static final long UID = 1003L;
    @Autowired
    EnvironmentService environmentService;
    @Value("${mbi.agency.checker.report.from.key}")
    private String reportFromKey;
    @Value("${mbi.agency.checker.report.to.key}")
    private String reportToKey;

    @BeforeEach
    void init() {
        environmentService.setValue(reportFromKey, "2019-07-01");
        environmentService.setValue(reportToKey, "2020-03-01");
    }

    @Test
    @DisplayName("Получение данных чекера")
    @DbUnitDataSet(before = "csv/getAgencyCheckerSummaryTest.before.csv")
    void getAgencyCheckerSummaryTest() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getCheckerUrl(UID));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/getAgencyCheckerSummaryTest.json");
    }

    @Test
    @DisplayName("Все поля кроме agencyId nullable")
    @DbUnitDataSet(before = "csv/getAgencyCheckerSummaryNullableTest.before.csv")
    void getAgencyCheckerSummaryNullableTest() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getCheckerUrl(UID));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/getAgencyCheckerSummaryNullableTest.json");
    }

    @Test
    @DisplayName("Если по агенству нет данных должно быть 404")
    void getAgencyCheckerSummaryNoDataTest() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getCheckerUrl(UID)));
        assertThat(exception.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    private String getCheckerUrl(final long uid) {
        return getUrl("agency/checker", uid);
    }

    private String getUrl(final String url, final long uid) {
        return baseUrl + url + "?_user_id=" + uid;
    }
}


