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
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.mockserver.model.HttpRequest.request;

@MockServerSettings(ports = 12233)
public class ProgramResetInteractorTest extends AbstractMonetizationMockServerTest {

    @Autowired
    private ProgramResetInteractor programResetInteractor;

    public ProgramResetInteractorTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Сброс неактивированных заявок участия в программах.")
    @DbUnitDataSet(
            before = "ProgramResetInteractor/csv/programReset_existExpired_doReset.before.csv",
            after = "ProgramResetInteractor/csv/programReset_existExpired_doReset.after.csv"
    )
    @Test
    public void programReset_existExpired_doReset() {
        mockServerPath("POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/notificationPartnerReset_1.json",
                Map.of(),
                200,
                "ProgramResetInteractor/json/response/notificationPartnerReset_1.json"
        );
        mockServerPath("POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/notificationPartnerReset_2.json",
                Map.of(),
                200,
                "ProgramResetInteractor/json/response/notificationPartnerReset_2.json"
        );
        mockServerPath("POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/notificationPartnerReset_3.json",
                Map.of(),
                200,
                "ProgramResetInteractor/json/response/notificationPartnerReset_3.json"
        );

        programResetInteractor.resetProgram();

        mockServerVerify(
                "POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/notificationPartnerReset_1.json"
        );
        mockServerVerify(
                "POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/notificationPartnerReset_2.json"
        );
        mockServerVerify(
                "POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/notificationPartnerReset_3.json"
        );
        mockServerVerifyTimes(3);
    }

    @DisplayName("Сброс неактивированных заявок участия в программах завершился ошибкой.")
    @DbUnitDataSet(
            before = "ProgramResetInteractor/csv/programReset_existExpired_error.before.csv",
            after = "ProgramResetInteractor/csv/programReset_existExpired_error.after.csv"
    )
    @Test
    public void programReset_existExpired_error() {
        mockServerPath("POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/programReset_existExpired_error_1.json",
                Map.of(),
                400,
                "ProgramResetInteractor/json/response/programReset_existExpired_error.json"
        );
        mockServerPath("POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/programReset_existExpired_error_2.json",
                Map.of(),
                400,
                "ProgramResetInteractor/json/response/programReset_existExpired_error.json"
        );
        mockServerPath("POST",
                "/notification/partner",
                "ProgramResetInteractor/json/request/programReset_existExpired_error_3.json",
                Map.of(),
                400,
                "ProgramResetInteractor/json/response/programReset_existExpired_error.json"
        );

        Assertions.assertThatThrownBy(() -> programResetInteractor.resetProgram())
                .isInstanceOf(MbiOpenApiClientResponseException.class);
    }

    private void mockServerVerifyTimes(int verificationTimes) {
        server.verify(
                request()
                        .withMethod("POST")
                        .withPath("/notification/partner"),
                VerificationTimes.exactly(verificationTimes)
        );
    }
}
