package ru.yandex.direct.teststeps.controller;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.teststeps.controller.model.TestStepsIdResponse;
import ru.yandex.direct.teststeps.controller.model.TestStepsSuccessResponse;
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
import static ru.yandex.direct.utils.CommonUtils.nvl;

@RestController
@RequestMapping("/pricepackages")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для степов прайсовых пакетов")
public class PricePackageStepsController {
    private static final String PRICE_PACKAGES_STEPS_TAG = "Price packages steps";
    private final TestStepsService testStepsService;

    @Autowired
    public PricePackageStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }

    @POST
    @ApiOperation(
            value = "Создание прайсового пакета без видео.",
            tags = PRICE_PACKAGES_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createPricePackage"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createPricePackage",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createPricePackage(
            @ApiParam(value = "clientId")
            @Nullable @RequestParam(value = "clientId") Long clientId,
            @ApiParam(value = "allowExpandedDesktopCreative", required = true)
            @Nonnull @RequestParam(value = "allowExpandedDesktopCreative") Boolean allowExpandedDesktopCreative,
            @ApiParam(value = "isApproved")
            @Nullable @RequestParam(value = "isApproved") Boolean isApproved,
            // Не все AdGroupType применимы к прайсовым. Для неприменимых ручка будет 500ить - это ок.
            @ApiParam(value = "availableAdGroupTypes")
            @Nullable @RequestParam(value = "availableAdGroupTypes") Set<String> inboundAvailableAdGroupTypes,
            @ApiParam(value = "isDraftApproveAllowed")
            @Nullable @RequestParam(value = "isDraftApproveAllowed") Boolean isDraftApproveAllowed) {
        try {
            Set<AdGroupType> availableAdGroupTypes = new HashSet();

            // availableAdGroupTypes принимаем массивом строк, т.к. генерилка типов на фронте не умеет в массив енумов
            if (inboundAvailableAdGroupTypes != null) {
                for (String type : inboundAvailableAdGroupTypes) {
                    availableAdGroupTypes.add(AdGroupType.valueOf(type));
                }
            }

            return new TestStepsIdResponse(testStepsService.pricePackageSteps()
                    .createPricePackage(clientId, allowExpandedDesktopCreative, isApproved, availableAdGroupTypes,
                            nvl(isDraftApproveAllowed, false)));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Удаление прайсового пакета.",
            tags = PRICE_PACKAGES_STEPS_TAG,
            httpMethod = "POST",
            nickname = "deletePricePackage"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "deletePricePackage",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse deletePricePackage(
            @ApiParam(value = "packageId", required = true)
            @Nonnull @RequestParam(value = "packageId") Long packageId) {
        try {
            testStepsService.pricePackageSteps().deletePricePackage(packageId);
            return new TestStepsSuccessResponse();
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

}
