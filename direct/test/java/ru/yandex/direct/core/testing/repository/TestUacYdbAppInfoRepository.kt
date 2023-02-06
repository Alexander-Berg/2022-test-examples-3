package ru.yandex.direct.core.testing.repository

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Repository
import ru.yandex.direct.common.configuration.UacYdbConfiguration
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.APP_INFO
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.APP_INFO_BUNDLE_ID_INDEX
import ru.yandex.direct.ydb.YdbPath
import ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder.deleteFrom
import ru.yandex.direct.ydb.client.YdbClient

@Lazy
@Repository
class TestUacYdbAppInfoRepository(
    @Qualifier(UacYdbConfiguration.UAC_YDB_CLIENT_BEAN) var ydbClient: YdbClient,
    @Qualifier(UacYdbConfiguration.UAC_YDB_PATH_BEAN) var path: YdbPath,
) {
    private fun cleanAppInfo() {
        val queryAndParams = deleteFrom(APP_INFO).queryAndParams(path)
        ydbClient.executeQuery(queryAndParams, true)
    }

    private fun cleanAppInfoBundleIdIndex() {
        val queryAndParams = deleteFrom(APP_INFO_BUNDLE_ID_INDEX).queryAndParams(path)
        ydbClient.executeQuery(queryAndParams, true)
    }

    fun clean() {
        cleanAppInfo()
        cleanAppInfoBundleIdIndex()
    }
}
