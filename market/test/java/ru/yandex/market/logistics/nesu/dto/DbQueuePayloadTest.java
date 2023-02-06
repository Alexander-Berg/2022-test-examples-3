package ru.yandex.market.logistics.nesu.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.jobs.model.ProcessUploadFeedPayload;
import ru.yandex.market.logistics.nesu.jobs.model.SenderModifiersUploadPayload;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPayload;

class DbQueuePayloadTest extends AbstractTest {
    private static final String REQUEST_ID = "request-id";

    @Test
    @DisplayName("Пэйлоады логируются корректно")
    void toStringMethod() {
        softly.assertThat(new ShopIdPayload(REQUEST_ID, 1L).toString())
            .isEqualTo(
                "ShopIdPayload(super=TraceableExecutionQueuePayload(requestId=request-id), shopId=1)"
            );
        softly.assertThat(new ProcessUploadFeedPayload(1L, REQUEST_ID).toString())
            .isEqualTo(
                "ProcessUploadFeedPayload(super=TraceableExecutionQueuePayload(requestId=request-id), feedId=1)"
            );
        softly.assertThat(new SenderModifiersUploadPayload(REQUEST_ID, 1L).toString())
            .isEqualTo(
                "SenderModifiersUploadPayload(super=TraceableExecutionQueuePayload(requestId=request-id), senderId=1)"
            );
    }
}
