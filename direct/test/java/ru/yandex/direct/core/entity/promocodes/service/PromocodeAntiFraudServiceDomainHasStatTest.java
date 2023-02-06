package ru.yandex.direct.core.entity.promocodes.service;

import java.net.IDN;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDomain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodeAntiFraudServiceDomainHasStatTest extends PromocodeTestBase {

    @Autowired
    private PromocodesAntiFraudService promocodesAntiFraudService;

    @Test
    public void mirrorHasStat() {
        String domain = TestDomain.testDomain().getDomain();
        String domainMirror = getMirror(domain);

        addMainMirror(domain, domainMirror);
        addDomainStat(domainMirror, 1L);

        assertTrue(promocodesAntiFraudService.domainHasStat(domain));
    }

    @Test
    public void ASCIIHasStat() {
        String domain = TestDomain.testRussianDomain().getDomain();
        String encodedDomain = IDN.toASCII(domain);

        addDomainStat(encodedDomain, 1L);

        assertTrue(promocodesAntiFraudService.domainHasStat(domain));
    }

    @Test
    public void unicodeHasStat() {
        String domain = TestDomain.testRussianDomain().getDomain();
        String encodedDomain = IDN.toASCII(domain);

        addDomainStat(domain, 1L);

        assertTrue(promocodesAntiFraudService.domainHasStat(encodedDomain));
    }

    @Test
    public void noStat() {
        String domain = TestDomain.testDomain().getDomain();

        addDomainStat(domain, 0L);

        assertFalse(promocodesAntiFraudService.domainHasStat(domain));
    }

    @Test
    public void mirrorOfEncodedHasStat() {
        String domain = TestDomain.testRussianDomain().getDomain();
        String encodedDomain = IDN.toASCII(domain);
        String mirrorOfEncoded = getMirror(encodedDomain);

        addMainMirror(encodedDomain, mirrorOfEncoded);
        addDomainStat(mirrorOfEncoded, 1L);

        assertTrue(promocodesAntiFraudService.domainHasStat(domain));
    }

    @Test
    public void mirrorOfEncodedHasNoStat() {
        String domain = TestDomain.testRussianDomain().getDomain();
        String encodedDomain = IDN.toASCII(domain);
        String mirrorOfEncoded = getMirror(encodedDomain);

        addMainMirror(encodedDomain, mirrorOfEncoded);
        addDomainStat(mirrorOfEncoded, 0L);

        assertFalse(promocodesAntiFraudService.domainHasStat(domain));
    }

    private String getMirror(String domain) {
        return "www." + domain;
    }
}
