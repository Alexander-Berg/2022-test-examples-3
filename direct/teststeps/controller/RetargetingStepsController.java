package ru.yandex.direct.teststeps.controller;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.direct.teststeps.controller.model.TestStepsIdResponse;
import ru.yandex.direct.teststeps.service.TestStepsService;
import ru.yandex.direct.web.annotations.AllowedSubjectRoles;
import ru.yandex.direct.web.core.model.WebErrorResponse;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.netacl.AllowNetworks;

import static ru.yandex.direct.common.net.NetworkName.INTERNAL;
import static ru.yandex.direct.rbac.RbacRole.AGENCY;
import static ru.yandex.direct.rbac.RbacRole.CLIENT;
import static ru.yandex.direct.rbac.RbacRole.INTERNAL_AD_ADMIN;
import static ru.yandex.direct.rbac.RbacRole.INTERNAL_AD_MANAGER;
import static ru.yandex.direct.rbac.RbacRole.INTERNAL_AD_SUPERREADER;
import static ru.yandex.direct.rbac.RbacRole.MANAGER;
import static ru.yandex.direct.rbac.RbacRole.MEDIA;
import static ru.yandex.direct.rbac.RbacRole.PLACER;
import static ru.yandex.direct.rbac.RbacRole.SUPER;
import static ru.yandex.direct.rbac.RbacRole.SUPERREADER;
import static ru.yandex.direct.rbac.RbacRole.SUPPORT;

@RestController
@RequestMapping("/retargetings")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для ретаргетингов")
public class RetargetingStepsController {

    private static final String RETARGETINGS_STEPS_TAG = "Retargetings steps";
    private final TestStepsService testStepsService;

    @Autowired
    public RetargetingStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }

    @POST
    @ApiOperation(
            value = "Создание ретагетинга с заданым условием ретаргетинга.",
            tags = RETARGETINGS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createRetargetingWithCondition"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createRetargeting",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createRetargeting(
            @ApiParam(value = "adGroupId", required = true)
            @Nonnull @RequestParam(value = "adGroupId") Long adGroupId,
            @ApiParam(value = "retCondId", required = true)
            @Nonnull @RequestParam(value = "retCondId") Long retCondId,
            @ApiParam(value = "priceContext", required = true)
            @Nonnull @RequestParam(value = "priceContext") BigDecimal priceContext
         ) {
        try {
            return new TestStepsIdResponse(testStepsService.retargetingSteps()
                    .createRetargeting(adGroupId, retCondId, priceContext));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

}
