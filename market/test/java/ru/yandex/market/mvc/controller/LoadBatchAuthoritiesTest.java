package ru.yandex.market.mvc.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.security.FunctionalTestHelper;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.security.core.HttpBatchAuthoritiesLoader;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.OperationAuthorities;
import ru.yandex.market.security.model.OperationPermission;
import ru.yandex.market.security.model.RequestParametersForLoadDTO;
import ru.yandex.market.util.MbiMatchers;

/**
 * Тесты для ручки {@link LoadBatchAuthoritiesController}
 * и loader'a {@link HttpBatchAuthoritiesLoader} для ручки /loadBatchAuthorities.
 *
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 */
@DbUnitDataSet(before = "batch_operation_authorities_data.csv")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadBatchAuthoritiesTest extends FunctionalTest {

    @Autowired
    private String baseUrl;

    private HttpBatchAuthoritiesLoader batchLoader;

    @BeforeAll
    void createLoader() {
        batchLoader = new HttpBatchAuthoritiesLoader();
        batchLoader.setHttpProxyUrl(baseUrl);
    }

    /**
     * Проверить, что ручка /loadBatchAuthorities возвращает корректные данные.
     */
    @Test
    void loadBatchAuthoritiesTest() {
        final String actualXmlResult = FunctionalTestHelper.
                post(baseUrl + "/loadBatchAuthorities", new RequestParametersForLoadDTO("MBI-PARTNER",
                        Arrays.asList("index", "tool"))).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_from_batch_authorities.xml")));
    }

    /**
     * Проверить, что ручка /loadBatchAuthorities возвращает корректные данные для операции без requires links.
     */
    @Test
    void loadBatchAuthoritiesWithoutRequiresLinksTest() {
        final String actualXmlResult = FunctionalTestHelper.
                post(baseUrl + "/loadBatchAuthorities", new RequestParametersForLoadDTO("MBI-PARTNER",
                        Arrays.asList("index"))).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_from_batch_authorities_without_requires_links.xml")));
    }

    /**
     * Проверить, что ручка /loadBatchAuthorities возвращает корректные данные для операции без sufficient links.
     */
    @Test
    void loadBatchAuthoritiesWithoutSufficientLinksTest() {
        final String actualXmlResult = FunctionalTestHelper.
                post(baseUrl + "/loadBatchAuthorities", new RequestParametersForLoadDTO("MBI-PARTNER",
                        Arrays.asList("tool"))).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_from_batch_authorities_without_sufficient_links.xml")));
    }

    /**
     * Проверить, что ручка /loadBatchAuthorities возвращает данные,
     * когда в запросе присутствует несуществующая операция.
     */
    @Test
    void loadBatchAuthoritiesWithNonexistentOperationTest() {
        final String actualXmlResult = FunctionalTestHelper.
                post(baseUrl + "/loadBatchAuthorities", new RequestParametersForLoadDTO("MBI-PARTNER",
                        Arrays.asList("tool", "upload"))).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_from_batch_authorities_without_sufficient_links.xml")));
    }

    /**
     * Проверить, что ручка /loadBatchAuthorities возвращает корректные данные, когда значение param непусто.
     */
    @Test
    void loadBatchAuthoritiesWithParamTest() {
        final String actualXmlResult = FunctionalTestHelper.
                post(baseUrl + "/loadBatchAuthorities", new RequestParametersForLoadDTO("MBI-PARTNER",
                        Arrays.asList("payment"))).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_from_batch_authorities_with_param.xml")));
    }

    /**
     * Проверить, что ручка /loadBatchAuthorities ничего не возвращает в случае несуществующей операции.
     */
    @Test
    void loadBatchAuthoritiesTestForWrongOperationName() {
        final String actualXmlResult = FunctionalTestHelper.
                post(baseUrl + "/loadBatchAuthorities", new RequestParametersForLoadDTO("MBI-PARTNER",
                        Collections.singletonList("wrong"))).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_from_batch_authorities_for_wrong_name.xml")));
    }

    /**
     * Проверить, что HttpBatchAuthoritiesLoader возвращает корректные данные.
     */
    @Test
    void httpBatchAuthoritiesLoaderTest() {
        final Map<String, OperationAuthorities> expectedOperationsAuthorities = getExpectedOperations();

        final Map<String, OperationAuthorities> actualOperationsAuthorities = batchLoader.
                loadBatch("MBI-PARTNER", Arrays.asList("index", "tool"));

        Assert.assertEquals(expectedOperationsAuthorities, actualOperationsAuthorities);
    }

    /**
     * Проверить, что HttpBatchAuthoritiesLoader возвращает корректные данные для операции,
     * у которой только sufficient links.
     */
    @Test
    void httpBatchAuthoritiesLoaderForOperationWithSufficientLinksOnlyTest() {
        final Map<String, OperationAuthorities> expectedOperationsAuthorities =
                getExpectedOperationsWithSufficientLinks();

        final Map<String, OperationAuthorities> actualOperationsAuthorities = batchLoader.
                loadBatch("MBI-PARTNER", Collections.singletonList("index"));

        Assert.assertEquals(expectedOperationsAuthorities, actualOperationsAuthorities);
    }

    /**
     * Проверить, что HttpBatchAuthoritiesLoader возвращает корректные данные для операции,
     * у которой только requires links.
     */
    @Test
    void httpBatchAuthoritiesLoaderForOperationWithRequiresLinksOnlyTest() {
        final Map<String, OperationAuthorities> expectedOperationsAuthorities =
                getExpectedOperationsWithRequiresLinks();

        final Map<String, OperationAuthorities> actualOperationsAuthorities = batchLoader.
                loadBatch("MBI-PARTNER", Collections.singletonList("tool"));

        Assert.assertEquals(expectedOperationsAuthorities, actualOperationsAuthorities);
    }

    /**
     * Проверить, что HttpBatchAuthoritiesLoader возвращает корректные данные для операций,
     * среди которых одна не существует.
     */
    @Test
    void httpBatchAuthoritiesLoaderForOperationsWithNonexistentNameTest() {
        final Map<String, OperationAuthorities> expectedOperationsAuthorities =
                getExpectedOperationsWithSufficientLinks();

        final Map<String, OperationAuthorities> actualOperationsAuthorities = batchLoader.
                loadBatch("MBI-PARTNER", Arrays.asList("index", "upload"));

        Assert.assertEquals(expectedOperationsAuthorities, actualOperationsAuthorities);
    }

    /**
     * Проверить, что HttpBatchAuthoritiesLoader ничего не возвращает в случае несуществующей операции.
     */
    @Test
    void httpBatchAuthoritiesLoaderTestForWrongOperationName() {
        final Map<String, OperationAuthorities> actualOperationsAuthorities = batchLoader.
                loadBatch("MBI-PARTNER", Collections.singletonList("wrong"));
        Assert.assertTrue(actualOperationsAuthorities.isEmpty());
    }

    /**
     * Проверить, что HttpBatchAuthoritiesLoader возвращает корректные данные для authority с параметрами.
     */
    @Test
    void httpBatchAuthoritiesLoaderForAuthorityWithParamTest() {
        final Map<String, OperationAuthorities> actualOperationsAuthorities = batchLoader.
                loadBatch("MBI-PARTNER", Collections.singletonList("payment"));
        Assert.assertEquals(getExpectedOperationsWithParam(), actualOperationsAuthorities);
    }

    private Map<String, OperationAuthorities> getExpectedOperations() {
        final Map<String, OperationAuthorities> operationAuthoritiesMap = new HashMap<>();

        Authority readerAuthority = new Authority("PARTNER_READER", "",
                "staticDomainAuthorityChecker");
        readerAuthority.setDomain("MBI-PARTNER");
        readerAuthority.setId(1);

        Authority qManagerAuthority = new Authority("QMANAGER", "",
                "staticDomainAuthorityChecker");
        qManagerAuthority.setDomain("MBI-PARTNER");
        qManagerAuthority.setId(2);

        Authority superAuthority = new Authority("YA_SUPER", "",
                "staticDomainAuthorityChecker");
        superAuthority.setDomain("MBI-PARTNER");
        superAuthority.setId(3);

        Authority super2Authority = new Authority("YA_SUPER_SUPER", "",
                "staticDomainAuthorityChecker");
        super2Authority.setDomain("MBI-PARTNER");
        super2Authority.setId(4);

        readerAuthority.setSufficientLinks(Collections.singleton(qManagerAuthority));

        superAuthority.setRequiresLinks(Collections.singleton(super2Authority));

        OperationPermission indexPermission = new OperationPermission();
        indexPermission.setOperationName("index");
        indexPermission.setAuthorities(Collections.singletonList(readerAuthority));

        OperationPermission toolPermission = new OperationPermission();
        toolPermission.setOperationName("tool");
        toolPermission.setAuthorities(Collections.singletonList(superAuthority));

        OperationAuthorities indexOperationAuthorities = new OperationAuthorities("index");
        indexOperationAuthorities.setPermissions(Collections.singletonList(indexPermission));

        OperationAuthorities toolOperationAuthorities = new OperationAuthorities("tool");
        toolOperationAuthorities.setPermissions(Collections.singletonList(toolPermission));

        operationAuthoritiesMap.put("index", indexOperationAuthorities);
        operationAuthoritiesMap.put("tool", toolOperationAuthorities);

        return operationAuthoritiesMap;
    }

    private Map<String, OperationAuthorities> getExpectedOperationsWithSufficientLinks() {
        final Map<String, OperationAuthorities> operationAuthoritiesMap = new HashMap<>();

        Authority readerAuthority = new Authority("PARTNER_READER", "",
                "staticDomainAuthorityChecker");
        readerAuthority.setDomain("MBI-PARTNER");
        readerAuthority.setId(1);

        Authority qManagerAuthority = new Authority("QMANAGER", "",
                "staticDomainAuthorityChecker");
        qManagerAuthority.setDomain("MBI-PARTNER");
        qManagerAuthority.setId(2);

        readerAuthority.setSufficientLinks(Collections.singleton(qManagerAuthority));

        OperationPermission indexPermission = new OperationPermission();
        indexPermission.setOperationName("index");
        indexPermission.setAuthorities(Collections.singletonList(readerAuthority));

        OperationAuthorities indexOperationAuthorities = new OperationAuthorities("index");
        indexOperationAuthorities.setPermissions(Collections.singletonList(indexPermission));

        operationAuthoritiesMap.put("index", indexOperationAuthorities);

        return operationAuthoritiesMap;
    }

    private Map<String, OperationAuthorities> getExpectedOperationsWithRequiresLinks() {
        final Map<String, OperationAuthorities> operationAuthoritiesMap = new HashMap<>();

        Authority superAuthority = new Authority("YA_SUPER", "",
                "staticDomainAuthorityChecker");
        superAuthority.setDomain("MBI-PARTNER");
        superAuthority.setId(1);

        Authority super2Authority = new Authority("YA_SUPER_SUPER", "",
                "staticDomainAuthorityChecker");
        super2Authority.setDomain("MBI-PARTNER");
        super2Authority.setId(2);

        superAuthority.setRequiresLinks(Collections.singleton(super2Authority));

        OperationPermission toolPermission = new OperationPermission();
        toolPermission.setOperationName("tool");
        toolPermission.setAuthorities(Collections.singletonList(superAuthority));

        OperationAuthorities toolOperationAuthorities = new OperationAuthorities("tool");
        toolOperationAuthorities.setPermissions(Collections.singletonList(toolPermission));

        operationAuthoritiesMap.put("tool", toolOperationAuthorities);

        return operationAuthoritiesMap;
    }

    private Map<String, OperationAuthorities> getExpectedOperationsWithParam() {
        final Map<String, OperationAuthorities> operationAuthoritiesMap = new HashMap<>();

        Authority shopEveryoneAuthority = new Authority("SHOP_EVERYONE", "very_important_param",
                "staticDomainAuthorityChecker");
        shopEveryoneAuthority.setDomain("MBI-PARTNER");
        shopEveryoneAuthority.setId(1);

        OperationPermission paymentPermission = new OperationPermission();
        paymentPermission.setOperationName("payment");
        paymentPermission.setAuthorities(Collections.singletonList(shopEveryoneAuthority));

        OperationAuthorities paymentOperationAuthorities = new OperationAuthorities("payment");
        paymentOperationAuthorities.setPermissions(Collections.singletonList(paymentPermission));

        operationAuthoritiesMap.put("payment", paymentOperationAuthorities);

        return operationAuthoritiesMap;
    }
}
