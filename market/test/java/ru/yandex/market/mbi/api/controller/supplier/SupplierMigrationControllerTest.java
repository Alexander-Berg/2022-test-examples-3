package ru.yandex.market.mbi.api.controller.supplier;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.business.CanMigrateVerdictDTO;
import ru.yandex.market.mbi.api.client.entity.supplier.SetUnitedCatalogStatusRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SupplierMigrationControllerTest extends FunctionalTest {

    @DisplayName("Поставщик уже в ЕКате -> падаем")
    @Test
    @DbUnitDataSet(before = {
            "SupplierMigrationControllerTest.canMigrate.before.csv",
            "SupplierMigrationControllerTest.canMigrate.isInUCatAlready.csv"
    })
    void canMigrate_supplierInUCat_fail() {
        checkCanMigrate(222, 11, false, "Поставщик (222) уже в едином каталоге!");
    }

    @DisplayName("Бизнес поставщика заблокирован другим процессом -> падаем")
    @Test
    @DbUnitDataSet(before = {
            "SupplierMigrationControllerTest.canMigrate.before.csv",
            "SupplierMigrationControllerTest.canMigrate.businessIsBlocked.csv"
    })
    void canMigrate_partnerBusinessIsBlocked_fail() {
        checkCanMigrate(
                222, 11, false,
                "Один или несколько бизнесов заблокированы другой миграцией "
                        + Arrays.toString(new Long[]{11L}) + " !"
        );
    }

    @DisplayName("Партнер задействован в другой незавершенной операции -> падаем")
    @Test
    @DbUnitDataSet(before = {
            "SupplierMigrationControllerTest.canMigrate.before.csv",
            "SupplierMigrationControllerTest.canMigrate.partnerEngagedInOtherOperation.csv"
    })
    void canMigrate_partnerEngagedInOtherNotFinishedOperation_fail() {
        checkCanMigrate(
                222, 11, false,
                "Партнер (222) уже находится в миграции!"
        );
    }

    @DisplayName("Всё ок -> миграция возможна")
    @Test
    @DbUnitDataSet(before = "SupplierMigrationControllerTest.canMigrate.before.csv")
    void canMigrate_ok() {
        checkCanMigrate(222, 11, true, null);
    }

    @DisplayName("Бизнес в черном списке")
    @Test
    @DbUnitDataSet(before = {
            "SupplierMigrationControllerTest.canMigrate.before.csv",
            "SupplierMigrationControllerTest.blacklist.before.csv"
    })
    void canMigrateBlacklistFail() {
        checkCanMigrate(222, 11, false,
                "Бизнес (11) в черном списке для миграции!");
    }

    private void checkCanMigrate(long supplierId, long businessId, boolean expectedVerdict, String expectedReason) {
        CanMigrateVerdictDTO expected = new CanMigrateVerdictDTO(expectedVerdict, expectedReason);
        CanMigrateVerdictDTO actual = mbiApiClient.canMigrateToUCat(supplierId, businessId);
        assertEquals(expected, actual);
    }

    @DisplayName("Включить партнеру Единый каталог -> падаем, потому что не нашли бизнес")
    @Test
    void enableUnitedCatalog_businessNotFound_fail() {
        runEnableUnitedCatalogAndCaptureException(
                new SetUnitedCatalogStatusRequest(),
                "businessId for partner 546564 not found!"
        );
    }

    @DisplayName("Включить партнеру Единый каталог -> падаем, потому что бизнес не залочен")
    @Test
    @DbUnitDataSet(before = "SupplierMigrationControllerTest.enableUnitedCatalog.businessIsNotLocked.csv")
    void enableUnitedCatalog_businessNotLocked_fail() {
        runEnableUnitedCatalogAndCaptureException(
                new SetUnitedCatalogStatusRequest(),
                "Business 404040 is not locked!"
        );
    }


    @DisplayName("Включить партнеру Единый каталог -> падаем, потому что" +
            " процесс который пытается установить признак ЕКата не тот, который заблокировал бизнес")
    @Test
    @DbUnitDataSet(before = "SupplierMigrationControllerTest.enableUnitedCatalog.ok.before.csv")
    void enableUnitedCatalog_differentProcessId_fail() {
        SetUnitedCatalogStatusRequest request = new SetUnitedCatalogStatusRequest();
        request.setProcessId("fdfd56be-NO-NO-NO-aeb9e0a14fbc");
        runEnableUnitedCatalogAndCaptureException(
                request,
                "Some other process is trying to set United Catalog status! " +
                        "Expected: fdfd56be-bla-bla-bla-aeb9e0a14fbc, " +
                        "actual: fdfd56be-NO-NO-NO-aeb9e0a14fbc!"
        );
    }

    private void runEnableUnitedCatalogAndCaptureException(SetUnitedCatalogStatusRequest request, String exMessage) {
        assertEquals(
                GenericCallResponse.exception(new IllegalStateException(exMessage)),
                mbiApiClient.enableUnitedCatalog(546564L, request)
        );
    }

    @DisplayName("Включить партнеру Единый каталог")
    @Test
    @DbUnitDataSet(
            before = "SupplierMigrationControllerTest.enableUnitedCatalog.ok.before.csv",
            after = "SupplierMigrationControllerTest.enableUnitedCatalog.ok.after.csv"
    )
    void enableUnitedCatalog_ok() {
        SetUnitedCatalogStatusRequest request = new SetUnitedCatalogStatusRequest("fdfd56be-bla-bla-bla-aeb9e0a14fbc");
        assertEquals(
                new GenericCallResponse(GenericCallResponseStatus.OK, "Признак Единого каталога установлен."),
                mbiApiClient.enableUnitedCatalog(546564L, request)
        );
    }
}
