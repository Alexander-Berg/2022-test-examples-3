package ru.yandex.market.mvc.controller.idm;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mvc.model.idm.dto.IdmStatusResponse;
import ru.yandex.market.mvc.model.idm.err.ErrorCode;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.util.MbiMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


public class IdmControllerTest extends FunctionalTest {

    @Autowired
    WireMockServer blackboxMockServer;
    @Autowired
    WireMockServer staffMockServer;

    RestTemplate restTemplate = new RestTemplate();


    @BeforeEach
    public void stubWiremock() {
        String staffResponse = getResourceAsString(
                "wiremock/staffResponse.json");
        staffMockServer.stubFor(WireMock.get(WireMock.urlMatching("/\\?login=test-login.*"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(staffResponse)));
        staffMockServer.stubFor(WireMock.get(WireMock.urlMatching("/\\?login=new-login.*"))
                .willReturn(WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(getResourceAsString("wiremock/staffResponseForNewLogin.json"))));
        String blackboxResponse = getResourceAsString(
                "wiremock/blackboxResponse.xml");
        blackboxMockServer.stubFor(WireMock.get(WireMock.urlMatching("/\\?method=userinfo.*login=yndx-test-login"))
                .willReturn(WireMock.aResponse().withBody(blackboxResponse)));
        blackboxMockServer.stubFor(WireMock.get(WireMock.urlMatching("/\\?method=userinfo.*login=yndx-new-login"))
                .willReturn(WireMock.aResponse().withBody(
                        getResourceAsString("wiremock/blackboxResponseForNewLogin.xml"))));
    }

    @Test
    @DbUnitDataSet(before = "csv/IdmControllerTest.before.csv")
    void testInfo() {
        String expectedResponse = getResourceAsString(
                "json/InfoResponse.json");
        String response = restTemplate.getForObject(getUrl("/idm/info/"), String.class);
        MatcherAssert.assertThat(response, MbiMatchers.jsonEquals(expectedResponse));
    }

    @DbUnitDataSet(before = "csv/IdmControllerTest.before.csv",
            after = "csv/IdmControllerTestAddRole.after.csv")
    @Test
    void testAddRole() {
        IdmStatusResponse response = restTemplate.postForObject(getUrl("/idm/add-role/"), createDefaultRequest(),
                IdmStatusResponse.class);
        Objects.requireNonNull(response);
        assertEquals(0, response.getCode(), response.toString());
        assertNull(response.getError(), response.toString());
        assertNull(response.getFatal(), response.toString());
        assertNull(response.getWarning(), response.toString());
    }

    @DbUnitDataSet(before = "csv/IdmControllerTestAddRole.after.csv",
            after = "csv/IdmControllerTestAddRole.after.csv")
    @Test
    void testDuplicateAddRole() {
        IdmStatusResponse response = restTemplate.postForObject(getUrl("/idm/add-role/"),
                createDefaultRequest(), IdmStatusResponse.class);
        Objects.requireNonNull(response);
        assertEquals(ErrorCode.USER_HAD_ROLE.ordinal(), response.getCode(), response.toString());
        assertNull(response.getError(), response.toString());
        assertNull(response.getFatal(), response.toString());
        assertNotNull(response.getWarning(), response.toString());
    }

    /**
     * Проверяет добавление роли новому пользователю.
     */
    @DbUnitDataSet(before = "csv/IdmControllerTestAddRole.after.csv",
            after = {"csv/IdmControllerTestAddRole.after.csv", "csv/IdmControllerTest.addRoleToNewUser.after.csv"})
    @Test
    void testAddRoleToNewUser() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("login", "new-login");
        params.add("uid", "111");
        params.add("role", "{\"role\": \"TEST_SUPPORT\", \"interface\": \"mbi\"}");
        params.add("fields", "{\"passport-login\": \"yndx-new-login\"}");
        HttpEntity<MultiValueMap<String, String>> request = createHttpFormRequest(params);

        IdmStatusResponse response = restTemplate.postForObject(getUrl("/idm/add-role/"),
                request, IdmStatusResponse.class);
        Objects.requireNonNull(response);
        assertEquals(ErrorCode.OK.ordinal(), response.getCode(), response.toString());
        assertNull(response.getError(), response.toString());
        assertNull(response.getFatal(), response.toString());
        assertNull(response.getWarning(), response.toString());
    }

