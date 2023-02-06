package ru.yandex.direct.core.entity.promocodes.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.core.entity.banner.type.href.BannerDomainRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.testing.data.TestDomain;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Тесты на получение ункального домена, на моках вместо базы.
 */
public class PromocodesAntiFraudDetermineRestrictedDomainTest {

    @Mock
    private BannerDomainRepository bannerDomainRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private HostingsHandler hostingsHandler;

    private final int shard = RandomNumberUtils.nextPositiveInteger(Short.MAX_VALUE);
    private final long campaignId = testCampaignId();
    private final long clientId = RandomNumberUtils.nextPositiveLong(Integer.MAX_VALUE);
    private final ClientId modelClientId = ClientId.fromLong(clientId);

    private PromocodesAntiFraudService service;

    // ответ репозитория баннеров
    private Set<String> domains = new HashSet<>();
    // типы кампаний под кошельком.
    private Map<Long, CampaignType> campaignTypeMap = new HashMap<>();
    // тип кампнии
    private CampaignType campaignType;
    // должна быть сама кампания или кампании под кошельком
    private Set<Long> affectedCampaignIds;
    // id кампаний под кошельком, релевантно для campaignType == WALLET
    private Set<Long> campaignIdsInWallet;

    @SuppressWarnings("ConstantConditions")
    @Before
    public void tuneMocks() {
        initMocks(this);

        when(hostingsHandler.stripWww(anyString()))
                .thenAnswer(this::stripWwwMock);

        when(shardHelper.getShardByCampaignId(campaignId))
                .thenReturn(shard);
        when(shardHelper.getClientIdByCampaignId(campaignId))
                .thenReturn(clientId);

        when(campaignRepository.getCampaignsTypeMap(shard, modelClientId, singletonList(campaignId)))
                .thenAnswer(this::getCampaignsTypeMapSingleMock);
        when(campaignRepository.getCampaignsTypeMap(anyInt(), any(), anyCollection(), anyCollection()))
                .thenAnswer(this::getCampaignsTypeMapMock);
        when(campaignRepository.getCampaignsTypeMap(anyInt(), any(), isNull(), anyCollection()))
                .thenAnswer(this::getCampaignsTypeMapMock);
        when(campaignRepository.getCampaignIdsUnderWallet(shard, campaignId))
                .thenAnswer(invocation -> new ArrayList<>(campaignIdsInWallet));

        when(bannerDomainRepository.getUniqueBannersDomainsByCampaignIds(anyInt(), anyCollection(), anyInt()))
                .thenAnswer(this::getUniqueBannersDomainsByCampaignIdsMock);

        service = new PromocodesAntiFraudServiceBuilder()
                .withCampaignRepository(campaignRepository)
                .withBannerRepository(bannerDomainRepository)
                .withDomainRepository(domainRepository)
                .withShardHelper(shardHelper)
                .withHostingsHandler(hostingsHandler)
                .build();
    }

