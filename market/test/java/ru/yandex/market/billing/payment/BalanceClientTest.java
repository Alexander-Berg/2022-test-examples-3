package ru.yandex.market.billing.payment;

import java.time.LocalDate;
import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.balance.ClientOverdraft;
import ru.yandex.market.core.balance.OverdraftService;
import ru.yandex.market.core.indexer.UnitTestTransactionTemplate;

/**
 * @author zoom
 */
@RunWith(MockitoJUnitRunner.class)
public class BalanceClientTest {

    @InjectMocks
    private BalanceClient balanceClient;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private OverdraftService overdraftService;

    @Before
    public void setUp() {
        balanceClient.setTransactionTemplate(new UnitTestTransactionTemplate());
    }

    @Test
    public void shouldStoreOverdraft() throws XmlRpcException {
        Mockito.doNothing().when(overdraftService).store(Mockito.any());
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3",
                                "Tid", "32",
                                "OverdraftLimit", "123.3",
                                "OverdraftSpent", "1.00",
                                "MinPaymentTerm", "2020-01-01")
                ));
        ClientOverdraft expectedOverdraft =
                new ClientOverdraft(3, 12330, 100, LocalDate.of(2020, 1, 1), 32);
        Mockito.verify(overdraftService).store(Mockito.eq(expectedOverdraft));
    }

    @Test
    public void shouldStoreZeroOverdraft() throws XmlRpcException {
        Mockito.doNothing().when(overdraftService).store(Mockito.any());
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3",
                                "Tid", "32",
                                "OverdraftLimit", "123.3",
                                "OverdraftSpent", "0",
                                "MinPaymentTerm", "2020-01-01")
                ));
        ClientOverdraft expectedOverdraft =
                new ClientOverdraft(3, 12330, 0, LocalDate.of(2020, 1, 1), 32);
        Mockito.verify(overdraftService).store(Mockito.eq(expectedOverdraft));
    }

    @Test
    public void shouldStoreZeroPaymentDeadline() throws XmlRpcException {
        Mockito.doNothing().when(overdraftService).store(Mockito.any());
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3",
                                "Tid", "32",
                                "OverdraftLimit", "123.3",
                                "OverdraftSpent", "0",
                                "MinPaymentTerm", "0000-00-00")
                ));
        ClientOverdraft expectedOverdraft =
                new ClientOverdraft(3, 12330, 0, null, 32);
        Mockito.verify(overdraftService).store(Mockito.eq(expectedOverdraft));
    }

    @Test(expected = XmlRpcException.class)
    public void shouldThrowExceptionWhenClientIdIsIncorrect() throws XmlRpcException {
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3a",
                                "Tid", "32",
                                "OverdraftLimit", "123.3",
                                "OverdraftSpent", "1.00",
                                "MinPaymentTerm", "2020-01-01")
                ));
        Assert.fail("Incorrect client id was parsed well");
    }

    @Test(expected = XmlRpcException.class)
    public void shouldThrowExceptionWhenUpdateIdIsIncorrect() throws XmlRpcException {
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3",
                                "Tid", "32a",
                                "OverdraftLimit", "123.3",
                                "OverdraftSpent", "1.00",
                                "MinPaymentTerm", "2020-01-01")
                ));
        Assert.fail("Incorrect update id was parsed well");
    }

    @Test(expected = XmlRpcException.class)
    public void shouldThrowExceptionWhenLimitIsIncorrect() throws XmlRpcException {
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3",
                                "Tid", "32",
                                "OverdraftLimit", "123.3a",
                                "OverdraftSpent", "1.00",
                                "MinPaymentTerm", "2020-01-01")
                ));
        Assert.fail("Incorrect limit was parsed well");
    }

    @Test(expected = XmlRpcException.class)
    public void shouldThrowExceptionWhenSpentIsIncorrect() throws XmlRpcException {
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3",
                                "Tid", "32",
                                "OverdraftLimit", "123.3",
                                "OverdraftSpent", "1.00a",
                                "MinPaymentTerm", "2020-01-01")
                ));
        Assert.fail("Incorrect spent was parsed well");
    }

    @Test(expected = XmlRpcException.class)
    public void shouldThrowExceptionWhenPaymentDeadlineIsIncorrect() throws XmlRpcException {
        balanceClient.NotifyClient2(
                new HashMap<>(
                        ImmutableMap.of(
                                "ClientID", "3",
                                "Tid", "32",
                                "OverdraftLimit", "123.3",
                                "OverdraftSpent", "1.00",
                                "MinPaymentTerm", "2020-01-01a")
                ));
        Assert.fail("Incorrect payment deadline was parsed well");
    }

}