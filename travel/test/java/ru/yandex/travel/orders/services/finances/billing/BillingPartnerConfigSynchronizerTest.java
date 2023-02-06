package ru.yandex.travel.orders.services.finances.billing;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.misc.test.Assert;
import ru.yandex.travel.orders.cache.HotelAgreementDictionary;
import ru.yandex.travel.orders.entities.partners.BillingPartnerConfig;
import ru.yandex.travel.orders.repository.BillingPartnerConfigRepository;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
//@TestExecutionListeners(
//        listeners = TruncateDatabaseTestExecutionListener.class,
//        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ActiveProfiles("test")
public class BillingPartnerConfigSynchronizerTest {

    @Autowired
    private BillingPartnerConfigSynchronizer billingPartnerConfigSynchronizer;

    @Autowired
    private BillingPartnerConfigRepository billingPartnerConfigRepository;

    @MockBean
    private HotelAgreementDictionary hotelAgreementDictionary;

    @Before
    public void init() {
        billingPartnerConfigRepository.deleteAll();
        Instant now = Instant.now();
        BillingPartnerConfig config1 = new BillingPartnerConfig(1L, true, true, true, true, now, now);
        BillingPartnerConfig config2 = new BillingPartnerConfig(2L, true, true, true, true, now, now);
        billingPartnerConfigRepository.saveAll(List.of(config1, config2));
    }

    @Test
    @Transactional
    public void testAgreementDictionaryIsNotReady() {
        when(hotelAgreementDictionary.getTableCacheUpdateTimestamp()).thenReturn(Instant.now().toEpochMilli());
        when(hotelAgreementDictionary.isReady()).thenReturn(false);
        billingPartnerConfigSynchronizer.insertMissingClientIdsIntoBillingPartnerConfig("task");
        List<BillingPartnerConfig> finalList = billingPartnerConfigRepository.findAll();
        Assert.assertEquals(2, finalList.size());
        Assert.assertTrue(getConfigWithId(finalList, 1L).isPresent());
        Assert.assertTrue(getConfigWithId(finalList, 2L).isPresent());
    }

    @Test
    @Transactional
    public void testPartnerConfigIsActual() {
        when(hotelAgreementDictionary.getTableCacheUpdateTimestamp()).thenReturn(0L);
        when(hotelAgreementDictionary.isReady()).thenReturn(true);
        billingPartnerConfigSynchronizer.insertMissingClientIdsIntoBillingPartnerConfig("task");
        List<BillingPartnerConfig> finalList = billingPartnerConfigRepository.findAll();
        Assert.assertEquals(2, finalList.size());
        Assert.assertTrue(getConfigWithId(finalList, 1L).isPresent());
        Assert.assertTrue(getConfigWithId(finalList, 2L).isPresent());
    }

    @Test
    @Transactional
    public void testPartnerConfigAlreadyHasClientIds() {
        when(hotelAgreementDictionary.getTableCacheUpdateTimestamp()).thenReturn(Instant.now().toEpochMilli());
        when(hotelAgreementDictionary.isReady()).thenReturn(true);
        when(hotelAgreementDictionary.getAllClientIds()).thenReturn(new HashSet<>(Arrays.asList(1L, 2L)));
        billingPartnerConfigSynchronizer.insertMissingClientIdsIntoBillingPartnerConfig("task");
        List<BillingPartnerConfig> finalList = billingPartnerConfigRepository.findAll();
        Assert.assertEquals(2, finalList.size());
        Assert.assertTrue(getConfigWithId(finalList, 1L).isPresent());
        Assert.assertTrue(getConfigWithId(finalList, 2L).isPresent());
    }

    @Test
    @Transactional
    public void testPartnerConfigNewClientId() {
        when(hotelAgreementDictionary.getTableCacheUpdateTimestamp()).thenReturn(Instant.now().toEpochMilli());
        when(hotelAgreementDictionary.isReady()).thenReturn(true);
        when(hotelAgreementDictionary.getAllClientIds()).thenReturn(new HashSet<>(Arrays.asList(1L, 3L)));
        billingPartnerConfigSynchronizer.insertMissingClientIdsIntoBillingPartnerConfig("task");
        List<BillingPartnerConfig> finalList = billingPartnerConfigRepository.findAll();
        Assert.assertEquals(3, finalList.size());
        Assert.assertTrue(getConfigWithId(finalList, 1L).isPresent());
        Assert.assertTrue(getConfigWithId(finalList, 2L).isPresent());
        Optional<BillingPartnerConfig> config = getConfigWithId(finalList, 3L);
        Assert.assertTrue(config.isPresent());
        Assert.assertFalse(config.get().isAgreementActive());
        Assert.assertTrue(config.get().isExportToYt());
        Assert.assertTrue(config.get().isGenerateTransactions());
        Assert.assertTrue(config.get().isSynchronizeAgreement());
        Assert.assertNull(config.get().getSynchronizedAt());
    }

    private Optional<BillingPartnerConfig> getConfigWithId(List<BillingPartnerConfig> configList, long clientId) {
        return configList.stream().filter(config -> config.getBillingClientId() == clientId).findFirst();
    }

}
