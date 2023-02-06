package ru.yandex.direct.core.testing.configuration

import com.yandex.ydb.core.Result
import com.yandex.ydb.core.rpc.RpcTransport
import com.yandex.ydb.table.Session
import com.yandex.ydb.table.SessionRetryContext
import com.yandex.ydb.table.TableClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.yandex.direct.common.configuration.UacYdbConfiguration
import ru.yandex.direct.common.configuration.UacYdbConfiguration.Companion.UAC_YDB_CLIENT_BEAN
import ru.yandex.direct.common.configuration.UacYdbConfiguration.Companion.UAC_YDB_INFO_BEAN
import ru.yandex.direct.common.configuration.UacYdbConfiguration.Companion.UAC_YDB_PATH_BEAN
import ru.yandex.direct.common.configuration.UacYdbConfiguration.Companion.UAC_YDB_RPC_TRANSPORT_BEAN
import ru.yandex.direct.common.configuration.UacYdbConfiguration.Companion.UAC_YDB_SESSION_PROPERTIES_BEAN
import ru.yandex.direct.common.configuration.UacYdbConfiguration.Companion.UAC_YDB_TABLE_CLIENT_BEAN
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.UAC_YDB_TABLES
import ru.yandex.direct.ydb.YdbPath
import ru.yandex.direct.ydb.client.YdbClient
import ru.yandex.direct.ydb.client.YdbSessionProperties
import ru.yandex.direct.ydb.table.Table
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfo
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfoFactory
import java.time.Duration

@Configuration
@Import(UacYdbConfiguration::class)
open class UacYdbTestingConfiguration {

    @Bean(UAC_YDB_INFO_BEAN)
    open fun ydbInfo(): YdbInfo {
        val ydbInfo: YdbInfo = YdbInfoFactory.getExecutor()
        createTables(ydbInfo)
        return ydbInfo
    }

    @Bean(UAC_YDB_PATH_BEAN)
    open fun ydbPath(@Qualifier(UAC_YDB_INFO_BEAN) ydbInfo: YdbInfo): YdbPath {
        return ydbInfo.db
    }

    @Bean(UAC_YDB_TABLE_CLIENT_BEAN)
    open fun tableClient(@Qualifier(UAC_YDB_INFO_BEAN) ydbInfo: YdbInfo): TableClient {
        return ydbInfo.client
    }

    @MockBean(name = UAC_YDB_RPC_TRANSPORT_BEAN)
    lateinit var grpcTransport: RpcTransport

    @Bean(UAC_YDB_SESSION_PROPERTIES_BEAN)
    open fun ydbProperties(): YdbSessionProperties {
        return YdbSessionProperties.builder().build()
    }

    @Bean(UAC_YDB_CLIENT_BEAN)
    open fun ydbClient(
        @Qualifier(UAC_YDB_TABLE_CLIENT_BEAN) tableClient: TableClient,
        @Qualifier(UAC_YDB_SESSION_PROPERTIES_BEAN) ydbProperties: YdbSessionProperties,
    ): YdbClient {
        val sessionRetryContext = SessionRetryContext.create(tableClient)
            .maxRetries(ydbProperties.maxQueryRetries)
            .retryNotFound(ydbProperties.isRetryNotFound)
            .build()
        return YdbClient(sessionRetryContext, Duration.ofMinutes(1))
    }

    private fun createTables(ydbInfo: YdbInfo) {
        UAC_YDB_TABLES.forEach { table -> createTable(ydbInfo, table) }
    }

    private fun createTable(
        ydbInfo: YdbInfo,
        table: Table,
    ) {
        val tableClient: TableClient = ydbInfo.client
        val db: YdbPath = ydbInfo.db
        tableClient.createSession().thenAccept { sessionResult: Result<Session> ->
            val session = sessionResult.expect("Cannot create session")
            val tableDescription = table.description
            val ydbPath = YdbPath.of(db.path, table.realName)
            session.createTable(ydbPath.path, tableDescription).join().expect("Cannot create table")
            session.close().join()
        }.join()
    }
}
