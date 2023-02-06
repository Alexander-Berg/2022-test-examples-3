package ru.yandex.direct.teststeps.controller;

import javax.annotation.Nonnull;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.logging.log4j.util.Strings;
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

/**
 * Контроллер для степов фильтров фидов
 */
@RestController
@RequestMapping("/performanceFilters")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для степов фильтров фидов")
public class PerformanceFiltersStepsController {

    private final TestStepsService testStepsService;
    private static final String PERFORMANCE_FILTERS_STEPS_TAG = "PerformanceFilters steps";

    @Autowired
    public PerformanceFiltersStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }

    @POST
    @ApiOperation(
            value = "Создание фильтров фидов",
            tags = PERFORMANCE_FILTERS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createPerformanceFilters"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createPerformanceFilter",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createPerformanceFilter(@ApiParam(value = "login", required = true)
                                               @Nonnull @RequestParam(value = "login") String login,
                                               @ApiParam(value = "adGroupId", required = true)
                                               @Nonnull @RequestParam(value = "adGroupId") Long adGroupId,
                                               @ApiParam(value = "performanceFilterName", required = true)
                                               @Nonnull @RequestParam(value = "performanceFilterName") String performanceFilterName) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(
                    testStepsService.performanceFiltersSteps()
                            .createPerformanceFilter(login, adGroupId, performanceFilterName));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }


}
