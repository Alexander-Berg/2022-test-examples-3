package ru.yandex.direct.core.entity.promocodes.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.promocodes.model.PromocodeClientDomain;
import ru.yandex.direct.core.entity.promocodes.repository.PromocodeDomainsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/*
На самом деле тесты опосредованно проверяют наши предположения о нормализации домена и промокода
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodesAntiFraudAddDomainTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final String PROMOCODE = "ASDSFFRTYCCCCCQQQQ";
    private final PromocodeDomainsRepository repository = mock(PromocodeDomainsRepository.class);
    @Autowired
    private HostingsHandler hostingsHandler;
    private PromocodesAntiFraudService service;

    @Before
    public void setup() {
        service = new PromocodesAntiFraudServiceBuilder()
            .withPromocodeDomainsRepository(repository)
            .withHostingsHandler(hostingsHandler)
            .build();
    }

    @Test
    public void promocodeNormalization() {
        service.addPromocodeDomain("asdsf-FRTY-cccccQQQQ", CLIENT_ID, "yandex.ru");
        verify(repository).addPromocodeDomain(
                argThat(new PromocodeDomainsMatcher(PROMOCODE, "yandex.ru"))
        );
    }

    @Test
    public void domainStripWww() {
        service.addPromocodeDomain(PROMOCODE, CLIENT_ID, "www.leningrad.spb.ru");
        verify(repository).addPromocodeDomain(
                argThat(new PromocodeDomainsMatcher(PROMOCODE, "leningrad.spb.ru"))
        );
    }

    @Test
    public void domainUnicode() {
        service.addPromocodeDomain(PROMOCODE, CLIENT_ID, "xn--c1aldgkov.xn--p1ai");
        verify(repository).addPromocodeDomain(
                argThat(new PromocodeDomainsMatcher(PROMOCODE, "мойкруг.рф"))
        );
    }

    @Test
    public void domainLowerCase() {
        service.addPromocodeDomain(PROMOCODE, CLIENT_ID, "AEROFLOT.RU");
        verify(repository).addPromocodeDomain(
                argThat(new PromocodeDomainsMatcher(PROMOCODE, "aeroflot.ru"))
        );
    }

    private static class PromocodeDomainsMatcher implements ArgumentMatcher<PromocodeClientDomain> {
        private final String expectedPromocode;
        private final String expectedDomain;

        public PromocodeDomainsMatcher(String promocode, String domain) {
            expectedPromocode = promocode;
            expectedDomain = domain;
        }

        @Override
        public boolean matches(PromocodeClientDomain argument) {
            return expectedPromocode.equals(argument.getPromocode()) && expectedDomain.equals(argument.getDomain());
        }
    }
}
