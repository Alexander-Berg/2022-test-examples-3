package ru.yandex.market.logistics.management.service.combinator;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;

@DatabaseSetup(
    value = "/data/service/combinator/db/before/services_meta_values_set_up.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class SyncMetaValuesAsTagsTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_value.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_meta_values_expected_two_values.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DisplayName("Inserting a new value should add that value to tags")
    void metaValueInserted() {
        // DB-only check
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_value.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_value.xml",
        type = DatabaseOperation.DELETE,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_meta_values_expected_one_value.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DisplayName("Deleting a value should remove that value from tags")
    void metaValueDeleted() {
        // DB-only check
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_value_modified.xml",
        type = DatabaseOperation.UPDATE,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_meta_values_expected_one_value_modified.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DisplayName("Updating a value should update that value in tags")
    void metaValueUpdated() {
        // DB-only check
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_key.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_meta_values_expected_one_value.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DisplayName("Inserting a new key should have no effect")
    void metaKeyInserted() {
        // DB-only check
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_key.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_key.xml",
        type = DatabaseOperation.DELETE,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_meta_values_expected_one_value.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DisplayName("Deleting a key should have no effect")
    void metaKeyDeleted() {
        // DB-only check
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/services_single_meta_single_key_modified.xml",
        type = DatabaseOperation.UPDATE,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_meta_values_expected_one_value_key_modified.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @DisplayName("Updating a key name should update the key name tags")
    void metaKeyUpdated() {
        // DB-only check
    }
}
