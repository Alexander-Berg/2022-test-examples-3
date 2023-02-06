package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.http.MatcherService;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.security.Role;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/EntryControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/EntryControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class EntryControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final WireMockServer blackboxMock;
    private final MatcherService matcherService;
    private final Clock clock;

    @Autowired
    EntryControllerFunctionalTest(WireMockServer blackboxMock, MatcherService matcherService, Clock clock) {
        this.blackboxMock = blackboxMock;
        this.matcherService = matcherService;
        this.clock = clock;
    }

    @Test
    void testGetEntriesByManager() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetEntriesByManager/blackbox_response.json"))));

        setVendorUserRoles(Collections.singleton(Role.manager_user), 100501);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/entries?uid=100501");
        String expected = getStringResource("/testGetEntriesByManager/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetEntriesByHardcodedManager() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetEntriesByHardcodedManager/blackbox_response.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/entries?uid=100503");
        String expected = getStringResource("/testGetEntriesByHardcodedManager/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetEntriesByNotManager() {
        setVendorUserRoles(Collections.singleton(Role.admin_user), 100501, 321L);

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/entries?uid=100501");
        String expected = getStringResource("/testGetEntriesByNotManager/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetEntriesByEntryCreatorUser() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetEntriesByEntryCreatorUser/blackbox_response.json"))));

        String response = FunctionalTestHelper.getWithAuth(baseUrl + "/entries?uid=200600");
        String expected = getStringResource("/testGetEntriesByEntryCreatorUser/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/EntryControllerFunctionalTest/testPostEntriesWithDocuments/after.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testPostEntriesWithDocuments() {
        doAnswer(invocation -> Matcher.MatchResponse.newBuilder().build())
                .when(matcherService)
                .multiMatchString(any());
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2019, 12, 19, 0, 0, 0)));

        String request = getStringResource("/testPostEntriesWithDocuments/request.json");
        String response = FunctionalTestHelper.postWithAuth(baseUrl + "/entries?uid=200600", request);
        String expected = getStringResource("/testPostEntriesWithDocuments/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Изменяет статус заявку и сохраняет в аудит")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/EntryControllerFunctionalTest/testChangeEntryStatus/before.csv",
            after = "/ru/yandex/market/vendor/controllers/EntryControllerFunctionalTest/testChangeEntryStatus/after.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testChangeEntryStatus() {
        setVendorUserRoles(Collections.singleton(Role.manager_user), 200600);

        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2019, 12, 19, 0, 0, 0)));

        String request = getStringResource("/testChangeEntryStatus/request.json");
        String response = FunctionalTestHelper.putWithAuth(baseUrl + "/entries/1/status?uid=200600", request);
        String expected = getStringResource("/testChangeEntryStatus/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

}
