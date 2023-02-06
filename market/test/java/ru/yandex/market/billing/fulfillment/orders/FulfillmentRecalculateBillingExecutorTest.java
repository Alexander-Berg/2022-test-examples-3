package ru.yandex.market.billing.fulfillment.orders;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.fulfillment.tariffs.FulfillmentTariff;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.billing.fulfillment.tariffs.TestTariffCreationUtil.createFulfillmentTariff;

@SuppressWarnings("ParameterNumber")
class FulfillmentRecalculateBillingExecutorTest extends FunctionalTest {

    private static final LocalDateTime DATE_2022_05_04 = LocalDateTime.of(2022, 5, 4, 10, 30, 0);
    private static final LocalDateTime DATE_2022_06_01_BEFORE_ELEVEN = LocalDateTime.of(2022, 6, 1, 10, 30, 0);
    private static final LocalDateTime DATE_2022_06_01_AFTER_ELEVEN = LocalDateTime.of(2022, 6, 1, 11, 30, 0);
    private static final LocalDateTime DATE_2022_06_02 = LocalDateTime.of(2022, 6, 2, 10, 30, 0);

    private static final Clock CLOCK = Clock.fixed(DateTimes.toInstantAtDefaultTz(DATE_2022_05_04),
            ZoneId.systemDefault());
    private static final List<FulfillmentTariff> DEFAULT_TARIFF = List.of(
            createFulfillmentTariff(
                    LocalDate.of(2022, 5, 1),
                    LocalDate.MAX,
                    BillingServiceType.DELIVERY_TO_CUSTOMER,
                    15000L,
                    25000L,
                    40000,
                    OrderType.FULFILLMENT)
    );

    @Mock
    private FulfillmentRecalculateBillingService mockedRecalculateBillingService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    @Qualifier("clientTariffsService")
    private TariffsService fulfillmentTariffDao;

    private FulfillmentRecalculateBillingExecutor recalculateBillingExecutor;

    @BeforeEach
    void setUp() {
        recalculateBillingExecutor = new FulfillmentRecalculateBillingExecutor(
                mockedRecalculateBillingService,
                environmentService,
                CLOCK
        );
    }

    @Test
    @DisplayName("Не отрабатывать если SKU еще не импортился")
    @DbUnitDataSet(
            before = "FulfillmentRecalculateBillingExecutorTest.skuNotImported.before.csv",
            after = "FulfillmentRecalculateBillingExecutorTest.skuNotImported.after.csv"
    )
    void testSkuNotImported() {

        recalculateBillingExecutor.doJob();
        Mockito.verify(mockedRecalculateBillingService, Mockito.times(0))
                .process(any(), any());
    }

    @Test
    @DisplayName("Второго числа уже не перебиливаем прошлый месяц, даже если биллинг остался в прошлом месяце")
    @DbUnitDataSet(
            before = "FulfillmentRecalculateBillingExecutorTest.testSecondDayOfMonth.before.csv",
            after = "FulfillmentRecalculateBillingExecutorTest.testSecondDayOfMonth.after.csv"
    )
    void testSecondDayOfMonth() {
        Clock clock = Clock.fixed(DateTimes.toInstantAtDefaultTz(DATE_2022_06_02),
                ZoneId.systemDefault());
        recalculateBillingExecutor = new FulfillmentRecalculateBillingExecutor(
                mockedRecalculateBillingService,
                environmentService,
                clock
        );

        Mockito.verify(mockedRecalculateBillingService, Mockito.times(0))
                .process(any(), any());
    }

    @Test
    @DisplayName("Не отрабатывать за сегодня если уже успешно отработал")
    @DbUnitDataSet(
            before = "FulfillmentRecalculateBillingExecutorTest.dontRecalculateTwice.before.csv",
            after = "FulfillmentRecalculateBillingExecutorTest.dontRecalculateTwice.after.csv"
    )
    void testDontRecalculateTwice() {
        //проверить конкретные даты
        Mockito.doNothing().when(mockedRecalculateBillingService).process(any(), any());

        //Вызываем 2 раза
        recalculateBillingExecutor.doJob();
        recalculateBillingExecutor.doJob();

        //Отрабатывает 1 раз
        Mockito.verify(mockedRecalculateBillingService, Mockito.times(1))
                .process(any(), any());
    }


    @Test
    @DisplayName("Если дата обилливания отстала от текущей даты, переобилливать до даты обилливания")
    @DbUnitDataSet(
            before = "FulfillmentRecalculateBillingExecutorTest.testRecalculateUntilBillingDate.before.csv",
            after = "FulfillmentRecalculateBillingExecutorTest.testRecalculateUntilBillingDate.after.csv"
    )
    void testRecalculateUntilBillingDate() {
        Mockito.when(fulfillmentTariffDao.getOrderedTariffs(Mockito.any())).thenReturn(DEFAULT_TARIFF);
        recalculateBillingExecutor.doJob();

        //проверить даты которые вызвались, а данные убрать
        Mockito.doNothing().when(mockedRecalculateBillingService)
                .process(eq(LocalDate.of(2022, 5, 1)), eq(LocalDate.of(2022, 5, 2)));

    }

    @Test
    @DisplayName("Первого числа до одиннацати должно пересчитать весь месяц")
    @DbUnitDataSet(
            before = "FulfillmentRecalculateBillingExecutorTest.testFirstDayOfMonthBeforeEleven.before.csv",
            after = "FulfillmentRecalculateBillingExecutorTest.testFirstDayOfMonthBeforeEleven.after.csv"
    )
    void testFirstDayOfMonthBeforeEleven() {
        Clock clock = Clock.fixed(DateTimes.toInstantAtDefaultTz(DATE_2022_06_01_BEFORE_ELEVEN),
                ZoneId.systemDefault());


        recalculateBillingExecutor = new FulfillmentRecalculateBillingExecutor(
                mockedRecalculateBillingService,
                environmentService,
                clock
        );
        //проверить конкретные даты
        Mockito.doNothing().when(mockedRecalculateBillingService)
                .process(eq(LocalDate.of(2022, 5, 1)), eq(LocalDate.of(2022, 5, 31)));

        recalculateBillingExecutor.doJob();

        Mockito.verify(mockedRecalculateBillingService, Mockito.times(1))
                .process(any(), any());

    }

    @Test
    @DisplayName("Первого числа после одиннацати не должно пересчитать весь месяц")
    @DbUnitDataSet(
            before = "FulfillmentRecalculateBillingExecutorTest.testFirstDayOfMonthAfterEleven.before.csv",
            after = "FulfillmentRecalculateBillingExecutorTest.testFirstDayOfMonthAfterEleven.after.csv"
    )
    void testFirstDayOfMonthAfterEleven() {
        Clock clock = Clock.fixed(DateTimes.toInstantAtDefaultTz(DATE_2022_06_01_AFTER_ELEVEN),
                ZoneId.systemDefault());
        recalculateBillingExecutor = new FulfillmentRecalculateBillingExecutor(
                mockedRecalculateBillingService,
                environmentService,
                clock
        );

        Mockito.doNothing().when(mockedRecalculateBillingService).process(any(), any());

        Mockito.verify(mockedRecalculateBillingService, Mockito.times(0))
                .process(any(), any());

    }

}
