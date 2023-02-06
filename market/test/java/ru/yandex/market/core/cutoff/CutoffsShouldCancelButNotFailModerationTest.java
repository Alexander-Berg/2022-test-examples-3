package ru.yandex.market.core.cutoff;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.ds.DatasourceCreationService;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingService;
import ru.yandex.market.core.testing.TestingStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "CutoffsShouldCancelButNotFailModerationTest.csv")
class CutoffsShouldCancelButNotFailModerationTest extends FunctionalTest {
    @Autowired
    private CutoffService cutoffService;

    @Autowired
    private TestingService testingService;

    @Autowired
    private DatasourceCreationService datasourceService;

    @Autowired
    private SandboxRepository sandboxRepository;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private ProtocolService protocolService;

    /**
     * Проверяем, что TECHNICAL_NEED_INFO отменяет модерацию, но не увеличивает счётчики попыток.
     *
     * <ol>
     * <li>Создаём магазин.
     * <li>Включаем CPC
     * <li>Отправляем магазин на модерацию
     * <li>Открываем TECHNICAL_NEED_INFO
     * </ol>
     *
     * <p>
     * Проверяем, что TECHNICAL_NEED_INFO отменяет модерацию, но не увеличивает счётчики попыток
     *
     * <ul>
     * <li>status=CANCELED
     * <li>Никакие счётчики попыток не инкрементятся
     * </ul>
     */
    @Test
    void testTECHNICAL_NEED_INFOShouldCancelButNotFailOngoingModeration() {
        protocolService.operationInTransaction(
                new UIDActionContext(ActionType.PROGRAM_MANAGEMENT, 100500L),
                (status, actionId) -> {
                    var datasource = new DatasourceInfo();
                    datasource.setInternalName("shop1.test.ru");
                    datasource.setManagerId(456);
                    datasource.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
                    datasourceService.createDatasource(datasource, actionId, CampaignType.SHOP);
                    var shopActionContext = new ShopActionContext(actionId, datasource.getId());
                    partnerTypeAwareService.activateCpcProgram(actionId, datasource.getId());

                    var testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.INITED);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(0);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);

                    var sandboxState = sandboxRepository.load(datasource.getId(), ShopProgram.CPC);
                    sandboxState.requestCpcModeration();
                    sandboxState.startTesting();
                    sandboxRepository.store(shopActionContext, sandboxState);

                    testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(1);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);
                    assertThat(testingState.isInProgress()).isTrue();
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.WAITING_FEED_FIRST_LOAD);

                    cutoffService.openCutoff(shopActionContext, CutoffType.TECHNICAL_NEED_INFO);

                    testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(0);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.CANCELED);
                }
        );
    }

    /**
     * Проверяем, что QMANAGER_OTHER заваливает модерацию и увеличивает счётчики попыток.
     *
     * <ol>
     * <li>Создаём магазин.
     * <li>Включаем CPC
     * <li>Отправляем магазин на модерацию
     * <li>Открываем QMANAGER_OTHER
     * </ol>
     *
     * <p>
     * Проверяем, что QMANAGER_OTHER заваливает модерацию и увеличивает счётчики попыток
     *
     * <ul>
     * <li>status=READY_TO_FAIL
     * <li>pushReadyButtonCount засчитывает одну попытку
     * </ul>
     */
    @Test
    void testQMANAGER_OTHERShouldFailAndNotCancelOngoingModeration() {
        protocolService.operationInTransaction(
                new UIDActionContext(ActionType.PROGRAM_MANAGEMENT, 100500L),
                (status, actionId) -> {
                    var datasource = new DatasourceInfo();
                    datasource.setInternalName("shop1.test.ru");
                    datasource.setManagerId(456);
                    datasource.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
                    datasourceService.createDatasource(datasource, actionId, CampaignType.SHOP);
                    var shopActionContext = new ShopActionContext(actionId, datasource.getId());
                    partnerTypeAwareService.activateCpcProgram(actionId, datasource.getId());

                    var testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.INITED);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(0);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);

                    var sandboxState = sandboxRepository.load(datasource.getId(), ShopProgram.CPC);
                    sandboxState.requestCpcModeration();
                    sandboxState.startTesting();
                    sandboxRepository.store(shopActionContext, sandboxState);

                    testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(1);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);
                    assertThat(testingState.isInProgress()).isTrue();
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.WAITING_FEED_FIRST_LOAD);

                    cutoffService.openCutoff(shopActionContext, CutoffType.QMANAGER_OTHER);

                    testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(1);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.READY_TO_FAIL);
                }
        );
    }


    /**
     * Проверяем, что QMANAGER_OTHER не заваливает модерацию, если она ещё не началась.
     *
     * <ol>
     * <li>Создаём магазин.
     * <li>Включаем CPC
     * <li><em>Не</em> отправляем магазин на модерацию
     * <li>Открываем QMANAGER_OTHER
     * </ol>
     *
     * <p>
     * Проверяем, что QMANAGER_OTHER не заваливает модерацию, если она ещё не началась.
     *
     * <ul>
     * <li>status=INITED
     * <li>Никакие счётчики попыток не инкрементятся
     * </ul>
     */
    @Test
    void testQMANAGER_OTHERShouldNotFailModerationWhenItIsNotStarted() {
        protocolService.operationInTransaction(
                new UIDActionContext(ActionType.PROGRAM_MANAGEMENT, 100500L),
                (status, actionId) -> {
                    var datasource = new DatasourceInfo();
                    datasource.setInternalName("shop1.test.ru");
                    datasource.setManagerId(456);
                    datasource.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
                    datasourceService.createDatasource(datasource, actionId, CampaignType.SHOP);
                    var shopActionContext = new ShopActionContext(actionId, datasource.getId());
                    partnerTypeAwareService.activateCpcProgram(actionId, datasource.getId());

                    var testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.INITED);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(0);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);

                    cutoffService.openCutoff(shopActionContext, CutoffType.QMANAGER_OTHER);

                    testingState = testingService.getTestingStatus(shopActionContext.getShopId(), ShopProgram.CPC);
                    assertThat(testingState.getPushReadyButtonCount()).isEqualTo(0);
                    assertThat(testingState.getIterationNum()).isEqualTo(1);
                    assertThat(testingState.getStatus()).isEqualTo(TestingStatus.INITED);
                }
        );
    }
}
