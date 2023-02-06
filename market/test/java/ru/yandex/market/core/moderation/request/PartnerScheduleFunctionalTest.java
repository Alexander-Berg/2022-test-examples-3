package ru.yandex.market.core.moderation.request;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;

@DbUnitDataSet(before = "AbstractModerationFunctionalTest.csv")
class PartnerScheduleFunctionalTest extends AbstractModerationFunctionalTest {

    /**
     * Создаём работающий CPC-only магазин.
     * Открываем PARTNER_SCHEDULE.
     * Смотрим, что CPC выключилось.
     */
    @Test
    void testCPCOnlyIsDisabledOnPARTNER_SCHEDULE() {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            cutoffService.openCutoff(context, CutoffType.PARTNER_SCHEDULE);
            assertThatAllProgramsAreCuttedOff(datasourceID, Collections.singleton(ShopProgram.CPC));
        });
    }

    /**
     * Создаём работающий CPC-only магазин.
     * Открываем PARTNER_SCHEDULE.
     * Смотрим, что CPC выключилось.
     * Снимаем PARTNER_SCHEDULE
     * Смотрим, что CPC сразу включилось без модерации
     */
    @Test
    void testCPCOnlyDoesNotRequireModerationForPARTNER_SCHEDULE() {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            cutoffService.openCutoff(context, CutoffType.PARTNER_SCHEDULE);
            assertThatAllProgramsAreCuttedOff(datasourceID, Collections.singleton(ShopProgram.CPC));
            cutoffService.closeCutoff(context, CutoffType.PARTNER_SCHEDULE);
            assertThatAllProgramsAreOn(datasourceID, Collections.singleton(ShopProgram.CPC));
        });
    }
}
