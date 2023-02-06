package ru.yandex.market.core.delivery.tariff.db.currency;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.currency.CurrencyConverterService;
import ru.yandex.market.core.delivery.service.ShopDeliveryCurrencyService;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(before = "ShopDeliveryCurrencyServiceTest.before.csv")
class ShopDeliveryCurrencyServiceTest extends FunctionalTest {

    @Autowired
    private ShopDeliveryCurrencyService shopDeliveryCurrencyService;

    @Autowired
    private CurrencyConverterService currencyConverterService;

    @Autowired
    private RegionService regionService;


    @BeforeEach
    void setUp() {
        Collection<Region> c = CollectionFactory.list(new Region(1, "test1", null), new Region(2, "test2", 1L),
                new Region(3, "test3", 2L), new Region(4, "test4", 3L),
                new Region(5, "test5", 4L), new Region(6, "test6", 5L),
                new Region(7, "test7", 6L), new Region(8, "test8", 7L),
                new Region(9, "test9", 8L)
        );
        doReturn(c).when(regionService).getRootBranchRegions(any());
    }


    /**
     * Изменение валюты доставки магазина
     */
    @Test
    @DbUnitDataSet(
            before = "ShopDeliveryCurrencyServiceTest.testUpdateShopDeliveryCurrency.before.csv",
            after = "ShopDeliveryCurrencyServiceTest.testUpdateShopDeliveryCurrency.after.csv"
    )
    void testUpdateShopDeliveryCurrency() {
        doReturn(new BigDecimal(5L)).when(currencyConverterService).getCurrenciesRatio(any(), any());

        shopDeliveryCurrencyService.updateShopCurrencyAndDeliveryCost(
                774L, Currency.EUR, 117L);
    }

    /**
     * Изменение локального региона
     */
    @Test
    @DbUnitDataSet(
            before = "ShopDeliveryCurrencyServiceTest.testUpdateShopLocalRegion.before.csv",
            after = "ShopDeliveryCurrencyServiceTest.testUpdateShopLocalRegion.after.csv"
    )
    void testUpdateShopLocalRegion() {
        doReturn(new BigDecimal(5L)).when(currencyConverterService).getCurrenciesRatio(Currency.RUR, Currency.AED);

        Map<Long, Pair<Currency, Bank>> currencyBank = new HashMap<>();
        currencyBank.put(4L, new Pair<>(Currency.AED, Bank.BUSD));
        doReturn(currencyBank).when(regionService).getRegionCurrencies(anyCollection());

        shopDeliveryCurrencyService.updateShopCurrencyAndDeliveryCost(
                774L, new BigDecimal(4L), 117L);
    }


    /**
     * Изменение локального региона для глобал магазина
     */
    @Test
    @DbUnitDataSet(
            before = "ShopDeliveryCurrencyServiceTest.testUpdateShopLocalRegionGlobal.before.csv",
            after = "ShopDeliveryCurrencyServiceTest.testUpdateShopLocalRegionGlobal.after.csv"
    )
    void testUpdateShopLocalRegionGlobal() {
        doReturn(new BigDecimal(5L)).when(currencyConverterService).getCurrenciesRatio(Currency.RUR, Currency.AED);
        doReturn(new BigDecimal(4L)).when(currencyConverterService).getCurrenciesRatio(Currency.RUR, Currency.USD);


        Map<Long, Pair<Currency, Bank>> currencyBank = new HashMap<>();
        currencyBank.put(4L, new Pair<>(Currency.USD, Bank.BUSD));
        doReturn(currencyBank).when(regionService).getRegionCurrencies(anyCollection());

        shopDeliveryCurrencyService.updateShopCurrencyAndDeliveryCost(
                774L, new BigDecimal(4L), 117L);
    }


    /**
     * несколько групп регионов с разными валютами,
     * при выполнении команды должны сконвертироваться в валюту
     * локального региона
     */
    @Test
    @DbUnitDataSet(
            before = "ShopDeliveryCurrencyServiceTest.testUpdateDifferentRegionGroups.before.csv",
            after = "ShopDeliveryCurrencyServiceTest.testUpdateDifferentRegionGroups.after.csv"
    )
    void testUpdateDifferentRegionGroups() {
        Map<Long, Pair<Currency, Bank>> currencyBank = new HashMap<>();
        currencyBank.put(1L, new Pair<>(Currency.RUR, Bank.YNDX));
        currencyBank.put(2L, new Pair<>(Currency.EUR, Bank.CBRF));
        currencyBank.put(3L, new Pair<>(Currency.AED, Bank.NBRB));
        currencyBank.put(5L, new Pair<>(Currency.USD, Bank.BUSD));
        doReturn(currencyBank).when(regionService).getRegionCurrencies(anyCollection());

        doReturn(new BigDecimal(0.5)).when(currencyConverterService).getCurrenciesRatio(Currency.EUR, Currency.RUR);
        doReturn(new BigDecimal(2)).when(currencyConverterService).getCurrenciesRatio(Currency.AED, Currency.RUR);

        shopDeliveryCurrencyService.updateShopRegionGroupToLocalRegion(
                774L, 117L);
    }

    /**
     * изменяем локальный регион
     * должны сконвертироваться все регионы в валюту
     * новоого локального региона
     */
    @Test
    @DbUnitDataSet(
            before = "ShopDeliveryCurrencyServiceTest.testUpdateDifferentRegionGroups.before.csv",
            after = "ShopDeliveryCurrencyServiceTest.testUpdateRegionGroupsAndLocal.after.csv"
    )
    void testUpdateRegionGroupsAndLocal() {
        Map<Long, Pair<Currency, Bank>> currencyBank = new HashMap<>();
        currencyBank.put(1L, new Pair<>(Currency.RUR, Bank.YNDX));
        currencyBank.put(2L, new Pair<>(Currency.EUR, Bank.CBRF));
        currencyBank.put(3L, new Pair<>(Currency.AED, Bank.NBRB));
        currencyBank.put(5L, new Pair<>(Currency.USD, Bank.BUSD));
        doReturn(currencyBank).when(regionService).getRegionCurrencies(anyCollection());

        doReturn(new BigDecimal(0.2)).when(currencyConverterService).getCurrenciesRatio(Currency.RUR, Currency.USD);
        doReturn(new BigDecimal(10)).when(currencyConverterService).getCurrenciesRatio(Currency.AED, Currency.USD);
        doReturn(new BigDecimal(1.5)).when(currencyConverterService).getCurrenciesRatio(Currency.EUR, Currency.USD);

        shopDeliveryCurrencyService.updateShopCurrencyAndDeliveryCost(
                774L, new BigDecimal(5L), 117L);
    }

}
