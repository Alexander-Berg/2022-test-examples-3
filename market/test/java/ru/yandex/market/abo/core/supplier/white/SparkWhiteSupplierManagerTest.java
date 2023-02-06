package ru.yandex.market.abo.core.supplier.white;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.hiding.rules.white.WhiteOfferHidingRuleService;
import ru.yandex.market.abo.core.spark.SparkManager;
import ru.yandex.market.abo.core.spark.api.SparkApiDataLoader;
import ru.yandex.market.abo.core.spark.dao.SparkService;
import ru.yandex.market.abo.core.spark.model.SparkCheckResult;
import ru.yandex.market.abo.core.spark.model.SparkStatusCheckResult;
import ru.yandex.market.abo.core.spark.status.SparkStatusService;
import ru.yandex.market.abo.core.supplier.white.model.WhiteSupplier;
import ru.yandex.market.abo.core.supplier.white.service.WhiteSupplierService;
import ru.yandex.market.abo.core.supplier.white.service.WhiteSupplierShopsYtIdxLoader;
import ru.yandex.market.abo.core.supplier.white.service.WhiteSupplierStTicketCreator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.01.2020
 */
class SparkWhiteSupplierManagerTest {
    private static final String TEST_OGRN = "1234567890123";
    private static final long TEST_SHOP_ID = 123L;
    private static final int CHECK_STATUS_CODE = 38;

    private static final WhiteSupplier TEST_WHITE_SUPPLIER = new WhiteSupplier(TEST_OGRN);
    private static final List<Long> WHITE_SUPPLIER_SHOPS = List.of(TEST_SHOP_ID);

    @Mock
    private SparkStatusCheckResult sparkStatusCheckResult;

    @Mock
    private SparkManager sparkManager;
    @Mock
    private WhiteSupplierShopsYtIdxLoader whiteSupplierShopsYtIdxLoader;
    @Mock
    private WhiteOfferHidingRuleService whiteOfferHidingRuleService;
    @Mock
    private SparkService sparkService;
    @Mock
    private SparkStatusService sparkStatusService;
    @Mock
    private WhiteSupplierStTicketCreator whiteSupplierStTicketCreator;
    @Mock
    private WhiteSupplierService whiteSupplierService;
    @Mock
    private SparkApiDataLoader sparkApiDataLoader;

    @InjectMocks
    private SparkWhiteSupplierManager sparkWhiteSupplierManager;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(whiteSupplierShopsYtIdxLoader.loadYesterdayWhiteSupplierShops()).thenReturn(testSupplierOgrnShops());
        when(sparkManager.checkStatus(TEST_OGRN)).thenReturn(sparkStatusCheckResult);
        when(sparkStatusCheckResult.getStatusCode()).thenReturn(CHECK_STATUS_CODE);
        when(sparkApiDataLoader.isDisabled()).thenReturn(false);
    }

    @Test
    void checkIgnoreActiveSuppliersTest() {
        when(sparkStatusCheckResult.getSparkCheckResult()).thenReturn(SparkCheckResult.ACTIVE);
        sparkWhiteSupplierManager.checkStatus();
        verify(whiteOfferHidingRuleService, never()).addIfNotExistsOrDeleted(any(), anyLong());
        verify(sparkStatusService, never()).saveStatusCheck(any());
    }

    @Test
    void checkNonActiveSupplierSavesToBlackListTest() {
        when(sparkStatusCheckResult.getSparkCheckResult()).thenReturn(SparkCheckResult.NOT_ACTIVE);
        sparkWhiteSupplierManager.checkStatus();
        verify(whiteOfferHidingRuleService, times(1))
                .addIfNotExistsOrDeleted(any(), anyLong());
        verify(sparkStatusService, times(1)).saveStatusCheck(any());
    }

    private static Multimap<WhiteSupplier, Long> testSupplierOgrnShops() {
        Multimap<WhiteSupplier, Long> result = ArrayListMultimap.create();
        result.putAll(TEST_WHITE_SUPPLIER, WHITE_SUPPLIER_SHOPS);
        return result;
    }
}
