package ru.yandex.market.hrms.tms.manager.outstaff;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.OutstaffV2SyncManager;

@DbUnitDataSet(schema = "public")
public class OutstaffV2SyncManagerTest extends AbstractTmsTest {
    @Autowired
    private OutstaffV2SyncManager outstaffV2SyncManager;

    @BeforeEach
    void setUp() {
        mockClock(LocalDateTime.of(2022, 8, 3, 11, 42, 56));
    }

    @Test
    @DisplayName("Успешная миграция стандартного аутстаффера")
    @DbUnitDataSet(before = "OutstaffV2SyncManagerTest.before.csv", after = "OutstaffV2SyncManagerTest.after.csv")
    void shouldMigrateOutstaffToNewScheme() {
        outstaffV2SyncManager.migrate();
    }

    @Test
    @DisplayName("Пропускаем в миграции аутстафферов без company_login_id")
    @DbUnitDataSet(
            before = "OutstaffV2SyncManagerTest.skipWithoutCompanyLoginId.before.csv",
            after = "OutstaffV2SyncManagerTest.skipWithoutCompanyLoginId.after.csv"
    )
    void skipWithoutCompanyLoginId() {
        outstaffV2SyncManager.migrate();
    }

    @Test
    @DisplayName("Успешная миграция аутстаффера без документов и domain_id")
    @DbUnitDataSet(
            before = "OutstaffV2SyncManagerTest.migrateWithoutDocsAndDomainId.before.csv",
            after = "OutstaffV2SyncManagerTest.migrateWithoutDocsAndDomainId.after.csv"
    )
    void shouldMigrateWithoutDocsAndDomainId() {
        outstaffV2SyncManager.migrate();
    }

    @Test
    @DisplayName("Пропускаем в миграции мигрированных ранее аутстафферов")
    @DbUnitDataSet(
            before = "OutstaffV2SyncManagerTest.skipWithOutstaffV2Id.csv",
            after = "OutstaffV2SyncManagerTest.skipWithOutstaffV2Id.csv"
    )
    void skipAlreadyMigratedOutstaff() {
        outstaffV2SyncManager.migrate();
    }
}
