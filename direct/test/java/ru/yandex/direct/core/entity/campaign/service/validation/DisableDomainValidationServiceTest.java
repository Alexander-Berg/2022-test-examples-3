package ru.yandex.direct.core.entity.campaign.service.validation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.repository.ClientLimitsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.campaign.service.validation.DisableDomainValidationService.isValidAppId;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DisableDomainValidationServiceTest {

    @Mock
    private FeatureService featureService;

    @Autowired
    ClientLimitsRepository clientLimitsRepository;

    @Autowired
    private DisableDomainValidationService disableDomainValidationService;

    @Autowired
    private ShardHelper shardHelper;

    @Before
    public void before() {
        initMocks(this);
        disableDomainValidationService = new DisableDomainValidationService(clientLimitsRepository, shardHelper,
                featureService);
    }

    @Test
    public void isValidDomains_ShortAndNumberId() {
        List<Boolean> disableNumberIdAndShortBundleIdAllowed = List.of(true, false);

        disableNumberIdAndShortBundleIdAllowed.forEach(feature -> {
            if (feature) {
                assertTrue(isValidDomains("imiphone", feature));
                assertTrue(isValidDomains("987654321", feature));
                assertTrue(isValidDomains("2", feature));
                assertTrue(isValidDomains("yandexbrowser", feature));
                assertTrue(isValidDomains("yandex-browser__01", feature));
            } else {
                assertFalse(isValidDomains("imiphone", feature));
                assertFalse(isValidDomains("987654321", feature));
                assertFalse(isValidDomains("2", feature));
                assertFalse(isValidDomains("yandexbrowser", feature));
                assertFalse(isValidDomains("yandex-browser__01", feature));
            }
        });
    }

    @Test
    public void isValidAppIdTest() {
        assertTrue(isValidAppId("com.google.app5", false));
        assertFalse(isValidAppId("id1046548168", false));
        assertFalse(isValidAppId("5.google.app5", false));
        assertFalse(isValidDomains(List.of("com.google.app5", "id1046548168"), false, false));
    }

    @Test
    public void isValidDomains_ValidDomains() {
        assertTrue(isValidDomain("planeta.ru", false));
        assertTrue(isValidDomain("planeta.ru", true));
        assertTrue(isValidDomain("google.com", false));
        assertTrue(isValidDomain("google.com", true));
        assertTrue(isValidDomain("gismeteo.ru", false));
        assertTrue(isValidDomain("gismeteo.ru", true));
        assertTrue(isValidDomain("megamail.ru", false));
        assertTrue(isValidDomain("megamail.ru", true));
        assertTrue(isValidDomain("whoyandex.ru", false));
        assertTrue(isValidDomain("whoyandex.ru", true));
        assertTrue(isValidDomain("superdomain.spb.ru", false));
        assertTrue(isValidDomain("superdomain.spb.ru", true));
        assertTrue(isValidDomains(List.of("24smi.org", "gismeteo.ru", "planeta.ru"),
                false));
        assertTrue(isValidDomains(List.of("24smi.org", "gismeteo.ru", "planeta.ru"),
                true));
        assertTrue(isValidDomains(List.of("gismeteo.ru", "24smi.org"), false));
        assertTrue(isValidDomains(List.of("gismeteo.ru", "24smi.org"), true));
        assertTrue(isValidDomain("яндекс.рф", true));
        assertTrue(isValidDomain("ya.ru", true));
        assertTrue(isValidDomain("yandex.ru", true));
        assertTrue(isValidDomain("direct.yandex.ru", true));
        assertTrue(isValidDomain("yandex.ua", true));

        assertTrue(isValidDomain("mail.ru", true));
        assertTrue(isValidDomain("www.mail.ru", true));

        assertTrue(isValidDomain("mail.ru", true));
        assertTrue(isValidDomain("www.mail.ru", true));

        assertTrue(isValidDomain("mail.ru", false));
        assertTrue(isValidDomain("www.mail.ru", false));
    }

    @Test
    public void isValidDomains_InvalidDomains() {
        assertFalse(isValidDomain("яндекс.рф", false));
        assertFalse(isValidDomain("ya.ru", false));
        assertFalse(isValidDomain("yandex.ru", false));
        assertFalse(isValidDomain("direct.yandex.ru", false));
        assertFalse(isValidDomain("yandex.ua", false));

        assertFalse(isValidDomain("spb.ru", false));
        assertFalse(isValidDomain("mobfox", false));
        assertFalse(isValidDomain("mob...asd asd. as.dasd. asfox.", false));
        assertFalse(isValidDomain("id1046548168", false));

        assertFalse(isValidDomains(List.of("24smi.org", "gismeteo.ru", "asddsaffds"),
                false));
        assertFalse(isValidDomains(List.of("gismeteo.ru", "24smi.org", "asddsaffds"),
                false));
    }

    private boolean isValidDomain(String domain, boolean disableAnyDomainsAllowed) {
        return isValidDomains(List.of(domain), disableAnyDomainsAllowed);
    }

    private boolean isValidDomains(List<String> domains, boolean disableAnyDomainsAllowed) {
        return isValidDomains(domains, disableAnyDomainsAllowed, false);
    }

    private boolean isValidDomains(String domains,
                                   boolean disableNumberIdAndShortBundleIdAllowed) {
        return isValidDomains(List.of(domains), false, disableNumberIdAndShortBundleIdAllowed);
    }

    private boolean isValidDomains(List<String> domains, boolean disableAnyDomainsAllowed,
                                   boolean disableNumberIdAndShortBundleIdAllowed) {
        when(featureService.isEnabledForClientId(Mockito.any(ClientId.class),
                Mockito.eq(FeatureName.DISABLE_ANY_DOMAINS_ALLOWED))).thenReturn(disableAnyDomainsAllowed);

        when(featureService.isEnabledForClientId(Mockito.any(ClientId.class),
                Mockito.eq(FeatureName.DISABLE_NUMBER_ID_AND_SHORT_BUNDLE_ID_ALLOWED))).thenReturn(disableNumberIdAndShortBundleIdAllowed);

        return disableDomainValidationService.isValidDomains(domains, ClientId.fromLong(52513L));
    }
}
