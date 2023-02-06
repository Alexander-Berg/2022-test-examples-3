package ru.yandex.market.abo.core.supplier.white.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.shop.org.ShopOrgService;
import ru.yandex.market.abo.core.spark.model.SparkCheckResult;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.supplier.white.model.WhiteSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.01.2020
 */
class WhiteSupplierStTicketCreatorTest {
    private static String TEST_SUPPLIER_OGRN = "1234567890123";
    private static String TEST_SHOP_OGRN = "1234567891123";
    private static final long TEST_SHOP_ID = 123L;
    private static final String TEST_SHOP_NAME = "test1";

    private static final WhiteSupplier TEST_WHITE_SUPPLIER = new WhiteSupplier(TEST_SUPPLIER_OGRN);

    private static final String EXPECTED_TICKET_BODY = "" +
            "Поставщики не найдены в СПАРК:\n" +
            "Идентификатор поставщика: 1234567890123 Магазин: 123 test1 1234567891123";

    @Mock
    private ShopInfo shopInfo;

    @Mock
    private StartrekTicketManager startrekTicketManager;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private ShopOrgService shopOrgService;

    @InjectMocks
    private WhiteSupplierStTicketCreator whiteSupplierStTicketCreator;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(shopInfoService.getShopInfo(TEST_SHOP_ID)).thenReturn(shopInfo);
        when(shopOrgService.loadOgrnByShop(TEST_SHOP_ID)).thenReturn(TEST_SHOP_OGRN);
        when(shopInfo.getSimpleName()).thenReturn(TEST_SHOP_NAME);
    }

    @Test
    void createTicketDescriptionTest() {
        String actualTicketBody = whiteSupplierStTicketCreator
                .createTicketDescription(testOgrnCheckResults(), testSupplierOgrnShops());
        assertEquals(EXPECTED_TICKET_BODY, actualTicketBody);
    }

    private static Multimap<SparkCheckResult, WhiteSupplier> testOgrnCheckResults() {
        Multimap<SparkCheckResult, WhiteSupplier> result = ArrayListMultimap.create();
        result.put(SparkCheckResult.DATA_NOT_FOUND, TEST_WHITE_SUPPLIER);
        return result;
    }

    private static Multimap<WhiteSupplier, Long> testSupplierOgrnShops() {
        Multimap<WhiteSupplier, Long> result = ArrayListMultimap.create();
        result.put(TEST_WHITE_SUPPLIER, TEST_SHOP_ID);
        return result;
    }
}
