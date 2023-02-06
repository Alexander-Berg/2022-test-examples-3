package ru.yandex.market.mbi.api.controller.operation.business_migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.business.BusinessChangeRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbi.open.api.client.model.GenericCallResponseStatus.OK;

@DbUnitDataSet(before = "BusinessMigrationController.before.csv")
public class BusinessMigrationControllerTest extends FunctionalTest {

    @Autowired
    private SaasService saasService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private LogbrokerService mboPartnerExportLogbrokerService;

    @DisplayName("Проверяем валидации")
    @Test
    void testBusinessMigrationChangeValidations() {
        // партнёра нет
        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 0L, 666L, processId);
        GenericCallResponse response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("Партнёр 666 не существует", response.getMessage());

        // партнёр не совсем партнёр
        request.setPartnerId(1L);
        response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("Партнёр 1 не может быть привязан к бизнесу, поскольку он 'BUSINESS'",
                response.getMessage());

        // отличаются переданный в запросе исходный бизнес и текущий бизнес партнера,
        // при этом переданный целевой бизнес не совпадает с текущим -> не ок (пишем в сообщении почему)
        request.setPartnerId(777L);
        request.setSrcBusinessId(2L);
        response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals(
                "Отличаются переданный srcBusinessId (2) и текущий (1) бизнес партнера!",
                response.getMessage()
        );

        // отличаются переданный в запросе исходный бизнес и текущий бизнес партнера,
        // при этом переданный целевой бизнес совпадает с текущим -> все ок
        request.setPartnerId(777L);
        request.setDstBusinessId(1L);
        response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Смена бизнеса прошла успешно", response.getMessage());

        // идентификатор внешней операции не существует ('m123456781')
        request.setSrcBusinessId(1L);
        request.setProcessId(processId + "1");
        response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("Некорректные идентификаторы блокировок:\n" +
                        "\tбизнес 1 блокирован идентификатором процесса 'm12345678'\n",
                response.getMessage());

