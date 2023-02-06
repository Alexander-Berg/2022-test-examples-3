package ru.yandex.market.mapi.controller.custom

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.yandex.market.mapi.core.MapiConstants
import ru.yandex.market.mapi.core.contract.ScreenProcessor
import ru.yandex.market.mapi.core.model.response.MapiResponseDto
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.ScreenRequest
import ru.yandex.market.mapi.core.util.tryPutIn

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.04.2022
 */
@RestController
@RequestMapping("/api/screen/test")
class CustomTestScreenController(
    private val screenProcessor: ScreenProcessor
) {

    @GetMapping("/cms", produces = [MapiConstants.JSON_UTF8])
    fun testCmsScreen(request: ScreenRequest): MapiResponseDto {
        request.cmsPageType = "test_cms_page"
        request.cmsQualifier = "mobile_scheme_product_card"

        return screenProcessor.getScreen(request).toDto()
    }

    @GetMapping("/static", produces = [MapiConstants.JSON_UTF8])
    fun testStaticScreen(request: ScreenRequest): MapiResponseDto {
        request.staticPage = "test_page"
        return screenProcessor.getScreen(request).toDto()
    }

    @GetMapping("/custom", produces = [MapiConstants.JSON_UTF8])
    fun testCustomScreen(
        request: ScreenRequest,
        @RequestParam(MapiConstants.SKU_ID, required = false) skuId: String?,
    ): MapiResponseDto {
        request.cmsPageType = "cms_page"
        request.customizeScreenTemplate = { screen ->
            // simple customization: join cms and static sections
            val custom = screenProcessor.getTemplate("custom_template")
            screen.sections = screen.sections + custom.sections
        }
        request.customResolverParams = mutableMapOf<String, Any?>()
            .tryPutIn("skuId", skuId)

        return screenProcessor.getScreen(request).toDto()
    }

    @PostMapping("/custom/post", produces = [MapiConstants.JSON_UTF8])
    fun testCustomScreen(
        request: ScreenRequest,
        @RequestParam(MapiConstants.SKU_ID, required = false) skuId: String?,
        @RequestBody body: MapiScreenRequestBody<Any>
    ): MapiResponseDto {
        request.cmsPageType = "cms_page"
        request.customResolverParams = mutableMapOf<String, Any?>()
            .tryPutIn("skuId", skuId)

        return screenProcessor.getScreen(request, body).toDto()
    }
}