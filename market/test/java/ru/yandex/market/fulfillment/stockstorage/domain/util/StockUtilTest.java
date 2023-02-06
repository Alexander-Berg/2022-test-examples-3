package ru.yandex.market.fulfillment.stockstorage.domain.util;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StockUtilTest {

    /**
     * Простой тест. Если нет стоков - не доступно
     */
    @Test
    public void isAvailableNoStock() {
        Sku sku = new Sku();
        assertFalse(StockUtil.isAvailable(sku));
    }

    /**
     * Простой тест. Если есть FIT сток без фризов - доступно
     */
    @Test
    public void isAvailableFitNoFreeze() {
        Sku sku = new Sku();
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(10);
        fit.setFreezeAmount(0);
        assertTrue(StockUtil.isAvailable(sku));
    }

    /**
     * Простой тест. Если фит сток 0 - не доступно
     */
    @Test
    public void isAvailableFit0() {
        Sku sku = new Sku();
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(0);
        fit.setFreezeAmount(0);
        assertFalse(StockUtil.isAvailable(sku));
    }

    /**
     * Простой тест. Если фит - фриз <= 0  - не доступно
     */
    @Test
    public void isAvailableFitFreezeLess0() {
        Sku eq = new Sku();
        Stock eqfit = Stock.createStock(StockType.FIT, eq);
        eqfit.setAmount(10);
        eqfit.setFreezeAmount(10);

        Sku ls = new Sku();
        Stock lsfit = Stock.createStock(StockType.FIT, ls);
        lsfit.setAmount(10);
        lsfit.setFreezeAmount(10);

        assertFalse(StockUtil.isAvailable(ls));
        assertFalse(StockUtil.isAvailable(ls));
    }

    /**
     * Простой тест. Если фит - фриз > 0  - доступно
     */
    @Test
    public void isAvailableFitFreezeMore0() {
        Sku sku = new Sku();
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(10);
        fit.setFreezeAmount(5);
        assertTrue(StockUtil.isAvailable(sku));
    }

    /**
     * Предзаказный тест. Если есть PREORDER сток без фризов - доступно
     */
    @Test
    public void isAvailablePreorderNoFreeze() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(10);
        assertTrue(StockUtil.isAvailable(sku));
    }

    /**
     * Предзаказный тест. Если PREORDER сток 0 - не доступно
     */
    @Test
    public void isAvailablePreorder0() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(0);
        assertFalse(StockUtil.isAvailable(sku));
    }

    /**
     * Предзаказный тест. Если PREORDER - фриз <= 0  - не доступно
     */
    @Test
    public void isAvailablePreorderFreezeLess0() {
        Sku eq = new Sku();
        Stock eqpreorder = Stock.createStock(StockType.PREORDER, eq);
        eqpreorder.setAmount(10);
        eqpreorder.setFreezeAmount(10);

        Sku ls = new Sku();
        Stock lspreorder = Stock.createStock(StockType.PREORDER, ls);
        lspreorder.setAmount(10);
        lspreorder.setFreezeAmount(10);

        assertFalse(StockUtil.isAvailable(ls));
        assertFalse(StockUtil.isAvailable(ls));
    }

    /**
     * Предзаказный тест. Если фит - фриз > 0  - доступно
     */
    @Test
    public void isAvailablePreorderFreezeMore0() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(10);
        preorder.setFreezeAmount(5);
        assertTrue(StockUtil.isAvailable(sku));
    }

    /**
     * Есть PREORDER > 0 и FIT. По преордеру доступно, по фиту недоступно - доступно
     */
    @Test
    public void isAvailablePreorderAvailableFitNotAvailable() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(10);
        preorder.setFreezeAmount(0);
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(0);
        fit.setFreezeAmount(10);

        assertTrue(StockUtil.isAvailable(sku));
    }

    /**
     * Есть PREORDER > 0 и FIT. По преордеру недоступно, по фиту недоступно - недоступно
     */
    @Test
    public void isAvailablePreorderNotAvailableFitNotAvailable() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(10);
        preorder.setFreezeAmount(15);
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(10);
        fit.setFreezeAmount(10);

        assertFalse(StockUtil.isAvailable(sku));
    }

    /**
     * Есть PREORDER > 0 и FIT. По преордеру недоступно, по фиту доступно - недоступно
     */
    @Test
    public void isAvailablePreorderNotAvailableFitAvailable() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(10);
        preorder.setFreezeAmount(15);
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(10);
        fit.setFreezeAmount(5);

        assertFalse(StockUtil.isAvailable(sku));
    }

    /**
     * Есть PREORDER > 0 и FIT. По преордеру доступно, по фиту доступно - доступно
     */
    @Test
    public void isAvailablePreorderAvailableFitAvailable() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(10);
        preorder.setFreezeAmount(5);
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(10);

        assertTrue(StockUtil.isAvailable(sku));
    }

    /**
     * Есть PREORDER = 0 и FIT. По преордеру недоступно, по фиту недоступно - недоступно
     */
    @Test
    public void isAvailablePreorderEmptyFitNotAvailable() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(0);
        preorder.setFreezeAmount(5);
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(10);
        fit.setFreezeAmount(15);

        assertFalse(StockUtil.isAvailable(sku));
    }

    /**
     * Есть PREORDER = 0 и FIT. По преордеру недоступно, по фиту доступно - доступно
     */
    @Test
    public void isAvailablePreorderEmptyFitAvailable() {
        Sku sku = new Sku();
        Stock preorder = Stock.createStock(StockType.PREORDER, sku);
        preorder.setAmount(0);
        Stock fit = Stock.createStock(StockType.FIT, sku);
        fit.setAmount(10);

        assertTrue(StockUtil.isAvailable(sku));
    }
}
