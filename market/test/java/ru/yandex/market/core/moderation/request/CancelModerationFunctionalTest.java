package ru.yandex.market.core.moderation.request;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.moderation.qc.result.PremoderationResult;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "AbstractModerationFunctionalTest.csv")
@ParametersAreNonnullByDefault
class CancelModerationFunctionalTest extends AbstractModerationFunctionalTest {

    @Test
    @DbUnitDataSet(before = "testCpaShopModeration.csv")
    void testCPA_FEEDShouldNotInterfereWithREADY_TO_FAIL() {
        withinAction(actionId -> {
            ShopActionContext context = new ShopActionContext(actionId, 200L);

            moderationRequestEntryPoint.requestRequiredModeration(context);
            skipModerationDelayAndConfirmModerationRequest(200L, ShopProgram.CPA);
            skipModerationDelayAndStartMainModerationProcess(200L, ShopProgram.CPA);
            confirmModerationSandboxFeedLoad(200L, ShopProgram.CPA);
            submitModerationResult(200L, ShopProgram.CPA,
                    new ModerationResult(
                            PremoderationResult.Status.PASSED,
                            PremoderationResult.Status.FAILED,
                            PremoderationResult.Status.SKIPPED,
                            PremoderationResult.Status.PASSED));

            cutoffService.openCutoff(context, CutoffType.CPA_FEED);

            TestingState moderationState = testingService.getTestingStatus(200L, ShopProgram.CPA);
            assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.READY_TO_FAIL);

            finishFailedModeration(200L, ShopProgram.CPA);

            moderationState = testingService.getTestingStatus(200L, ShopProgram.CPA);
            assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.FAILED);
        });
    }
}
