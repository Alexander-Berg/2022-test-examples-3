package ru.yandex.direct.logicprocessor.processors.promocodescheckcampaignchanges;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.promocodes.model.CampPromocodes;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.entity.promocodes.model.TearOffReason;
import ru.yandex.direct.core.entity.promocodes.repository.CampPromocodesRepository;
import ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService;
import ru.yandex.direct.ess.logicobjects.promocodescheckcampaignchanges.IdTypeEnum;
import ru.yandex.direct.ess.logicobjects.promocodescheckcampaignchanges.PromocodesCheckCampaignChangesObject;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


class PromocodesCheckCampaignChangesServiceTest {

    private static final int SHARD = 1;
    private static final int DIRECT_SERVICE_ID = 7;
    private static final String DOMAIN_1 = "original.domain.ru";
    private static final String DOMAIN_2 = "modified.domain.ru";
    private static final LocalDateTime PROMOCODE_ACTIVATED_AT = LocalDateTime.now();

    private PromocodesCheckCampaignChangesService promocodesCheckCampaignChangesService;

    @Mock
    private CampPromocodesRepository campPromocodesRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private PromocodesAntiFraudService antiFraudService;

    @BeforeEach
    void before() {
        initMocks(this);

        promocodesCheckCampaignChangesService = new PromocodesCheckCampaignChangesService(campPromocodesRepository,
                campaignRepository, adGroupRepository, antiFraudService, DIRECT_SERVICE_ID);
    }

    /**
     * Кампания с кошельком и промокодом (промокод активирован после "граничной даты").
     * На входе: pid группы динамических баннеров кампании.
     * По pid находим cid кампании, находим cid кошелька, запускаем проверку.
     * У кампании изменен домен, поэтому отрываем промокод.
     */
    @Test
    void testPromocodesChecks_CampaignChangeByPid_PromocodeTearedOffOnce() {
        PromocodesCheckCampaignChangesObject object1 = new PromocodesCheckCampaignChangesObject(1L, IdTypeEnum.PID);

        doReturn(Map.of(1L, 2L)).when(adGroupRepository).getCampaignIdsByAdGroupIds(SHARD,
                singletonSet(1L));
        doReturn(Map.of(2L, 3L)).when(campaignRepository).getWalletIdsByCampaingIds(SHARD,
                singletonSet(2L));

        CampPromocodes campPromocodes = makeCampPromocodeWithDomain(3L, DOMAIN_1);
        doReturn(campPromocodes).when(campPromocodesRepository).getCampaignPromocodes(SHARD, 3L);

        doReturn(DOMAIN_2).when(antiFraudService).determineRestrictedDomain(3L);

        doReturn(PROMOCODE_ACTIVATED_AT.minusMonths(1)).when(antiFraudService).getAntiFraudStartDate();

        var badCids = promocodesCheckCampaignChangesService.processObjects(SHARD, singletonList(object1));
        assumeThat("Коллекция badCids должна быть пустая", badCids, empty());

        verify(antiFraudService).tearOffPromocodes(DIRECT_SERVICE_ID, 3L, campPromocodes.getPromocodes(),
                TearOffReason.CAMPAIGN_CHANGES);
    }

    /**
     * Кампания БЕЗ кошелька, но с промокодом (промокод активирован после "граничной даты").
     * На входе: pid группы динамических баннеров кампании.
     * По pid находим cid кампании, пытаемся найти cid кошелька, получаем 0L, используем cid кампании. Запускаем
     * проверку.
     * У кампании изменен домен, поэтому отрываем промокод.
     */
    @Test
    void testPromocodesChecks_CampaignWithoutWalletChangeByPid_PromocodeTearedOffOnce() {
        PromocodesCheckCampaignChangesObject object1 = new PromocodesCheckCampaignChangesObject(1L, IdTypeEnum.PID);

        doReturn(Map.of(1L, 2L)).when(adGroupRepository).getCampaignIdsByAdGroupIds(SHARD,
                singletonSet(1L));
        doReturn(Map.of(2L, 0L)).when(campaignRepository).getWalletIdsByCampaingIds(SHARD,
                singletonSet(2L));

        CampPromocodes campPromocodes = makeCampPromocodeWithDomain(2L, DOMAIN_1);
        doReturn(campPromocodes).when(campPromocodesRepository).getCampaignPromocodes(SHARD, 2L);

        doReturn(DOMAIN_2).when(antiFraudService).determineRestrictedDomain(2L);

        doReturn(PROMOCODE_ACTIVATED_AT.minusMonths(1)).when(antiFraudService).getAntiFraudStartDate();

        var badCids = promocodesCheckCampaignChangesService.processObjects(SHARD, singletonList(object1));
        assumeThat("Коллекция badCids должна быть пустая", badCids,
                empty());

        verify(antiFraudService).tearOffPromocodes(DIRECT_SERVICE_ID, 2L, campPromocodes.getPromocodes(),
                TearOffReason.CAMPAIGN_CHANGES);
    }


