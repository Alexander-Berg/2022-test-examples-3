package ru.yandex.direct.core.entity.promocodes.service;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.promocodes.model.PromocodeClientDomain;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeDomainsCheckResult;
import ru.yandex.direct.core.entity.promocodes.repository.PromocodeDomainsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodesAntiFraudCheckPromocodeDomainsTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final String PROMOCODE = "ASDSFFRTYCCCCCQQQQ";
    private static final String DOMAIN = "яндекс.рф";
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
    public void allOk() {
        when(repository.getPromocodeDomains(List.of(PROMOCODE)))
                .thenReturn(Map.of(PROMOCODE,
                        new PromocodeClientDomain()
                            .withClientId(CLIENT_ID)
                            .withDomain(DOMAIN)
        ));
        assertEquals(PromocodeDomainsCheckResult.OK,
                service.checkPromocodeDomains(List.of(PROMOCODE), CLIENT_ID, DOMAIN).get(PROMOCODE));
    }

    @Test
    public void notFound() {
        assertEquals(PromocodeDomainsCheckResult.NOT_FOUND,
                service.checkPromocodeDomains(List.of(PROMOCODE), CLIENT_ID, DOMAIN).get(PROMOCODE));
    }

    @Test
    public void nullDomainNotFound() {
        assertEquals(PromocodeDomainsCheckResult.NOT_FOUND,
                service.checkPromocodeDomains(List.of(PROMOCODE), CLIENT_ID, null).get(PROMOCODE));
    }

    @Test
    public void domainMismatch() {
        when(repository.getPromocodeDomains(List.of(PROMOCODE))).thenReturn(Map.of(PROMOCODE,
                new PromocodeClientDomain()
                        .withClientId(CLIENT_ID)
                        .withDomain(DOMAIN)
        ));
        assertEquals(PromocodeDomainsCheckResult.DOMAIN_MISMATCH,
                service.checkPromocodeDomains(List.of(PROMOCODE), CLIENT_ID, "cats.com").get(PROMOCODE));
    }

    @Test
    public void nullDomainMismatch() {
        when(repository.getPromocodeDomains(List.of(PROMOCODE))).thenReturn(Map.of(PROMOCODE,
                new PromocodeClientDomain()
                        .withClientId(CLIENT_ID)
                        .withDomain(DOMAIN)
        ));
        assertEquals(PromocodeDomainsCheckResult.DOMAIN_MISMATCH,
                service.checkPromocodeDomains(List.of(PROMOCODE), CLIENT_ID, null).get(PROMOCODE));
    }

    @Test
    public void clientMismatch() {
        when(repository.getPromocodeDomains(List.of(PROMOCODE))).thenReturn(Map.of(PROMOCODE,
                new PromocodeClientDomain()
                        .withClientId(CLIENT_ID)
                        .withDomain(DOMAIN)
        ));
        assertEquals(PromocodeDomainsCheckResult.CLIENT_MISMATCH,
                service.checkPromocodeDomains(List.of(PROMOCODE), ClientId.fromLong(2L), DOMAIN).get(PROMOCODE));
    }
}
