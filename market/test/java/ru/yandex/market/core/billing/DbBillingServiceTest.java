package ru.yandex.market.core.billing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.balance.model.BalanceException;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.billing.model.NotEnoughFundsForTransferException;
import ru.yandex.market.core.billing.model.TransferBalanceRequest;
import ru.yandex.market.core.contact.ContactService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link DbBillingService}
 *
 * @author vladvin
 */
class DbBillingServiceTest extends FunctionalTest {
    private static final long CLIENT_ID = 10L;
    private static final long CAMPAIGN_ID = 10042L;
    private static final long CAMPAIGN_ID_56 = 56;
    private static final long ACTED_CAMPAIGN_ID = 10043L;
    private static final long LOCK_NO_WAIT = 0L;
    private static final long SOME_ACTION_ID = 123L;
    private static final BigInteger SERIAL_ID_5555 = BigInteger.valueOf(5555);
    private static final BigDecimal PAYMENT_2222 = BigDecimal.valueOf(2222.55);

    @Autowired
    private Integer marketServiceId;

    @Autowired
    private DbBillingService billingService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        billingService.setMarketServiceId(marketServiceId);

        when(balanceService.getClient(eq(CLIENT_ID))).thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO));

        // h2 не переварит plsql ofc
        doNothing().when(billingService).updateCampaignActualBalance(anyLong());

        XmlRpcException notEnoughFoundsException = new XmlRpcException("<error>" +
                "<msg>Cannot make transfer to &lt;free funds&gt;: not enough free funds None</msg>" +
                "<remain /><wo-rollback>0</wo-rollback><order-eid>&lt;free funds&gt;</order-eid>" +
                "<method>Balance2.CreateTransfer</method><code>NOT_ENOUGH_FUNDS_FOR_TRANSFER</code>" +
                "<parent-codes><code>EXCEPTION</code></parent-codes>" +
                "<contents>Cannot make transfer to &lt;free funds&gt;: not enough free funds None</contents>" +
                "</error>");

        doThrow(new BalanceException(notEnoughFoundsException)).when(balanceService)
                .transfer(anyLong(), eq(ACTED_CAMPAIGN_ID), anyLong(), any(BigDecimal.class), any(BigDecimal.class));

        when(contactService.getClientIdByUid(anyLong())).thenReturn(CLIENT_ID);
    }

    @Test
    @DbUnitDataSet(before = "DbBillingServiceTest.before.csv")
    void shouldThrowErrorOnActedFundsTransfer() {
        TransferBalanceRequest request = new TransferBalanceRequest();
        request.setFromCampaignId(ACTED_CAMPAIGN_ID);
        request.setToCampaignId(CAMPAIGN_ID);
        request.setAmount(BigDecimal.valueOf(1));

        assertThrows(
                NotEnoughFundsForTransferException.class,
                () -> billingService.transferBalance(request, CLIENT_ID, 0L)
        );
    }

    /**
     * Проверяем рабостоспособность campaign_balance_details_view.
     * Она используется в процедуре sp_update_campaign_state, которая запускается в {@link DbBillingService}
     */
    @Test
    @DbUnitDataSet(before = "CampaignBalanceDetailsSmokeTest.csv")
    void campaignBalanceDetailsSmokeTest() {
        List<Long> actualResult = jdbcTemplate.query("select * from market_billing.campaign_balance_details_view",
                (rs, rowNum) -> rs.getLong("days_to_spend_remainder"));
        assertThat(actualResult, hasSize(3));
        assertThat(actualResult, containsInAnyOrder(365L, 0L, 365L));
    }

    @DisplayName("Регистрации платежа для кампании с отсутствующей историей платежей, но добавленной в исключения, не приведет к ошибке")
    @Test
    @DbUnitDataSet(
            before = "DbBillingServiceTest.registerPayment.before.csv",
            after = "DbBillingServiceTest.registerPayment.nothing_changed.after.csv"
    )
    void test_registerPayment_campaignNotFoundDoesNotThrownWhenMarked() {
        billingService.registerPayment(
                marketServiceId,
                CAMPAIGN_ID_56,
                PAYMENT_2222,
                SERIAL_ID_5555,
                LOCK_NO_WAIT,
                SOME_ACTION_ID
        );

        Mockito.verify(billingService, never())
                .updateCampaignActualBalance(CAMPAIGN_ID);
    }

    @DisplayName("Платеж без serialNumber - ok")
    @Test
    @DbUnitDataSet(
            before = "DbBillingServiceTest.registerPayment.before.csv",
            after = "DbBillingServiceTest.registerPayment.after.csv"
    )
    void test_registerPayment_paymentOkWithNullSerialNumber() {
        billingService.registerPayment(
                marketServiceId,
                CAMPAIGN_ID,
                PAYMENT_2222,
                null,
                LOCK_NO_WAIT,
                SOME_ACTION_ID
        );

        Mockito.verify(billingService)
                .updateCampaignActualBalance(CAMPAIGN_ID);
    }

    @DisplayName("Если серийный номер меньше существующего, то ничего не сохраняется")
    @Test
    @DbUnitDataSet(
            before = "DbBillingServiceTest.registerPayment.before.csv",
            after = "DbBillingServiceTest.registerPayment.nothing_changed.after.csv"
    )
    void test_registerPayment_when_serialNumberLtExisted_then_doNothing() {
        billingService.registerPayment(
                marketServiceId,
                CAMPAIGN_ID,
                PAYMENT_2222,
                BigInteger.TEN,
                LOCK_NO_WAIT,
                SOME_ACTION_ID
        );

        Mockito.verify(billingService, never())
                .updateCampaignActualBalance(CAMPAIGN_ID);
    }

    @DisplayName("Платеж без изменения общей суммы начислений")
    @Test
    @DbUnitDataSet(
            before = "DbBillingServiceTest.registerPayment.before.csv",
            after = "DbBillingServiceTest.registerPayment.same_balance.after.csv"
    )
    void test_registerPayment_when_sameBalance_then_changeSerialOnly() {
        billingService.registerPayment(
                marketServiceId,
                CAMPAIGN_ID,
                BigDecimal.valueOf(101),
                SERIAL_ID_5555,
                LOCK_NO_WAIT,
                SOME_ACTION_ID
        );

        Mockito.verify(billingService, never())
                .updateCampaignActualBalance(CAMPAIGN_ID);
    }

    @DisplayName("Понижение значения paidTotalUe")
    @Test
    @DbUnitDataSet(
            before = "DbBillingServiceTest.registerPayment.before.csv",
            after = "DbBillingServiceTest.registerPayment.reduce_paid_total.after.csv"
    )
    void test_check_lowering_of_balance() {
        billingService.registerPayment(
                marketServiceId,
                CAMPAIGN_ID,
                BigDecimal.ONE,
                SERIAL_ID_5555,
                LOCK_NO_WAIT,
                SOME_ACTION_ID
        );

        Mockito.verify(billingService)
                .updateCampaignActualBalance(CAMPAIGN_ID);
    }

    @DisplayName("Ошибка, если идет регистрации платежа для кампании с отсутствующей историей платежей")
    @Test
    @DbUnitDataSet(before = "DbBillingServiceTest.registerPayment.before.csv")
    void test_registerPayment_campaignNotFound() {
        RuntimeException ex = Assertions.assertThrows(
                RuntimeException.class,
                () -> billingService.registerPayment(
                        marketServiceId,
                        1000500774,
                        PAYMENT_2222,
                        SERIAL_ID_5555,
                        LOCK_NO_WAIT,
                        SOME_ACTION_ID
                )
        );

        assertThat(ex.getMessage(), Matchers.is("Campaign not found: 1000500774"));

        Mockito.verify(billingService, never())
                .updateCampaignActualBalance(CAMPAIGN_ID);
    }

    @DisplayName("Ошибка, в случае отрицательного значения в поле суммы платежей")
    @Test
    @DbUnitDataSet(before = "DbBillingServiceTest.registerPayment.before.csv")
    void test_registerPayment_when_negativePaidTotal_then_throw() {
        RuntimeException ex = Assertions.assertThrows(
                RuntimeException.class,
                () ->
                        billingService.registerPayment(
                                marketServiceId,
                                CAMPAIGN_ID,
                                PAYMENT_2222.negate(),
                                null,
                                LOCK_NO_WAIT,
                                SOME_ACTION_ID
                        )
        );

        assertThat(ex.getMessage(), Matchers.is("Invalid payments total sum: -2222.55"));
    }

}