    /**
     * Кампания с кошельком и промокодом (промокод активирован после "граничной даты").
     * На входе: cid кампании.
     * По cid кампании, находим cid кошелька, запускаем проверку.
     * У кампании изменен домен, поэтому отрываем промокод.
     */
    @Test
    void testPromocodesChecks_CampaignChangeByCid_PromocodeTearedOffOnce() {
        PromocodesCheckCampaignChangesObject object1 = new PromocodesCheckCampaignChangesObject(2L, IdTypeEnum.CID);

        doReturn(Map.of(2L, 3L)).when(campaignRepository).getWalletIdsByCampaingIds(SHARD,
                singletonSet(2L));

        CampPromocodes campPromocodes = makeCampPromocodeWithDomain(3L, DOMAIN_1);
        doReturn(campPromocodes).when(campPromocodesRepository).getCampaignPromocodes(SHARD, 3L);

        doReturn(DOMAIN_2).when(antiFraudService).determineRestrictedDomain(3L);

        doReturn(PROMOCODE_ACTIVATED_AT.minusMonths(1)).when(antiFraudService).getAntiFraudStartDate();

        var badCids = promocodesCheckCampaignChangesService.processObjects(SHARD, singletonList(object1));
        assumeThat("Коллекция badCids должна быть пустая", badCids,
                empty());

        verify(antiFraudService).tearOffPromocodes(DIRECT_SERVICE_ID, 3L, campPromocodes.getPromocodes(),
                TearOffReason.CAMPAIGN_CHANGES);
    }

