package ru.yandex.market.tpl.billing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CourierShiftCommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CourierShiftJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CourierShiftTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.CourierTariffTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.mbi.tariffs.client.model.TariffZoneEnum;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.entity.CourierTransaction;
import ru.yandex.market.tpl.billing.model.entity.UserShiftAggregated;
import ru.yandex.market.tpl.billing.model.entity.enums.CourierShiftType;
import ru.yandex.market.tpl.billing.model.exception.TariffsException;
import ru.yandex.market.tpl.billing.repository.CourierTransactionRepository;
import ru.yandex.market.tpl.billing.repository.UserShiftAggregatedRepository;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.dto.BillingDropOffReturnMovementContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingDropOffReturnMovementDto;
import ru.yandex.market.tpl.client.billing.dto.BillingIntakeContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "/database/service/tplshiftprocessing/before/sorting_centers.csv")
class TplShiftProcessingServiceTest extends AbstractFunctionalTest {

    private static final String MAX_NON_BUSINESS_MULTI_ORDERS_COUNT_PROPERTY_KEY = "maxNonBusinessMultiOrdersCount";

    private static final long USER_SHIFT_ID = 1;
    private static final long USER_SHIFT_ID_2 = 2;
    private static final long USER_SHIFT_ID_3 = 3;
    private static final LocalDate BILLING_DATE = LocalDate.of(2021, 3, 12);

    private static final List<Long> USER_SHIFTS = List.of(USER_SHIFT_ID, USER_SHIFT_ID_2);

    private static final List<ServiceTypeEnum> SERVICE_TYPES_FOR_GETTING_TARIFFS = List.of(
            ServiceTypeEnum.COURIER_SHIFT,
            ServiceTypeEnum.COURIER_VELO_SHIFT,
            ServiceTypeEnum.COURIER_DROPOFF_RETURN,
            ServiceTypeEnum.COURIER_SMALL_GOODS,
            ServiceTypeEnum.COURIER_BULKY_CARGO,
            ServiceTypeEnum.COURIER_WITHDRAW,
            ServiceTypeEnum.COURIER_YANDEX_DRIVE
    );

    @Autowired
    private BillingClient billingClient;

    @Autowired
    private TplShiftProcessingService tplShiftProcessingService;

    @Autowired
    private TariffService tariffService;

    @Autowired
    private UserShiftAggregatedRepository userShiftAggregatedRepository;

