package ru.yandex.market.adv.b2bmonetization.programs.interactor;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockserver.model.HttpRequest.request;

@MockServerSettings(ports = 12233)
class ProgramSendToActivationQueueInteractorTest extends AbstractMonetizationMockServerTest {

    @Autowired
    private ProgramSendToActivationQueueInteractor programSendToActivationQueueInteractor;

    ProgramSendToActivationQueueInteractorTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Все заявки отправлены в PL")
    @DbUnitDataSet(
            before = "ProgramSendToActivationQueueInteractor/csv/sendForActivation_sent_StatusEnqueued.before.csv",
            after = "ProgramSendToActivationQueueInteractor/csv/sendForActivation_sent_StatusEnqueued.after.csv"
    )
    @Test
    void sendForActivation_sent_StatusEnqueued() {
        mockServerPath("POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_StatusEnqueued_1.json",
                Map.of(),
                200,
                "ProgramSendToActivationQueueInteractor/json/response/sendForActivation_sent_StatusEnqueued.json"
        );
        mockServerPath("POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_StatusEnqueued_2.json",
                Map.of(),
                200,
                "ProgramSendToActivationQueueInteractor/json/response/sendForActivation_sent_StatusEnqueued.json"
        );
        mockServerPath("POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_StatusEnqueued_3.json",
                Map.of(),
                200,
                "ProgramSendToActivationQueueInteractor/json/response/sendForActivation_sent_StatusEnqueued.json"
        );

        programSendToActivationQueueInteractor.sendForActivation();

        mockServerVerify(
                "POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_StatusEnqueued_1.json"
        );
        mockServerVerify(
                "POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_StatusEnqueued_2.json"
        );
        mockServerVerify(
                "POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_StatusEnqueued_3.json"
        );

        mockServerVerifyTimes(3);
    }

    @DisplayName("Исключительная ситуация из-за ошибки в PL")
    @DbUnitDataSet(
            before = "ProgramSendToActivationQueueInteractor/csv/sendForActivation_sent_500StatusNotChanged.before.csv",
            after = "ProgramSendToActivationQueueInteractor/csv/sendForActivation_sent_500StatusNotChanged.after.csv"
    )
    @Test
    void sendForActivation_sent_500StatusNotChanged() {
        mockServerPath("POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_500StatusNotChanged_1.json",
                Map.of(),
                500,
                null
        );
        mockServerPath("POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_500StatusNotChanged_2.json",
                Map.of(),
                500,
                null
        );
        mockServerPath("POST",
                "/api/v1/public/programs/activation",
                "ProgramSendToActivationQueueInteractor/json/request/sendForActivation_sent_500StatusNotChanged_3.json",
                Map.of(),
                500,
                null
        );

        Assertions.assertThatThrownBy(() -> programSendToActivationQueueInteractor.sendForActivation())
                .isInstanceOf(IllegalStateException.class);
    }

    private void mockServerVerifyTimes(int verificationTimes) {
        server.verify(
                request()
                        .withMethod("POST")
                        .withPath("/api/v1/public/programs/activation"),
                VerificationTimes.exactly(verificationTimes)
        );
    }
}
