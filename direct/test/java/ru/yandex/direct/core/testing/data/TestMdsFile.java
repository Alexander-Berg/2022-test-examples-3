package ru.yandex.direct.core.testing.data;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.mdsfile.model.MdsFileCustomName;
import ru.yandex.direct.core.entity.mdsfile.model.MdsFileMetadata;
import ru.yandex.direct.core.entity.mdsfile.model.MdsStorageHost;
import ru.yandex.direct.core.entity.mdsfile.model.MdsStorageType;
import ru.yandex.direct.dbutil.model.ClientId;

public class TestMdsFile {

    private TestMdsFile() {
    }

    public static MdsFileMetadata testMdsFileMetadata(@Nullable ClientId clientId) {
        String filename = "test_" + RandomStringUtils.randomAlphanumeric(20);
        return new MdsFileMetadata()
                .withType(MdsStorageType.XLS_REPORTS)
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withStorageHost(MdsStorageHost.STORAGE_INT_MDS_YANDEX_NET)
                .withMdsKey("999")
                .withFilename(filename)
                .withFileImprint(filename)
                .withSize(999L);
    }

    public static MdsFileCustomName testMdsFileCustomName(Long mdsFileMetadataId) {
        String filename = "test_" + RandomStringUtils.randomAlphanumeric(20);
        return new MdsFileCustomName()
                .withMdsId(mdsFileMetadataId)
                .withFilename(filename);
    }
}
