package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.promocodes.model.CampPromocodes;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeClientDomain;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeDomainsCheckResult;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.entity.promocodes.model.TearOffReason;
import ru.yandex.direct.core.entity.promocodes.repository.CampPromocodesRepository;
import ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyPromocodeParameters;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.TimeProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService.DEFAULT_ANTI_FRAUD_START_DATE;
import static ru.yandex.direct.core.testing.data.TestPromocodeInfo.createPromocodeInfo;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты на логику обработки нотификации.
 * Основные сценарии - из https://st.yandex-team.ru/DIRECT-79101#1535546693000
 */
public class NotifyPromocodeServiceProcessNotificationTest {
    private static final int SERVICE_ID = 7;
    private static final long CAMPAIGN_ID = RandomNumberUtils.nextPositiveLong();
    private static final long CLIENT_ID = RandomNumberUtils.nextPositiveLong();
    private static final int SHARD = RandomNumberUtils.nextPositiveInteger(Short.MAX_VALUE);
    private static final LocalDateTime BORDER = LocalDateTime.of(DEFAULT_ANTI_FRAUD_START_DATE, LocalTime.MIDNIGHT);
    private static final String DOMAIN = "kupislona.ru";

    private final NotifyPromocodeService service;
    private final PromocodesAntiFraudService antiFraudService;
    private final CampaignRepository campaignRepository;
    private final CampPromocodesRepository promocodesRepository;
    private final NotifyPromocodeParameters request;
    private final LocalDateTime now;


    private CampPromocodes campPromocodes;
    private String restrictedDomain;

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public NotifyPromocodeServiceProcessNotificationTest() {
        now = LocalDateTime.now();

        antiFraudService = mock(PromocodesAntiFraudService.class);
        when(antiFraudService.determineRestrictedDomain(CAMPAIGN_ID)).thenAnswer(invocation -> restrictedDomain);
        when(antiFraudService.getAntiFraudStartDate()).thenReturn(BORDER);
        when(antiFraudService.isAcceptableWallet(any())).thenCallRealMethod();
        when(antiFraudService.checkPromocodeDomains(anyCollection(), any(), any(), any()))
                .then(i -> ((Collection<String>) i.getArgument(0))
                        .stream().collect(Collectors.toMap(Function.identity(), x -> PromocodeDomainsCheckResult.OK)));
        when(antiFraudService.shouldTearOffMismatched(anyInt(), anyLong(), any())).thenReturn(true);

        promocodesRepository = mock(CampPromocodesRepository.class);
        when(promocodesRepository.getCampaignPromocodes(SHARD, CAMPAIGN_ID)).thenAnswer(invocation -> campPromocodes);

        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByCampaignId(CAMPAIGN_ID)).thenReturn(SHARD);
        when(shardHelper.getClientIdByCampaignId(CAMPAIGN_ID)).thenReturn(CLIENT_ID);

        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(now);

        campaignRepository = mock(CampaignRepository.class);
        when(campaignRepository.getCampaigns(SHARD, singletonList(CAMPAIGN_ID)))
            .thenReturn(singletonList(new Campaign().withType(CampaignType.TEXT).withWalletId(0L)));

        service = new NotifyPromocodeService(null, antiFraudService, promocodesRepository, campaignRepository,
            shardHelper, null, null, timeProvider);

