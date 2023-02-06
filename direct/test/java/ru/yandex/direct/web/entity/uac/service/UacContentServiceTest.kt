package ru.yandex.direct.web.entity.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.getExpectedImageContentMeta
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.samples.CONTENT_IMAGE_META
import ru.yandex.direct.core.entity.uac.service.UacContentService
import ru.yandex.direct.utils.fromJson

class UacContentServiceTest {

    private val uacContentService = UacContentService(mock(UacYdbContentRepository::class.java),
        mock(UacYdbCampaignContentRepository::class.java),
        mock(UacYdbAccountRepository::class.java),
    )

    @Test
    fun fillContentTest() {
        val meta = fromJson<Map<String, Any>>(CONTENT_IMAGE_META)
        val ydbContent = createDefaultImageContent(
            meta = meta,
            thumb = "https://avatars.mds.yandex.net/get-uac-test/4220162/121e631f-db50-463e-9fd0-dfe799c78df8/thumb",
        )
        val expectedContent = getExpectedContent(ydbContent, meta["direct_mds_meta"])

        val content = uacContentService.fillContent(ydbContent)
        assertThat(content).isEqualTo(expectedContent)
    }

    @Test
    fun fillContentAbsentMdsMetaTest() {
        val meta = fromJson<Map<String, Any>>(CONTENT_IMAGE_META).filter { it.key != "direct_mds_meta" }.toMap()
        val ydbContent = createDefaultImageContent(
            meta = meta,
            thumb = "https://avatars.mds.yandex.net/get-uac-test/4220162/121e631f-db50-463e-9fd0-dfe799c78df8/thumb",
        )
        val expectedContent = getExpectedContent(ydbContent, directMdsMeta = null)

        val content = uacContentService.fillContent(ydbContent)
        assertThat(content).isEqualTo(expectedContent)
    }

    private fun getExpectedContent(ydbContent: UacYdbContent, directMdsMeta: Any?) = Content(
        id = ydbContent.id,
        type = ydbContent.type,
        thumb = ydbContent.thumb,
        thumbId = "4220162/121e631f-db50-463e-9fd0-dfe799c78df8",
        sourceUrl = ydbContent.sourceUrl,
        directImageHash = ydbContent.directImageHash,
        iw = 850,
        ih = 850,
        ow = 950,
        oh = 950,
        tw = 850,
        th = 850,
        meta = getExpectedImageContentMeta(directMdsMeta),
        videoDuration = null,
        filename = null,
        mdsUrl = null,
    )
}
