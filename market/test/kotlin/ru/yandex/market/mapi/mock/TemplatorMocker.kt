package ru.yandex.market.mapi.mock

import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.mockito.stubbing.OngoingStubbing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.client.cms.TemplatorClient
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.model.screen.params.TemplatorParams
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * @author Ilya Kislitsyn / ilyakis@ / 04.02.2022
 */
@Service
class TemplatorMocker {
    @Autowired
    private lateinit var templatorClient: TemplatorClient

    fun getClient() = templatorClient

    fun mockPageResponse(file: String, pageType: String? = null, qualifier: String? = null) {
        whenTemplate(pageType, qualifier).then {
            // generate new response on each call
            val response = JsonHelper.parse<ScreenResponse>(file.asResource())
            CompletableFuture.completedFuture(Supplier { response })
        }
    }

    fun mockPageErrorResponse(pageType: String? = null, qualifier: String? = null) {
        whenTemplate(pageType, qualifier).then {
            CompletableFuture.failedFuture<Void>(RuntimeException("cms error call"))
        }
    }

    fun mockFailedCallPageResponse(pageType: String? = null, qualifier: String? = null) {
        whenTemplate(pageType, qualifier).then {
            throw RuntimeException("cms error call")
        }
    }

    fun mockCachedPageResponse(file: String, pageType: String? = null, qualifier: String? = null) {
        whenSimpleTemplate(pageType, qualifier).then {
            // generate new response on each call
            val response = JsonHelper.parse<ScreenResponse>(file.asResource())
            CompletableFuture.completedFuture(Supplier { response })
        }
    }

    fun mockCachedPageResponseError(pageType: String? = null, qualifier: String? = null) {
        whenSimpleTemplate(pageType, qualifier).then {
            CompletableFuture.failedFuture<Void>(RuntimeException("simple cms error call"))
        }
    }

    private fun whenTemplate(
        pageType: String? = null,
        qualifier: String? = null,
        templatorParams: TemplatorParams? = null
    ): OngoingStubbing<CompletableFuture<Supplier<ScreenResponse>>> {
        return whenever(
            templatorClient.getCmsPageTemplate(
                when (pageType) {
                    null -> anyString()
                    else -> eq(pageType)
                },
                when (qualifier) {
                    null -> isNull()
                    "any" -> anyString()
                    else -> eq(qualifier)
                },
                when (templatorParams) {
                    null -> isNull()
                    else -> eq(templatorParams)
                }
            )
        )
    }

    private fun whenSimpleTemplate(
        pageType: String? = null,
        qualifier: String? = null
    ): OngoingStubbing<CompletableFuture<Supplier<ScreenResponse>>> {
        return whenever(
            templatorClient.getCmsPageTemplateSimple(
                when (pageType) {
                    null -> anyString()
                    else -> eq(pageType)
                },
                when (qualifier) {
                    null -> isNull()
                    "any" -> anyString()
                    else -> eq(qualifier)
                }
            )
        )
    }
}
