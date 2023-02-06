package ru.yandex.direct.teststeps.controller

import io.swagger.annotations.*
import org.apache.logging.log4j.util.Strings
import org.springframework.web.bind.annotation.*
import ru.yandex.direct.common.net.NetworkName
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate
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
 * Контроллер для степов турболендинга
 */
@RestController
@RequestMapping("/turbolanding")
@AllowedSubjectRoles(RbacRole.SUPER, RbacRole.SUPERREADER, RbacRole.SUPPORT, RbacRole.PLACER, RbacRole.MEDIA, RbacRole.MANAGER, RbacRole.AGENCY, RbacRole.INTERNAL_AD_ADMIN, RbacRole.INTERNAL_AD_MANAGER, RbacRole.INTERNAL_AD_SUPERREADER, RbacRole.CLIENT)
@Api(value = "Контроллер для степов турболендинга")
class TurboLandingStepsController(
    private val testStepsService: TestStepsService
) {
    companion object {
        private const val TURBO_LANDING_STEPS_TAG = "TurboLanding steps"
    }

    @POST
    @ApiOperation(value = "Создать турболендинг с дефолтными настройками.",
        tags = [TURBO_LANDING_STEPS_TAG],
        httpMethod = "POST",
        nickname = "createDefaultTurboLanding")
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(path = ["createDefaultTurboLanding"], consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @ResponseBody
    fun createDefaultTurboLanding(
        @ApiParam(value = "login", required = true) @RequestParam(value = "login") login: String
    ): WebResponse {
        return if (Strings.isBlank(login)) {
            WebErrorResponse(500, "'login' can't be empty'")
        } else try {
            TestStepsIdResponse(testStepsService.turboLandingStepsService().createDefaultTurboLanding(login))
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }

    @POST
    @ApiOperation(value = "Удаление турболендингов.",
        tags = [TURBO_LANDING_STEPS_TAG],
        httpMethod = "POST",
        nickname = "deleteTurboLandings")
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(path = ["deleteTurboLandings"], consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @ResponseBody
    fun deleteTurboLandings(
        @ApiParam(value = "login", required = true) @RequestParam(value = "login") login: String,
        @ApiParam(value = "ids", required = true) @RequestParam(value = "ids") ids: List<Long>
    ): WebResponse {
        return if (Strings.isBlank(login)) {
            WebErrorResponse(500, "'login' can't be empty'")
        } else try {
            testStepsService.turboLandingStepsService().deleteTurboLandings(login, ids)
            TestStepsSuccessResponse()
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }

    @POST
    @ApiOperation(value = "Связать баннер с турболендингом.",
        tags = [TURBO_LANDING_STEPS_TAG],
        httpMethod = "POST",
        nickname = "linkBannerWithTurboLanding")
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(path = ["linkBannerWithTurboLanding"], consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @ResponseBody
    fun linkBannerWithTurboLanding(
        @ApiParam(value = "login", required = true) @RequestParam(value = "login") login: String,
        @ApiParam(value = "turboLandingId", required = true) @RequestParam(value = "turboLandingId") turboLandingId: Long,
        @ApiParam(value = "bannerId", required = true) @RequestParam(value = "bannerId") bannerId: Long,
        @ApiParam(value = "campaignId", required = true) @RequestParam(value = "campaignId") campaignId: Long,
        @ApiParam(value = "isDisabled", required = true, defaultValue = "false") @RequestParam(value = "isDisabled", defaultValue = "false") isDisabled: Boolean,
        @ApiParam(value = "statusModerate", required = true, defaultValue = "YES") @RequestParam(value = "statusModerate", defaultValue = "YES") statusModerate: BannerTurboLandingStatusModerate,
    ): WebResponse {
        return if (Strings.isBlank(login)) {
            WebErrorResponse(500, "'login' can't be empty'")
        } else try {
            testStepsService.turboLandingStepsService()
                .linkBannerWithTurboLanding(
                    login = login,
                    turboLandingId = turboLandingId,
                    bannerId = bannerId,
                    campaignId = campaignId,
                    isDisabled = isDisabled,
                    statusModerate = statusModerate)
            TestStepsSuccessResponse()
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }
}
