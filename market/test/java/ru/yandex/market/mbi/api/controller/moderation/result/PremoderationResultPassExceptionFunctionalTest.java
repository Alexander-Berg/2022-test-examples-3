package ru.yandex.market.mbi.api.controller.moderation.result;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.api.client.entity.moderation.MessageRequest;
import ru.yandex.market.mbi.api.client.entity.moderation.ModerationResultResponse;
import ru.yandex.market.mbi.api.client.entity.moderation.PremoderationResultRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static ru.yandex.market.core.moderation.qc.result.PremoderationResult.Status.PASSED;

class PremoderationResultPassExceptionFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Возвращается статус IGNORED в случае, если возник Exception")
    @DbUnitDataSet(before = "PremoderationResultPassExceptionFunctionalTest.csv")
    void testConstraintViolationIsReturnedAsIgnoredModerationResultResponse() {
        PremoderationResultRequest request = new PremoderationResultRequest.Builder()
                .setShopId(700)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setQualityCheckStatus(PASSED)
                .setCloneCheckStatus(PASSED)
                .setOrderCheckStatus(PASSED)
                .setMessage(new MessageRequest()).build();

        ModerationResultResponse response = mbiApiClient.registerPremoderationResult(request);

        Assertions.assertEquals(
                new ModerationResultResponse(
                        ModerationResultResponse.Status.IGNORED,
                        "Shop's testing status is illegal for requested action. Status: 6"
                ),
                response
        );
    }
}
