package ru.yandex.market.logistics.management.function;

import java.sql.Connection;
import java.sql.SQLException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class InsertScheduleWithPartnerRelationTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("insert_schedule_with_partner_relation кидает если partner_relation не существует")
    void insertScheduleWithPartnerRelationThrows() throws SQLException {
        Assertions.assertThrows(
            PSQLException.class,
            () -> insertImportSchedule("{}"),
            "ERROR: Partner relation from 1 to 2 does not exist"
        );
    }

    @Test
    @DisplayName("insert_schedule_with_partner_relation успешно добавляет новое расписание")
    @DatabaseSetup("/data/function/before/partner_relation_without_intake_schedule.xml")
    @ExpectedDatabase(
        value = "/data/function/after/partner_relation_with_intake_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertScheduleWithPartnerRelationSuccess() throws SQLException {
        insertImportSchedule(
            "{\"import_schedule\": [{\"day\":1,\"time_from\":\"11:00:00\",\"time_to\":\"20:00:00\",\"is_main\":true}]}"
        );
    }

    @Test
    @DisplayName("insert_schedule_with_partner_relation успешно добавляет новое расписание и удаляет старое")
    @DatabaseSetup("/data/function/before/partner_relation_with_intake_schedule.xml")
    @ExpectedDatabase(
        value = "/data/function/after/partner_relation_with_intake_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertScheduleWithPartnerRelationSuccessRemovesOldSchedule() throws SQLException {
        insertImportSchedule(
            "{\"import_schedule\": [{\"day\":1,\"time_from\":\"11:00:00\",\"time_to\":\"20:00:00\",\"is_main\":true}]}"
        );
    }

    void insertImportSchedule(String data) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.prepareStatement(
            "SELECT insert_schedule_with_partner_relation(1, 2, '" + data + "')"
        ).executeQuery();
    }
}
