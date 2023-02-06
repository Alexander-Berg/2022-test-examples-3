package ru.yandex.chemodan.app.psbilling.core.billing.groups.payment;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.exception.BalanceErrorCodeException;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.misc.test.Assert;

public class PaymentCheckerTest extends AbstractPsBillingCoreTest {

    @Autowired
    private PaymentChecker paymentChecker;

    @Autowired
    private GroupTrustPaymentRequestDao trustPaymentRequestDao;

    @Test
    public void doNothing() {
        Assert.isEmpty(trustPaymentRequestDao.findAll());

        paymentChecker.checkPaymentRequestStatus("какой-то идентификатор");

        Assert.isEmpty(trustPaymentRequestDao.findAll());
    }

    @Test
    public void successPayCheck() {
        long clientId = 123L;
        String transactionId = "какой-то идентификатор";

        val request = paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.empty())
        );

        setupCheckRequest(s -> {
            s.setRequestId(request.getRequestId());
            s.setResponseCode(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);
            s.setTransactionId(transactionId);
        });

        paymentChecker.checkPaymentRequestStatus(request.getRequestId());

        val updated = trustPaymentRequestDao.findById(request.getId());

        Assert.equals(PaymentRequestStatus.SUCCESS, updated.getStatus());
        Assert.none(updated.getError());
        Assert.some(transactionId, updated.getTransactionId());
    }

    @Test
    public void cancelPayCheck() {
        long clientId = 123L;
        String transactionId = "какой-то идентификатор";
        String error = "какая-то ошибка";

        val request = paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.empty())
        );

        setupCheckRequest(s -> {
            s.setResponseCode(error);
            s.setRequestId(request.getRequestId());
            s.setTransactionId(transactionId);
        });

        paymentChecker.checkPaymentRequestStatus(request.getRequestId());

        val updated = trustPaymentRequestDao.findById(request.getId());

        Assert.equals(PaymentRequestStatus.CANCELLED, updated.getStatus());
        Assert.some(error, updated.getError());
        Assert.some(transactionId, updated.getTransactionId());
    }

    @Test
    public void withoutResponseCodePayCheck() {
        long clientId = 123L;

        val request = paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.empty())
        );

        setupCheckRequest(s -> s.setRequestId(request.getRequestId()));

        paymentChecker.checkPaymentRequestStatus(request.getRequestId());

        val notUpdated = trustPaymentRequestDao.findById(request.getId());

        Assert.equals(request, notUpdated);
    }

    @Test
    public void noPaymentExceptionPayCheck() {
        long clientId = 123L;

        val request = paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.empty())
        );

        setupErrorCheckRequest(
                balanceErrorCodeException(BalanceErrorCodeException.BalanceErrorCode.NO_PAYMENTS_FOR_REQUEST)
        );

        paymentChecker.checkPaymentRequestStatus(request.getRequestId());

        val notUpdated = trustPaymentRequestDao.findById(request.getId());

        Assert.equals(request, notUpdated);
    }

    @Test
    public void noPaymentExceptionExpiredPayCheck() {
        long clientId = 123L;

        val request = paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.empty())
        );


        setupErrorCheckRequest(
                balanceErrorCodeException(BalanceErrorCodeException.BalanceErrorCode.NO_PAYMENTS_FOR_REQUEST)
        );

        DateUtils.shiftTime(settings.getAcceptableGroupPaymentExpirationTime().plus(1));

        paymentChecker.checkPaymentRequestStatus(request.getRequestId());

        val updated = trustPaymentRequestDao.findById(request.getId());

        Assert.equals(PaymentRequestStatus.CANCELLED, updated.getStatus());
        Assert.some(updated.getError());
        Assert.none(updated.getTransactionId());
    }

    @Test
    public void noPaymentExceptionExpiredPayCheckWithTransaction() {
        long clientId = 123L;
        String transactionId = "какой-то идентификатор";

        val request = paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.empty())
        );


        setupErrorCheckRequest(
                balanceErrorCodeException(BalanceErrorCodeException.BalanceErrorCode.NO_PAYMENTS_FOR_REQUEST)
        );

        DateUtils.shiftTime(settings.getAcceptableGroupPaymentExpirationTime().plus(1));

        paymentChecker.checkPaymentRequestStatus(request.getRequestId(), transactionId);

        val updated = trustPaymentRequestDao.findById(request.getId());

        Assert.equals(PaymentRequestStatus.CANCELLED, updated.getStatus());
        Assert.some(updated.getError());
        Assert.some(transactionId, updated.getTransactionId());
    }

    @NotNull
    private BalanceErrorCodeException balanceErrorCodeException(BalanceErrorCodeException.BalanceErrorCode... codes) {
        return new BalanceErrorCodeException(Cf.arrayList(codes).map(Enum::name), "", "");
    }

    @Test
    public void undefinedExceptionPayCheck() {
        long clientId = 123L;

        val request = paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.empty())
        );

        setupErrorCheckRequest(balanceErrorCodeException());

        Assert.assertThrows(() -> paymentChecker.checkPaymentRequestStatus(request.getRequestId()),
                BalanceErrorCodeException.class);

        val notUpdated = trustPaymentRequestDao.findById(request.getId());

        Assert.equals(request, notUpdated);
    }

    private void setupErrorCheckRequest(BalanceErrorCodeException classException) {
        psBillingCoreMocksConfig.balanceClientStub().turnOnMockitoForMethod("checkRequestPayment");

        Mockito.doThrow(classException)
                .when(psBillingCoreMocksConfig.balanceClientStub().getBalanceClientMock())
                .checkRequestPayment(
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.anyLong()
                );
    }

    private void setupCheckRequest(Function1V<CheckRequestPaymentResponse> applyFunction) {
        val resp = new CheckRequestPaymentResponse();
        applyFunction.apply(resp);

        psBillingCoreMocksConfig.balanceClientStub().addPaymentRequestStatusCheck(resp);
    }
}
