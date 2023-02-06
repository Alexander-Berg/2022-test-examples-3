package ru.yandex.market.antifraud.orders.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import ru.yandex.market.antifraud.orders.cache.async.CacheBuilder;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.util.concurrent.ThreadPoolRegistry;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.domain.UidType;
import ru.yandex.market.sdk.userinfo.service.ResolveUidService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlacklistServiceTest {

    private static final String CANCEL_ORDER_ACTION = Utils.getBlacklistAction(AntifraudAction.CANCEL_ORDER);

    @Mock
    private CacheManager cacheManager;
    @Mock
    private AntifraudDao antifraudDao;
    @Mock
    private ResolveUidService resolveUidService;

    private BlacklistService service;

    @Before
    public void setUp() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(eq("blacklist_cache"))).thenReturn(cache);
        when(resolveUidService.resolve(1L)).thenReturn(new Uid(1L, UidType.MUID, false));
        service = new BlacklistService(
                antifraudDao,
                new CacheBuilder(cacheManager),
                mock(ThreadPoolRegistry.class),
                resolveUidService);
    }

    @Test
    public void getAntifraudRules() {
        Date expiryAt = Date.valueOf(LocalDate.now().plusYears(100));
        List<Pair<AntifraudBlacklistRuleType, String>> valuesToCheck = List.of(
                new ImmutablePair<>(AntifraudBlacklistRuleType.UID, "1"),
                new ImmutablePair<>(AntifraudBlacklistRuleType.UUID, "2"));
        when(antifraudDao.getAntifraudRestrictions(valuesToCheck)).thenReturn(List.of(buildRule(expiryAt)));
        List<AntifraudBlacklistRule> rules =
                service.getAntifraudRules(List.of(MarketUserId.fromUid(1L), MarketUserId.fromUuid("2")));
        assertThat(rules).hasSize(1);
        verify(antifraudDao).getAntifraudRestrictions(valuesToCheck);
    }

    @Test
    public void saveRule() {
        Date expiryAt = Date.valueOf(LocalDate.now().plusYears(100));
        service.saveBlacklistRule(buildRule(expiryAt));

        ArgumentCaptor<AntifraudBlacklistRule> captor = ArgumentCaptor.forClass(AntifraudBlacklistRule.class);
        verify(antifraudDao).saveBlacklistRule(captor.capture());
        AntifraudBlacklistRule rule = captor.getValue();
        assertThat(rule.getExpiryAt()).isEqualTo(expiryAt);
    }

    @Test
    public void saveRuleWithoutExpiry() {
        service.saveBlacklistRule(buildRuleWithoutExpiry());

        ArgumentCaptor<AntifraudBlacklistRule> captor = ArgumentCaptor.forClass(AntifraudBlacklistRule.class);
        verify(antifraudDao).saveBlacklistRule(captor.capture());
        AntifraudBlacklistRule rule = captor.getValue();
        assertThat(rule.getExpiryAt()).isEqualTo(AntifraudDao.DEFAULT_EXPIRE_DATE);
    }

    @Test
    public void saveRuleWithIncorrectExpiry() {
        service.saveBlacklistRule(buildRule(Date.valueOf(LocalDate.now().minusDays(1))));

        ArgumentCaptor<AntifraudBlacklistRule> captor = ArgumentCaptor.forClass(AntifraudBlacklistRule.class);
        verify(antifraudDao).saveBlacklistRule(captor.capture());
        AntifraudBlacklistRule rule = captor.getValue();
        assertThat(rule.getExpiryAt()).isEqualTo(AntifraudDao.DEFAULT_EXPIRE_DATE);
    }

    private AntifraudBlacklistRule buildRuleWithoutExpiry() {
        return buildRule(null);
    }

    private AntifraudBlacklistRule buildRule(Date expiryAt) {
        return AntifraudBlacklistRule.builder()
                .type(AntifraudBlacklistRuleType.UID)
                .value("1")
                .action(CANCEL_ORDER_ACTION)
                .expiryAt(expiryAt)
                .build();
    }
}
