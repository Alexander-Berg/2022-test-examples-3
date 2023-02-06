package ru.yandex.market.billing.commission;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.core.billing.commission.DbSupplierCategoryFeeDao;
import ru.yandex.market.core.billing.commission.FeeForOrderItemNotFoundException;
import ru.yandex.market.core.billing.commission.OrderFeeFinder;
import ru.yandex.market.core.billing.commission.SupplierCategoryFee;
import ru.yandex.market.core.billing.commission.SupplierCategoryFeeCacheService;
import ru.yandex.market.core.category.CategoryWalker;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.order.model.MbiBlueOrderType;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link OrderFeeFinder}
 */
public class OrderFeeFinderTest {
    private SupplierCategoryFeeCacheService feeCachedService;

    private OrderFeeFinder feeFinder;

    private EnvironmentService environmentService;

    private DbSupplierCategoryFeeDao dbSupplierCategoryFeeDao;

    @Before
    public void init() {
        final CategoryWalker categoryWalker = mock(CategoryWalker.class);
        doReturn(Optional.empty()).when(categoryWalker).getParentHyperIdForHyperId(anyLong());

        dbSupplierCategoryFeeDao = mock(DbSupplierCategoryFeeDao.class);
        environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValue(eq("market.mbi-billing.cash_only_fee_enabled"), anyBoolean())).thenReturn(true);
    }

    @Test
    public void thereIsCategoryWithSupplier() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, 991L, 600,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(600, feeFinder.findFee(3L, 991L, BillingServiceType.FEE));
    }

    @Test
    public void thereIsParentCategoryWithSupplier() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(1L, 992L, 700,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(700, feeFinder.findFee(3L, 992L, BillingServiceType.FEE));
    }

    @Test
    public void thereIsCategoryWithNullSupplier() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, null, 800,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(800, feeFinder.findFee(3L, 993L, BillingServiceType.FEE));
    }

    @Test
    public void thereIsParentCategoryWithNullSupplier() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(1L, null, 900,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(900, feeFinder.findFee(3L, 994L, BillingServiceType.FEE));
    }

    @Test(expected = FeeForOrderItemNotFoundException.class)
    public void noRootCategoryWithNullSupplier() {

        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, 991L, 600,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCacheServiceForFee(supplierCategoryFee);

        feeFinder.findFee(3L, 994L, BillingServiceType.FEE);
    }

    @Test
    public void thereIsExpressWithSupplier() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, 991L, 200,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE, true);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(200, feeFinder.findFee(3L, 991L, BillingServiceType.FEE, true));
    }

    private void initFeeCacheServiceForFee(SupplierCategoryFee supplierCategoryFee) {
        List<SupplierCategoryFee> FEES = List.of(supplierCategoryFee);
        doReturn(FEES).when(dbSupplierCategoryFeeDao).getFee(any(), eq(MbiBlueOrderType.FULFILLMENT));

        feeCachedService = new SupplierCategoryFeeCacheService(dbSupplierCategoryFeeDao, LocalDate.now(), MbiBlueOrderType.FULFILLMENT);
        CategoryWalker categoryWalker = mock(CategoryWalker.class);
        feeFinder = new OrderFeeFinder(feeCachedService, categoryWalker);
        doReturn(Optional.of(1L)).when(categoryWalker).getParentHyperIdForHyperId(eq(2L));
        doReturn(Optional.of(2L)).when(categoryWalker).getParentHyperIdForHyperId(eq(3L));
    }

    @Test
    public void thereIsExpressWithNullSupplier() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, null, 200,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE, true);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(200, feeFinder.findFee(3L, 991L, BillingServiceType.FEE, true));
    }

    @Test
    public void thereIsExpressWithParentCategory() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(1L, 994L, 200,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE, true);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(200, feeFinder.findFee(3L, 994L, BillingServiceType.FEE, true));
    }

    /**
     * Мы получаем неэкспресс тариф для экспресс заказа, если экспресс тарифов для пары категория + поставщик
     * мы не находим
     */
    @Test
    public void thereIsExpressWithNoExpressTariff() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, 994L, 300,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE, false);
        initFeeCacheServiceForFee(supplierCategoryFee);

        assertEquals(300, feeFinder.findFee(3L, 994L, BillingServiceType.FEE, true));
    }

    @Test
    @DisplayName("Наличие тарифа по конкретной категории + поставщику")
    public void thereIsCategoryWithSupplierForCashOnlyFee() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, 991L, 600,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCachedServiceForCashOnlyFee(supplierCategoryFee);

        assertEquals(600, feeFinder.findCashOnlyFee(3L, 991L, BillingServiceType.FEE));
    }

    private void initFeeCachedServiceForCashOnlyFee(SupplierCategoryFee supplierCategoryFee) {
        List<SupplierCategoryFee> CASH_ONLY_FEES = List.of(supplierCategoryFee);
        doReturn(CASH_ONLY_FEES).when(dbSupplierCategoryFeeDao).getCashOnly(any(), eq(MbiBlueOrderType.FULFILLMENT));

        feeCachedService = new SupplierCategoryFeeCacheService(dbSupplierCategoryFeeDao, LocalDate.now(), MbiBlueOrderType.FULFILLMENT);
        CategoryWalker categoryWalker = mock(CategoryWalker.class);
        feeFinder = new OrderFeeFinder(feeCachedService, categoryWalker);
        doReturn(Optional.of(1L)).when(categoryWalker).getParentHyperIdForHyperId(eq(2L));
        doReturn(Optional.of(2L)).when(categoryWalker).getParentHyperIdForHyperId(eq(3L));
    }

    @Test
    @DisplayName("Наличие тарифа по родительской категории + поставщику")
    public void thereIsParentCategoryWithSupplierForCashOnlyFee() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(1L, 992L, 700,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCachedServiceForCashOnlyFee(supplierCategoryFee);
        assertEquals(700, feeFinder.findCashOnlyFee(3L, 992L, BillingServiceType.FEE));
    }

    @Test
    @DisplayName("Наличие тарифа по конкретной категории + без поставщика")
    public void thereIsCategoryWithNullSupplierForCashOnlyFee() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(3L, null, 800,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCachedServiceForCashOnlyFee(supplierCategoryFee);

        assertEquals(800, feeFinder.findCashOnlyFee(3L, 993L, BillingServiceType.FEE));
    }

    @Test
    @DisplayName("Наличие тарифа по родительской категории + без поставщика")
    public void thereIsParentCategoryWithNullSupplierForCashOnlyFee() {
        SupplierCategoryFee supplierCategoryFee = new SupplierCategoryFee(1L, null, 900,
                new Period(Instant.MIN, Instant.MAX), BillingServiceType.FEE);
        initFeeCachedServiceForCashOnlyFee(supplierCategoryFee);
        assertEquals(900, feeFinder.findCashOnlyFee(3L, 994L, BillingServiceType.FEE));
    }
}
