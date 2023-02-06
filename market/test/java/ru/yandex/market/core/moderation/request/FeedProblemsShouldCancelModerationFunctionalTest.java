package ru.yandex.market.core.moderation.request;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.core.RemoteFile;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.moderation.TestingShop;
import ru.yandex.market.core.moderation.feed.failed.ModerationFeedLoadFailedEntryPoint;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.tags.Features;
import ru.yandex.market.tags.Tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(before = "AbstractModerationFunctionalTest.csv")
@ParametersAreNonnullByDefault
class FeedProblemsShouldCancelModerationFunctionalTest extends AbstractModerationFunctionalTest {
    @Autowired
    private ModerationFeedLoadFailedEntryPoint moderationFeedLoadFailedEntryPoint;

    @Autowired
    private FeedFileStorage feedFileStorage;

    /**
     * Отмена CPC-модерации при проблемах с feed'ом.
     * <ul>
     * <li>Создаём CPC-only магазин.
     * <li>Проверяем, что нет отключения FORTESTING
     * <li>Проверяем, что нет записи в datasources_in_testing
     * <li>Открываем отключение QMANAGER_OTHER
     * <li>Проверяем, что теперь есть отключение FORTESTING
     * <li>Проверяем, что теперь есть запись в datasources_in_testing и
     * <ul>
     * <li>номер попытки - 0
     * <li>статус - INITED
     * </ul>
     * <li>Запрашиваем модерацию
     * <li>Подтверждаем запрос модерации
     * <li>Переходим к основному процессу модерации
     * <li>Проверяем, что есть отключение FORTESTING
     * <li>Проверяем, что есть запись в datasources_in_testing и
     * <ul>
     * <li>номер попытки - 1
     * <li>статус - WAITING_FEED_FIRST_LOAD
     * </ul>
     * <li>Сообщаем о проблеме с фидом
     * <li>Проверяем, что есть отключение FORTESTING
     * <li>Проверяем, что есть запись в datasources_in_testing и
     * <ul>
     * <li>номер попытки - 0
     * <li>статус - CANCELED
     * </ul>
     * </ul>
     */
    @Test
    @DisplayName("Отмена CPC-модерации при проблемах с feed'ом")
    @Tags({@Tag(Tests.INTEGRATIONAL),
            @Tag(Features.DB_INTEGRATION),
            @Tag(Features.MODERATION)})
    void testCPCModerationShouldBeCanceledOnFeedProblems() {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));
            ShopActionContext context = new ShopActionContext(actionId, datasourceID);

            assertDoesNotHaveCutoff(datasourceID, CutoffType.FORTESTING);
            assertDoesNotHaveDatasourcesInTesting(datasourceID, ShopProgram.CPC);

            openCutoff(context, CutoffType.QMANAGER_OTHER);

            assertHasCutoff(datasourceID, CutoffType.FORTESTING);
            assertTestingState(datasourceID, ShopProgram.CPC, testingState -> {
                assertTestingStatePushReadyButtonCount(0, testingState);
                assertTestingStateTestingStatus(TestingStatus.INITED, testingState);
            });

            requestRequiredModeration(context);
            skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.CPC);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.CPC);

            assertHasCutoff(datasourceID, CutoffType.FORTESTING);
            assertTestingState(datasourceID, ShopProgram.CPC, testingState -> {
                assertTestingStatePushReadyButtonCount(1, testingState);
                assertTestingStateTestingStatus(TestingStatus.WAITING_FEED_FIRST_LOAD, testingState);
            });

            TestingShop testingShop = new TestingShop(
                    testingService.getTestingStatus(datasourceID, ShopProgram.CPC).getId(),
                    datasourceID);
            reportFeedLoadFailure(new SystemActionContext(ActionType.TEST_ACTION), testingShop);

            assertHasCutoff(datasourceID, CutoffType.FORTESTING);
            assertTestingState(datasourceID, ShopProgram.CPC, testingState -> {
                assertTestingStatePushReadyButtonCount(0, testingState);
                assertTestingStateTestingStatus(TestingStatus.CANCELED, testingState);
            });
        });
    }

    /**
     * Тест перехода из WAITING_FEED_FIRST_LOAD в CHECKING если есть только протухший фид.
     * <ul>
     * <li>Создаём CPC-only магазин.
     * <li>Создаём протухший фид
     * <li>Открываем катоф QMANAGER_OTHER
     * <li>Запрашиваем модерацию
     * <li>Подтверждаем запрос
     * <li>Пропускаем задёржку повторной модерации
     * <li>Поддтверждаем загрузку фида в тестовый индекс
     * <li>Проверям что поставился статус CHECKING
     * </ul>
     */
    @Test
    @DisplayName("Тест перехода из WAITING_FEED_FIRST_LOAD в CHECKING")
    void testModerationOnlyExpiredFeed() throws IOException {
        doReturn(new StoreInfo(1, "url"))
                .when(feedFileStorage).upload(any(RemoteFile.class), any(Long.class));

        withinAction(actionId -> {
            long datasourceID = createDatasourceWithUploadFeed(actionId, Set.of(ShopProgram.CPC), getExpireDate());

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            passModeration(context, Set.of(ShopProgram.CPC));

            cutoffService.openCutoff(context, CutoffType.QMANAGER_OTHER);

            moderationRequestEntryPoint.requestCPCModeration(context);
            skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.CPC);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.CPC);

            confirmModerationSandboxFeedLoad(datasourceID, ShopProgram.CPC);

            TestingState testingState = testingService.getTestingStatus(datasourceID, ShopProgram.CPC);
            assertThat(testingState.getStatus()).isEqualTo(TestingStatus.CHECKING);
        });
    }

    private static Date getExpireDate() {
        Calendar expireDate = Calendar.getInstance();
        expireDate.set(2016, 6, 10);
        return expireDate.getTime();
    }

    @Test
    @DisplayName("Проверка, что магазин пройдет WAITING_FEED_FIRST_LOAD, если у него просрочены все аплоадные фиды")
    void testModerationWithExpiredUploadAndUrlFeed() throws IOException {
        doReturn(new StoreInfo(1, "url"))
                .when(feedFileStorage).upload(any(RemoteFile.class), any(Long.class));

        withinAction(actionId -> {
            long datasourceID = createDatasourceWithUploadFeed(actionId, Set.of(ShopProgram.CPC), getExpireDate());
            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            passModeration(context, Set.of(ShopProgram.CPC));
        });
    }

    private void reportFeedLoadFailure(SystemActionContext systemActionContext, TestingShop testingShop) {
        moderationFeedLoadFailedEntryPoint.rollback(systemActionContext, testingShop);
    }

}