        // идентификатор процесса задан неверно (не тот processId)
        request.setDstBusinessId(2L);
        response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("Некорректные идентификаторы блокировок:\n" +
                "\tбизнес 1 блокирован идентификатором процесса 'm12345678'\n" +
                "\tбизнес 2 блокирован идентификатором процесса 'm12345678'\n", response.getMessage());
    }

    @DisplayName("Проверяем перепривязку бизнеса")
    @Test
    @DbUnitDataSet(before = "BusinessMigrationController.testBusinessMigrationChange.before.csv",
            after = "BusinessMigrationController.testBusinessMigrationChange.after.csv")
    void testBusinessMigrationChange() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 2L, 777L, processId);
        GenericCallResponse response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Смена бизнеса прошла успешно", response.getMessage());
        verify(mboPartnerExportLogbrokerService, atLeast(1)).publishEvent(any());
    }

    @DisplayName("Проверяем перепривязку Openapi")
    @Test
    @DbUnitDataSet(before = "BusinessMigrationController.testBusinessMigrationChange.before.csv",
            after = "BusinessMigrationController.testBusinessMigrationChange.after.csv")
    void testBusinessMigrationChangeOpenapi() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "m12345678";
        ru.yandex.market.mbi.open.api.client.model.BusinessChangeRequest request =
                new ru.yandex.market.mbi.open.api.client.model.BusinessChangeRequest();
        request.setSrcBusinessId(1L);
        request.setDstBusinessId(2L);
        request.setPartnerId(777L);
        request.setProcessId(processId);
        ru.yandex.market.mbi.open.api.client.model.GenericCallResponse response =
                getMbiOpenApiClient().changeBusiness(request);
        ru.yandex.market.mbi.open.api.client.model.GenericCallResponse expected =
                new ru.yandex.market.mbi.open.api.client.model.GenericCallResponse();
        expected.setStatus(OK);
        expected.setMessage("Смена бизнеса прошла успешно");
        Assertions.assertEquals(expected, response);
        verify(mboPartnerExportLogbrokerService, atLeast(1)).publishEvent(any());
    }

    @DisplayName("Проверяем перепривязку бизнеса")
    @Test
    @DbUnitDataSet(before = "BusinessMigrationController.testHistory.before.csv")
    void testBusinessMigrationChangeWithHistory() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "camunda";
        BusinessChangeRequest request = new BusinessChangeRequest(3L, 4L, 779L, processId);
        GenericCallResponse response = mbiApiClient.changeBusiness(request);
        Assertions.assertEquals(new GenericCallResponse(
                GenericCallResponseStatus.OK,
                "Смена бизнеса прошла успешно"), response);
    }

    @DisplayName("Проверяем блокировку бизнеса")
    @Test
    @DbUnitDataSet(before = "BusinessMigrationController.testBusinessMigrationLock.before.csv",
            after = "BusinessMigrationController.testBusinessMigrationLock.after.csv")
    void testBusinessMigrationLock() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 2L, 777L, processId);
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Successful acquiring", response.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "BusinessMigrationController.testBusinessMigrationLock.before.csv",
            after = "BusinessMigrationController.testBusinessMigrationLock.after.csv")
    void testBusinessMigrationSelfLock() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 1L, 777L, processId);
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Successful acquiring", response.getMessage());
    }

    @DisplayName("Проверяем блокировку бизнеса при отсутствии реальной операции")
    @Test
    @DbUnitDataSet(before = "BusinessMigrationController.testBusinessMigrationLock.before.csv",
            after = "BusinessMigrationController.testBusinessMigrationLockOnEmptyOperation.after.csv")
    void testBusinessMigrationLockOnEmptyOperation() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "0";
        BusinessChangeRequest request = new BusinessChangeRequest(4L, 3L, 780L, processId);
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Successful acquiring", response.getMessage());
    }

    @DisplayName("Проверяем reentrancy")
    @Test
    @DbUnitDataSet(after = "BusinessMigrationController.testBusinessMigrationLock.fail.after.csv")
    void testBusinessMigrationLockReentrancy() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 2L, 777L, processId);
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Successful acquiring", response.getMessage());
    }

    @DisplayName("Проверяем заблокированную блокировку бизнеса")
    @Test
    @DbUnitDataSet(after = "BusinessMigrationController.testBusinessMigrationLock.fail.after.csv")
    void testBusinessMigrationLockFailOnAlreadyBlockedBusiness() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(2L, 3L, 777L, processId);
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
        Assertions.assertEquals("Lock was acquired by another process_id", response.getMessage());
    }

    @DisplayName("Проверяем разблокировку бизнеса")
    @Test
    @DbUnitDataSet(after = "BusinessMigrationController.testBusinessMigrationUnlock.after.csv")
    void testBusinessMigrationUnlock() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        // меняем бизнес
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.LINK_SERVICE_TO_BUSINESS,
                        "Миграция лого и названий на уровень бизнеса"),
                (transactionStatus, actionId) -> {
                    businessService.linkService(2L, 777L, actionId);
                });

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 2L, 777L, processId);
        GenericCallResponse response = mbiApiClient.unlockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Successful releasing", response.getMessage());
    }

    @DisplayName("Проверяем разблокировку бизнеса")
    @Test
    @DbUnitDataSet(after = "BusinessMigrationController.testBusinessMigrationUnlock.after.csv")
    void testBusinessMigrationSelfUnlock() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        // меняем бизнес
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.LINK_SERVICE_TO_BUSINESS,
                        "Миграция лого и названий на уровень бизнеса"),
                (transactionStatus, actionId) -> {
                    businessService.linkService(2L, 777L, actionId);
                });

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 2L, 777L, processId);
        GenericCallResponse response = mbiApiClient.unlockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Successful releasing", response.getMessage());
    }

    @DisplayName("Проверяем разблокировку бизнеса после неуспешной смены бизнеса")
    @Test
    @DbUnitDataSet(after = "BusinessMigrationController.testBusinessMigrationUnlock.after.csv")
    void testBusinessMigrationUnlockAfterFailBusinessChange() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 2L, 777L, processId);
        GenericCallResponse response = mbiApiClient.unlockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Successful releasing", response.getMessage());
    }

    @DisplayName("Проверяем разблокированную разблокировку бизнеса")
    @Test
    @DbUnitDataSet(after = "BusinessMigrationController.testBusinessMigrationUnlock.fail.after.csv")
    void testBusinessMigrationUnlockFailOnAlreadyReleased() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());

        // меняем бизнес
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.LINK_SERVICE_TO_BUSINESS,
                        "Миграция лого и названий на уровень бизнеса"),
                (transactionStatus, actionId) -> {
                    businessService.linkService(2L, 777L, actionId);
                    // удаляем параметры блокировки, т.е. она уже завершилась
                    paramService.listParams(ParamType.LOCK_BUSINESS_MIGRATION).forEach(
                            paramValue -> paramService.deleteParam(actionId, paramValue));
                });

        String processId = "m12345678";
        BusinessChangeRequest request = new BusinessChangeRequest(1L, 2L, 777L, processId);
        GenericCallResponse response = mbiApiClient.unlockBusiness(request);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
        Assertions.assertEquals("Разблокировка выполнена другим запросом", response.getMessage());
    }
}
