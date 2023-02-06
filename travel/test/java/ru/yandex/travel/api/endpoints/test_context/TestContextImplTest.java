package ru.yandex.travel.api.endpoints.test_context;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import ru.yandex.travel.api.config.common.EncryptionConfigurationProperties;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.AviaTestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.AviaTestContextRspV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.PaymentTestContextReqV1;
import ru.yandex.travel.api.endpoints.test_context.req_rsp.PaymentTestContextRspV1;
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter;
import ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOnRedirOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaConfirmationOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaMqEventOutcome;
import ru.yandex.travel.orders.commons.proto.EAviaTokenizationOutcome;
import ru.yandex.travel.orders.commons.proto.EPaymentOutcome;
import ru.yandex.travel.orders.commons.proto.TAviaTestContext;
import ru.yandex.travel.orders.commons.proto.TPaymentTestContext;

import static org.assertj.core.api.Assertions.assertThat;

public class TestContextImplTest {
    private final ApiTokenEncrypter encrypter = new ApiTokenEncrypter(EncryptionConfigurationProperties.builder()
            .encryptionKey("someCryptoKey")
            .build());
    private final TestContextImpl service = new TestContextImpl(null, null, encrypter);

    @Test
    public void testPaymentTestContextParamsPassed() {
        CompletableFuture<PaymentTestContextRspV1> result = service
                .generatePaymentToken(PaymentTestContextReqV1.builder()
                        .minUserActionDelay(Duration.ofSeconds(10))
                        .maxUserActionDelay(Duration.ofSeconds(30))
                        .paymentUrl("some://payment.url")
                        .build());

        PaymentTestContextRspV1 rsp = result.join();
        // have no idea why it's implemented this way
        assertThat(rsp.getToken()).isEqualTo(rsp.getPaymentTestContextToken());

        TPaymentTestContext ptc = encrypter.fromPaymentTestContextToken(rsp.getToken());
        assertThat(ptc.getPaymentOutcome()).isEqualTo(EPaymentOutcome.PO_SUCCESS);
        assertThat(ptc.getUserActionDelay().getDelayMin()).isEqualTo(10_000);
        assertThat(ptc.getUserActionDelay().getDelayMax()).isEqualTo(30_000);
        assertThat(ptc.getPaymentUrl()).isEqualTo("some://payment.url");
    }

    @Test
    public void testPaymentTestContextFailsWithFuture() {
        CompletableFuture<PaymentTestContextRspV1> result = service.generatePaymentToken(null);
        assertThat(result).isCompletedExceptionally();
    }

    @Test
    public void testGenerateAviaToken_withoutVariants() {
        CompletableFuture<AviaTestContextRspV1> result = service
                .generateAviaToken(AviaTestContextReqV1.builder()
                        .checkAvailabilityBeforeBookingOutcome(EAviaCheckAvailabilityOutcome.CAO_SUCCESS)
                        .checkAvailabilityOnRedirOutcome(EAviaCheckAvailabilityOnRedirOutcome.CAOR_SUCCESS)
                        .confirmationOutcome(EAviaConfirmationOutcome.CO_SUCCESS)
                        .tokenizationOutcome(EAviaTokenizationOutcome.TO_SUCCESS)
                        .mqEventOutcome(EAviaMqEventOutcome.MEO_SUCCESS)
                        .build());

        AviaTestContextRspV1 rsp = result.join();

        TAviaTestContext ptc = encrypter.fromAviaTestContextToken(rsp.getToken());
        assertThat(ptc.getCheckAvailabilityOutcome()).isEqualTo(EAviaCheckAvailabilityOutcome.CAO_SUCCESS);
        assertThat(ptc.getCheckAvailabilityOnRedirOutcome()).isEqualTo(EAviaCheckAvailabilityOnRedirOutcome.CAOR_SUCCESS);
        assertThat(ptc.getConfirmationOutcome()).isEqualTo(EAviaConfirmationOutcome.CO_SUCCESS);
        assertThat(ptc.getMqEventOutcome()).isEqualTo(EAviaMqEventOutcome.MEO_SUCCESS);
    }
}
