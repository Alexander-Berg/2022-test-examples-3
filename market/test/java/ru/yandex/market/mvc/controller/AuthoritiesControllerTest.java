package ru.yandex.market.mvc.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.security.FunctionalTestHelper;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.security.core.HttpAuthorityNamesLoader;
import ru.yandex.market.security.core.HttpBatchAuthoritiesLoader;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.RequestParametersForLoadAuthoritiesDTO;
import ru.yandex.market.util.MbiMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "batch_operation_authorities_data.csv")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthoritiesControllerTest extends FunctionalTest {

    @Autowired
    private String baseUrl;

    private HttpBatchAuthoritiesLoader authoritiesLoader;
    private HttpAuthorityNamesLoader namesLoader;

    @BeforeAll
    void createLoader() {
        authoritiesLoader = new HttpBatchAuthoritiesLoader();
        authoritiesLoader.setHttpProxyUrl(baseUrl);
        namesLoader = new HttpAuthorityNamesLoader();
        namesLoader.setHttpProxyUrl(baseUrl);
    }

    @Test
    @DbUnitDataSet(before = "auth_links_conflicts.csv")
    void loadBatchAuthoritiesWithLinksTest() {
        String actualXmlResult = FunctionalTestHelper.post(
                        baseUrl + "/authorities/batch",
                        new RequestParametersForLoadAuthoritiesDTO("MBI-PARTNER", List.of("PARTNER_READER")))
                .getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_authorities_without_operations.xml")));
    }

    @Test
    @DbUnitDataSet(before = "auth_links_conflicts.csv")
    void loadBatchAuthoritiesWithConflictLinksTest() {
        String actualXmlResult = FunctionalTestHelper.post(
                        baseUrl + "/authorities/batch",
                        new RequestParametersForLoadAuthoritiesDTO("MBI-PARTNER",
                                List.of("TEST_ROOT_1", "TEST_ROOT_2")))
                .getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                        getClass(),
                        "result_authorities_without_operations_with_conflicts.xml"),
                // игнорим порядок в хэшмапе
                new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes)));
    }

    @Test
    @DbUnitDataSet(before = "auth_links_conflicts.csv")
    void loadBatchAuthoritiesWithConflictLinksViaClientTest() {
        var response = authoritiesLoader.
                loadAuthoritiesBatch("MBI-PARTNER", List.of("TEST_ROOT_1", "TEST_ROOT_2"));

        Authority nested1 = new Authority.Builder()
                .setDomain("MBI-PARTNER")
                .setName("TEST_NESTED")
                .setId(2L)
                .setParams("test1")
                .setChecker("staticDomainAuthorityChecker")
                .build();

        Authority nested2 = new Authority.Builder()
                .setDomain("MBI-PARTNER")
                .setName("TEST_NESTED")
                .setId(4L)
                .setParams("test2")
                .setChecker("staticDomainAuthorityChecker")
                .build();

        Authority testRoot1 = new Authority.Builder()
                .setDomain("MBI-PARTNER")
                .setName("TEST_ROOT_1")
                .setId(1L)
                .setParams("")
                .setChecker("staticDomainAuthorityChecker")
                .setRequiresLinks(Set.of(nested1))
                .build();

        Authority testRoot2 = new Authority.Builder()
                .setDomain("MBI-PARTNER")
                .setName("TEST_ROOT_2")
                .setId(3L)
                .setParams("")
                .setChecker("staticDomainAuthorityChecker")
                .setRequiresLinks(Set.of(nested2))
                .build();

        final Map<String, Authority> expected = new HashMap<>();
        expected.put(testRoot1.getName(), testRoot1);
        expected.put(testRoot2.getName(), testRoot2);
        expected.put(nested1.getName(), new Authority.Builder(nested1).setParams("").build());

        assertEquals(expected, response);
    }

    @Test
    void loadAuthoritiesViaClient() {
        var response = authoritiesLoader.
                loadAuthoritiesBatch("MBI-PARTNER", Collections.singletonList("PARTNER_READER"));
        assertEquals(getExpectedAuthorities(), response);
    }

    @Test
    void loadAuthoritiesViaClientWithConflict() {
        var response = authoritiesLoader.
                loadAuthoritiesBatch("MBI-PARTNER", List.of("PARTNER_READER", "QMANAGER"));
        assertEquals(getExpectedAuthorities(), response);
    }


    @Test
    void loadNamesViaClient() {
        var response = namesLoader.loadAuthorityNames("MBI-PARTNER");
        assertEquals(Set.of("SHOP_EVERYONE", "YA_SUPER", "PARTNER_READER", "YA_SUPER_SUPER", "QMANAGER"),
                new HashSet<>(response));
    }

    @Test
    void loadNamesXml() {
        String actualXmlResult = FunctionalTestHelper.get(getDomainUrl("MBI-PARTNER"))
                .getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "authorities_names.xml")));
    }


    private Map<String, Authority> getExpectedAuthorities() {
        Authority readerAuthority = new Authority("PARTNER_READER", "",
                "staticDomainAuthorityChecker");
        readerAuthority.setDomain("MBI-PARTNER");
        readerAuthority.setId(1);

        Authority qManagerAuthority = new Authority("QMANAGER", "",
                "staticDomainAuthorityChecker");
        qManagerAuthority.setDomain("MBI-PARTNER");
        qManagerAuthority.setId(2);

        readerAuthority.setSufficientLinks(Collections.singleton(qManagerAuthority));

        final Map<String, Authority> authoritiesMap = new HashMap<>();
        authoritiesMap.put(readerAuthority.getName(), readerAuthority);
        authoritiesMap.put(qManagerAuthority.getName(), qManagerAuthority);
        return authoritiesMap;
    }

    private String getDomainUrl(String domain) {
        return getUrl("/authorities/names", Map.of("domain", domain)).toString();
    }
}
