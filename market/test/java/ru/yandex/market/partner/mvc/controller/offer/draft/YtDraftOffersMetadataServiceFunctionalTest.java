package ru.yandex.market.partner.mvc.controller.offer.draft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.cache.memcached.cacheable.ServiceMethodMemCacheable;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Функциональные тесты на {@link YtDraftOffersMetadataService}
 */
public class YtDraftOffersMetadataServiceFunctionalTest extends FunctionalTest {

    private static final Long SUPPLIER_ID = 774L;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private YtDraftOffersMetadataService service = null;

    @BeforeEach
    void setUp() {
        MemCachingService memCachingService = mock(MemCachingService.class);
        doAnswer(invocation -> {
            ServiceMethodMemCacheable<Long, Long> getOffersCountOp = invocation.getArgument(0);
            Long partnerId = invocation.getArgument(1);
            return getOffersCountOp.queryNonCached(partnerId);
        }).when(memCachingService).query(any(), any());

        service = new YtDraftOffersMetadataService(
                memCachingService,
                new MemCachedServiceConfig(),
                namedParameterJdbcTemplate);
    }

    @Test
    @DbUnitDataSet(before = "YtDraftOffersMetadataServiceFunctionalTest.queryNotCachedTest.before.csv")
    public void queryNotCachedTest() {
        DraftOffersMetadataDTO dto = service.getOffersCountForSupplier(SUPPLIER_ID);
        Assertions.assertEquals(SUPPLIER_ID, dto.getSupplierId());
        Assertions.assertEquals(11L, dto.getOffersCount());
    }

    @Test
    @DbUnitDataSet(before = "YtDraftOffersMetadataServiceFunctionalTest.queryNotCachedFromWhiteTest.before.csv")
    public void queryNotCachedFromWhiteTest() {
        DraftOffersMetadataDTO dto = service.getOffersCountForSupplier(SUPPLIER_ID);
        Assertions.assertEquals(SUPPLIER_ID, dto.getSupplierId());
        Assertions.assertEquals(12L, dto.getOffersCount());
    }

    @Test
    @DbUnitDataSet(before = "YtDraftOffersMetadataServiceFunctionalTest.queryNotCachedFromWhiteRepeatedTest.before.csv")
    public void queryNotCachedFromWhiteRepeatedTest() {
        DraftOffersMetadataDTO dto = service.getOffersCountForSupplier(SUPPLIER_ID);
        Assertions.assertEquals(SUPPLIER_ID, dto.getSupplierId());
        Assertions.assertEquals(14L, dto.getOffersCount());
    }
}
