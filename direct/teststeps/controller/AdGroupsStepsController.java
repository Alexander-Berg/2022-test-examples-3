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

/**
 * Контроллер для степов групп
 */
@RestController
@RequestMapping("/adgroups")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для степов групп")
public class AdGroupsStepsController {

    private static final String HONEST_ADGROUPS_STEPS_TAG = "AdGroups steps";
    private final TestStepsService testStepsService;

    @Autowired
    public AdGroupsStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }

    @POST
    @ApiOperation(
            value = "Создание текстовой группы.",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveTextAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveTextAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveTextAdGroup(@ApiParam(value = "login", required = true)
                                               @Nonnull @RequestParam(value = "login") String login,
                                               @ApiParam(value = "campaignId", required = true)
                                               @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adGroupSteps().createActiveTextAdGroup(login, campaignId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание CPM BANNER группы.",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveCpmBannerAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveCpmBannerAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveCpmBannerAdGroup(@ApiParam(value = "login", required = true)
                                                    @Nonnull @RequestParam(value = "login") String login,
                                                    @ApiParam(value = "campaignId", required = true)
                                                    @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adGroupSteps().createActiveCpmBannerAdGroup(login,
                    campaignId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание ДО группы по фиду (с дефолтным фидом).",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveDynamicFeedAdGroupWithDefaultFeed"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveDynamicFeedAdGroupWithDefaultFeed",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveDynamicFeedAdGroupWithDefaultFeed(
            @ApiParam(value = "login", required = true)
            @Nonnull @RequestParam(value = "login") String login,
            @ApiParam(value = "campaignId", required = true)
            @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adGroupSteps().createActiveDynamicFeedAdGroup(login,
                    campaignId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание ДО группы по фиду (с указанным фидом).",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveDynamicFeedAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveDynamicFeedAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveDynamicFeedAdGroup(@ApiParam(value = "login", required = true)
                                                      @Nonnull @RequestParam(value = "login") String login,
                                                      @ApiParam(value = "campaignId", required = true)
                                                      @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                                      @ApiParam(value = "feedId", required = true)
                                                      @Nonnull @RequestParam(value = "feedId") Long feedId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adGroupSteps().createActiveDynamicFeedAdGroup(login,
                    campaignId, feedId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание ДО группы.",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveDynamicTextAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveDynamicTextAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveDynamicTextAdGroup(@ApiParam(value = "login", required = true)
                                                      @Nonnull @RequestParam(value = "login") String login,
                                                      @ApiParam(value = "campaignId", required = true)
                                                      @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adGroupSteps().createActiveDynamicTextAdGroup(login,
                    campaignId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание прайсовой группы.",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveCpmPriceAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveCpmPriceAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveCpmPriceAdGroup(
            @ApiParam(value = "login", required = true)
            @Nonnull @RequestParam(value = "login") String login,
            @ApiParam(value = "campaignId", required = true)
            @Nonnull @RequestParam(value = "campaignId") Long campaignId,
            @ApiParam(value = "isDefaultGroup", required = true)
            @Nonnull @RequestParam(value = "isDefaultGroup") Boolean isDefaultGroup,
            @ApiParam(value = "name")
            @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            Long adGroupId = testStepsService.adGroupSteps().createActiveCpmPriceAdGroup(login, campaignId,
                    isDefaultGroup);
            testStepsService.adGroupSteps().updateAdGroupName(adGroupId, name);
            return new TestStepsIdResponse(adGroupId);
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание группы внутренней рекламы.",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createInternalAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createInternalAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createInternalAdGroup(
            @ApiParam(value = "login", required = true)
            @Nonnull @RequestParam(value = "login") String login,
            @ApiParam(value = "campaignId", required = true)
            @Nonnull @RequestParam(value = "campaignId") Long campaignId,
            @ApiParam(value = "name")
            @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            Long adGroupId = testStepsService.adGroupSteps().createInternalAdGroup(login, campaignId, name);
            return new TestStepsIdResponse(adGroupId);
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание группы для рекламы мобильных приложений.",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createMobileContentAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createMobileContentAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createMobileContentAdGroup(
            @ApiParam(value = "login", required = true)
            @Nonnull @RequestParam(value = "login") String login,
            @ApiParam(value = "campaignId", required = true)
            @Nonnull @RequestParam(value = "campaignId") Long campaignId,
            @ApiParam(value = "name")
            @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            Long adGroupId = testStepsService.adGroupSteps().createMobileContentAdGroup(login, campaignId, name);
            return new TestStepsIdResponse(adGroupId);
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Удаление группы.",
            tags = HONEST_ADGROUPS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "deleteAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "deleteAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse deleteAdGroup(@ApiParam(value = "login", required = true)
                                     @Nonnull @RequestParam(value = "login") String login,
                                     @ApiParam(value = "adGroupId", required = true)
                                     @Nonnull @RequestParam(value = "adGroupId") Long adGroupId) {
        try {
            testStepsService.adGroupSteps().deleteAdgroups(login, adGroupId);
            return new TestStepsSuccessResponse();
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

}
