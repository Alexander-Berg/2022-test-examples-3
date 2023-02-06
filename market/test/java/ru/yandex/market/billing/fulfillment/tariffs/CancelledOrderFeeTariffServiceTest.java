package ru.yandex.market.billing.fulfillment.tariffs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.CancelledOrderFeeTariffService;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.core.order.model.OrderItemForBilling;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CancelledOrderFeeJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link CancelledOrderFeeTariffService}
 */
class CancelledOrderFeeTariffServiceTest extends FunctionalTest {
    private static final LocalDate LOCAL_DATE_BEFORE_START_BILLING = LocalDate.of(2020, 6, 7);
    private static final LocalDate LOCAL_DATE_AFTER_START_BILLING = LocalDate.of(2020, 6, 8);
    private static final LocalDate LOCAL_DATE_END_OF_TARIFF = LocalDate.of(2020, 12, 31);

    private EnvironmentService environmentService;

    @Autowired
    private TariffsService tariffsService;

    private static OrderItemForBilling createOrderItem(long supplierId, boolean isDSBS) {
        return OrderItemForBilling.builder()
                .withDimensionsSum(3L)
                .withDepth(1L)
                .withWidth(1L)
                .withHeight(1L)
                .withWeight(1L)
                .withRealLength(100L)
                .withRealHeight(100L)
                .withRealWidth(100L)
                .withRealWeight(BigDecimal.valueOf(0.1D))
                .withBillingPrice(BigDecimal.valueOf(310))
                .withSupplierId(supplierId)
                .withCategoryId(1)
                .withItemId(0L)
                .withOrderId(0L)
                .withOrderCreatedAt(LocalDate.of(2020, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .withFeedId(0L)
                .withOfferId("")
                .withPrice(BigDecimal.ZERO)
                .withCount(0)
                .withDropshipItem(false)
                .withCrossdock(false)
                .withDSBSItem(isDSBS)
                .withClickAndCollectItem(false)
                .withExpressOrder(false)
                .withCashOnlyOrder(false)
                .build();
    }

    @BeforeEach
    void setUp() {
        environmentService = mock(EnvironmentService.class);
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }

                return getCancelledOrderTariffs()
                        .stream()
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
    }

    @Test
    @DisplayName("Проверка отсутствия комиссии за отмену заказа до начала обилливаний для DSBS")
    void testFreeTariffBeforeStartBilling() {
        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_BEFORE_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, true));

        assertEquals(priceAndTariff.getPrice(), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Проверка отсутствия комиссии отмены заказов DSBS по программе")
    void testFreeTariffForDSBSProgramIsFree() {
        when(environmentService.getBooleanValue("mbi.dsbs.program.cancelled.order.fee.is.free", false))
                .thenReturn(true);
        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, true));
        assertEquals(priceAndTariff.getPrice(), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Проверка отсутствия комиссии отмены заказов DSBS для выбранных партнеров")
    void testFreeTariffForDSBSSupplier() {
        when(environmentService.getBooleanValue("mbi.dsbs.program.cancelled.order.fee.is.free", false))
                .thenReturn(false);
        when(environmentService.getValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of()))
                .thenReturn(List.of("1", "2"));

        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, true));
        assertEquals(priceAndTariff.getPrice(), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Проверка наличия комиссии отмены заказов для выбранных НЕ DSBS партнеров")
    void testNotFreeExternalTariffForNotDSBS() {
        when(environmentService.getBooleanValue("mbi.billing.use.external.tariffs.cancelled_order_fee", false))
                .thenReturn(true);
        when(environmentService.getBooleanValue("mbi.dsbs.program.cancelled.order.fee.is.free", false))
                .thenReturn(false);
        when(environmentService.getValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of()))
                .thenReturn(List.of("1", "2"));

        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, false)); // айдишник в енв есть, но это не DSBS заказ

        assertEquals(priceAndTariff.getPrice(), new BigDecimal(22200));
    }

    @DisplayName("Проверка отсутствия комиссии отмены заказов по программе для выбранных не DSBS партнеров")
    @Test
    void testNotFreeExternalCommonTariffForNotDSBS() {
        when(environmentService.getBooleanValue("mbi.billing.use.external.tariffs.cancelled_order_fee", false))
                .thenReturn(true);
        when(environmentService.getBooleanValue("mbi.dsbs.program.cancelled.order.fee.is.free", false))
                .thenReturn(false);
        when(environmentService.getValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of()))
                .thenReturn(List.of("1", "2"));

        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(3L, false));

        assertEquals(priceAndTariff.getPrice(), new BigDecimal(11100));
    }

    @Test
    @DisplayName("Проверка отсутствия тарифа после окончания его действия")
    void testTariffNotFoundInTariffsService() {
        when(environmentService.getBooleanValue("mbi.billing.use.external.tariffs.cancelled_order_fee", false))
                .thenReturn(true);
        when(environmentService.getBooleanValue("mbi.dsbs.program.cancelled.order.fee.is.free", false))
                .thenReturn(false);
        when(environmentService.getValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of()))
                .thenReturn(List.of("1", "2"));


        Exception exception = Assertions.assertThrows(TariffsException.class, () -> {
            new CancelledOrderFeeTariffService(
                    LOCAL_DATE_END_OF_TARIFF, environmentService, tariffsService
            ).getForCancelledOrderFee(createOrderItem(1L, false));
        });

        String expectedMsg = "Tariff from tariff service is not found for supplierId:";
        assertTrue(exception.getMessage().startsWith(expectedMsg));
    }

    private List<TariffDTO> getCancelledOrderTariffs() {
        return List.of(
                createTariff(1L, LOCAL_DATE_BEFORE_START_BILLING, LOCAL_DATE_END_OF_TARIFF, null, List.of(
                        createMeta(new BigDecimal(111))
                )),
                createTariff(2L, LOCAL_DATE_BEFORE_START_BILLING, LOCAL_DATE_END_OF_TARIFF, new Partner().id(1L),
                        List.of(createMeta(new BigDecimal(222)))
                )
        );
    }

    private TariffDTO createTariff(long id, LocalDate from, LocalDate to, Partner partner, List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setMeta(meta);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setPartner(partner);
        return tariff;
    }

    private CommonJsonSchema createMeta(BigDecimal amount) {
        return new CancelledOrderFeeJsonSchema()
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ITEM);
    }
}
