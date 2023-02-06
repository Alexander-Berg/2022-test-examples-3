package ru.yandex.market.core.moderation.request;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.moderation.ModerationDisabledReason;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "AbstractModerationFunctionalTest.csv")
class CutoffClosingOnModerationFunctionalTest extends AbstractModerationFunctionalTest {


    /**
     * Смотрим, что при запуске CPC-проверки закрываются CPC-катофы, но не закрываются CPA-катофы
     *
     * <ul>
     * <li>Создаём гибридный магазин магазин
     * <li>Открываем отключение QMANAGER_OTHER
     * <li>Открываем отключение CPA_QUALITY_OTHER
     * <li>Запускаем CPC-проверку
     * </ul>
     * <p>
     * Проверяем, что
     *
     * <ul>
     * <li>После старта CPC-модерации QMANAGER_OTHER закрылось
     * <li>После старта CPC-модерации CPA_QUALITY_OTHER осталось висеть
     * </ul>
     */
    @Test
    void testClosingCutoffsOnCPCModerationStart() {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            cutoffService.openCutoff(context, CutoffType.QMANAGER_OTHER);
            cutoffService.openCutoff(context, CutoffType.CPA_QUALITY_OTHER);
            moderationRequestEntryPoint.requestCPCModeration(context);
            skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.CPC);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.CPC);
            Set<CutoffType> openCutoffs =
                    cutoffService.getCutoffs(datasourceID, CutoffType.ALL_CUTOFFS).keySet();
            assertThat(openCutoffs)
                    .doesNotContain(CutoffType.QMANAGER_OTHER)
                    .contains(CutoffType.CPA_QUALITY_OTHER);
        });
    }

    /**
     * Смотрим, что при запуске CPC-проверки закрываются CHEESY CPC-катофы, но не закрываются CPA-катофы
     *
     * <ul>
     * <li>Создаём гибридный магазин магазин
     * <li>Открываем отключение QMANAGER_CHEESY
     * <li>Открываем отключение CPA_QUALITY_CHEESY
     * <li>Задаём время старта CPC-модерации
     * <li>Запускаем CPC-проверку
     * </ul>
     * <p>
     * Проверяем, что
     *
     * <ul>
     * <li>После старта CPC-модерации QMANAGER_CHEESY закрылось
     * <li>После старта CPC-модерации CPA_QUALITY_CHEESY осталось висеть
     * </ul>
     */
    @Test
    void testClosingCheesyCutoffsOnCPCModerationStart() {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            cutoffService.openCutoff(context, CutoffType.QMANAGER_CHEESY);
            cutoffService.openCutoff(context, CutoffType.CPA_QUALITY_CHEESY);

            SandboxState sandboxState = sandboxRepository.load(datasourceID, ShopProgram.CPC);
            sandboxState.enableQuickStart();
            sandboxRepository.store(context, sandboxState);

            moderationRequestEntryPoint.requestCPCModeration(context);
            skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.CPC);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.CPC);
            Set<CutoffType> openCutoffs =
                    cutoffService.getCutoffs(datasourceID, CutoffType.ALL_CUTOFFS).keySet();
            assertThat(openCutoffs)
                    .doesNotContain(CutoffType.QMANAGER_CHEESY)
                    .contains(CutoffType.CPA_QUALITY_CHEESY);
        });
    }

}