    /**
     * Кампания с кошельком и промокодом (промокод активирован ДО "граничной даты").
     * На входе: cid кампании.
     * По cid кампании, находим cid кошелька, запускаем проверку.
     * У кампании изменен домен, однако промокод НЕ отрываем (потому что слишком старый промокод).
     */
    @Test
    void testPromocodesChecks_CampaignChangeByCid_OldPromocodeShouldNotBeTearedOff() {
        PromocodesCheckCampaignChangesObject object1 = new PromocodesCheckCampaignChangesObject(2L, IdTypeEnum.CID);

        doReturn(Map.of(2L, 3L)).when(campaignRepository).getWalletIdsByCampaingIds(SHARD,
                singletonList(2L));

        doReturn(makeCampPromocodeWithDomain(3L, DOMAIN_1))
                .when(campPromocodesRepository).getCampaignPromocodes(SHARD, 3L);

        doReturn(DOMAIN_2).when(antiFraudService).determineRestrictedDomain(3L);

        doReturn(PROMOCODE_ACTIVATED_AT.plusMonths(1)).when(antiFraudService).getAntiFraudStartDate();

        var badCids = promocodesCheckCampaignChangesService.processObjects(SHARD, Arrays.asList(object1));
        assumeThat("Коллекция badCids должна быть пустая", badCids,
                empty());

        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    /**
     * Три кампании, все с кошельком и промокодом (промокод активирован после "граничной даты").
     * На входе: pid 1-ой кампании, cid 2-ой кампании и cid 3-ей кампании.
     * По pid кампании находим cid кампании (для первой). Далее для всех,
     * по cid кампании, находим cid кошелька, запускаем проверку.
     * У кампании 1 изменен домен, поэтому промокод отрываем.
     * У кампании 2 изменен домен, поэтому промокод отрываем.
     * У кампании 3 НЕ изменен домен, поэтому промокод НЕ отрываем.
     */
    @Test
    void testPromocodesChecks_CampaignChangeByPidAndCid_PromocodeTearedOffTwice() {
        PromocodesCheckCampaignChangesObject object1 = new PromocodesCheckCampaignChangesObject(1L, IdTypeEnum.PID);
        PromocodesCheckCampaignChangesObject object2 = new PromocodesCheckCampaignChangesObject(20L, IdTypeEnum.CID);
        PromocodesCheckCampaignChangesObject object3 = new PromocodesCheckCampaignChangesObject(200L, IdTypeEnum.CID);

        doReturn(Map.of(1L, 2L)).when(adGroupRepository).getCampaignIdsByAdGroupIds(SHARD,
                singletonSet(1L));

        doReturn(Map.of(2L, 3L, 20L, 30L, 200L, 300L)).when(campaignRepository).getWalletIdsByCampaingIds(SHARD,
                Set.of(2L, 20L, 200L));

        var campPromocodes1 = makeCampPromocodeWithDomain(3L, DOMAIN_1);
        doReturn(campPromocodes1).when(campPromocodesRepository).getCampaignPromocodes(SHARD, 3L);

        var campPromocodes2 = makeCampPromocodeWithDomain(30L, DOMAIN_1);
        doReturn(campPromocodes2).when(campPromocodesRepository).getCampaignPromocodes(SHARD, 30L);

        var campPromocodes3 = makeCampPromocodeWithDomain(300L, DOMAIN_1);
        doReturn(campPromocodes3)
                .when(campPromocodesRepository).getCampaignPromocodes(SHARD, 300L);

        doReturn(DOMAIN_2).when(antiFraudService).determineRestrictedDomain(3L);
        doReturn(DOMAIN_2).when(antiFraudService).determineRestrictedDomain(30L);
        doReturn(DOMAIN_1).when(antiFraudService).determineRestrictedDomain(300L);

        doReturn(PROMOCODE_ACTIVATED_AT.minusMonths(1)).when(antiFraudService).getAntiFraudStartDate();

        var badCids = promocodesCheckCampaignChangesService.processObjects(SHARD, Arrays.asList(object1, object2,
                object3));
        assumeThat("Коллекция badCids должна быть пустая", badCids,
                empty());

        verify(antiFraudService).tearOffPromocodes(DIRECT_SERVICE_ID, 3L, campPromocodes1.getPromocodes(),
                TearOffReason.CAMPAIGN_CHANGES);
        verify(antiFraudService).tearOffPromocodes(DIRECT_SERVICE_ID, 30L, campPromocodes2.getPromocodes(),
                TearOffReason.CAMPAIGN_CHANGES);
        verify(antiFraudService, never()).tearOffPromocodes(anyInt(),
                eq(300L),
                anyList(),
                any(TearOffReason.class));
    }

    /**
     * Кампания БЕЗ кошелька, с промокодом (промокод активирован ДО "граничной даты").
     * На входе: cid кампании.
     * По cid кампании, пытаемся найти cid кошелька, получаем 0L, используем cid кампании. Запускаем проверку.
     * У кампании изменен домен, поэтому промокод отрываем.
     */
    @Test
    void testPromocodesChecks_CampaignChangeByCid_CampaignHasNoWalletId() {
        PromocodesCheckCampaignChangesObject object1 = new PromocodesCheckCampaignChangesObject(2L, IdTypeEnum.CID);

        doReturn(Map.of(2L, 0L)).when(campaignRepository).getWalletIdsByCampaingIds(SHARD,
                singletonSet(2L));

        CampPromocodes campPromocodes = makeCampPromocodeWithDomain(2L, DOMAIN_1);
        doReturn(campPromocodes).when(campPromocodesRepository).getCampaignPromocodes(SHARD, 2L);

        doReturn(DOMAIN_2).when(antiFraudService).determineRestrictedDomain(2L);

        doReturn(PROMOCODE_ACTIVATED_AT.minusMonths(1)).when(antiFraudService).getAntiFraudStartDate();

        var badCids = promocodesCheckCampaignChangesService.processObjects(SHARD, singletonList(object1));
        assumeThat("Коллекция badCids должна быть пустая", badCids,
                empty());

        verify(antiFraudService).tearOffPromocodes(DIRECT_SERVICE_ID, 2L, campPromocodes.getPromocodes(),
                TearOffReason.CAMPAIGN_CHANGES);
    }

    /**
     * Кампания БЕЗ кошелька, БЕЗ промокода.
     * На входе: cid кампании.
     * По cid кампании, пытаемся найти cid кошелька, получаем 0L, используем cid кампании. Запускаем проверку.
     * С кампанией ничего не происходит.
     */
    @Test
    void testPromocodesChecks_CampaignWithoutPromocode() {
        PromocodesCheckCampaignChangesObject object1 = new PromocodesCheckCampaignChangesObject(2L, IdTypeEnum.CID);

        doReturn(Map.of(2L, 0L)).when(campaignRepository).getWalletIdsByCampaingIds(SHARD,
                singletonList(2L));

        doReturn(null).when(campPromocodesRepository).getCampaignPromocodes(SHARD, 2L);

        var badCids = promocodesCheckCampaignChangesService.processObjects(SHARD, singletonList(object1));
        assumeThat("Коллекция badCids должна быть пустая", badCids, empty());

        verify(antiFraudService, never()).tearOffPromocodes(anyInt(), anyLong(), anyList(),
                any(TearOffReason.class));
    }

    private CampPromocodes makeCampPromocodeWithDomain(Long campaignId, String domain) {
        CampPromocodes campPromocodes = new CampPromocodes()
                .withCampaignId(campaignId)
                .withRestrictedDomain(domain)
                .withPromocodes(singletonList(new PromocodeInfo()
                        .withInvoiceEnabledAt(PROMOCODE_ACTIVATED_AT)
                ));
        return campPromocodes;
    }
}