        request = new NotifyPromocodeParameters().withServiceId(SERVICE_ID).withCampaignId(CAMPAIGN_ID);
    }

    private static BalancePromocodeInfo convertToBalancePromocodeInfo(PromocodeInfo promocodeInfo) {
        return new BalancePromocodeInfo()
            .withId(promocodeInfo.getId())
            .withCode(promocodeInfo.getCode())
            .withInvoiceId(promocodeInfo.getInvoiceId())
            .withInvoiceExternalId(promocodeInfo.getInvoiceExternalId())
            .withInvoiceEnabledAt(promocodeInfo.getInvoiceEnabledAt())
            .withAvailableQty(BigDecimal.TEN)
            .withAnySameSeries(false)
            .withForNewClientsOnly(promocodeInfo.getForNewClientsOnly())
            .withGlobalUnique(false)
            .withUniqueUrlNeeded(true);
    }

    /**
     * Тесткейс 1, позитивный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Добавляется новая ограничивающая запись.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_singleApplicablePromocode_added() {
        PromocodeInfo promocode = createPromocodeInfo();
        restrictedDomain = "direct.yandex.ru";

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode))
                .withRestrictedDomain(restrictedDomain)
                .withLastChange(now);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 2, негативный сценарий: клиент без уникального домена, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Дергается ручка отрыва указанного промокода
     */
    @Test
    public void noUniqueDomainWithoutPromocodes_singleApplicablePromocode_tearedOff() {
        PromocodeInfo promocode = createPromocodeInfo();

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, singletonList(promocode),
                TearOffReason.BAD_DOMIAN);
    }

    /**
     * Тесткейс 3, позитивный сценарий: клиент без уникального домена, без использованных ранее промокодов.
     * Получаем информацию об оплате без промокодов.
     * Ничего не происходит.
     */
    @Test
    public void noUniqueDomainWithoutPromocodes_noPromocodes_nothing() {
        service.processNotification(request.withPromocodes(emptyList()));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 4, позитивный сценарий: клиент с уникальным доменом, с использованным ранее промокодом.
     * Получаем нотификацию, содержащую два промокода (использованный и новый).
     * Обновляется список промокодов.
     */
    @Test
    public void uniqueDomainWithSinglePromocode_twoApplicablePromocodes_updated() {
        PromocodeInfo dbPromocode = createPromocodeInfo();
        restrictedDomain = "direct.yandex.ru";
        campPromocodes = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(dbPromocode))
                .withRestrictedDomain(restrictedDomain);

        PromocodeInfo newPromocode = createPromocodeInfo()
                // промокоды сортируются по дате выставления счета
                .withInvoiceEnabledAt(dbPromocode.getInvoiceEnabledAt().plusHours(3));

        service.processNotification(request.withPromocodes(mapList(asList(dbPromocode, newPromocode),
                NotifyPromocodeServiceProcessNotificationTest::convertToBalancePromocodeInfo)));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository)
                .updateCampaignPromocodesList(SHARD, CAMPAIGN_ID, asList(dbPromocode, newPromocode));
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 5, позитивный сценарий: клиент с уникальным доменом, с использованными ранее двумя промокодами.
     * Получаем нотификацию, содержащую только один из прокомодов(из тех что есть в базе).
     * Обновляется список промокодов.
     */
    @Test
    public void uniqueDomainWithTwoPromocodes_oneApplicablePromocode_updated() {
        PromocodeInfo dbPromocode1 = createPromocodeInfo();
        PromocodeInfo dbPromocode2 = createPromocodeInfo();
        restrictedDomain = "direct.yandex.ru";
        campPromocodes = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(asList(dbPromocode1, dbPromocode2))
                .withRestrictedDomain(restrictedDomain);

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(dbPromocode2))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository).updateCampaignPromocodesList(SHARD, CAMPAIGN_ID, singletonList(dbPromocode2));
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 6, позитивный сценарий: клиент с уникальным доменом, с использованным ранее промокодом.
     * Получаем информацию об оплате без промокодов.
     * Удаляется ограничивающая запись.
     */
    @Test
    public void uniqueDomainWithPromocode_noPromocodes_removed() {
        PromocodeInfo dbPromocode = createPromocodeInfo();
        restrictedDomain = "direct.yandex.ru";
        campPromocodes = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(dbPromocode))
                .withRestrictedDomain(restrictedDomain);

        service.processNotification(request.withPromocodes(emptyList()));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository).deleteCampaignPromocodes(SHARD, CAMPAIGN_ID);
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 7 (модифицированный 1): клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с двумя промокодами, подходящими под наши условия и одним не подходящим (без
     * требования уникальности домена).
     * Добавляется новая ограничивающая запись с двумя промокодами.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_twoApplicablePromocodes_added() {
        PromocodeInfo promocode1 = createPromocodeInfo();
        PromocodeInfo promocode2 = createPromocodeInfo()
                .withInvoiceEnabledAt(promocode1.getInvoiceEnabledAt().minusHours(3));
        restrictedDomain = "direct.yandex.ru";

        List<BalancePromocodeInfo> balancePromocodes = asList(convertToBalancePromocodeInfo(promocode1),
                convertToBalancePromocodeInfo(promocode2),
                convertToBalancePromocodeInfo(createPromocodeInfo()).withUniqueUrlNeeded(false));

        service.processNotification(request.withPromocodes(balancePromocodes));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                // в таком порядке (2,1), так как промокоды сортируются по дате выставления счета
                .withPromocodes(asList(promocode2, promocode1))
                .withRestrictedDomain(restrictedDomain)
                .withLastChange(now);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 8 (модифицированный 2): клиент без уникального домена, без использованных ранее промокодов.
     * Получаем информацию об оплате с тремя промокодам, два из которых подходят под наши условия.
     * Дергается ручка отрыва для двух подходящих промокодов.
     */
    @Test
    public void noUniqueDomainWithoutPromocodes_twoApplicablePromocodesAndOneNot_twoTearedOff() {
        PromocodeInfo promocode1 = createPromocodeInfo();
        PromocodeInfo promocode2 = createPromocodeInfo()
                .withInvoiceEnabledAt(promocode1.getInvoiceEnabledAt().plusHours(1));
        PromocodeInfo promocode3 = createPromocodeInfo()
                .withInvoiceEnabledAt(promocode1.getInvoiceEnabledAt().plusHours(2));

        List<BalancePromocodeInfo> balancePromocodes = asList(convertToBalancePromocodeInfo(promocode1),
                convertToBalancePromocodeInfo(promocode2).withUniqueUrlNeeded(false),
                convertToBalancePromocodeInfo(promocode3));

        service.processNotification(request.withPromocodes(balancePromocodes));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, asList(promocode1, promocode3),
                TearOffReason.BAD_DOMIAN);
    }

    /**
     * Тесткейс 9 (модифицированный 3): клиент без уникального домена, без использованных ранее промокодов.
     * Получаем информацию об оплате с неподходящим промокодом (нет требования уникальности домена).
     * Ничего не происходит.
     */
    @Test
    public void noUniqueDomainWithoutPromocodes_PromocodeWithoutUniqueUrlNeededIsNotApplicable_nothing() {
        List<BalancePromocodeInfo> balancePromocodes = singletonList(
                convertToBalancePromocodeInfo(createPromocodeInfo()).withUniqueUrlNeeded(false));
        service.processNotification(request.withPromocodes(balancePromocodes));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 10: клиент с уникальным доменом, с использованными ранее промокодом.
     * Получаем нотификацию, содержащую этот же промокод.
     * Ничего не происходит.
     */
    @Test
    public void uniqueDomainWithPromocode_sameApplicablePromocode_nothing() {
        PromocodeInfo promocode = createPromocodeInfo();
        campPromocodes = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode))
                .withRestrictedDomain("direct.yandex.ru");

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 11 (модифицированный 3): клиент без уникального домена, без использованных ранее промокодов.
     * Получаем информацию об оплате с неподходящим промокодом (нет свободных средств промокода).
     * Ничего не происходит.
     */
    @Test
    public void noUniqueDomainWithoutPromocodes_PromocodeWithoutAvailableQtyIsNotApplicable_nothing() {
        List<BalancePromocodeInfo> balancePromocodes = singletonList(
                convertToBalancePromocodeInfo(createPromocodeInfo()).withAvailableQty(BigDecimal.ZERO));
        service.processNotification(request.withPromocodes(balancePromocodes));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 12 (модифицированный 6), позитивный сценарий: клиент с уникальным доменом,
     * с использованным ранее промокодом.
     * Получаем информацию об оплате с промокодом, у которого все доступные средства истрачены.
     * Удаляется ограничивающая запись.
     */
    @Test
    public void uniqueDomainWithPromocode_PromocodeWithoutAvailableQty_removed() {
        PromocodeInfo dbPromocode = createPromocodeInfo();
        restrictedDomain = "direct.yandex.ru";
        campPromocodes = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(dbPromocode))
                .withRestrictedDomain(restrictedDomain);

        List<BalancePromocodeInfo> balancePromocodes = singletonList(
                convertToBalancePromocodeInfo(dbPromocode).withAvailableQty(BigDecimal.ZERO));
        service.processNotification(request.withPromocodes(balancePromocodes));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository).deleteCampaignPromocodes(SHARD, CAMPAIGN_ID);
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 13 (модифицированный 1): клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с двумя промокодами: одним - подходящим под наши условия
     * и одним не подходящим (без свободных промокодных средств).
     * Добавляется новая ограничивающая запись с двумя промокодами.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_oneApplicablePromocodeOneWithoutAvailableQty_oneAdded() {
        PromocodeInfo promocode1 = createPromocodeInfo();
        restrictedDomain = "direct.yandex.ru";

        List<BalancePromocodeInfo> balancePromocodes = asList(convertToBalancePromocodeInfo(promocode1),
                convertToBalancePromocodeInfo(createPromocodeInfo()).withAvailableQty(BigDecimal.ZERO));

        service.processNotification(request.withPromocodes(balancePromocodes));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode1))
                .withRestrictedDomain(restrictedDomain)
                .withLastChange(now);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 14: клиент с уникальным доменом, с использованным ранее промокодом
     * (с датой активации - после граничной).
     * Получаем информацию об оплате с этим же промокодом.
     * Удаляется ограничивающая запись.
     * <p>
     * Поясненение: такая ситуация возникнет в случае, если промокод еще не исчерпан полностью, но граничную дату
     * передвинули в будущее
     */
    @Test
    public void uniqueDomainWithPromocodeActivatedAfterBorderDate_samePromocodes_removed() {
        PromocodeInfo promocode = createPromocodeInfo().withInvoiceEnabledAt(BORDER.minusDays(4));
        restrictedDomain = "direct.yandex.ru";
        campPromocodes = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode))
                .withRestrictedDomain(restrictedDomain);

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository).deleteCampaignPromocodes(SHARD, CAMPAIGN_ID);
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).determineRestrictedDomain(anyLong());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 15: клиент с уникальным доменом, без использованных ранее промокодов, по домену есть статистика показов.
     * Промокод отрывается.
     */
    @Test
    public void uniqueDomainWithoutPromocode_domainWithStat_tearedOff() {
        restrictedDomain = "direct.yandex.ru";
        PromocodeInfo promocode = createPromocodeInfo().withForNewClientsOnly(true);
        List<BalancePromocodeInfo> balancePromocodes = singletonList(convertToBalancePromocodeInfo(promocode));
        when(antiFraudService.domainHasStat(restrictedDomain)).thenReturn(true);

        service.processNotification(request.withPromocodes(balancePromocodes));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, singletonList(promocode),
                TearOffReason.HAS_IMPRESSIONS);
    }

    /**
     * Тесткейс 16: клиент с уникальным доменом, без использованных ранее промокодов,
     * домен годный, со статистикой, кампания — ОС, создана давно
     * Промокод отрывается.
     */
    @Test
    public void uniqueDomainWithoutPromocode_oldWallet_tornOff() {
        restrictedDomain = "direct.yandex.ru";
        PromocodeInfo promocode = createPromocodeInfo().withForNewClientsOnly(true);
        List<BalancePromocodeInfo> balancePromocodes = singletonList(convertToBalancePromocodeInfo(promocode));
        when(antiFraudService.domainHasStat(restrictedDomain)).thenReturn(true);
        when(campaignRepository.getCampaigns(SHARD, singletonList(CAMPAIGN_ID)))
            .thenAnswer(invocation ->
                singletonList(new Campaign().withType(CampaignType.WALLET).withCreateTime(LocalDateTime.MIN)));

        service.processNotification(request.withPromocodes(balancePromocodes));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, singletonList(promocode),
                TearOffReason.HAS_IMPRESSIONS);
    }

    /**
     * Тесткейс 17: клиент с уникальным доменом, без использованных ранее промокодов,
     * домен годный, со статистикой, кампания — ОС, создана недавно
     * Промокод сохраняется.
     */
    @Test
    public void uniqueDomainWithoutPromocode_newWallet_oneAdded() {
        restrictedDomain = "direct.yandex.ru";
        PromocodeInfo promocode = createPromocodeInfo();
        List<BalancePromocodeInfo> balancePromocodes = singletonList(convertToBalancePromocodeInfo(promocode));
        when(antiFraudService.domainHasStat(restrictedDomain)).thenReturn(true);
        when(campaignRepository.getCampaigns(SHARD, singletonList(CAMPAIGN_ID)))
            .thenAnswer(invocation ->
                singletonList(new Campaign().withType(CampaignType.WALLET)
                    .withCreateTime(LocalDateTime.now().minusHours(23))));

        service.processNotification(request.withPromocodes(balancePromocodes));

        verify(promocodesRepository).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 18, позитивный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Есть ограничение на домен промокода, Модификация кейса 1
     * Добавляется новая ограничивающая запись.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_matchedPromocodeDomain_added() {
        PromocodeInfo promocode = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService
                .checkPromocodeDomains(eq(List.of(promocode.getCode())),
                        eq(ClientId.fromLong(CLIENT_ID)),
                        eq(DOMAIN),
                        any()))
                .thenReturn(Map.of(promocode.getCode(), PromocodeDomainsCheckResult.OK));

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode))
                .withRestrictedDomain(DOMAIN)
                .withLastChange(now);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 19, позитивный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * нет ограничения на домен промокода, Модификация кейса 1
     * Добавляется новая ограничивающая запись.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_notFoundPromocodeDomain_added() {
        PromocodeInfo promocode = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService
                .checkPromocodeDomains(eq(List.of(promocode.getCode())),
                        eq(ClientId.fromLong(CLIENT_ID)),
                        eq(DOMAIN),
                        any()))
                    .thenReturn(Map.of(promocode.getCode(), PromocodeDomainsCheckResult.NOT_FOUND));

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode))
                .withRestrictedDomain(DOMAIN)
                .withLastChange(now);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 20, негативный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Есть ограничение на домен промокода, Что-то не совпадает. Модификация кейса 1
     * Промокод отрывается.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_mismatchedPromocodeDomain_tornOff() {
        PromocodeInfo promocode = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService
                .checkPromocodeDomains(eq(List.of(promocode.getCode())),
                        eq(ClientId.fromLong(CLIENT_ID)),
                        eq(DOMAIN),
                        any()))
                    .thenReturn(Map.of(promocode.getCode(), PromocodeDomainsCheckResult.DOMAIN_MISMATCH));

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, List.of(promocode), TearOffReason.MISMATCH);
    }

    /**
     * Тесткейс 21, негативный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Два промокода.
     * Есть ограничение на домен промокода, Что-то не совпадает в каждом.
     * Оба промокода отрываются.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_twoMismatchedPromocodeDomains_tornOff() {
        PromocodeInfo promocode = createPromocodeInfo();
        PromocodeInfo promocode2 = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService.checkPromocodeDomains(
                argThat(t -> t.containsAll(List.of(promocode.getCode(), promocode2.getCode()))),
                eq(ClientId.fromLong(CLIENT_ID)),
                eq(restrictedDomain),
                any()))
                .thenReturn(Map.of(promocode.getCode(), PromocodeDomainsCheckResult.DOMAIN_MISMATCH,
                        promocode2.getCode(), PromocodeDomainsCheckResult.DOMAIN_MISMATCH));

        service.processNotification(request.withPromocodes(
                List.of(convertToBalancePromocodeInfo(promocode), convertToBalancePromocodeInfo(promocode2))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(eq(SERVICE_ID),
                eq(CAMPAIGN_ID),
                argThat(t -> t.containsAll(List.of(promocode, promocode2))),
                eq(TearOffReason.MISMATCH));
    }

    /**
     * Тесткейс 22, смешанный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Два промокода.
     * Есть ограничение на домен промокода, Что-то не совпадает в одном.
     * Один промокод отрывается, другой зачислятся.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_twoCodesOneMismatchedPromocodeDomain_oneTornOffAnotherAdded() {
        PromocodeInfo promocode = createPromocodeInfo();
        PromocodeInfo promocode2 = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService.checkPromocodeDomains(
                argThat(t -> t.containsAll(List.of(promocode.getCode(), promocode2.getCode()))),
                eq(ClientId.fromLong(CLIENT_ID)),
                eq(restrictedDomain),
                any()))
                    .thenReturn(Map.of(promocode.getCode(), PromocodeDomainsCheckResult.DOMAIN_MISMATCH,
                        promocode2.getCode(), PromocodeDomainsCheckResult.OK));

        service.processNotification(request.withPromocodes(
                List.of(convertToBalancePromocodeInfo(promocode), convertToBalancePromocodeInfo(promocode2))));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(List.of(promocode))
                .withRestrictedDomain(DOMAIN)
                .withLastChange(now);
        CampPromocodes newRecord2 = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(List.of(promocode2))
                .withRestrictedDomain(DOMAIN)
                .withLastChange(now);
        verify(promocodesRepository, never()).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord2);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, List.of(promocode), TearOffReason.MISMATCH);
        verify(antiFraudService, never()).tearOffPromocodes(eq(SERVICE_ID), eq(CAMPAIGN_ID), eq(List.of(promocode2)),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 23, позитивный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Промокод без флага уникального домена — withUniqueUrlNeeded(false), но код нам известен.
     * Есть ограничение на домен промокода, Модификация кейса 18
     * Добавляется новая ограничивающая запись.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_noUniqueUrlNeeded_matchedPromocodeDomain_added() {
        PromocodeInfo promocode = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService.getNormalizedPromocodeDomains(List.of(promocode.getCode())))
                .thenReturn(Map.of(PromocodesAntiFraudService.normalizePromocode(promocode.getCode()),
                        new PromocodeClientDomain()));
        when(antiFraudService
                .checkPromocodeDomains(eq(List.of(promocode.getCode())),
                        eq(ClientId.fromLong(CLIENT_ID)),
                        eq(DOMAIN),
                        any()))
                .thenReturn(Map.of(promocode.getCode(), PromocodeDomainsCheckResult.OK));

        service.processNotification(request.withPromocodes(singletonList(
                convertToBalancePromocodeInfo(promocode).withUniqueUrlNeeded(false))));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode))
                .withRestrictedDomain(DOMAIN)
                .withLastChange(now);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 24, негативный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * Модификация кейса 21.
     * На промокодах нет флага требования уникального домена — withUniqueUrlNeeded(false).
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Два промокода.
     * Есть ограничение на домен промокода, Что-то не совпадает в каждом.
     * Оба промокода отрываются.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_noUniqueUrlNeeded_twoMismatchedPromocodeDomains_tornOff() {
        PromocodeInfo promocode = createPromocodeInfo();
        PromocodeInfo promocode2 = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService.getNormalizedPromocodeDomains(
                argThat(t -> t.containsAll(List.of(promocode.getCode(), promocode2.getCode())))))
                .thenReturn(Map.of(PromocodesAntiFraudService.normalizePromocode(promocode.getCode()),
                        new PromocodeClientDomain(),
                        PromocodesAntiFraudService.normalizePromocode(promocode2.getCode()),
                        new PromocodeClientDomain()));
        when(antiFraudService.checkPromocodeDomains(
                argThat(t -> t.containsAll(List.of(promocode.getCode(), promocode2.getCode()))),
                eq(ClientId.fromLong(CLIENT_ID)),
                eq(restrictedDomain),
                any()))
                .thenReturn(Map.of(promocode.getCode(), PromocodeDomainsCheckResult.DOMAIN_MISMATCH,
                        promocode2.getCode(), PromocodeDomainsCheckResult.DOMAIN_MISMATCH));

        service.processNotification(request.withPromocodes(
                List.of(convertToBalancePromocodeInfo(promocode).withUniqueUrlNeeded(false),
                        convertToBalancePromocodeInfo(promocode2).withUniqueUrlNeeded(false))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(eq(SERVICE_ID),
                eq(CAMPAIGN_ID),
                argThat(t -> t.containsAll(List.of(promocode, promocode2))),
                eq(TearOffReason.MISMATCH));
    }

    /**
     * Тесткейс 25, негативный сценарий: клиент без уникального домена, без использованных ранее промокодов.
     * Модификация кейса 2.
     * У промокода нет требования уникального домена — withUniqueUrlNeeded(false), но сам домен для промокода записан.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Дергается ручка отрыва указанного промокода
     */
    @Test
    public void noUniqueDomainWithoutPromocodes_noUniqueUrlNeeded_singleApplicablePromocode_tornOff() {
        PromocodeInfo promocode = createPromocodeInfo();
        when(antiFraudService.getNormalizedPromocodeDomains(List.of(promocode.getCode())))
                .thenReturn(Map.of(PromocodesAntiFraudService.normalizePromocode(promocode.getCode()),
                        new PromocodeClientDomain()));

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        verify(promocodesRepository, never()).addCampaignPromocodes(anyInt(), any());
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, singletonList(promocode),
                TearOffReason.BAD_DOMIAN);
    }

    /**
     * Тесткейс 26, позитивный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * На домене есть статистика, промокод не требует новизны клиента — withForNewClientsOnly(false).
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Добавляется новая ограничивающая запись.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_notForNewClientsOnly_hasStat_singleApplicablePromocode_added() {
        PromocodeInfo promocode = createPromocodeInfo().withForNewClientsOnly(false);
        restrictedDomain = "direct.yandex.ru";
        when(antiFraudService.domainHasStat(restrictedDomain)).thenReturn(true);

        service.processNotification(request.withPromocodes(singletonList(convertToBalancePromocodeInfo(promocode))));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(singletonList(promocode))
                .withRestrictedDomain(restrictedDomain)
                .withLastChange(now);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Тесткейс 27, смешанный сценарий: клиент с уникальным доменом, без использованных ранее промокодов.
     * На домене есть статистика, один промокод не требует новизны клиента — withForNewClientsOnly(false),
     * другой промокод требует новизны.
     * Получаем информацию об оплате с промокодом, подходящим под наши условия.
     * Добавляется новая ограничивающая запись, один промокод отрывается.
     */
    @Test
    public void uniqueDomainWithoutPromocodes_oneNotForNewClientsOnly_hasStat_singleApplicablePromocode_oneAdded() {
        PromocodeInfo promocode = createPromocodeInfo().withForNewClientsOnly(true);
        PromocodeInfo promocode2 = createPromocodeInfo();
        restrictedDomain = DOMAIN;
        when(antiFraudService.domainHasStat(restrictedDomain)).thenReturn(true);

        service.processNotification(request.withPromocodes(
                List.of(convertToBalancePromocodeInfo(promocode), convertToBalancePromocodeInfo(promocode2))));

        CampPromocodes newRecord = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(List.of(promocode))
                .withRestrictedDomain(DOMAIN)
                .withLastChange(now);
        CampPromocodes newRecord2 = new CampPromocodes()
                .withCampaignId(CAMPAIGN_ID)
                .withPromocodes(List.of(promocode2))
                .withRestrictedDomain(DOMAIN)
                .withLastChange(now);
        verify(promocodesRepository, never()).addCampaignPromocodes(SHARD, newRecord);
        verify(promocodesRepository).addCampaignPromocodes(SHARD, newRecord2);
        verify(promocodesRepository, never()).deleteCampaignPromocodes(anyInt(), anyLong());
        verify(promocodesRepository, never()).updateCampaignPromocodesList(anyInt(), anyLong(), anyList());
        verify(antiFraudService).tearOffPromocodes(SERVICE_ID, CAMPAIGN_ID, List.of(promocode),
                TearOffReason.HAS_IMPRESSIONS);
        verify(antiFraudService, never()).tearOffPromocodes(eq(SERVICE_ID), eq(CAMPAIGN_ID), eq(List.of(promocode2)),
                any(TearOffReason.class));
    }
}