    @Autowired
    private CourierTransactionRepository courierTransactionRepository;

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/external_tariffs.csv",
            after = "/database/service/tplshiftprocessing/after/user_shift_processed.csv")
    void processUserShift() {
        testProcessUserShift();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/tariff_with_only_min_value.csv",
            after = "/database/service/tplshiftprocessing/after/tariff_with_only_min_value.csv")
    void processUserShiftWithTariffWithOnlyMinValueAndNullDistance() {
        when(billingClient.findOrders(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingOrderContainerDto());
        when(billingClient.findIntakes(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingIntakeContainerDto());
        when(billingClient.findDropOffReturnMovements(eq(Set.of(USER_SHIFT_ID)))).thenReturn(new BillingDropOffReturnMovementContainerDto(List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ? List.of(createMinTariff()) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        tplShiftProcessingService.importAndProcessUserShifts(List.of(USER_SHIFT_ID));

        verify(billingClient).findOrders(eq(Set.of(USER_SHIFT_ID)));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/no_orders.csv",
            after = "/database/service/tplshiftprocessing/after/no_orders.csv")
    void processUserShiftImportNoTplOrders() {
        when(billingClient.findOrders(eq(Set.of(USER_SHIFT_ID)))).thenReturn(new BillingOrderContainerDto(List.of()));
        when(billingClient.findIntakes(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingIntakeContainerDto());
        when(billingClient.findDropOffReturnMovements(eq(Set.of(USER_SHIFT_ID)))).thenReturn(new BillingDropOffReturnMovementContainerDto(List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        tplShiftProcessingService.importAndProcessUserShifts(List.of(USER_SHIFT_ID));

        verify(billingClient).findOrders(eq(Set.of(USER_SHIFT_ID)));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/no_orders.csv",
            after = "/database/service/tplshiftprocessing/after/no_delivered_orders.csv")
    void processUserShiftImportNoDeliveredOrders() {
        testProcessUserShiftImportNoDeliveredOrders();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/calculated_transactions_for_user_shift_1.csv",
            after = "/database/service/tplshiftprocessing/after/no_new_transactions.csv")
    void processUserShiftWithAlreadyCalculatedTransactionsImportNoDeliveredOrders() {
        testProcessUserShiftImportNoDeliveredOrders();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/external_tariffs.csv",
            after = "/database/service/tplshiftprocessing/after/courier_orders_imported_several_user_shifts.csv")
    void processSeveralUserShifts() {
        testProcessSeveralUserShifts();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/calculated_transactions_for_user_shift_2.csv",
            after = "/database/service/tplshiftprocessing/after/already_calculated_trans_exist.csv")
    void processSeveralUserShiftsOneOfWhichIsWithAlreadyCalculatedTransactions() {
        testProcessSeveralUserShifts();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/transit_distance_150.csv",
            after = "/database/service/tplshiftprocessing/after/transit_distance_150.csv")
    void processUserShiftWithoutCorrespondingTariff() {
        Assertions.assertThatThrownBy(this::testProcessUserShift)
                .as("Asserting that the valid exception is thrown")
                .hasMessage("Can't find MIN tariff for shift 1 id")
                .isInstanceOf(TariffsException.class);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/no_user_shift.csv",
            after = "/database/service/tplshiftprocessing/after/no_user_shift.csv")
    void processNonExistingUserShift() {
        tplShiftProcessingService.importAndProcessUserShifts(List.of(USER_SHIFT_ID));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/no_environment.csv",
            after = "/database/service/tplshiftprocessing/after/no_environment.csv")
    void processUserShiftWithoutEnvironmentProperties() {
        when(billingClient.findOrders(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingOrderContainerDto());
        when(billingClient.findIntakes(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingIntakeContainerDto());
        when(billingClient.findDropOffReturnMovements(eq(Set.of(USER_SHIFT_ID)))).thenReturn(new BillingDropOffReturnMovementContainerDto(List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        Assertions.assertThatThrownBy(
                () -> tplShiftProcessingService.importAndProcessUserShifts(List.of(USER_SHIFT_ID))
        )
                .as("Asserting that the valid exception is thrown")
                .hasCause(new IllegalArgumentException(
                        MAX_NON_BUSINESS_MULTI_ORDERS_COUNT_PROPERTY_KEY + " property value was null or unparseable " +
                                "for int"
                ));

        verify(billingClient).findOrders(eq(Set.of(USER_SHIFT_ID)));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/testProcessUserShiftWithNoSuccessIntake.csv",
            after = "/database/service/tplshiftprocessing/after/testProcessUserShiftWithNoSuccessIntake.csv")
    void testProcessUserShiftWithNoSuccessIntake() {
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));
        tplShiftProcessingService.processUserShifts(List.of(USER_SHIFT_ID));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/no_orders.csv",
            after = "/database/service/tplshiftprocessing/after/fashion_partial_return_allowed_orders.csv")
    void processFashionAndPartialReturnAllowedOrders() {
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));
        testFashionAndPartialReturnAllowedOrders();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/bulky_cargo.csv",
            after = "/database/service/tplshiftprocessing/after/bulky_cargo_processed.csv")
    void processUserShiftWithBulkyCargo() {
        testProcessUserShift();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/courierOrdersWithIntakes.csv",
            after = "/database/service/tplshiftprocessing/after/courierOrdersWithIntakes.csv")
    void testProcessCourierOrdersWithIntakes() {
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));
        tplShiftProcessingService.processUserShifts(List.of(USER_SHIFT_ID));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/shiftWithIntakes.csv",
            after = "/database/service/tplshiftprocessing/after/shiftWithIntakes.csv")
    void processUserShiftWithIntakesOnly() {
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));
        tplShiftProcessingService.processUserShifts(List.of(USER_SHIFT_ID));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/shiftWithDropoffReturn.csv",
            after = "/database/service/tplshiftprocessing/after/shiftWithDropoffReturn.csv"
    )
    void processUserShiftWithDropoffReturnsOnly() {
        //для 2021-03-12
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ? List.of(
                createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L),
                createCourierTariff(ServiceTypeEnum.COURIER_DROPOFF_RETURN, getDropoffReturnMeta(), 2L)
        ) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        //для 2021-03-01
        LocalDate billingLocalDate = LocalDate.of(2021, 3, 1);
        when(tariffService.findTariffs(
                eq(getTariffQuery(billingLocalDate, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ? List.of(
                createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L),
                createCourierTariff(ServiceTypeEnum.COURIER_DROPOFF_RETURN, getDropoffReturnMeta(), 2L)
        ) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(billingLocalDate, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        tplShiftProcessingService.processUserShifts(List.of(USER_SHIFT_ID, USER_SHIFT_ID_2, USER_SHIFT_ID_3));
    }

    @Test
    @DisplayName("Тест на то, что если в смене есть что-то помимо возвратного дропофа, то мы смену считаем по тарифу НЕ возвратного дропоффа")
    @DbUnitDataSet(before = "/database/service/tplshiftprocessing/before/processUserShiftWithDropoffReturnAndCourierOrders.csv")
    void processUserShiftWithDropoffReturnAndCourierOrders() {
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 101L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        tplShiftProcessingService.processUserShifts(List.of(USER_SHIFT_ID));

        UserShiftAggregated firstShift = userShiftAggregatedRepository.findById(USER_SHIFT_ID).orElseThrow();
        //у нас есть и возвратный дропоф, и обычный заказ. Явно убеждаемся, что у нас НЕ возвратный тариф
        assertThat(firstShift.getShiftType(), not(CourierShiftType.DROPOFF_RETURN));
        List<Long> courierTransactionsTariffIds =
                StreamSupport.stream(courierTransactionRepository.findAll().spliterator(), false)
                .map(CourierTransaction::getTariffId)
                .distinct()
                .collect(Collectors.toList());
        assertThat(courierTransactionsTariffIds, hasSize(1));
        assertThat(courierTransactionsTariffIds.get(0), is(101L));

    }

    @Test
    @DisplayName("Тест на расчет смен для самозанятых")
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/self_employed_user_shift.csv",
            after = "/database/service/tplshiftprocessing/after/self_employed_user_shift.csv")
    void processSelfEmployedUserShiftsTest() {
        testSelfEmployedTariffs();
    }

    @Test
    @DisplayName("Тест на расчет смен для самозанятых и партнерских смен вместе")
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/process_self_employed_with_partner_couriers.csv",
            after = "/database/service/tplshiftprocessing/after/process_self_employed_with_partner_couriers.csv")
    void processSelfEmployedAndPartnerUserShiftsTest() {
        testSelfEmployedTariffs();
    }

    @Test
    @DisplayName("Тест на расчет вело смен")
    @DbUnitDataSet(
            before = "/database/service/tplshiftprocessing/before/process_velo_shifts.csv",
            after = "/database/service/tplshiftprocessing/after/process_velo_shifts.csv"
    )
    void testVeloShift() {
        when(tariffService.findTariffs(eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))))
                .thenReturn(new TariffsIterator((page, batchSize) -> page != 0 ?
                        List.of() :
                        List.of(createCourierTariff(
                                ServiceTypeEnum.COURIER_VELO_SHIFT,
                                List.of(
                                        getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.MIN).amount(BigDecimal.ZERO),
                                        getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.BUSINESS).amount(BigDecimal.ZERO),
                                        getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.LOCKER).amount(BigDecimal.ZERO),
                                        getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.LOCKER_ORDER).amount(BigDecimal.ZERO),
                                        getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.PVZ).amount(BigDecimal.ZERO),
                                        getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.PVZ_ORDER).amount(BigDecimal.ZERO),
                                        getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.STANDARD).amount(BigDecimal.valueOf(62.5))
                                ),
                                1L)
                        ))
                );

        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER)))
        )
                .thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        tplShiftProcessingService.processUserShifts(BILLING_DATE);
    }

    void testSelfEmployedTariffs() {
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0
                ? List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L))
                : List.of())
        );
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0
                ? List.of(createSelfEmployedTariff(ServiceTypeEnum.COURIER_SMALL_GOODS, getCommonMeta(), 2L))
                : List.of())
        );

        tplShiftProcessingService.processUserShifts(BILLING_DATE);
    }

    private void testProcessUserShift() {
        when(billingClient.findOrders(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingOrderContainerDto());
        when(billingClient.findIntakes(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingIntakeContainerDto());
        when(billingClient.findDropOffReturnMovements(eq(Set.of(USER_SHIFT_ID)))).thenReturn(new BillingDropOffReturnMovementContainerDto(List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        tplShiftProcessingService.importAndProcessUserShifts(List.of(USER_SHIFT_ID));

        verify(billingClient).findOrders(eq(Set.of(USER_SHIFT_ID)));
    }

    private void testProcessUserShiftImportNoDeliveredOrders() {
        when(billingClient.findOrders(eq(Set.of(USER_SHIFT_ID))))
                .thenReturn(getBillingOrderContainerDtoWithoutDeliveredOrders());
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ?
                List.of(createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L)) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        when(billingClient.findIntakes(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingIntakeContainerDto());
        when(billingClient.findDropOffReturnMovements(eq(Set.of(USER_SHIFT_ID)))).thenReturn(new BillingDropOffReturnMovementContainerDto(List.of()));

        tplShiftProcessingService.importAndProcessUserShifts(List.of(USER_SHIFT_ID));

        verify(billingClient).findOrders(eq(Set.of(USER_SHIFT_ID)));
    }

    private void testProcessSeveralUserShifts() {
        when(billingClient.findOrders(eq(Set.of(USER_SHIFT_ID, USER_SHIFT_ID_2))))
                .thenReturn(
                        new BillingOrderContainerDto(
                                Stream.of(
                                        getBillingOrderContainerDto(),
                                        getBillingOrderContainerDtoForUserShift2()
                                )
                                        .map(BillingOrderContainerDto::getOrders)
                                        .flatMap(List::stream)
                                        .collect(Collectors.toList())
                        )
                );

        when(billingClient.findIntakes(eq(Set.of(USER_SHIFT_ID, USER_SHIFT_ID_2))))
                .thenReturn(getBillingIntakeContainerDto());
        when(billingClient.findDropOffReturnMovements(eq(Set.of(USER_SHIFT_ID, USER_SHIFT_ID_2))))
                .thenReturn(new BillingDropOffReturnMovementContainerDto(List.of()));

        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> page == 0 ? List.of(
                createCourierTariff(ServiceTypeEnum.COURIER_SHIFT, getCourierShiftMeta(), 1L),
                createCourierTariff(ServiceTypeEnum.COURIER_DROPOFF_RETURN, getDropoffReturnMeta(), 2L)
        ) : List.of()));
        when(tariffService.findTariffs(
                eq(getTariffQuery(BILLING_DATE, ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER))
        )).thenReturn(new TariffsIterator((page, batchSize) -> List.of()));

        tplShiftProcessingService.importAndProcessUserShifts(USER_SHIFTS);

        verify(billingClient).findOrders(eq(Set.of(USER_SHIFT_ID, USER_SHIFT_ID_2)));
    }

    private void testFashionAndPartialReturnAllowedOrders() {
        when(billingClient.findOrders(eq(Set.of(USER_SHIFT_ID))))
                .thenReturn(getFashionAndPartialReturnOrderContainerDto());
        when(billingClient.findIntakes(eq(Set.of(USER_SHIFT_ID)))).thenReturn(getBillingIntakeContainerDto());
        when(billingClient.findDropOffReturnMovements(eq(Set.of(USER_SHIFT_ID)))).thenReturn(new BillingDropOffReturnMovementContainerDto(List.of()));

        tplShiftProcessingService.importAndProcessUserShifts(List.of(USER_SHIFT_ID));
    }

    private BillingDropOffReturnMovementContainerDto getDropoffReturnsFromExternal() {
        return new BillingDropOffReturnMovementContainerDto(List.of(
                BillingDropOffReturnMovementDto.builder()
                        .userShiftId(1L)
                        .longitude(BigDecimal.valueOf(37.6))
                        .latitude(BigDecimal.valueOf(73.6))
                        .taskStatus("FINISHED")
                        .movementId(123L)
                        .movementExternalId("123EXT")
                        .warehouseYandexId("WAREHOUSE123")
                        .taskId(1234L)
                        .routePointId(1234321L)
                        .taskType("LOCKER_DELIVERY")
                        .finishedAt(Instant.parse("2021-03-12T16:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-03-12T15:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-03-12T20:00:00Z"))
                        .deliveredCargoCount(3L)
                        .build()
        ));
    }

    private BillingOrderContainerDto getFashionAndPartialReturnOrderContainerDto() {
        return new BillingOrderContainerDto(List.of(
                BillingOrderDto.builder()
                        .id(1L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(1L)
                        .placeCount(2)
                        .multiOrderId("ABC123")
                        .recipientPhone("88005553535")
                        .marketOrderId("12345")
                        .taskId(1L)
                        .dimensionsClass("BULKY_CARGO")
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .fashion(true)
                        .partialReturnAllowed(true)
                        .partialReturned(true)
                        .pickupPointId(1L)
                        .pickupPointType("PVZ")
                        .build(),
                BillingOrderDto.builder()
                        .id(2L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(35.75222))
                        .placeCount(2)
                        .multiOrderId("ABC123")
                        .routePointId(123L)
                        .recipientPhone("88005553535")
                        .marketOrderId("12346")
                        .taskId(1L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T15:00:00Z"))
                        .weight(BigDecimal.valueOf(0.2))
                        .dimensionsClass("REGULAR_CARGO")
                        .build()));
    }

    private BillingIntakeContainerDto getBillingIntakeContainerDto() {
        return new BillingIntakeContainerDto(Collections.emptyList());
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private BillingOrderContainerDto getBillingOrderContainerDto() {
        return new BillingOrderContainerDto(List.of(
                BillingOrderDto.builder()
                        .id(1L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(1L)
                        .placeCount(2)
                        .multiOrderId("ABC123")
                        .recipientPhone("88005553535")
                        .marketOrderId("12345")
                        .taskId(1L)
                        .dimensionsClass("BULKY_CARGO")
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(2L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(1L)
                        .placeCount(2)
                        .multiOrderId("ABC123")
                        .recipientPhone("88005553535")
                        .marketOrderId("12346")
                        .taskId(1L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T15:00:00Z"))
                        .weight(BigDecimal.valueOf(0.2))
                        .dimensionsClass("REGULAR_CARGO")
                        .build(),
                BillingOrderDto.builder()
                        .id(3L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(1L)
                        .placeCount(2)
                        .multiOrderId("ABC123")
                        .recipientPhone("88005553535")
                        .marketOrderId("12347")
                        .taskId(1L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T14:30:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(4L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(2L)
                        .placeCount(2)
                        .multiOrderId("ABC124")
                        .recipientPhone("88005553536")
                        .marketOrderId("12348")
                        .taskId(2L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .weight(BigDecimal.valueOf(0.4))
                        .build(),
                BillingOrderDto.builder()
                        .id(5L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61557))
                        .latitude(BigDecimal.valueOf(55.75223))
                        .routePointId(3L)
                        .placeCount(2)
                        .multiOrderId("ABC125")
                        .recipientPhone("88005553537")
                        .marketOrderId("12349")
                        .taskId(3L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T13:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(6L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61557))
                        .latitude(BigDecimal.valueOf(55.75223))
                        .routePointId(4L)
                        .placeCount(2)
                        .multiOrderId("ABC126")
                        .recipientPhone("88005553538")
                        .marketOrderId("12350")
                        .taskId(4L)
                        .deliveryTaskStatus("DELIVERED")
                        .weight(BigDecimal.valueOf(0.6))
                        .build(),
                BillingOrderDto.builder()
                        .id(7L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61557))
                        .latitude(BigDecimal.valueOf(55.75223))
                        .routePointId(5L)
                        .placeCount(2)
                        .multiOrderId("ABC127")
                        .recipientPhone("88005553539")
                        .marketOrderId("12351")
                        .taskId(5L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(8L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61557))
                        .latitude(BigDecimal.valueOf(55.75223))
                        .routePointId(6L)
                        .placeCount(2)
                        .multiOrderId("ABC128")
                        .recipientPhone("88005553540")
                        .marketOrderId("12352")
                        .taskId(6L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .weight(BigDecimal.valueOf(0.8))
                        .build(),
                BillingOrderDto.builder()
                        .id(9L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61557))
                        .latitude(BigDecimal.valueOf(55.75223))
                        .routePointId(7L)
                        .placeCount(2)
                        .multiOrderId("ABC129")
                        .recipientPhone("88005553541")
                        .marketOrderId("12353")
                        .taskId(7L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(10L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61557))
                        .latitude(BigDecimal.valueOf(55.75223))
                        .routePointId(8L)
                        .placeCount(2)
                        .multiOrderId("ABC130")
                        .recipientPhone("88005553542")
                        .marketOrderId("12354")
                        .taskId(8L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .weight(BigDecimal.valueOf(1.0))
                        .build(),
                BillingOrderDto.builder()
                        .id(11L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61558))
                        .latitude(BigDecimal.valueOf(55.75224))
                        .routePointId(9L)
                        .placeCount(2)
                        .multiOrderId("ABC131")
                        .recipientPhone("88005553544")
                        .marketOrderId("12355")
                        .taskId(9L)
                        .deliveryTaskStatus("DELIVERY_FAILED")
                        .failSource(Source.COURIER)
                        .build(),
                BillingOrderDto.builder()
                        .id(12L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(10L)
                        .placeCount(2)
                        .multiOrderId("ABC132")
                        .recipientPhone("88005553535")
                        .marketOrderId("12356")
                        .taskId(10L)
                        .deliveryTaskStatus("DELIVERED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .weight(BigDecimal.valueOf(1.2))
                        .build(),
                BillingOrderDto.builder()
                        .id(13L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(11L)
                        .placeCount(0)
                        .recipientPhone("88005553535")
                        .marketOrderId("12357")
                        .multiOrderId("ABC133")
                        .taskId(11L)
                        .pickupPointId(1L)
                        .pickupPointType("PVZ")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(14L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(11L)
                        .placeCount(3)
                        .recipientPhone("88005553535")
                        .marketOrderId("12358")
                        .multiOrderId("ABC134")
                        .taskId(11L)
                        .pickupPointId(1L)
                        .pickupPointType("PVZ")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T14:30:00Z"))
                        .weight(BigDecimal.valueOf(1.4))
                        .build(),
                BillingOrderDto.builder()
                        .id(15L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(12L)
                        .placeCount(3)
                        .recipientPhone("88005553535")
                        .marketOrderId("12359")
                        .multiOrderId("ABC135")
                        .taskId(12L)
                        .pickupPointId(2L)
                        .pickupPointType("PVZ")
                        .pickupSubtaskStatus("FINISHED")
                        .build(),
                BillingOrderDto.builder()
                        .id(16L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(13L)
                        .placeCount(4)
                        .recipientPhone("88005553535")
                        .marketOrderId("12360")
                        .multiOrderId("ABC136")
                        .taskId(13L)
                        .pickupPointId(3L)
                        .pickupPointType("PVZ")
                        .pickupSubtaskStatus("FAILED")
                        .failSource(Source.COURIER)
                        .weight(BigDecimal.valueOf(1.6))
                        .build(),
                BillingOrderDto.builder()
                        .id(17L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(14L)
                        .placeCount(3)
                        .recipientPhone("88005553535")
                        .marketOrderId("12361")
                        .multiOrderId("ABC137")
                        .taskId(14L)
                        .pickupPointId(1L)
                        .pickupPointType("PVZ")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(18L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(15L)
                        .placeCount(0)
                        .recipientPhone("88005553535")
                        .marketOrderId("12362")
                        .multiOrderId("ABC138")
                        .taskId(15L)
                        .pickupPointId(4L)
                        .pickupPointType("LOCKER")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .weight(BigDecimal.valueOf(1.8))
                        .build(),
                BillingOrderDto.builder()
                        .id(19L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(15L)
                        .placeCount(3)
                        .recipientPhone("88005553535")
                        .marketOrderId("12363")
                        .multiOrderId("ABC139")
                        .taskId(15L)
                        .pickupPointId(4L)
                        .pickupPointType("LOCKER")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T14:30:00Z"))
                        .build(),
                BillingOrderDto.builder()
                        .id(20L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(16L)
                        .placeCount(3)
                        .recipientPhone("88005553535")
                        .marketOrderId("12364")
                        .multiOrderId("ABC140")
                        .taskId(16L)
                        .pickupPointId(5L)
                        .pickupPointType("LOCKER")
                        .pickupSubtaskStatus("FINISHED")
                        .weight(BigDecimal.valueOf(2.0))
                        .build(),
                BillingOrderDto.builder()
                        .id(21L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(17L)
                        .placeCount(4)
                        .recipientPhone("88005553535")
                        .marketOrderId("12365")
                        .multiOrderId("ABC141")
                        .taskId(17L)
                        .pickupPointId(6L)
                        .pickupPointType("LOCKER")
                        .pickupSubtaskStatus("FAILED")
                        .failSource(Source.COURIER)
                        .build(),
                BillingOrderDto.builder()
                        .id(22L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(38.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(18L)
                        .placeCount(3)
                        .recipientPhone("88005553535")
                        .marketOrderId("12366")
                        .multiOrderId("ABC142")
                        .taskId(18L)
                        .pickupPointId(4L)
                        .pickupPointType("LOCKER")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .weight(BigDecimal.valueOf(2.2))
                        .build()
        ));
    }

    private BillingOrderContainerDto getBillingOrderContainerDtoForUserShift2() {
        return new BillingOrderContainerDto(List.of(
                BillingOrderDto.builder()
                        .id(105L)
                        .userShiftId(2L)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(1L)
                        .placeCount(2)
                        .multiOrderId("ABC124")
                        .recipientPhone("88005553536")
                        .marketOrderId("12349")
                        .taskId(2L)
                        .pickupPointId(1L)
                        .pickupPointType("LOCKER")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .pickupPointSubtype("LOCKER")
                        .build(),
                BillingOrderDto.builder()
                        .id(107L)
                        .userShiftId(2L)
                        .longitude(BigDecimal.valueOf(36.61556))
                        .latitude(BigDecimal.valueOf(56.75222))
                        .routePointId(2L)
                        .placeCount(2)
                        .multiOrderId("ABC1245")
                        .recipientPhone("88005553536")
                        .marketOrderId("123490")
                        .taskId(3L)
                        .pickupPointId(1L)
                        .pickupPointType("PVZ")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .pickupPointSubtype("LOCKER_GO")
                        .build(),
                BillingOrderDto.builder()
                        .id(106L)
                        .userShiftId(2L)
                        .longitude(BigDecimal.valueOf(37.61556))
                        .latitude(BigDecimal.valueOf(55.75222))
                        .routePointId(1L)
                        .placeCount(2)
                        .multiOrderId("ABC124")
                        .recipientPhone("88005553536")
                        .marketOrderId("12349")
                        .taskId(2L)
                        .pickupPointId(1L)
                        .pickupPointType("PVZ")
                        .pickupSubtaskStatus("FINISHED")
                        .finishedAt(Instant.parse("2021-01-04T15:00:00Z"))
                        .deliveryIntervalFrom(Instant.parse("2021-01-04T14:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("2021-01-04T16:00:00Z"))
                        .pickupPointSubtype("LAVKA")
                        .build()
        ));
    }

    private BillingOrderContainerDto getBillingOrderContainerDtoWithoutDeliveredOrders() {
        return new BillingOrderContainerDto(List.of(
                BillingOrderDto.builder()
                        .id(1L)
                        .userShiftId(USER_SHIFT_ID)
                        .longitude(BigDecimal.valueOf(37.61558))
                        .latitude(BigDecimal.valueOf(55.75224))
                        .routePointId(3L)
                        .placeCount(2)
                        .multiOrderId("ABC131")
                        .recipientPhone("88005553544")
                        .marketOrderId("12355")
                        .taskId(9L)
                        .deliveryTaskStatus("DELIVERY_FAILED")
                        .failSource(Source.CLIENT)
                        .failReasonType(OrderDeliveryTaskFailReasonType.CANNOT_PAY)
                        .build()
        ));
    }

    private TariffFindQuery getTariffQuery(LocalDate date, ModelType modelType) {
        return new TariffFindQuery()
                .targetDate(date)
                .modelType(modelType)
                .serviceTypes(SERVICE_TYPES_FOR_GETTING_TARIFFS)
                .isActive(true);
    }

    private TariffDTO createCourierTariff(ServiceTypeEnum serviceType, List<Object> metaList, long id) {
        return createCourierTariff(serviceType, metaList, id, ModelType.THIRD_PARTY_LOGISTICS_COURIER);
    }

    private TariffDTO createSelfEmployedTariff(ServiceTypeEnum serviceTypeEnum, List<Object> metaList, long id) {
        return createCourierTariff(serviceTypeEnum, metaList, id,
                ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER);
    }

    private TariffDTO createCourierTariff(ServiceTypeEnum serviceType, List<Object> metaList, long id,
                                          ModelType modelType) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setServiceType(serviceType);
        tariff.setModelType(modelType);
        tariff.setDateFrom(LocalDate.of(2021, 1, 1));
        tariff.setPartner(null);
        tariff.setDateTo(null);
        tariff.setMeta(metaList);
        return tariff;
    }

    private List<Object> getCourierShiftMeta() {
        return List.of(
                //first shift
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(3000)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(20)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(10)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(30)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(40)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(50)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(60)),
                //second shift
                getMetaCommonPart(0, 100, TariffZoneEnum.SPB)
                        .tariffType(CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(6000)),
                getMetaCommonPart(0, 100, TariffZoneEnum.SPB)
                        .tariffType(CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(40)),
                getMetaCommonPart(0, 100, TariffZoneEnum.SPB)
                        .tariffType(CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(20)),
                getMetaCommonPart(0, 100, TariffZoneEnum.SPB)
                        .tariffType(CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(60)),
                getMetaCommonPart(0, 100, TariffZoneEnum.SPB)
                        .tariffType(CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(80)),
                getMetaCommonPart(0, 100, TariffZoneEnum.SPB)
                        .tariffType(CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(100)),
                getMetaCommonPart(0, 100, TariffZoneEnum.SPB)
                        .tariffType(CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(120)),
                //first shift bulky cargo
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.BULKY_CARGO)
                        .tariffType(CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(3000)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.BULKY_CARGO)
                        .tariffType(CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(20)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.BULKY_CARGO)
                        .tariffType(CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(10)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.BULKY_CARGO)
                        .tariffType(CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(30)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.BULKY_CARGO)
                        .tariffType(CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(40)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.BULKY_CARGO)
                        .tariffType(CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(50)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.BULKY_CARGO)
                        .tariffType(CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(60)),
                //WITHDRAW
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.WITHDRAW)
                        .tariffType(CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(3000)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.WITHDRAW)
                        .tariffType(CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(20)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.WITHDRAW)
                        .tariffType(CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(10)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.WITHDRAW)
                        .tariffType(CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(30)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.WITHDRAW)
                        .tariffType(CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(40)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.WITHDRAW)
                        .tariffType(CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(50)),
                getMetaCommonPart(101, 500, TariffZoneEnum.MOSCOW, CourierShiftTypeEnum.WITHDRAW)
                        .tariffType(CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(60))
        );
    }

    private List<Object> getCommonMeta() {
        return List.of(
                getMetaCourierShiftCommon(0, 150L, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(111)),
                getMetaCourierShiftCommon(151, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(222)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(99)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(89)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(79)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(69)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(59)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(35))
        );
    }

    private List<Object> getDropoffReturnMeta() {
        return List.of(
                getMetaCourierShiftCommon(0, 150L, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(111)),
                getMetaCourierShiftCommon(151, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(222)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(0)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(0)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(0)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(0)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(0)),
                getMetaCourierShiftCommon(0, null, TariffZoneEnum.MOSCOW, CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(0))
        );
    }

    private static CourierShiftJsonSchema getMetaCommonPart(long from, long to, TariffZoneEnum zone) {
        return getMetaCommonPart(from, to, zone, CourierShiftTypeEnum.SMALL_GOODS);
    }

    private static CourierShiftJsonSchema getMetaCommonPart(
            long from,
            long to,
            TariffZoneEnum zone,
            CourierShiftTypeEnum shiftType
    ) {
        return new CourierShiftJsonSchema()
                .fromDistance(from)
                .toDistance(to)
                .shiftType(shiftType)
                .tariffZone(zone);
    }

    private static CourierShiftCommonJsonSchema getMetaCourierShiftCommon(
            long from,
            @Nullable Long to,
            TariffZoneEnum zone,
            CourierTariffTypeEnum tariffType
    ) {
        return new CourierShiftCommonJsonSchema()
                .fromDistance(from)
                .toDistance(to)
                .tariffZone(zone)
                .tariffType(tariffType);
    }

    private TariffDTO createMinTariff() {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(1L);
        tariff.setServiceType(ServiceTypeEnum.COURIER_SHIFT);
        tariff.setModelType(ModelType.THIRD_PARTY_LOGISTICS_COURIER);
        tariff.setDateFrom(LocalDate.of(2021, 1, 1));
        tariff.setPartner(null);
        tariff.setDateTo(null);
        tariff.setMeta(List.of(
                getMetaCommonPart(0, 100, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(3000)),
                getMetaCommonPart(0, 100, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.ZERO),
                getMetaCommonPart(0, 100, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.ZERO),
                getMetaCommonPart(0, 100, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.ZERO),
                getMetaCommonPart(0, 100, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.ZERO),
                getMetaCommonPart(0, 100, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.ZERO),
                getMetaCommonPart(0, 100, TariffZoneEnum.MOSCOW)
                        .tariffType(CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.ZERO)
        ));

        return tariff;
    }
}
