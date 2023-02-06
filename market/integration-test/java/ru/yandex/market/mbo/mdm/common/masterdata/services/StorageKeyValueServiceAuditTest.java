package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author dmserebr
 * @date 18/12/2020
 */
public class StorageKeyValueServiceAuditTest extends MdmServicesAuditTestBase {
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Override
    protected String getEntityType() {
        return "storage_key_value";
    }

    @Override
    protected AuditRecordTestInfo insertTestValue() {
        storageKeyValueService.putValue("test-key", "test-value");

        return new AuditRecordTestInfo("test-key", List.of(
            Pair.of("storage_key", "test-key"),
            Pair.of("storage_value", "\"test-value\"")));
    }

    @Override
    protected AuditRecordTestInfo updateTestValue() {
        storageKeyValueService.putValue("test-key", "test-value-2");

        return new AuditRecordTestInfo("test-key", List.of(
            Pair.of("storage_value", "[\"test-value\", \"test-value-2\"]")));
    }
}
