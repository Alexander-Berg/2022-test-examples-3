package ru.yandex.direct.teststeps.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.logging.log4j.util.Strings
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import ru.yandex.direct.common.net.NetworkName
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.teststeps.controller.model.TestStepsSuccessResponse
import ru.yandex.direct.teststeps.service.TestStepsService
import ru.yandex.direct.web.annotations.AllowedSubjectRoles
import ru.yandex.direct.web.core.model.WebErrorResponse
import ru.yandex.direct.web.core.model.WebResponse
import ru.yandex.direct.web.core.security.netacl.AllowNetworks
import javax.ws.rs.POST
import javax.ws.rs.core.MediaType

/**
 * Контроллер для еком сущностей
 */
@RestController
@RequestMapping("/ecom")
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
@Api(value = "Контроллер для еком сущностей")
class EcomStepsController(
    private val testStepsService: TestStepsService
) {
    companion object {
        private const val ECOM_STEPS_TAG = "Ecom steps"
    }

    @POST
    @ApiOperation(
        value = "Добавить известный ecom-домен в таблицу еком доменов.",
        tags = [ECOM_STEPS_TAG],
        httpMethod = "POST",
        nickname = "addEcomDomain"
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse::class),
        ApiResponse(code = 500, message = "Error", response = WebErrorResponse::class)
    )
    @AllowNetworks(NetworkName.INTERNAL)
    @PostMapping(
        path = ["addEcomDomain"],
        consumes = [MediaType.APPLICATION_JSON],
        produces = [MediaType.APPLICATION_JSON]
    )
    @ResponseBody
    fun addEcomDomain(
        @ApiParam(value = "domain", required = true) @RequestParam(value = "domain") domain: String,
        @ApiParam(value = "offersCount", required = true) @RequestParam(value = "offersCount") offersCount: Long
    ): WebResponse {
        return if (Strings.isBlank(domain)) {
            WebErrorResponse(500, "'domain' can't be empty'")
        } else if (offersCount < 0) {
            WebErrorResponse(500, "'offersCount' should be positive'")
        } else try {
            testStepsService.ecomStepsService().addEcomDomain(domain, offersCount)
            TestStepsSuccessResponse()
        } catch (e: IllegalArgumentException) {
            WebErrorResponse(500, e.message)
        }
    }
}
