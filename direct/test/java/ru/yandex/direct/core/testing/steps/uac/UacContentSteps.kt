package ru.yandex.direct.core.testing.steps.uac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.uac.createDefaultHtml5Content
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImageFormat
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.BannerSteps
import ru.yandex.direct.core.testing.steps.CreativeSteps
import ru.yandex.direct.utils.JsonUtils

@Lazy
@Component
class UacContentSteps {

    @Autowired
    private lateinit var bannerSteps: BannerSteps

    @Autowired
    private lateinit var creativeSteps: CreativeSteps

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    fun createHtml5Content(
        clientInfo: ClientInfo,
        accountId: String,
    ): UacYdbContent {
        val creative = creativeSteps.addDefaultHtml5CreativeWithSize(
            clientInfo, 1600, 900
        )

        val html5Content = createDefaultHtml5Content(
            accountId = accountId,
            creativeId = creative.creativeId,
        )
        uacYdbContentRepository.saveContents(listOf(html5Content))

        return html5Content
    }

    fun createImageContent(
        clientInfo: ClientInfo,
        accountId: String,
        imageContent: UacYdbContent = createDefaultImageContent(accountId = accountId)
    ): UacYdbContent {
        val contentSize = imageContent.meta["orig-size"] as Map<String, Any>
        val imageFormat = defaultBannerImageFormat(null)
            .withImageHash(imageContent.directImageHash)
            .withMdsMetaJson(JsonUtils.MAPPER.writeValueAsString(imageContent.meta["direct_mds_meta"]))
            .withHeight((contentSize["height"] as Int).toLong())
            .withWidth((contentSize["width"] as Int).toLong())

        uacYdbContentRepository.saveContents(listOf(imageContent))
        bannerSteps.createBannerImageFormat(clientInfo, imageFormat)

        return imageContent
    }

    fun createVideoContent(clientInfo: ClientInfo, accountId: String): UacYdbContent {
        val creative = creativeSteps.addDefaultVideoAdditionCreative(clientInfo)
        val videoContent = createDefaultVideoContent(
            accountId = accountId,
            creativeId = creative.creativeId,
        )
        uacYdbContentRepository.saveContents(listOf(videoContent))
        return videoContent
    }

    fun createVideoContent(videoContent: UacYdbContent): UacYdbContent {
        uacYdbContentRepository.saveContents(listOf(videoContent))
        return videoContent
    }
}