    @DbUnitDataSet(before = "csv/IdmControllerTestRemoveRole.before.csv",
            after = "csv/IdmControllerTestRemoveRole.after.csv")
    @Test
    void testRemoveRole() {

        IdmStatusResponse response = restTemplate.postForObject(getUrl("/idm/remove-role/"),
                createDefaultRequest(), IdmStatusResponse.class);
        Objects.requireNonNull(response);
        assertEquals(0, response.getCode(), response.toString());
        assertNull(response.getError(), response.toString());
        assertNull(response.getFatal(), response.toString());
        assertNull(response.getWarning(), response.toString());
    }

    @DbUnitDataSet(before = "csv/IdmControllerTestRemoveRole.after.csv",
            after = "csv/IdmControllerTestRemoveRole.after.csv")
    @Test
    void testRemoveAbsentRole() {
        IdmStatusResponse response = restTemplate.postForObject(getUrl("/idm/remove-role/"),
                createDefaultRequest(), IdmStatusResponse.class);
        Objects.requireNonNull(response);
        assertEquals(0, response.getCode(), response.toString());
        assertNull(response.getError(), response.toString());
        assertNull(response.getFatal(), response.toString());
        assertNotNull(response.getWarning(), response.toString());
    }

    @DbUnitDataSet(before = "csv/IdmControllerTestGetAllRoles.before.csv")
    @Test
    void testGetAllRoles() {
        String expected = getResourceAsString("json/GetAllRolesResponse.json");
        String response = restTemplate.getForObject(getUrl("/idm/get-all-roles/"), String.class);
        MatcherAssert.assertThat(response, MbiMatchers.jsonEquals(expected));
    }

    @DbUnitDataSet(before = "csv/IdmControllerTestSyncUsers.before.csv",
            after = "csv/IdmControllerTestSyncUsers.after.csv")
    @Test
    void testSyncUsers() {
        IdmStatusResponse response = restTemplate.getForObject(getUrl("staff/sync-users"), IdmStatusResponse.class);
        assertNotNull(response);
        assertEquals(0, response.getCode(), response.toString());
        assertNull(response.getError(), response.toString());
        assertNull(response.getFatal(), response.toString());
        assertNull(response.getWarning(), response.toString());
    }

    @DbUnitDataSet(before = "csv/IdmControllerTestSyncUsers.before.csv",
            after = "csv/IdmControllerTestSyncUsers.after.csv")
    @Test
    void testSyncUsersWithStaffSingleError() {
        singleFaultStub(staffMockServer, "/\\?login=test-login.*",
                getResourceAsString("wiremock/staffResponse.json"));
        URI url = getUrl("staff/sync-users", Map.of("async", "false"));
        IdmStatusResponse response = restTemplate.getForObject(url, IdmStatusResponse.class);
        assertNotNull(response);
        assertEquals(0, response.getCode(), response.toString());
        assertNull(response.getError(), response.toString());
        assertNull(response.getFatal(), response.toString());
        assertNull(response.getWarning(), response.toString());
    }

    @DbUnitDataSet(before = "csv/IdmControllerTestGetUsers.before.csv")
    @Test
    void testGetUsers() {
        String expected = getResourceAsString("json/GetUsersResponse.json");
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl("staff/get-users",
                Map.of("service", "{\"interface\":\"mbi\"}")), String.class);
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue(), response.getBody());
        MatcherAssert.assertThat(response.getBody(), MbiMatchers.jsonEquals(expected));
    }

    private HttpEntity<MultiValueMap<String, String>> createHttpFormRequest(MultiValueMap<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(params, headers);
    }

    private HttpEntity<MultiValueMap<String, String>> createDefaultRequest() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("login", "test-login");
        params.add("uid", "123456");
        params.add("role", "{\"role\": \"TEST_SUPPORT\", \"interface\": \"mbi\"}");
        params.add("fields", "{\"passport-login\": \"yndx-test-login\"}");
        return createHttpFormRequest(params);
    }

    private String getResourceAsString(String path) {
        return StringTestUtil.getString(this.getClass(), path);
    }

    private void singleFaultStub(WireMockServer server, String urlPattern, String response) {
        staffMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPattern))
                .inScenario(server.baseUrl() + " single fault")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("Normal")
                .willReturn(WireMock.status(500).withBody("Something has gone wrong")));

        staffMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPattern))
                .inScenario(server.baseUrl() + " single fault")
                .whenScenarioStateIs("Normal")
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(response)));
    }
}

