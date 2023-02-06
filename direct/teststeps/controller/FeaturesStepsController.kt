package ru.yandex.direct.teststeps.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.logging.log4j.util.Strings
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import ru.yandex.direct.common.net.NetworkName
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.teststeps.controller.model.TestStepsIdResponse
import ru.yandex.direct.teststeps.controller.model.TestStepsSuccessResponse
import ru.yandex.direct.teststeps.service.TestStepsService
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.annotations.AllowedSubjectRoles
import ru.yandex.direct.web.core.model.WebErrorResponse
import ru.yandex.direct.web.core.model.WebResponse
import ru.yandex.direct.web.core.security.netacl.AllowNetworks
import javax.ws.rs.POST
import javax.ws.rs.core.MediaType

/**
 * Контроллер для степов фичей
 */
@RestController
@RequestMapping("/features")
@AllowedSubjectRoles(
    RbacRole.SUPER,
    RbacRole.SUPERREADER,
    RbacRole.SUPPORT,
    RbacRole.PLACER,
    RbacRole.MEDIA,
    RbacRole.MANAGER,
    RbacRole.AGENCY,
    RbacRole.INTERNAL_AD_ADMIN,
    RbacRole.INTERNAL_AD_MANAGER,
    RbacRole.INTERNAL_AD_SUPERREADER,
    RbacRole.CLIENT
)
@Api(value = "Контроллер для степов фичей")
class FeaturesStepsController(
    private val testStepsService: TestStepsService
) {
    companion object {
        private const val FEATURES_STEPS_TAG = "Features steps"
    }

    @POST
    @ApiOperation(
        value = "Выставить фичи по логину. ",
        tags = [FEATURES_STEPS_TAG],
        httpMethod = "POST",
        nickname = "setFeatureForLogin",
        notes = "targetFeaturesStates — строка в json хэшем пар 'имя фичи'-'true/false'. Именно эти значения " +
            "для фичей для указанного логина будут переопределены в базе",
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(
        path = ["setFeatureForLogin"],
        consumes = [MediaType.APPLICATION_JSON],
        produces = [MediaType.APPLICATION_JSON]
    )
    @ResponseBody
    fun createDefaultTurboLanding(
        @ApiParam(value = "login", required = true) @RequestParam(value = "login") login: String,
        @RequestBody targetFeaturesStates: String
    ): WebResponse {
        return if (Strings.isBlank(login)) {
            WebErrorResponse(500, "'login' can't be empty'")
        } else try {
            testStepsService.featuresStepsService()
                .setFeatureForLogin(
                    login,
                    fromJson(targetFeaturesStates)
                )
            return TestStepsSuccessResponse()
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }
}
