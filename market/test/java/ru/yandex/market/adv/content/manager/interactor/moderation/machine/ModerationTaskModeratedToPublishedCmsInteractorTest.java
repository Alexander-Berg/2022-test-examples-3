package ru.yandex.market.adv.content.manager.interactor.moderation.machine;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.content.manager.AbstractContentManagerMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.mockserver.model.HttpRequest.request;

/**
 * Date: 21.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@SuppressWarnings("checkstyle:lineLength")
@MockServerSettings(ports = 12233)
public class ModerationTaskModeratedToPublishedCmsInteractorTest extends AbstractContentManagerMockServerTest {

    @Autowired
    @Qualifier("tmsModerationTaskModeratedToPublishedExecutor")
    private Executor tmsModerationTaskModeratedToPublishedExecutor;

    ModerationTaskModeratedToPublishedCmsInteractorTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Проверка работоспособности job tmsModerationTaskModeratedToPublishedExecutor.")
    @DbUnitDataSet(
            before = "ModerationTaskModeratedToPublishedCmsInteractor/csv/" +
                    "tmsModerationTaskModeratedToPublishedExecutor_threeModerationTask_threeTemplateUpdate.before.csv",
            after = "ModerationTaskModeratedToPublishedCmsInteractor/csv/" +
                    "tmsModerationTaskModeratedToPublishedExecutor_threeModerationTask_threeTemplateUpdate.after.csv"
    )
    @Test
    public void tmsModerationTaskModeratedToPublishedExecutor_threeModerationTask_threeTemplateUpdate() {
        mockServerPath("PUT",
                "/v1/documents/4123/published",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "revision_id", List.of("5232")
                ),
                404,
                null
        );
        mockServerPath("PUT",
                "/v1/documents/4233/published",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "revision_id", List.of("5232")
                ),
                200,
                "ModerationTaskModeratedToPublishedCmsInteractor/json/server/response/put_documents_4223.json"
        );
        mockServerPath("PUT",
                "/v1/documents/4963/published",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "revision_id", List.of("8132")
                ),
                400,
                "ModerationTaskModeratedToPublishedCmsInteractor/json/server/response/put_documents_4963.json"
        );

        Assertions.assertThatThrownBy(() -> tmsModerationTaskModeratedToPublishedExecutor.doJob(mockContext()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Document 4123 not found");

        server.verify(
                request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/4233/published")
                        .withQueryStringParameters(
                                Map.of(
                                        "userId", List.of("1513471018"),
                                        "revision_id", List.of("5232")
                                )
                        ),
                VerificationTimes.once()
        );
        server.verify(
                request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/4123/published")
                        .withQueryStringParameters(
                                Map.of(
                                        "userId", List.of("1513471018"),
                                        "revision_id", List.of("5232")
                                )
                        ),
                VerificationTimes.once()
        );
        server.verify(
                request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/4963/published")
                        .withQueryStringParameters(
                                Map.of(
                                        "userId", List.of("1513471018"),
                                        "revision_id", List.of("8132")
                                )
                        ),
                VerificationTimes.once()
        );
    }
}
