package ru.yandex.market.security;

import java.util.Map;
import java.util.Set;

import org.dbunit.database.DatabaseConfig;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.util.MbiMatchers;

@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"),
})
public class LoadAuthoritiesTest extends FunctionalTest {
    private final RestTemplate restTemplate = new RestTemplate();
    private final Set<String> ignoredResponseAttributes = Set.of("host", "executing-time");

    @Test
    @DbUnitDataSet(before = "csv/DomainsWithOperations.before.csv")
    public void testLoadAuthorities2() {
        var params = Map.of(
                "domain", "mbi-admin-test",
                "operation", "supplierUI@getSupplierContacts");
        String expectedXml = StringTestUtil.getString(this.getClass(),
                "xml/loadAuthorities2response.xml");
        String responseXml = restTemplate.getForObject(getUrl("loadAuthorities2", params), String.class);
        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedXml, ignoredResponseAttributes));
    }

    @Test
    @DbUnitDataSet(before = "csv/DomainsWithOperations.before.csv")
    public void testLoadAuthorities() {
        var params = Map.of(
                "domain", "mbi-admin-test",
                "operation", "supplierUI@getSupplierContacts");
        String expectedXml = StringTestUtil.getString(this.getClass(),
                "xml/loadAuthoritiesResponse.xml");
        String responseXml = restTemplate.getForObject(getUrl("loadAuthorities", params), String.class);
        System.out.println(responseXml);
        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedXml, ignoredResponseAttributes));
    }
}
