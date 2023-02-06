package ru.yandex.market.security;

import java.util.Map;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.util.MbiMatchers;

@DbUnitDataSet(before = "csv/DomainsWithRoles.before.csv")
public class LoadStaticDomainAuthoritiesTest extends FunctionalTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void loadAuthoritiesTest() {
        String expectedXml = StringTestUtil.getString(this.getClass(), "xml/loadStaticDomainAuthoritiesResponse.xml");
        String responseXml = restTemplate.getForObject(
                getUrl("loadStaticDomainAuthorities", Map.of("domain", "MBI-PARTNER")), String.class);
        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedXml, Set.of("host", "executing-time")));
    }

    @Test
    void loadAuthoritiesUnknownDomain() {
        String expectedXml = StringTestUtil.getString(this.getClass(),
                "xml/loadStaticDomainAuthoritiesUnknownDomainResponse.xml");
        String responseXml = restTemplate.getForObject(
                getUrl("loadStaticDomainAuthorities", Map.of("domain", "abcd")), String.class);
        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedXml, Set.of("host", "executing-time")));
    }
}
