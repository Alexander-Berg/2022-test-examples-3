package ru.yandex.market.adv.content.manager.interactor.moderation.machine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.MatchType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.content.manager.AbstractContentManagerMockServerTest;
import ru.yandex.market.adv.content.manager.exception.TemplateNotificationException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;

/**
 * Date: 09.11.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@MockServerSettings(ports = 12233)
class ModerationTaskPublishedToEndedMbiInteractorTest extends AbstractContentManagerMockServerTest {

    @Autowired
    @Qualifier("tmsModerationTaskPublishedToEndedExecutor")
    private Executor tmsModerationTaskPublishedToEndedExecutor;

    ModerationTaskPublishedToEndedMbiInteractorTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Проверка работоспособности job tmsModerationTaskPublishedToEndedExecutor.")
    @DbUnitDataSet(
            before = "ModerationTaskPublishedToEndedMbiInteractor/csv/" +
                    "tmsModerationTaskPublishedToEndedExecutor_threeModerationTask_twoNotificationSend.before.csv",
            after = "ModerationTaskPublishedToEndedMbiInteractor/csv/" +
                    "tmsModerationTaskPublishedToEndedExecutor_threeModerationTask_twoNotificationSend.after.csv"
    )
    @Test
    void tmsModerationTaskPublishedToEndedExecutor_threeModerationTask_twoNotificationSend() {
        mockServerPath("POST",
                "/notification/business/batch",
                "ModerationTaskPublishedToEndedMbiInteractor/json/request/427.json",
                Map.of(),
                404,
                null
        );
        mockServerPath("POST",
                "/notification/business/batch",
                "ModerationTaskPublishedToEndedMbiInteractor/json/request/428.json",
                Map.of(),
                200,
                "ModerationTaskPublishedToEndedMbiInteractor/json/response/428.json"
        );
        mockServerPath("POST",
                "/notification/business/batch",
                "ModerationTaskPublishedToEndedMbiInteractor/json/request/523.json",
                Map.of(),
                200,
                "ModerationTaskPublishedToEndedMbiInteractor/json/response/523.json"
        );
        mockServerPath("POST",
                "/notification/business/batch",
                "ModerationTaskPublishedToEndedMbiInteractor/json/request/429.json",
                Map.of(),
                200,
                "ModerationTaskPublishedToEndedMbiInteractor/json/response/429.json"
        );

        Assertions.assertThatThrownBy(() -> tmsModerationTaskPublishedToEndedExecutor.doJob(mockContext()))
                .isInstanceOf(MbiOpenApiClientResponseException.class)
                .hasMessage("Not Found")
                .hasSuppressedException(
                        new TemplateNotificationException("java.lang.NullPointerException")
                );

        server.verify(
                request()
                        .withMethod("POST")
                        .withPath("/notification/business/batch")
                        .withBody(
                                json(
                                        loadFile(
                                                "ModerationTaskPublishedToEndedMbiInteractor/json/request/" +
                                                        "427.json"
                                        ),
                                        StandardCharsets.UTF_8,
                                        MatchType.STRICT
                                )
                        ),
                VerificationTimes.once()
        );
        server.verify(
                request()
                        .withMethod("POST")
                        .withPath("/notification/business/batch")
                        .withBody(
                                json(
                                        loadFile(
                                                "ModerationTaskPublishedToEndedMbiInteractor/json/request/" +
                                                        "523.json"
                                        ),
                                        StandardCharsets.UTF_8,
                                        MatchType.STRICT
                                )
                        ),
                VerificationTimes.once()
        );
        server.verify(
                request()
                        .withMethod("POST")
                        .withPath("/notification/business/batch")
                        .withBody(
                                json(
                                        loadFile(
                                                "ModerationTaskPublishedToEndedMbiInteractor/json/request/" +
                                                        "427.json"
                                        ),
                                        StandardCharsets.UTF_8,
                                        MatchType.STRICT
                                )
                        ),
                VerificationTimes.once()
        );
        server.verify(
                request()
                        .withMethod("POST")
                        .withPath("/notification/business/batch")
                        .withBody(
                                json(
                                        loadFile(
                                                "ModerationTaskPublishedToEndedMbiInteractor/json/request/" +
                                                        "429.json"
                                        ),
                                        StandardCharsets.UTF_8,
                                        MatchType.STRICT
                                )
                        ),
                VerificationTimes.once()
        );
    }
}
