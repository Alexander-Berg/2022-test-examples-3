package ru.yandex.market.partner.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.core.safemode.RequestPattern;
import ru.yandex.market.core.safemode.SafeModeService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.core.safemode.SafeModeService.SAFE_MODE_ALLOWED_REQUESTS;

/**
 * https://wiki.yandex-team.ru/users/sergey-fed/safe-mode-v-lk/
 */
@DbUnitDataSet(before = "safemode/SafeModeServiceTest.csv")
public class SafeModeTest extends FunctionalTest {

    private static final List<RequestPattern> BLACKLISTED_REQUESTS = List.of(
            new RequestPattern("GET", "/testBlacklisted1"),
            new RequestPattern("POST", "/testBlacklisted2"),
            new RequestPattern("PUT", "/testBlacklisted3")
    );

    private static final List<String> ALLOWED_PAGED = List.of(
            "page:allowed1",
            "page:allowed2",
            "page:allowed3"
    );

    private static final List<RequestPattern> ALLOWED_REQUESTS;

    static {
        ALLOWED_REQUESTS = new ArrayList<>(SAFE_MODE_ALLOWED_REQUESTS);
        ALLOWED_REQUESTS.add(new RequestPattern("GET", "/testAllowed1"));
        ALLOWED_REQUESTS.add(new RequestPattern("POST", "/testAllowed2"));
        ALLOWED_REQUESTS.add(new RequestPattern("PUT", "/testAllowed3"));
    }

    @Autowired
    private SafeModeService safeModeService;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private CachingSafeModeService cachingSafeModeService;

    @Autowired
    private EnvironmentService environmentService;

    @AfterEach
    @BeforeEach
    public void reload() {
        environmentService.setValue(SafeModeService.ENV_SAFE_MODE, "false");
        cachingSafeModeService.reloadCaches();
    }

    @Test
    public void testSafeModeReturns429() {
        enableSafeMode();

        assertThrows(
                HttpClientErrorException.TooManyRequests.class,
                () -> FunctionalTestHelper.get(baseUrl + "/test")
        );
    }

    @Test
    public void testSafeModeReturnsOk() {
        enableSafeMode();

        FunctionalTestHelper.get(baseUrl + "/ping");
    }

    @Test
    public void testSafeModeWhiteList() {
        enableSafeMode();
        assertThrows(
                HttpClientErrorException.TooManyRequests.class,
                () -> FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method")
        );

        environmentService.setValues(SafeModeService.ENV_ALLOWED_PATHS, List.of("GET,/url-capacity-test/test" +
                "-method"));
        enableSafeMode();

        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method");
    }

    @Test
    public void testBlackList() {
        environmentService.setValues(SafeModeService.ENV_BLACK_LIST_PATHS, List.of("GET,/ping"));
        enableSafeMode();

        assertThrows(
                HttpClientErrorException.TooManyRequests.class,
                () -> FunctionalTestHelper.get(baseUrl + "/ping")
        );
    }

    @Test
    public void testSafeModeWithIncorrectLine() {
        environmentService.setValues(SafeModeService.ENV_ALLOWED_PATHS, List.of("abc", "GET,/url-capacity-test" +
                "/test-method"));
        enableSafeMode();

        FunctionalTestHelper.get(baseUrl + "/url-capacity-test/test-method");
    }

    @Test
    public void testAllowedPagePositiveHit() {
        environmentService.setValues(SafeModeService.ENV_ALLOWED_PAGES, List.of("market-partner:html:url-capacity" +
                "-test:get"));
        enableSafeMode();

        HttpHeaders headers = new HttpHeaders();
        headers.add(CachingSafeModeService.PAGE_ID_HEADER, "market-partner:html:url-capacity-test:get");

        FunctionalTestHelper.exchange(
                baseUrl + "/url-capacity-test/test-method",
                HttpMethod.GET,
                headers,
                Void.class
        );
    }

    @Test
    public void testAllowedPageNegativeHit() {
        environmentService.setValues(SafeModeService.ENV_ALLOWED_PAGES, List.of("market-partner:html:url-capacity" +
                "-test:get"));
        enableSafeMode();

        HttpHeaders headers = new HttpHeaders();
        headers.add(CachingSafeModeService.PAGE_ID_HEADER, "market-partner:html:forbidden-page:get");

        assertThrows(
                HttpClientErrorException.TooManyRequests.class,
                () -> FunctionalTestHelper.exchange(
                        baseUrl + "/url-capacity-test/test-method",
                        HttpMethod.GET,
                        headers,
                        Void.class
                )
        );
    }

