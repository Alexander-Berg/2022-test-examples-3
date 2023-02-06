package ru.yandex.direct.jobs.uac.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.direct.common.configuration.UacYdbConfiguration
import ru.yandex.direct.ydb.YdbPath
import ru.yandex.direct.ydb.client.YdbClient

abstract class AbstractUacRepositoryJobTest {

    @Autowired
    @Qualifier(UacYdbConfiguration.UAC_YDB_CLIENT_BEAN)
    protected lateinit var ydbClient: YdbClient

    @Autowired
    @Qualifier(UacYdbConfiguration.UAC_YDB_PATH_BEAN)
    protected lateinit var path: YdbPath

}
