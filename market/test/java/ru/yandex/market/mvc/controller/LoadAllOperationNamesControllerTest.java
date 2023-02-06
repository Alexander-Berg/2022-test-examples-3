package ru.yandex.market.mvc.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.security.FunctionalTestHelper;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.security.core.HttpAllOperationNamesLoader;
import ru.yandex.market.util.MbiMatchers;

/**
 * Тесты для ручки {@link LoadAllOperationNamesController}
 * и loader'a {@link HttpAllOperationNamesLoader} для ручки /loadAllOperationNames.
 *
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 */
@DbUnitDataSet(before = "all_operation_names_data.csv")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadAllOperationNamesControllerTest extends FunctionalTest {

    @Autowired
    private String baseUrl;

    private HttpAllOperationNamesLoader loader;

    @BeforeAll
    void createLoader() {
        loader = new HttpAllOperationNamesLoader();
        loader.setHttpProxyUrl(baseUrl);
    }

    /**
     * Проверить, что ручка /loadAllOperationNames возвращает корректные данные.
     */
    @Test
    void loadAllOperationNamesTest() {
        final String actualXmlResult = FunctionalTestHelper.
                get(getDomainUrl("MBI-PARTNER")).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals(StringTestUtil.getString(
                getClass(),
                "result_from_all_operation_names.xml")));
    }

    /**
     * Проверить, что ручка /loadAllOperationNames ничего не возвращает в случае несуществующего домена.
     */
    @Test
    void loadAllOperationNamesForNonexistentDomainTest() {

        final String actualXmlResult = FunctionalTestHelper.
                get(getDomainUrl("MBI-ADMIN")).getBody();

        MatcherAssert.assertThat(actualXmlResult, MbiMatchers.xmlEquals("<names/>"));
    }

    /**
     * Проверить, что HttpAllOperationNamesLoader возвращает корректные данные.
     */
    @Test
    void httpAllOperationNamesLoaderTest() {

        Collection<String> actualOperationNames = loader.loadAllNames("MBI-PARTNER");

        Assertions.assertEquals(getExpectedOperationNames(), actualOperationNames);
    }

    /**
     * Проверить, что HttpAllOperationNamesLoader возвращает пустую коллекцию для несуществующего домена.
     */
    @Test
    void httpAllOperationNamesLoaderForNonexistentDomainTest() {
        Collection<String> actualOperationNames = loader.loadAllNames("MBI-ADMIN");

        Assertions.assertTrue(actualOperationNames.isEmpty());
    }

    private Collection<String> getExpectedOperationNames() {
        return Arrays.asList("index", "payment", "uploadFeed", "tool");
    }

    private String getDomainUrl(String domain) {
        return getUrl("/loadAllOperationNames", Map.of("domain", domain)).toString();
    }
}
