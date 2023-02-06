package ru.yandex.direct.core.testing.repository

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Repository
import ru.yandex.direct.common.configuration.UacYdbConfiguration
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.CONTENT
import ru.yandex.direct.ydb.YdbPath
import ru.yandex.direct.ydb.builder.querybuilder.DeleteBuilder
import ru.yandex.direct.ydb.client.YdbClient

@Lazy
@Repository
class TestUacYdbContentRepository(
    @Qualifier(UacYdbConfiguration.UAC_YDB_CLIENT_BEAN) var ydbClient: YdbClient,
    @Qualifier(UacYdbConfiguration.UAC_YDB_PATH_BEAN) var path: YdbPath,
    private var uacYdbContentRepository: UacYdbContentRepository,
) {

    fun getContentsByAccountId(accountId: String): Collection<UacYdbContent> {
        val queryBuilder = uacYdbContentRepository.selectAllContentFields(CONTENT)
            .from(CONTENT)
            .where(CONTENT.ACCOUNT_ID.eq(accountId.toIdLong()))

        val queryAndParams = queryBuilder.queryAndParams(path)
        val result = ydbClient.executeOnlineRoQuery(queryAndParams, true).getResultSet(0)
        val contents = mutableListOf<UacYdbContent>()
        while (result.next()) {
            val content = uacYdbContentRepository.convertResultToYdbContent(result)
            contents.add(content)
        }
        return contents
    }

    fun clean() {
        val queryAndParams = DeleteBuilder
            .deleteFrom(CONTENT)
            .queryAndParams(path)
        ydbClient.executeQuery(queryAndParams, true)
    }
}
