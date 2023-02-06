package ru.yandex.market.cpa;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import ru.yandex.market.core.feature.subsidies.impl.BalanceServiceId;
import ru.yandex.market.core.order.payment.BankOrder;
import ru.yandex.market.core.order.payment.BankOrderStatus;
import ru.yandex.market.core.order.payment.OebsPaymentStatus;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankOrdersImportExecutorTest {

    private static final Instant TRANTIME_FROM = Instant.parse("2019-01-27T00:00:00Z");
    private static final Instant TRANTIME_TO = Instant.parse("2019-02-26T00:00:00Z");
    private static final Instant NEW_TRANTIME_FROM = Instant.parse("2019-03-10T00:00:00Z");
    private static final BalanceServiceId BLUE_PAYMENTS = BalanceServiceId.BLUE_PAYMENTS_ID;
    private static final String LAST_TIME_PROPERTY = "market.billing.bank_orders_import_executor.last.time.610";
    private static final String IMPORT_ENABLED = "market.billing.bank_order_import.balance.enabled";
    private static final Long PAST_WINDOW_DAYS = 14L;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private BankOrdersImportService bankOrdersImportService;

    private BankOrdersImportExecutor bankOrdersImportExecutor;

    private static Stream<Arguments> dateRangeData() {
        return Stream.of(
                Arguments.of("Разница будет 20 дней", 6, 20),
                Arguments.of("Разница будет 30 дней (потому что не больше месяца)", 26, 30)
        );
    }

    @BeforeEach
    void setUp() {
        bankOrdersImportExecutor = new BankOrdersImportExecutor(
                BLUE_PAYMENTS, environmentService, bankOrdersImportService, PAST_WINDOW_DAYS
        );
    }

    @Test
    void testImportBankOrders() {
        when(bankOrdersImportService.importBankOrders(any(BalanceServiceId.class), any(Instant.class), any(Instant.class)))
                .thenReturn(NEW_TRANTIME_FROM);

        when(environmentService.getValue(eq(LAST_TIME_PROPERTY), any()))
                .thenReturn("1549756800000");
        when(environmentService.getBooleanValue(eq(IMPORT_ENABLED), anyBoolean())).thenReturn(true);

        bankOrdersImportExecutor.doJob(null);

        verify(environmentService).setValue(
                eq(LAST_TIME_PROPERTY),
                eq(String.valueOf(NEW_TRANTIME_FROM.toEpochMilli()))
        );
        verify(bankOrdersImportService).importBankOrders(eq(BLUE_PAYMENTS), eq(TRANTIME_FROM), eq(TRANTIME_TO));
    }

    @Test
    void testWithNoProperty() {
        when(environmentService.getBooleanValue(eq(IMPORT_ENABLED), anyBoolean())).thenReturn(true);
        NullPointerException exception = Assertions.assertThrows(NullPointerException.class, () -> bankOrdersImportExecutor.doJob(null));
        assertEquals("Property market.billing.bank_orders_import_executor.last.time.610 not found", exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dateRangeData")
    void testDateRange(String descripion, int xDays, int expected) {
        when(environmentService.getBooleanValue(eq(IMPORT_ENABLED), anyBoolean())).thenReturn(true);
        when(bankOrdersImportService.importBankOrders(any(BalanceServiceId.class), any(Instant.class), any(Instant.class)))
                .thenReturn(NEW_TRANTIME_FROM);

        Instant xDaysAgo = Instant.now().minus(Duration.ofDays(xDays));
        Instant fromInstant = Instant.ofEpochMilli(xDaysAgo.toEpochMilli()).minus(PAST_WINDOW_DAYS, ChronoUnit.DAYS);

        when(environmentService.getValue(eq(LAST_TIME_PROPERTY), any()))
                .thenReturn("" + (Long) xDaysAgo.toEpochMilli());

        final BankOrder mockStat = new BankOrder.Builder()
                .setServiceId(123L)
                .setBankOrderId("123")
                .setEventtime(LocalDate.now())
                .setOebsPaymentStatus(OebsPaymentStatus.CONFIRMED)
                .setPaymentBatchId("batchId")
                .setStatus(BankOrderStatus.DONE)
                .setSum(10L)
                .setTrantime(Instant.now())
                .build();

        when(bankOrdersImportService.getBankOrders(eq(BLUE_PAYMENTS), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.singletonList(mockStat));

        bankOrdersImportExecutor.doJob(null);

        ArgumentCaptor<Instant> toInstantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(bankOrdersImportService).importBankOrders(
                eq(BLUE_PAYMENTS),
                eq(fromInstant),
                toInstantCaptor.capture()
        );
        Instant toInstant = toInstantCaptor.getValue();
        Duration between = Duration.between(fromInstant, toInstant);
        assertEquals((int) between.toDays(), expected); // разница не больше месяца
    }
}
