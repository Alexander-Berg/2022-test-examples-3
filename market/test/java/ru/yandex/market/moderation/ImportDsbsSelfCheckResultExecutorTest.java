package ru.yandex.market.moderation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod.API;
import static ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod.PI;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

public class ImportDsbsSelfCheckResultExecutorTest extends FunctionalTest {
    @Autowired
    private AboPublicRestClient aboPublicRestClient;
    @Autowired
    private ImportDsbsSelfCheckResultExecutor tested;
    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    /**
     * Тестирование загрузки результатов самопроверки из АБО.
     * <p>
     * Информация о магазинах:
     * <p>
     * 1) 1-ый магазин (partner_id = 111)
     * Описание: DSBS, который уже прошел модерацию, самопроверка в прогрессе. Из АБО приходит
     * информация о том, что все сценарии самопроверки магазина успешно пройдены;
     * Ожидание: У этого магазина datasource_in_testing c типом SELF_TEST должен удалиться.
     * Фича MARKETPLACE_SELF_DELIVERY перейдет в SUCCESS.
     * <p>
     * 2) 2-ый магазин (partner_id = 222)
     * Описание: DSBS, который уже прошел модерацию, самопроверка в прогрессе. Из АБО приходит
     * информация о том, что все сценарии самопроверки магазина зафейлены;
     * Ожидание: У этого магазина datasource_in_testing c типом SELF_TEST остается.
     * Фича MARKETPLACE_SELF_DELIVERY остается в NEW.
     * <p>
     * 3) 3-ий магазин (partner_id = 333)
     * Описание: DSBS, который еще не прошел модерацию, самопроверка в прогрессе. Из АБО приходит
     * информация о том, что все сценарии самопроверки пройдены;
     * Ожидание: У этого магазина datasource_in_testing c типом SELF_TEST удаляется.
     * Фича MARKETPLACE_SELF_DELIVERY остается в NEW.
     * <p>
     * 4) 4-ий магазин (partner_id = 444)
     * Описание: не DSBS, прошедший модерацию, самопроверка в прогрессе. Из АБО приходит
     * информация о том, что все сценарии самопроверки пройдены;
     * Ожидание: Для магазина ничего не происходит, так как нас интересует только ДСБС
     * <p>
     * 5) 5-ий магазин (partner_id = 555)
     * Описание: DSBS, прошедший модерацию, самопроверка по нашим данным не начата. Из АБО приходит
     * информация о том, что все сценарии самопроверки пройдены;
     * Ожидание: Пытаемся перевести фичу в SUCCESS еще раз. Успешно переводим.
     * <p>
     * 6) 6-ий магазин (partner_id = 666)
     * Описание: DSBS, прошедший модерацию, мы долго не получали результатов от АБО, и закэнселили
     * проверку джобой RemoveExpiredShopFromSandboxExecutor. И тут из АБО внезапно приходит информация
     * об успешности проверки.
     * Ожидание: Пытаемся перевести фичу в SUCCESS. Успешно переводим.
     * <p>
     * 7) 7-ой магазин (partner_id = 777)
     * Описание: DSBS, прошедший модерацию одновременно с самопроверкой. Из-за этой одновеменности фича не смогла
     * перевестисб в саксесс.
     * Ожидание: Т.к. записи в datasources_in_testing нет, оставляем фичу в NEW и переводим такой магазин
     * в SUCCESS в PassModerationChecksExecutorTest
     * <p>
     * 8) 8-ой магазин (partner_id = 888)
     * Описание: Турбо, который уже прошел модерацию, самопроверка в прогрессе. Из АБО приходит
     * информация о том, что все сценарии самопроверки магазина успешно пройдены;
     * Ожидание: У этого магазина datasource_in_testing c типом SELF_TEST должен удалиться.
     * Фича MARKETPLACE_SELF_DELIVERY перейдет в SUCCESS.
     */
    @Test
    @DbUnitDataSet(before = "importDsbsSelfcheckResult.before.csv",
            after = "importDsbsSelfcheckResult.after.csv")
    void testSuccessfulJobExecution() {
        mockAboCall();
        var result = new CompletableFuture<PartnerChangesProtoLBEvent>();
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any())).thenReturn(result);

        tested.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 4, 1614169151L);
    }

    private void mockAboCall() {
        when(aboPublicRestClient.getSelfCheckScenarios(or(or(eq(111L), eq(777L)), eq(888L)),
                eq(PlacementType.DSBS),
                eq(PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(111L,
                                CheckOrderScenarioDTO.builder(111L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()
                        )));
        when(aboPublicRestClient.getSelfCheckScenarios(222L, PlacementType.DSBS, API))
                .thenReturn(List.of(
                        new SelfCheckDTO(222L,
                                CheckOrderScenarioDTO.builder(222L)
                                        .withStatus(CheckOrderScenarioStatus.FAIL)
                                        .build()
                        )));
        when(aboPublicRestClient.getSelfCheckScenarios(or(or(or(eq(333L), eq(444L)), eq(555L)), eq(666L)),
                eq(PlacementType.DSBS), eq(API)))
                .thenReturn(List.of(
                        new SelfCheckDTO(3L,
                                CheckOrderScenarioDTO.builder(3L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()
                        )));
    }

    /**
     * Находим все магазины с катофом SELFCHECK_REQUIRED
     *
     * 1) (partner_id = 111)
     * У магазина пройдены самопроверка и модерация
     * Ожидание: Переводим в SUCCESS. Снимаем катоф SELFCHECK_REQUIRED
     * 2) (partner_id = 222)
     * У магазина пройдена самопроверка, но не пройдена модерация
     * Ожидание: Оставляем в NEW, но снимаем SELFCHECK_REQUIRED
     * 3) (partner_id = 333)
     * Магазин размещается, но скрыт катофом SELFCHECK_REQUIRED для переключения на API
     * Ожидание: Снимаем катоф, накладываем катоф MARKETPLACE_PLACEMENT
     * 4) (partner_id = 444)
     * У размещающегося магазина есть катофы за качество, и ему необходимо пройти самопроверку.
     * В або самопроверка пройдена
     * Ожидание: Снимаем катоф SELFCHECK_REQUIRED, фича остается в FAIL. Все катофы остаются
     * 5) (partner_id = 555)
     * У размещающегося магазина есть катофы за качество, и ему необходимо пройти самопроверку.
     * В або самопроверка не пройдена
     * Ожидание: Оставляем катоф SELFCHECK_REQUIRED, фича остается в FAIL
     * 6) (partner_id = 666)
     * Магазин размещается, но скрыт катофом SELFCHECK_REQUIRED для переключения на API.
     * Есть катоф DAILY_ORDER_LIMIT
     * Ожидание: Снимаем катоф, накладываем катоф MARKETPLACE_PLACEMENT, DAILY_ORDER_LIMIT остается
     * 7) (partner_id = 777)
     * Магазин в NEW без катофа.
     * Ожидание: с ним ничего не происходит
     * 8) (partner_id = 888)
     * У магазина пройдена модерация, но не пройдена самопроверка
     * Ожидание: не снимаем катоф
     * 9) (partner_id = 999)
     * Магазин размещается, но скрыт катофом SELFCHECK_REQUIRED для переключения на API.
     * Есть катоф BY_PARTNER
     * Ожидание: оставляем BY_PARTNER
     */
    @Test
    @DbUnitDataSet(before = "testSelfcheckViaSelfcheckRequiredCutoff.before.csv",
            after = "testSelfcheckViaSelfcheckRequiredCutoff.after.csv")
    void testSelfcheckViaSelfcheckRequiredCuttoff() {
        mockAboCallForSelfchekRequired();

        tested.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 1, 1614169151L);
        verify(aboPublicRestClient, never()).getSelfCheckScenarios(eq(777L), any(), any());
    }

    private void mockAboCallForSelfchekRequired() {
        when(aboPublicRestClient.getSelfCheckScenarios(or(or(or(or(or(eq(111L), eq(222L)), eq(333L)), eq(444L)), eq(666L)), eq(999L)),
                eq(PlacementType.DSBS), eq(API)))
                .thenReturn(List.of(
                        new SelfCheckDTO(3L,
                                CheckOrderScenarioDTO.builder(3L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()
                        )));
    }
}