    @Test
    public void performanceCampaign_expectNull() {
        campaignType = CampaignType.PERFORMANCE;
        affectedCampaignIds = emptySet();
        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignNoDomains_expectNull() {
        campaignType = CampaignType.TEXT;
        affectedCampaignIds = asSet(campaignId);

        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignSingleDomain_expectDomain() {
        String domain = testDomain();

        campaignType = CampaignType.TEXT;
        domains.add(domain);
        affectedCampaignIds = asSet(campaignId);

        assertEquals(domain, service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignSingleDomainWithWww_expectDomain() {
        String domain = testDomain();

        campaignType = CampaignType.TEXT;
        domains.add("www." + domain);
        affectedCampaignIds = asSet(campaignId);

        assertEquals(domain, service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignSingleUnicodeDomainWithoutWww_expectUnicodeDomain() {
        String domain = "окна.рф";

        campaignType = CampaignType.TEXT;
        domains.add(domain);
        affectedCampaignIds = asSet(campaignId);

        assertEquals(domain, service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignSingleUnicodeDomainWithWww_expectUnicodeDomain() {
        String domain = "окна.рф";

        campaignType = CampaignType.TEXT;
        domains.add("www." + domain);
        affectedCampaignIds = asSet(campaignId);

        assertEquals(domain, service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignSinglePunycodeDomain_expectUnicodeDomain() {
        campaignType = CampaignType.TEXT;
        domains.add("xn--80atjc.xn--p1ai");
        affectedCampaignIds = asSet(campaignId);

        assertEquals("окна.рф", service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignSinglePunycodeDomainWithWww_expectUnicodeDomain() {
        campaignType = CampaignType.TEXT;
        domains.add("www.xn--80atjc.xn--p1ai");
        affectedCampaignIds = asSet(campaignId);

        assertEquals("окна.рф", service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignPunycodeDomainWithAndWithoutWww_expectUnicodeDomain() {
        String domain = "xn--80atjc.xn--p1ai";

        campaignType = CampaignType.TEXT;
        domains.add("www." + domain);
        domains.add(domain);
        affectedCampaignIds = asSet(campaignId);

        assertEquals("окна.рф", service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignDomainInPunycodeAndUnicode_expectUnicodeDomain() {
        campaignType = CampaignType.TEXT;
        domains.add("xn--80atjc.xn--p1ai");
        domains.add("окна.рф");
        affectedCampaignIds = asSet(campaignId);

        assertEquals("окна.рф", service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignTwoPunycodeDomains_expectNull() {
        campaignType = CampaignType.TEXT;
        domains.add("xn--80atjc.xn--p1ai");
        domains.add("xn--b1adem3b.xn--p1ai");
        affectedCampaignIds = asSet(campaignId);

        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignDomainWithAndWithoutWww_expectDomain() {
        String domain = testDomain();

        campaignType = CampaignType.TEXT;
        domains.add("www." + domain);
        domains.add(domain);
        affectedCampaignIds = asSet(campaignId);

        assertEquals(domain, service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignTwoDomains_expectNull() {
        campaignType = CampaignType.TEXT;
        domains.add(testDomain());
        domains.add(testDomain());

        affectedCampaignIds = asSet(campaignId);

        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaignThreeDomains_expectNull() {
        campaignType = CampaignType.TEXT;
        domains.add(testDomain());
        domains.add(testDomain());
        domains.add(testDomain());

        affectedCampaignIds = asSet(campaignId);

        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void walletCampaignWithoutCampaigns_expectNull() {
        campaignType = CampaignType.WALLET;
        campaignIdsInWallet = emptySet();
        affectedCampaignIds = emptySet();


        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void walletCampaignWithPerformanceCampaign_expectNull() {
        campaignType = CampaignType.WALLET;
        long campaignIdUnderWallet = testCampaignId();
        campaignTypeMap.put(campaignIdUnderWallet, CampaignType.PERFORMANCE);
        campaignIdsInWallet = asSet(campaignIdUnderWallet);
        affectedCampaignIds = emptySet();

        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void walletCampaignWithTwoTextCampaignsWithDomain_expectDomain() {
        String domain = testDomain();
        domains.add(domain);

        campaignType = CampaignType.WALLET;

        long campaignIdUnderWallet1 = testCampaignId();
        long campaignIdUnderWallet2 = testCampaignId();
        campaignTypeMap.put(campaignIdUnderWallet1, CampaignType.TEXT);
        campaignTypeMap.put(campaignIdUnderWallet2, CampaignType.TEXT);

        campaignIdsInWallet = asSet(campaignIdUnderWallet1, campaignIdUnderWallet2);
        affectedCampaignIds = asSet(campaignIdUnderWallet1, campaignIdUnderWallet2);

        assertEquals(domain, service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void walletCampaignWithPerformanceAndTextCampaignWithDomain_expectDomain() {
        String domain = testDomain();
        domains.add(domain);

        campaignType = CampaignType.WALLET;

        long campaignIdUnderWallet1 = testCampaignId();
        long campaignIdUnderWallet2 = testCampaignId();
        campaignTypeMap.put(campaignIdUnderWallet1, CampaignType.PERFORMANCE);
        campaignTypeMap.put(campaignIdUnderWallet2, CampaignType.TEXT);

        campaignIdsInWallet = asSet(campaignIdUnderWallet1, campaignIdUnderWallet2);
        affectedCampaignIds = asSet(campaignIdUnderWallet2);

        assertEquals(domain, service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void walletCampaignWithTextCampaignWithTwoDomains_expectNull() {
        domains.add(testDomain());
        domains.add(testDomain());

        campaignType = CampaignType.WALLET;

        long campaignIdUnderWallet = testCampaignId();
        campaignTypeMap.put(campaignIdUnderWallet, CampaignType.TEXT);

        campaignIdsInWallet = asSet(campaignIdUnderWallet);
        affectedCampaignIds = asSet(campaignIdUnderWallet);

        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaign_oneThirdLevelDomain_expectNull() {
        campaignType = CampaignType.TEXT;
        domains.add("asdf.bayandex.ru");

        affectedCampaignIds = asSet(campaignId);

        assertNull(service.determineRestrictedDomain(campaignId));
    }

    @Test
    public void textCampaign_oneThirdLevelPublicSubdomain_expectDomain() {
        campaignType = CampaignType.TEXT;
        String domain = "asdf.bayandex.ru";
        domains.add(domain);
        when(hostingsHandler.isFirstSubdomainOfPublicDomainOrHosting(domain)).thenReturn(true);

        affectedCampaignIds = asSet(campaignId);

        assertEquals(service.determineRestrictedDomain(campaignId), domain);
    }

    private String stripWwwMock(InvocationOnMock invocation) {
        String domain = invocation.getArgument(0);
        String wwwPrefix = "www.";
        if (domain.startsWith(wwwPrefix)) {
            return domain.replaceFirst(wwwPrefix, "");
        } else {
            return domain;
        }
    }

    private Map<Long, CampaignType> getCampaignsTypeMapSingleMock(InvocationOnMock invocation) {
        return singletonMap(campaignId, campaignType);
    }

    private Map<Long, CampaignType> getCampaignsTypeMapMock(InvocationOnMock invocation) {
        int iShard = invocation.getArgument(0);
        ClientId iClientId = invocation.getArgument(1);
        Set<Long> iCampaignIds = new HashSet<>(invocation.getArgument(2));
        Collection<CampaignType> types = invocation.getArgument(3);

        if (shard == iShard && Objects.equals(modelClientId, iClientId) && campaignIdsInWallet.equals(iCampaignIds)) {
            return EntryStream.of(campaignTypeMap)
                    .filterValues(types::contains)
                    .toMap();
        } else {
            return null;
        }
    }

    private Set<String> getUniqueBannersDomainsByCampaignIdsMock(InvocationOnMock invocation) {
        int iShard = invocation.getArgument(0);
        Set<Long> iCampaignIds = new HashSet<>(invocation.getArgument(1));

        if (shard == iShard && iCampaignIds.equals(affectedCampaignIds)) {
            return domains;
        } else {
            return null;
        }
    }

    private static String testDomain() {
        return TestDomain.randomDomain();
    }

    private static long testCampaignId() {
        return RandomNumberUtils.nextPositiveLong();
    }

    private static Set<Long> asSet(long... ids) {
        return new HashSet<>(asList(ids));
    }
}
