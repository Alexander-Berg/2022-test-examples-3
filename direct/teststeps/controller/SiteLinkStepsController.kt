package ru.yandex.direct.teststeps.controller

import io.swagger.annotations.*
import org.apache.logging.log4j.util.Strings
import org.springframework.web.bind.annotation.*
import ru.yandex.direct.common.net.NetworkName
import ru.yandex.direct.core.entity.sitelink.model.Sitelink
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.teststeps.controller.model.TestStepsIdResponse
import ru.yandex.direct.teststeps.controller.model.TestStepsSuccessResponse
import ru.yandex.direct.teststeps.service.TestStepsService
import ru.yandex.direct.web.annotations.AllowedSubjectRoles
import ru.yandex.direct.web.core.model.WebErrorResponse
import ru.yandex.direct.web.core.model.WebResponse
import ru.yandex.direct.web.core.security.netacl.AllowNetworks
import javax.ws.rs.POST
import javax.ws.rs.core.MediaType

/**
 * Контроллер для степов быстрых ссылок (сайтлинков)
 */
@RestController
@RequestMapping("/sitelink")
@AllowedSubjectRoles(RbacRole.SUPER, RbacRole.SUPERREADER, RbacRole.SUPPORT, RbacRole.PLACER, RbacRole.MEDIA, RbacRole.MANAGER, RbacRole.AGENCY, RbacRole.INTERNAL_AD_ADMIN, RbacRole.INTERNAL_AD_MANAGER, RbacRole.INTERNAL_AD_SUPERREADER, RbacRole.CLIENT)
@Api(value = "Контроллер для степов быстрых ссылок (сайтлинков)")
class SiteLinkStepsController(
    private val testStepsService: TestStepsService
) {
    companion object {
        private const val SITE_LINK_STEPS_TAG = "SiteLink steps"
    }

    @POST
    @ApiOperation(value = "Создать дефолтный набор быстрых ссылок.",
        tags = [SITE_LINK_STEPS_TAG],
        httpMethod = "POST",
        nickname = "createDefaultSiteLinkSet")
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(path = ["createDefaultSiteLinkSet"], consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @ResponseBody
    fun createDefaultSiteLinkSet(
        @ApiParam(value = "login", required = true) @RequestParam(value = "login") login: String
    ): WebResponse {
        return if (Strings.isBlank(login)) {
            WebErrorResponse(500, "'login' can't be empty'")
        } else try {
            TestStepsIdResponse(testStepsService.siteLinkSteps().createDefaultSiteLinkSet(login))
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }

    @POST
    @ApiOperation(value = "Создать набор быстрых ссылок.",
        tags = [SITE_LINK_STEPS_TAG],
        httpMethod = "POST",
        nickname = "createSiteLinkSet")
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(path = ["createSiteLinkSet"], consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @ResponseBody
    fun createSiteLinkSet(
        @ApiParam(value = "login", required = true) @RequestParam(value = "login") login: String,
        @ApiParam(value = "sitelinkTitles", required = true) @RequestParam(value = "sitelinkTitles") sitelinkTitles: List<String>,
        @ApiParam(value = "sitelinkHrefs", required = true) @RequestParam(value = "sitelinkHrefs") sitelinkHrefs: List<String>,
        @ApiParam(value = "sitelinkDescriptions", required = true) @RequestParam(value = "sitelinkDescriptions") sitelinkDescriptions: List<String>,
    ): WebResponse {
        return if (Strings.isBlank(login)) {
            WebErrorResponse(500, "'login' can't be empty'")
        } else try {
            val siteLinks = List<Sitelink>(sitelinkTitles.size){
                Sitelink()
                    .withTitle(sitelinkTitles[it])
                    .withHref(sitelinkHrefs[it])
                    .withDescription(sitelinkDescriptions[it])
            }
            TestStepsIdResponse(testStepsService.siteLinkSteps().createSiteLinkSet(login, siteLinks))
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }

    @POST
    @ApiOperation(value = "Связать баннер с набором быстрых ссылок.",
        tags = [SITE_LINK_STEPS_TAG],
        httpMethod = "POST",
        nickname = "linkBannerWithSiteLinkSet")
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(path = ["linkBannerWithSiteLinkSet"], consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @ResponseBody
    fun linkBannerWithSiteLinkSet(
        @ApiParam(value = "login", required = true) @RequestParam(value = "login") login: String,
        @ApiParam(value = "siteLinkSetId", required = true) @RequestParam(value = "siteLinkSetId") siteLinkSetId: Long,
        @ApiParam(value = "bannerId", required = true) @RequestParam(value = "bannerId") bannerId: Long,
        @ApiParam(value = "campaignId", required = true) @RequestParam(value = "campaignId") campaignId: Long
    ): WebResponse {
        return if (Strings.isBlank(login)) {
            WebErrorResponse(500, "'login' can't be empty'")
        } else try {
            testStepsService.siteLinkSteps()
                .linkBannerWithSiteLinkSet(
                    login = login,
                    siteLinkSetId = siteLinkSetId,
                    bannerId = bannerId,
                    campaignId = campaignId)
            TestStepsSuccessResponse()
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }
}