    @Test
    public void getSafeModeStateTest() {
        Assertions.assertThat(safeModeService.getSafeModeState()).isFalse();
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.changeSafeModeStateChanged.csv")
    public void changeSafeModeStateChangedTest() {
        transactional(actionId -> safeModeService.changeSafeModeState(actionId, true));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void changeSafeModeStateNotChangedTest() {
        transactional(actionId ->
                safeModeService.changeSafeModeState(actionId, false));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.addToAllowedPathsChanged.csv")
    public void addToAllowedPathsChangedTest() {
        transactional(actionId ->
                safeModeService.addToAllowedPaths(actionId, new RequestPattern("GET", "/newAllowedPath")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void addToAllowedPathsNotChangedTest() {
        transactional(actionId ->
                safeModeService.addToAllowedPaths(actionId, new RequestPattern("GET", "/testAllowed1")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void addToAllowedPathsAlreadyHardcodedTest() {
        transactional(actionId ->
                safeModeService.addToAllowedPaths(actionId, new RequestPattern("GET", "/ping")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.addBlackListedPathsChanged.csv")
    public void addBlackListedPathsChangedTest() {
        transactional(actionId ->
                safeModeService.addToBlackListedPaths(actionId, new RequestPattern("GET", "/newBlackListedPath")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void addBlackListedPathsNotChangedTest() {
        transactional(actionId ->
                safeModeService.addToBlackListedPaths(actionId, new RequestPattern("GET", "/testBlacklisted1")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.addAllowedPagesChanged.csv")
    public void addAllowedPagesChangedTest() {
        transactional(actionId ->
                safeModeService.addToAllowedPages(actionId, "page:newallowed"));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void addAllowedPagesNotChangedTest() {
        transactional(actionId ->
                safeModeService.addToAllowedPages(actionId, "page:allowed1"));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.removeFromAllowedPathsChanged.csv")
    public void removeFromAllowedPathsChangedTest() {
        transactional(actionId ->
                safeModeService.removeFromAllowedPaths(actionId, new RequestPattern("GET", "/testAllowed1")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void removeFromAllowedPathsNotChangedTest() {
        transactional(actionId ->
                safeModeService.removeFromAllowedPaths(actionId, new RequestPattern("GET", "/notExists")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.removeFromBlacklistedPathsChanged.csv")
    public void removeFromBlacklistedPathsChangedTest() {
        transactional(actionId ->
                safeModeService.removeFromBlacklistedPaths(actionId, new RequestPattern("GET", "/testBlacklisted1")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void removeFromBlacklistedPathsNotChangedTest() {
        transactional(actionId ->
                safeModeService.removeFromBlacklistedPaths(actionId, new RequestPattern("GET", "/notExists")));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.removeFromAllowedPagesChanged.csv")
    public void removeFromAllowedPagesChangedTest() {
        transactional(actionId ->
                safeModeService.removeFromAllowedPages(actionId, "page:allowed1"));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.notChanged.csv")
    public void removeFromAllowedPagesNotChangedTest() {
        transactional(actionId ->
                safeModeService.removeFromAllowedPages(actionId, "page:notexist"));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.cleanAllowedPaths.csv")
    public void cleanAllowedPathsTest() {
        transactional(actionId -> safeModeService.cleanAllowedPaths(actionId));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.cleanBlacklistedPaths.csv")
    public void cleanBlacklistedPathsTest() {
        transactional(actionId -> safeModeService.cleanBlacklistedPaths(actionId));
    }

    @Test
    @DbUnitDataSet(after = "safemode/SafeModeServiceTest.cleanAllowedPages.csv")
    public void cleanAllowedPagesTest() {
        transactional(actionId -> safeModeService.cleanAllowedPages(actionId));
    }

    @Test
    public void getAllowedPathsTest() {
        Assertions.assertThat(safeModeService.getAllowedPaths())
                .containsExactlyInAnyOrderElementsOf(ALLOWED_REQUESTS);
    }

    @Test
    public void getBlacklistedPathsTest() {
        Assertions.assertThat(safeModeService.getBlacklistedPaths())
                .containsExactlyInAnyOrderElementsOf(BLACKLISTED_REQUESTS);
    }

    @Test
    public void getAllowedPagesTest() {
        Assertions.assertThat(safeModeService.getAllowedPages()).containsExactlyInAnyOrderElementsOf(ALLOWED_PAGED);
    }

    private void transactional(Consumer<Long> action) {
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.SAFE_MODE, "Safemode test"),
                (transactionStatus, actionId) -> action.accept(actionId));
    }

    private void enableSafeMode() {
        environmentService.setValue(SafeModeService.ENV_SAFE_MODE, "true");
        cachingSafeModeService.reloadCaches();
    }

}
