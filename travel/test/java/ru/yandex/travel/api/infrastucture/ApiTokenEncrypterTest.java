package ru.yandex.travel.api.infrastucture;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.commons.proto.EAviaTokenizationOutcome;
import ru.yandex.travel.orders.commons.proto.EPaymentOutcome;
import ru.yandex.travel.orders.commons.proto.TAviaPaymentTestContext;
import ru.yandex.travel.orders.commons.proto.TPaymentTestContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles(value = "test")
public class ApiTokenEncrypterTest {
    @Autowired
    private ApiTokenEncrypter apiTokenEncrypter;

    @Test
    public void testAviaTestContextMapsToPaymentContext() {
        String token;
        TAviaPaymentTestContext aviaContext;
        TPaymentTestContext paymentContext;
        // PaymentCtx -> AviaCtx
        // UNKNOWN
        token = apiTokenEncrypter.toPaymentTestContextToken(
                TPaymentTestContext.newBuilder().setPaymentOutcome(EPaymentOutcome.PO_UNKNOWN)
                        .setPaymentFailureResponseCode("123")
                        .setPaymentFailureResponseDescription("456").build()
        );
        aviaContext = apiTokenEncrypter.fromAviaPaymentTestContextToken(token);
        Assert.assertEquals(EAviaTokenizationOutcome.TO_UNKNOWN, aviaContext.getTokenizationOutcome());


        // SUCCESS
        token = apiTokenEncrypter.toPaymentTestContextToken(
                TPaymentTestContext.newBuilder().setPaymentOutcome(EPaymentOutcome.PO_SUCCESS)
                        .setPaymentFailureResponseCode("123")
                        .setPaymentFailureResponseDescription("456").build()
        );
        aviaContext = apiTokenEncrypter.fromAviaPaymentTestContextToken(token);
        Assert.assertEquals(EAviaTokenizationOutcome.TO_SUCCESS, aviaContext.getTokenizationOutcome());


        // FAILURE
        token = apiTokenEncrypter.toPaymentTestContextToken(
                TPaymentTestContext.newBuilder().setPaymentOutcome(EPaymentOutcome.PO_FAILURE)
                        .setPaymentFailureResponseCode("123")
                        .setPaymentFailureResponseDescription("456").build()
        );
        aviaContext = apiTokenEncrypter.fromAviaPaymentTestContextToken(token);
        Assert.assertEquals(EAviaTokenizationOutcome.TO_FAILURE, aviaContext.getTokenizationOutcome());


        // AviaCtx -> PaymentCtx
        // UNKNOWN
        token = apiTokenEncrypter.toAviaPaymentTestContextToken(
                TAviaPaymentTestContext.newBuilder().setTokenizationOutcome(EAviaTokenizationOutcome.TO_UNKNOWN).build()
        );
        paymentContext = apiTokenEncrypter.fromPaymentTestContextToken(token);
        Assert.assertEquals(EPaymentOutcome.PO_UNKNOWN, paymentContext.getPaymentOutcome());
        Assert.assertEquals("", paymentContext.getPaymentFailureResponseCode());
        Assert.assertEquals("", paymentContext.getPaymentFailureResponseDescription());


        // SUCCESS
        token = apiTokenEncrypter.toAviaPaymentTestContextToken(
                TAviaPaymentTestContext.newBuilder().setTokenizationOutcome(EAviaTokenizationOutcome.TO_SUCCESS).build()
        );
        paymentContext = apiTokenEncrypter.fromPaymentTestContextToken(token);
        Assert.assertEquals(EPaymentOutcome.PO_SUCCESS, paymentContext.getPaymentOutcome());
        Assert.assertEquals("", paymentContext.getPaymentFailureResponseCode());
        Assert.assertEquals("", paymentContext.getPaymentFailureResponseDescription());


        // FAILURE
        token = apiTokenEncrypter.toAviaPaymentTestContextToken(
                TAviaPaymentTestContext.newBuilder().setTokenizationOutcome(EAviaTokenizationOutcome.TO_FAILURE).build()
        );
        paymentContext = apiTokenEncrypter.fromPaymentTestContextToken(token);
        Assert.assertEquals(EPaymentOutcome.PO_FAILURE, paymentContext.getPaymentOutcome());
        Assert.assertEquals("", paymentContext.getPaymentFailureResponseCode());
        Assert.assertEquals("", paymentContext.getPaymentFailureResponseDescription());
    }
}
