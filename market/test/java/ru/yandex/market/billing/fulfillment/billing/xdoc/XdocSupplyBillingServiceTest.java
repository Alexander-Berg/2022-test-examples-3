package ru.yandex.market.billing.fulfillment.billing.xdoc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.mbi.tariffs.client.model.XdocSupplyServiceEnum;
import ru.yandex.market.mbi.tariffs.client.model.XdocSupplyTariffJsonSchema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Тесты для {@link XdocSupplyBillingService}.
 *
 * @author vbudnev
 */
class XdocSupplyBillingServiceTest extends FunctionalTest {
    private static final BigDecimal AMOUNT_250_RUB = new BigDecimal("250.00");
    private static final BigDecimal AMOUNT_2900_RUB = new BigDecimal("2900.00");
    private static final BigDecimal AMOUNT_1_RUB = new BigDecimal("1.00");

    @Autowired
    private XdocSupplyBillingService xdocSupplyBillingService;

    @Autowired
    private TariffsService tariffsService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");
            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }

                return getXdocTariffs()
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

    @DisplayName("Проверка обиливания магистрального xdoc")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.before.csv",
            after = "db/XdocSupplyBillingServiceTest.after.csv"
    )
    @Test
    void test_calculateXdocSupplies() {
        xdocSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("До момента активации услуги \"билим\" бесплатно")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.before_billing_start.before.csv",
            after = "db/XdocSupplyBillingServiceTest.before_billing_start.after.csv"
    )
    @Test
    void test_calculateXdocSupplies_free() {
        xdocSupplyBillingService.process(LocalDate.of(2019, 1, 5));
    }

    @DisplayName("Разные направления и разные типы единицы")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.different_units.before.csv",
            after = "db/XdocSupplyBillingServiceTest.different_units.after.csv"
    )
    @Test
    void test_calculateXdocSupplies_differentUnitsAndDirections() {
        xdocSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    /**
     * Добавлено явно а не в рамках общего теста, так как ошибка прецедент =(
     */
    @DisplayName("Несколько позиций в рамках поставки")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.multiple_items.before.csv",
            after = "db/XdocSupplyBillingServiceTest.multiple_items.after.csv"
    )
    @Test
    void test_calculateXdocSupplies_supplyWithMultipleItems() {
        xdocSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    /**
     * Такую ситуацию поймает мониторинг <code>market_billing.mon_xdoc_billing</code>.
     */
    @DisplayName("Если позиции в рамках поставки имеют разные supplier_id то не биллим")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.different_suppliers.before.csv",
            after = "db/XdocSupplyBillingServiceTest.different_suppliers.after.csv"
    )
    @Test
    void test_calculateXdocSupplies_supplyItemsWithDifferentSupplierIds() {
        xdocSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("Бесплатное обиливание, если счетчики кривые")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.free_on_error.before.csv",
            after = "db/XdocSupplyBillingServiceTest.free_on_error.after.csv"
    )
    @Test
    void test_calculateXdocSupplies_freeTariffOnError() {
        xdocSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("Используется проверка EnvironmentAwareDateValidationService")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.date_check.before.csv"
    )
    @Test
    void test_calculateXdocSupplies_when_invalidBillingDate() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> xdocSupplyBillingService.process(LocalDate.of(2019, 1, 1))
        );

        assertThat(ex.getMessage(), is("Required date 2019-01-01 cannot be used."));
    }

    @DisplayName("Проверка на льготные условия из Москвы в Ростов")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.preferential_tariff.before.csv",
            after = "db/XdocSupplyBillingServiceTest.preferential_tariff.after.csv"
    )
    @Test
    void test_calculateXdocSupplies_preferentialTariff() {
        xdocSupplyBillingService.process(LocalDate.of(2020, 9, 14));
        xdocSupplyBillingService.process(LocalDate.of(2020, 9, 15));
        xdocSupplyBillingService.process(LocalDate.of(2020, 10, 30));
        xdocSupplyBillingService.process(LocalDate.of(2020, 12, 1));
        xdocSupplyBillingService.process(LocalDate.of(2021, 1, 1));
        xdocSupplyBillingService.process(LocalDate.of(2021, 3, 31));
        xdocSupplyBillingService.process(LocalDate.of(2021, 4, 1));
    }

    @DisplayName("Проверка обиливания магистрального xdoc для поставщиков с промо тарифами")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.promo.before.csv",
            after = "db/XdocSupplyBillingServiceTest.promo.after.csv"
    )
    @Test
    void test_calculateXdocSuppliesWithPromo() {
        xdocSupplyBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("Проверка обиливания для нужных записей xdoc")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.billable.before.csv",
            after = "db/XdocSupplyBillingServiceTest.billable.after.csv"
    )
    @Test
    void test_calculateBillableXdocSupplies() {
        xdocSupplyBillingService.process(LocalDate.of(2021, 3, 25));
    }

    @DisplayName("Проверка на не удаление записей xdoc")
    @DbUnitDataSet(
            before = "db/XdocSupplyBillingServiceTest.notDeleteBilledRecords.before.csv",
            after = "db/XdocSupplyBillingServiceTest.notDeleteBilledRecords.after.csv"
    )
    @Test
    void test_notDeleteBilledRecords() {
        xdocSupplyBillingService.process(LocalDate.of(2021, 3, 25));
    }

    private List<TariffDTO> getXdocTariffs() {
        return List.of(
                createTariff(1L, LocalDate.of(2019, 8, 16), LocalDate.of(2020, 9, 15), List.of(
                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.MSK, XdocSupplyServiceEnum.ROSTOV_ND, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.MSK, XdocSupplyServiceEnum.ROSTOV_ND, BillingUnitEnum.SUPPLY_PALLET),

                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.ROSTOV_ND, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.ROSTOV_ND, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_PALLET),

                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.SPB, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.SPB, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_PALLET)
                )),
                createTariff(2L, LocalDate.of(2020, 9, 15), LocalDate.of(2021, 4, 1), List.of(
                        createMeta(AMOUNT_1_RUB, XdocSupplyServiceEnum.MSK, XdocSupplyServiceEnum.ROSTOV_ND, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_1_RUB, XdocSupplyServiceEnum.MSK, XdocSupplyServiceEnum.ROSTOV_ND, BillingUnitEnum.SUPPLY_PALLET),

                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.ROSTOV_ND, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.ROSTOV_ND, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_PALLET),

                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.SPB, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.SPB, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_PALLET)
                )),
                createTariff(3L, LocalDate.of(2021, 4, 1), null, List.of(
                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.MSK, XdocSupplyServiceEnum.ROSTOV_ND, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.MSK, XdocSupplyServiceEnum.ROSTOV_ND, BillingUnitEnum.SUPPLY_PALLET),

                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.ROSTOV_ND, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.ROSTOV_ND, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_PALLET),

                        createMeta(AMOUNT_250_RUB, XdocSupplyServiceEnum.SPB, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_BOX),
                        createMeta(AMOUNT_2900_RUB, XdocSupplyServiceEnum.SPB, XdocSupplyServiceEnum.MSK, BillingUnitEnum.SUPPLY_PALLET)
                ))
        );
    }

    private TariffDTO createTariff(long id, LocalDate from, LocalDate to, List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setIsActive(true);
        tariff.setServiceType(ServiceTypeEnum.FF_XDOC_SUPPLY);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setMeta(meta);
        return tariff;
    }

    private CommonJsonSchema createMeta(BigDecimal amount, XdocSupplyServiceEnum from, XdocSupplyServiceEnum to, BillingUnitEnum billingUnit) {
        return new XdocSupplyTariffJsonSchema()
                .supplyDirectionFrom(from)
                .supplyDirectionTo(to)
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(billingUnit);
    }
}
