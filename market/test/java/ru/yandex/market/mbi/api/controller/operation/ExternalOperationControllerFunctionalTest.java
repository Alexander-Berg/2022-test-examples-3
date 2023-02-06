package ru.yandex.market.mbi.api.controller.operation;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.operation.ExternalOperationResult;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatistics;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;
import ru.yandex.market.mbi.api.config.FunctionalTest;

/**
 * Функциональный тест для {@link ExternalOperationController}.
 */
@DbUnitDataSet(before = "ExternalOperationControllerFunctionalTest.before.csv")
class ExternalOperationControllerFunctionalTest extends FunctionalTest {

    @DisplayName("Проверяем, что вызов по заранее имеющейся операции проходит корректно")
    @Test
    @DbUnitDataSet(after = "ExternalOperationControllerFunctionalTest.success.after.csv")
    void updateStatus_withoutStats_success() {
        String externalId = "m12345678";
        OperationStatus status = OperationStatus.OK;
        ExternalOperationResult result = new ExternalOperationResult(externalId, status, null);
        GenericCallResponse response = mbiApiClient.updateOperationStatus(result);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @DisplayName("Обновление статуса операции со статистикой")
    @Test
    @DbUnitDataSet(after = "ExternalOperationControllerFunctionalTest.withStats.after.csv")
    void updateStatus_withStats_success() {
        String externalId = "m12345678";
        OperationStatus status = OperationStatus.OK;
        OperationStatistics stats = new OperationStatistics(Map.of("key1", "123", "key2", "value2"));
        ExternalOperationResult result = new ExternalOperationResult(externalId, status, stats);
        GenericCallResponse response = mbiApiClient.updateOperationStatus(result);
        Assertions.assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @DisplayName("Проверяем, что вызов по неизвестной операции проходит с ошибкой")
    @Test
    @DbUnitDataSet(after = "ExternalOperationControllerFunctionalTest.fail.after.csv")
    void testOperationStatusUpdateFail() {
        String externalId = "qwerty";
        OperationStatus status = OperationStatus.OK;
        ExternalOperationResult result = new ExternalOperationResult(externalId, status, null);
        GenericCallResponse response = mbiApiClient.updateOperationStatus(result);
        Assertions.assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
    }
}
