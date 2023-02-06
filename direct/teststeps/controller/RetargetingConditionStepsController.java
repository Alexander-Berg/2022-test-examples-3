package ru.yandex.direct.teststeps.controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.teststeps.controller.model.TestStepsIdAndNameResponse;
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
@RequestMapping("/retargetingConditions")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для условий ретаргетингов")
public class RetargetingConditionStepsController {

    private static final String RETARGETING_CONDITIONS_STEPS_TAG = "Retargeting conditions steps";
    private final TestStepsService testStepsService;

    @Autowired
    public RetargetingConditionStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }


    @POST
    @ApiOperation(
            value = "Создание условия ретагетинга без правил.",
            tags = RETARGETING_CONDITIONS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createRetargetingConditionWithNoRules"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdAndNameResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createRetargetingConditionWithNoRules",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createRetargetingConditionWithNoRules(
            @ApiParam(value = "login", required = true)
            @Nonnull @RequestParam(value = "login") String login,
            @ApiParam(value = "conditionType")
            @RequestParam(value = "conditionType") ConditionType conditionType) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            RetConditionInfo response = testStepsService.retargetingConditionSteps()
                    .createRetargetingConditionWithNoRules(login, conditionType);

            return new TestStepsIdAndNameResponse(response.getRetConditionId(), response.getRetConditionName());
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание условия ретагетинга с одним правилом в котором одна цель.",
            tags = RETARGETING_CONDITIONS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createRetargetingConditionWithSingleGoal"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdAndNameResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createRetargetingConditionWithSingleGoal",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createRetargetingConditionWithSingleGoal(
            @ApiParam(value = "login", required = true)
            @Nonnull @RequestParam(value = "login") String login,
            @ApiParam(value = "conditionType")
            @RequestParam(value = "conditionType") ConditionType conditionType,
            @ApiParam(value = "goalId", required = true)
            @Nonnull @RequestParam(value = "goalId") Long goalId,
            @ApiParam(value = "goalType")
            @RequestParam(value = "goalType") GoalType goalType,
            @ApiParam(value = "period")
            @Nullable @RequestParam(value = "period") Integer period) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            RetConditionInfo response = testStepsService.retargetingConditionSteps()
                    .createRetargetingConditionWithSingleGoal(login, conditionType, RuleType.OR, goalId, goalType, period);

            return new TestStepsIdAndNameResponse(response.getRetConditionId(), response.getRetConditionName());
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Удаление условия ретагетинга.",
            tags = RETARGETING_CONDITIONS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "deleteRetargetingCondition"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "deleteRetargetingCondition",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse deleteRetargetingCondition(
            @ApiParam(value = "login", required = true)
            @Nonnull @RequestParam(value = "login") String login,
            @ApiParam(value = "retConditionId", required = true)
            @Nonnull @RequestParam(value = "retConditionId") Long retConditionId
    ) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.retargetingConditionSteps()
                    .deleteRetargetingCondition(login, retConditionId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }
}
