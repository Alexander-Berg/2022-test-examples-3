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

public class CheckStaticDomainAuthorityTest extends FunctionalTest {
    private final RestTemplate restTemplate = new RestTemplate();
    private final Set<String> ignoredResponseAttributes = Set.of("host", "executing-time");

    @Test
    @DbUnitDataSet(before = "csv/DomainsWithRoles.before.csv")
    public void testHasAuthority() {
        Map<String, String> params = Map.of(
                "domain", "MBI-PARTNER",
                "authority-name", "TEST_SUPER",
                "uid", "123000");

        String expectedXml = StringTestUtil.getString(this.getClass(),
                "xml/checkStaticDomainAuthorityTrueResponse.xml");
        String responseXml = restTemplate.getForObject(
                getUrl("checkStaticDomainAuthority", params), String.class);
        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedXml, ignoredResponseAttributes));
    }

    @Test
    @DbUnitDataSet(before = "csv/DomainsWithRoles.before.csv")
    public void testHasNoAuthority() {
        Map<String, String> params = Map.of(
                "domain", "MBI-PARTNER",
                "authority-name", "PARTNER_READER",
                "uid", "123000");

        String expectedXml = StringTestUtil.getString(this.getClass(),
                "xml/checkStaticDomainAuthorityFalseResponse.xml");
        String responseXml = restTemplate.getForObject(
                getUrl("checkStaticDomainAuthority", params), String.class);
        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedXml, ignoredResponseAttributes));
    }

    @Test
    @DbUnitDataSet(before = "csv/DomainsWithRoles.before.csv")
    public void testUnknownDomain() {
        Map<String, String> params = Map.of(
                "domain", "unknown-domain",
                "authority-name", "PARTNER_READER",
                "uid", "123000");

        String expectedXml = StringTestUtil.getString(this.getClass(),
                "xml/checkStaticDomainAuthorityFalseResponse.xml");
        String responseXml = restTemplate.getForObject(
                getUrl("checkStaticDomainAuthority", params), String.class);
        MatcherAssert.assertThat(responseXml, MbiMatchers.xmlEquals(expectedXml, ignoredResponseAttributes));
    }
}
