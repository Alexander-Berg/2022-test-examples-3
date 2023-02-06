package ru.yandex.market.core.moderation.request;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "AbstractModerationFunctionalTest.csv")
class ModerationTypeFunctionalTest extends AbstractModerationFunctionalTest {

    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;

    /**
     * Проверка установки типа модерации до начала модерации.
     *
     * <ul>
     * <li>Открываем QMANAGER_OTHER</li>
     * <li>Смотрим, что тип модерации: CPC_LITE_CHECK</li>
     * <li>Открываем QMANAGER_CHEESY</li>
     * <li>Смотрим, что тип модерации: CPC_PREMODERATION</li>
     * <li>Закрываем QMANAGER_CHEESY</li>
     * <li>Смотрим, что тип модерации всё ещё: CPC_PREMODERATION</li>
     * </ul>
     */
    @Test
    void testCutoffsShouldSetMostStrictModerationType1() {
        withinAction(actionId -> {

            var datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            var context = new ShopActionContext(actionId, datasourceID);

            openCutoff(context, CutoffType.QMANAGER_OTHER);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.INITED);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_LITE_CHECK);
            });

            openCutoff(context, CutoffType.QMANAGER_CHEESY);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.INITED);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_PREMODERATION);
            });

            closeCutoff(new ShopActionContext(actionId, datasourceID), CutoffType.QMANAGER_CHEESY);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.INITED);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_PREMODERATION);
            });
        });
    }

    /**
     * Проверка установки типа модерации до начала модерации.
     *
     * <ul>
     * <li>Открываем QMANAGER_CHEESY</li>
     * <li>Смотрим, что тип модерации: CPC_PREMODERATION</li>
     * <li>Открываем QMANAGER_OTHER</li>
     * <li>Смотрим, что тип модерации остался: CPC_PREMODERATION</li>
     * <li>Закрываем QMANAGER_CHEESY</li>
     * <li>Смотрим, что тип модерации так и остался: CPC_PREMODERATION</li>
     * </ul>
     */
    @Test
    void testCutoffsShouldSetMostStrictModerationType2() {
        withinAction(actionId -> {
            var datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            var context = new ShopActionContext(actionId, datasourceID);

            openCutoff(context, CutoffType.QMANAGER_CHEESY);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.INITED);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_PREMODERATION);
            });

            openCutoff(context, CutoffType.QMANAGER_OTHER);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.INITED);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_PREMODERATION);
            });

            closeCutoff(new ShopActionContext(actionId, datasourceID), CutoffType.QMANAGER_CHEESY);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.INITED);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_PREMODERATION);
            });
        });
    }

    /**
     * Проверка установки типа модерации при запущенной модерации.
     *
     * <ul>
     * <li>Открываем QMANAGER_OTHER</li>
     * <li>Смотрим, что тип модерации: CPC_LITE_CHECK</li>
     * <li>Отправляем магазин на модерацию
     * <li>Открываем QMANAGER_CHEESY</li>
     * <li>Смотрим, что модерация зафейлилась, а тип модерации стал: CPC_PREMODERATION</li>
     * </ul>
     */
    @Test
    void testCutoffsOpeningShouldFailtModerationAndChangeModerationType() {
        withinAction(actionId -> {
            var datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            var context = new ShopActionContext(actionId, datasourceID);

            openCutoff(context, CutoffType.QMANAGER_OTHER);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.INITED);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_LITE_CHECK);
            });

            requestRequiredModeration(context);
            skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.CPC);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.CPC);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.WAITING_FEED_FIRST_LOAD);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_LITE_CHECK);
            });

            openCutoff(context, CutoffType.QMANAGER_CHEESY);
            assertTestingState(datasourceID, ShopProgram.CPC, moderationState -> {
                assertThat(moderationState.getStatus()).isEqualTo(TestingStatus.READY_TO_FAIL);
                assertThat(moderationState.getTestingType()).isEqualTo(TestingType.CPC_PREMODERATION);
            });
        });
    }

    @Test
    void testStartModeration() {
        withinAction(actionId -> {
            var datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            var context = new ShopActionContext(actionId, datasourceID);
            openCutoff(context, CutoffType.TECHNICAL_NEED_INFO);
            var program = partnerPlacementProgramService.getPartnerPlacementProgram(datasourceID,
                    PartnerPlacementProgramType.CPC).get();
            assertThat(PartnerPlacementProgramStatus.CONFIGURE).isEqualTo(program.getStatus());
            closeCutoff(context, CutoffType.TECHNICAL_NEED_INFO);
            requestRequiredModeration(context);
            program = partnerPlacementProgramService.getPartnerPlacementProgram(datasourceID,
                    PartnerPlacementProgramType.CPC).get();
            assertThat(PartnerPlacementProgramStatus.TESTED).isEqualTo(program.getStatus());
        });
    }
}
