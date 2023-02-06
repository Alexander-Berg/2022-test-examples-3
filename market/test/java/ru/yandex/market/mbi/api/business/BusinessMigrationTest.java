package ru.yandex.market.mbi.api.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.business.BusinessChangeRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BusinessMigrationTest extends FunctionalTest {

    @Test
    @DisplayName("Проверка успешной блокировки бизнесов")
    @DbUnitDataSet(before = "BusinessMigrationTest.lock.success.before.csv",
            after = "BusinessMigrationTest.lock.success.after.csv")
    void testLockSuccess() {
        BusinessChangeRequest request =
                new BusinessChangeRequest(1L, 2L, 777L, "m12345678");
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @Test
    @DisplayName("Проверка безуспешной блокировки бизнесов - некорректный идентификатор процесса")
    @DbUnitDataSet(before = "BusinessMigrationTest.lock.success.before.csv",
            after = "BusinessMigrationTest.lock.fail.notBusinesses.after.csv")
    void testLockFail() {
        BusinessChangeRequest request =
                new BusinessChangeRequest(1L, 2L, 777L, "m-1");
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @DisplayName("Проверка безуспешной блокировки бизнесов - один из бизнесов заблокирован другой транзакцией")
    @DbUnitDataSet(before = "BusinessMigrationTest.lock.fail.anotherTransaction.before.csv",
            after = "BusinessMigrationTest.lock.fail.anotherTransaction.after.csv")
    void testLockFailBlockedByAnotherTransaction() {
        BusinessChangeRequest request =
                new BusinessChangeRequest(1L, 2L, 777L, "m12345678");
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        assertEquals("Некорректные идентификаторы блокировок:\n" +
                "\tбизнес 1 блокирован идентификатором процесса 'm-0'\n", response.getMessage());
        assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @DisplayName("Проверка успешной блокировки бизнесов - проверяем reentrancy")
    @DbUnitDataSet(before = "BusinessMigrationTest.lock.success.reentrancy.before.csv",
            after = "BusinessMigrationTest.lock.success.after.reentrancy.csv")
    void testLockSuccessReentrancy() {
        BusinessChangeRequest request =
                new BusinessChangeRequest(1L, 2L, 777L, "m12345678");
        GenericCallResponse response = mbiApiClient.lockBusiness(request);
        assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @Test
    @DisplayName("Проверка удачной разблокировки бизнесов")
    @DbUnitDataSet(before = "BusinessMigrationTest.unlock.success.before.csv",
            after = "BusinessMigrationTest.unlock.success.after.csv")
    void testUnlock() {
        BusinessChangeRequest request =
                new BusinessChangeRequest(1L, 2L, 777L, "m12345678");
        GenericCallResponse response = mbiApiClient.unlockBusiness(request);
        assertEquals(GenericCallResponseStatus.OK, response.getStatus());
    }

    @Test
    @DisplayName("Проверка неудачной разблокировки бизнесов - один из бизнесов заблокирован другой транзакцией")
    @DbUnitDataSet(before = "BusinessMigrationTest.lock.fail.anotherTransaction.before.csv",
            after = "BusinessMigrationTest.lock.fail.anotherTransaction.after.csv")
    void testUnlockFailBlockedByAnotherTransaction() {
        BusinessChangeRequest request =
                new BusinessChangeRequest(1L, 2L, 777L, "m12345678");
        GenericCallResponse response = mbiApiClient.unlockBusiness(request);
        assertEquals("Некорректные идентификаторы блокировок:\n" +
                "\tбизнес 1 блокирован идентификатором процесса 'm-0'\n", response.getMessage());
        assertEquals(GenericCallResponseStatus.ERROR, response.getStatus());
    }
}
