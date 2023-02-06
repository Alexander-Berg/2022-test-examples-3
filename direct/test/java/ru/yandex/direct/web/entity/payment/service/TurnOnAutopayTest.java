package ru.yandex.direct.web.entity.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.balance.client.model.response.GetCardBindingURLResponse;
import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes;
import ru.yandex.direct.core.entity.payment.model.AutopayParams;
import ru.yandex.direct.core.entity.payment.model.TurnOnAutopayJobParams;
import ru.yandex.direct.core.entity.payment.model.TurnOnAutopayJobResult;
import ru.yandex.direct.core.entity.payment.service.AutopayService;
import ru.yandex.direct.core.entity.payment.service.PaymentService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TurnOnAutopayTest {

    private static final String HOST = "host.ru";
    private static final int SHARD = 1;
    private static final Long WALLET_CID = 5L;
    private static final String BINDING_URL = "www.bind.ru";
    private static final String PURCHASE_TOKEN = "ffffffff";
    private static final LocalDateTime AUTOPAY_LAST_CHANGE = LocalDateTime.now();
    private static final String CARD_ID = "card-12345";
    private static final Long JOB_ID = 666L;
    private static final CurrencyCode DEFAULT_CURRENCY = CurrencyCode.RUB;

    @Autowired
    private Steps steps;

    private PaymentService paymentService;
    private DbQueueRepository dbQueueRepository;
    private AutopayService autopayService;
    private UserInfo userInfo;


    @Before
    public void prepare() {
        userInfo = steps.userSteps().createUser(generateNewUser());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn(HOST);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        BalanceService balanceService = mock(BalanceService.class);
        when(balanceService.getCardBinding(any(), any(), any(), eq(false))).thenReturn(
                new GetCardBindingURLResponse()
                        .withBindingUrl(BINDING_URL)
                        .withPurchaseToken(PURCHASE_TOKEN)
        );

        WalletRepository walletRepository = mock(WalletRepository.class);
        when(walletRepository.getAutopayLastChange(eq(SHARD), any())).thenReturn(AUTOPAY_LAST_CHANGE);

        dbQueueRepository = mock(DbQueueRepository.class);

        when(dbQueueRepository.insertJob(eq(SHARD), eq(DbQueueJobTypes.TURN_ON_AUTOPAY), eq(userInfo.getClientId()),
                eq(userInfo.getUid()), any()))
                .thenReturn(new DbQueueJob<TurnOnAutopayJobParams, TurnOnAutopayJobResult>().withId(JOB_ID));

        autopayService = spy(new AutopayService(walletRepository, null, null, null, balanceService, dbQueueRepository, null));
        doNothing().when(autopayService).turnOnAutopay(anyInt(), any(), any(), any());

        paymentService = new PaymentService(null, balanceService, walletRepository, null,
                autopayService, null, null, null, null);
    }

    @Test
    public void test_emptyParams() {

        AutopayParams autopayParams = new AutopayParams();

        String resultBindingUrl = paymentService.turnOnAutopay(userInfo.getUser(), SHARD, WALLET_CID,
                DEFAULT_CURRENCY, autopayParams, false, "");
        assertNull(resultBindingUrl);

        verify(autopayService, never()).turnOnAutopay(anyInt(), any(), any(), any());
        verify(dbQueueRepository, never()).insertJob(anyInt(), any(), any(), anyLong(), any());
    }

    @Test
    public void test_onlyPaymentSum() {

        AutopayParams autopayParams = new AutopayParams().withPaymentSum(BigDecimal.valueOf(1200L));

        String resultBindingUrl = paymentService.turnOnAutopay(userInfo.getUser(), SHARD, WALLET_CID, DEFAULT_CURRENCY,
                autopayParams, false, "");
        assertNull(resultBindingUrl);

        verify(autopayService, never()).turnOnAutopay(anyInt(), any(), any(), any());
        verify(dbQueueRepository, never()).insertJob(anyInt(), any(), any(), anyLong(), any());
    }

    @Test
    public void test_withCardId() {

        AutopayParams autopayParams = new AutopayParams()
                .withPaymentSum(BigDecimal.valueOf(1200L))
                .withRemainingSum(BigDecimal.ZERO)
                .withCardId(CARD_ID);

        String resultBindingUrl = paymentService.turnOnAutopay(userInfo.getUser(), SHARD, WALLET_CID,
                DEFAULT_CURRENCY, autopayParams, false, "");
        assertNull(resultBindingUrl);

        verify(autopayService).turnOnAutopay(SHARD, userInfo.getUid(), userInfo.getClientId(), autopayParams);
        verify(dbQueueRepository, never()).insertJob(anyInt(), any(), any(), anyLong(), any());
    }

    @Test
    public void test_withoutCardId() {

        AutopayParams autopayParams = new AutopayParams()
                .withPaymentSum(BigDecimal.valueOf(1200L))
                .withRemainingSum(BigDecimal.ZERO);

        String resultBindingUrl = paymentService.turnOnAutopay(userInfo.getUser(), SHARD, WALLET_CID,
                DEFAULT_CURRENCY, autopayParams, false, "");
        assertEquals(BINDING_URL, resultBindingUrl);

        verify(autopayService, never()).turnOnAutopay(anyInt(), any(), any(), any());

        verify(dbQueueRepository).
                insertJob(
                        SHARD,
                        DbQueueJobTypes.TURN_ON_AUTOPAY,
                        userInfo.getClientId(),
                        userInfo.getUid(),
                        new TurnOnAutopayJobParams()
                                .withPurchaseToken(PURCHASE_TOKEN)
                                .withAutopayParams(autopayParams)
                                .withActualTime(AUTOPAY_LAST_CHANGE)
                );
    }

    @After
    public void after() {
        RequestContextHolder.resetRequestAttributes();
    }
}
