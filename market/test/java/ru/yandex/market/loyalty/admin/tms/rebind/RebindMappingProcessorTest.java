package ru.yandex.market.loyalty.admin.tms.rebind;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.gdpr.RebindMapping;
import ru.yandex.market.loyalty.core.gdpr.RebindMappingDao;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.service.coin.CoinLifecycleService;
import ru.yandex.market.loyalty.core.service.coin.CoinSearchService;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RebindMappingProcessorTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    RebindMappingDao rebindMappingDao;
    @Autowired
    TransactionTemplate transactionTemplate;

    private CoinSearchService coinSearchServiceMock;
    private RebindCoinMappingProcessor rebindMappingService;

    private final List<RebindMapping> unprocessedMappings = ImmutableList.of(
            new RebindMapping(1, 2),
            new RebindMapping(3, 4)
    );

    @Before
    public void initRebindProcessor() {
        CoinLifecycleService coinLifecycleServiceMock = mock(CoinLifecycleService.class);
        coinSearchServiceMock = mock(CoinSearchService.class);
        configurationService.set(RebindCoinMappingProcessor.COIN_REBIND_ENABLED, true);
        rebindMappingService = new RebindCoinMappingProcessor(
                rebindMappingDao,
                coinLifecycleServiceMock,
                coinSearchServiceMock,
                transactionTemplate,
                configurationService
        );
    }

    @Test
    public void shouldMarksMappingAsProcessedAfterRebind() {
        insertMappings(unprocessedMappings, 0);
        setupCoinSearchResult(Collections.emptyList());
        verifyUnprocessedCount(unprocessedMappings.size());

        rebindMappingService.rebindCoins();

        verifyUnprocessedCount(0);
    }

    @Test
    public void shouldIncreaseRetryCountOnFail() {
        insertMappings(unprocessedMappings, 0);
        setupCoinSearchResult(null);

        rebindMappingService.rebindCoins();

        unprocessedMappings.forEach(rebindMapping ->
                assertEquals(1, getRetryCount(rebindMapping)));
    }

    @Test
    public void shouldNotRebindCoinsWhenMappingRetryCountExceed() {
        insertMappings(unprocessedMappings, RebindMappingDao.MAX_RETRY_COUNT);

        rebindMappingService.rebindCoins();

        verify(coinSearchServiceMock, never()).getCoinsByUid(
                anyLong(),
                anyInt()
        );
    }

    private void setupCoinSearchResult(List<Coin> result) {
        unprocessedMappings.stream().map(RebindMapping::getFrom)
                .forEach(uid -> when(coinSearchServiceMock.getCoinsByUid(eq(uid), anyInt()))
                        .thenReturn(result));
    }

    private int getRetryCount(RebindMapping rebindMapping) {
        Integer retryCount = jdbcTemplate.queryForObject(
                "SELECT retry_count FROM gdpr_delete_user WHERE puid = ? AND muid = ?",
                Integer.class,
                rebindMapping.getFrom(), rebindMapping.getTo()
        );
        assertNotNull(retryCount);
        return retryCount;
    }

    private void insertMappings(List<RebindMapping> mappings, int retryCount) {
        mappings.forEach(rebindMapping ->
                jdbcTemplate.update(
                        "INSERT INTO gdpr_delete_user(puid, muid, retry_count) VALUES (?,?,?)",
                        rebindMapping.getFrom(),
                        rebindMapping.getTo(),
                        retryCount
                ));
    }

    private void verifyUnprocessedCount(int count) {
        assertThat(
                rebindMappingDao.getUnprocessedMappings(),
                hasSize(count)
        );
    }
}
