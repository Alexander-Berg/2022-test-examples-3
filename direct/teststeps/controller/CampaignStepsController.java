package ru.yandex.direct.teststeps.controller;

import java.math.BigDecimal;
import java.util.List;

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

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsDayBudgetShowMode;
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
 * Контроллер для степов кампаний
 */
@RestController
@RequestMapping("/campaigns")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для степов кампаний")
public class CampaignStepsController {
    private static final String FAKE_CAMPAIGNS_STEPS_TAG = "Fake campaigns steps";
    private static final String HONEST_CAMPAIGNS_STEPS_TAG = "Campaigns steps";
    private final TestStepsService testStepsService;

    @Autowired
    public CampaignStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }

    @POST
    @ApiOperation(
            value = "Создание дефолтной текстовой кампании.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createDefaultTextCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createDefaultTextCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createDefaultTextCampaign(@ApiParam(value = "login", required = true)
                                                 @Nonnull @RequestParam(value = "login") String login,
                                                 @ApiParam(value = "name")
                                                 @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.campaignSteps().createDefaultTextCampaign(login, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание дефолтной CPM BANNER кампании.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createDefaultCpmBannerCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createDefaultCpmBannerCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createDefaultCpmBannerCampaign(@ApiParam(value = "login", required = true)
                                                      @Nonnull @RequestParam(value = "login") String login,
                                                      @ApiParam(value = "name")
                                                      @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(
                    testStepsService.campaignSteps().createDefaultCpmBannerCampaign(login, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание дефолтной smart кампании.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createDefaultSmartCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createDefaultSmartCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createDefaultSmartCampaign(@ApiParam(value = "login", required = true)
                                                  @Nonnull @RequestParam(value = "login") String login,
                                                  @ApiParam(value = "name")
                                                  @Nullable @RequestParam(value = "name") String name,
                                                  @ApiParam(value = "metrikaCounters")
                                                  @Nullable @RequestParam(value = "metrikaCounters") List<Long> metrikaCounters) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(
                    testStepsService.campaignSteps().createDefaultSmartCampaign(login, name, metrikaCounters));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание дефолтной мобильной кампании.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createDefaultMobileContentCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createDefaultMobileContentCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createDefaultMobileContentCampaign(@ApiParam(value = "login", required = true)
                                                          @Nonnull @RequestParam(value = "login") String login,
                                                          @ApiParam(value = "name")
                                                          @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(
                    testStepsService.campaignSteps().createDefaultMobileContentCampaign(login, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание дефолтной ДО кампании.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createDefaultDynamicCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createDefaultDynamicCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createDefaultDynamicCampaign(@ApiParam(value = "login", required = true)
                                                    @Nonnull @RequestParam(value = "login") String login,
                                                    @ApiParam(value = "name")
                                                    @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.campaignSteps().createDefaultDynamicCampaign(login, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание прайсовой кампании (для заданного пакета).",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createCpmPriceCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createCpmPriceCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createCpmPriceCampaign(@ApiParam(value = "login", required = true)
                                              @Nonnull @RequestParam(value = "login") String login,
                                              @ApiParam(value = "packageId", required = true)
                                              @Nonnull @RequestParam(value = "packageId") Long packageId,
                                              @ApiParam(value = "name")
                                              @Nullable @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.campaignSteps().createCpmPriceCampaign(login,
                    packageId, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание бесплатной кампании внутренней рекламы.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createFreeInternalCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createFreeInternalCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createFreeInternalCampaign(@ApiParam(value = "login", required = true)
                                                  @Nonnull @RequestParam(value = "login") String login,
                                                  @ApiParam(value = "placeId", required = true)
                                                  @Nonnull @RequestParam(value = "placeId") Long placeId,
                                                  @ApiParam(value = "isMobile", required = true)
                                                  @Nonnull @RequestParam(value = "isMobile") Boolean isMobile,
                                                  @ApiParam(value = "name", required = true)
                                                  @Nonnull @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.campaignSteps().createFreeInternalCampaign(login,
                    placeId, isMobile, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание дистрибуционной кампании внутренней рекламы.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createDistribInternalCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createDistribInternalCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createDistribInternalCampaign(@ApiParam(value = "login", required = true)
                                                     @Nonnull @RequestParam(value = "login") String login,
                                                     @ApiParam(value = "placeId", required = true)
                                                     @Nonnull @RequestParam(value = "placeId") Long placeId,
                                                     @ApiParam(value = "isMobile", required = true)
                                                     @Nonnull @RequestParam(value = "isMobile") Boolean isMobile,
                                                     @ApiParam(value = "rotationGoalId", required = true)
                                                     @Nonnull @RequestParam(value = "rotationGoalId") Long rotationGoalId,
                                                     @ApiParam(value = "name", required = true)
                                                     @Nonnull @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.campaignSteps().createDistribInternalCampaign(login,
                    placeId, isMobile, rotationGoalId, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание автобюджетной кампании внутренней рекламы.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createAutobudgetInternalCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createAutobudgetInternalCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createAutobudgetInternalCampaign(@ApiParam(value = "login", required = true)
                                                        @Nonnull @RequestParam(value = "login") String login,
                                                        @ApiParam(value = "placeId", required = true)
                                                        @Nonnull @RequestParam(value = "placeId") Long placeId,
                                                        @ApiParam(value = "isMobile", required = true)
                                                        @Nonnull @RequestParam(value = "isMobile") Boolean isMobile,
                                                        @ApiParam(value = "name", required = true)
                                                        @Nonnull @RequestParam(value = "name") String name) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.campaignSteps().createAutobudgetInternalCampaign(login,
                    placeId, isMobile, name));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Удалить кампанию.",
            tags = HONEST_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "deleteCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "deleteCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse deleteCampaign(@ApiParam(value = "campaignId", required = true)
                                      @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        try {
            testStepsService.campaignSteps().deleteCampaign(campaignId);
            return new TestStepsSuccessResponse();
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Выставить статус модерации кампании.",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "setStatusModerate"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "setStatusModerate",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse setStatusModerate(@ApiParam(value = "login", required = true)
                                         @Nonnull @RequestParam(value = "login") String login,
                                         @ApiParam(value = "campaignId", required = true)
                                         @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                         @ApiParam(value = "status", required = true)
                                         @Nonnull @RequestParam(value = "status") CampaignStatusModerate status) {
        testStepsService.campaignSteps().setStatusModerate(campaignId, status);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Выставить статус синхронизации кампании с БК.",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "setStatusBsSynced"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "setStatusBsSynced",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse setStatusBsSynced(@ApiParam(value = "login", required = true)
                                         @Nonnull @RequestParam(value = "login") String login,
                                         @ApiParam(value = "campaignId", required = true)
                                         @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                         @ApiParam(value = "status", required = true)
                                         @Nonnull @RequestParam(value = "status") CampaignStatusBsSynced status) {
        testStepsService.campaignSteps().setStatusBsSynced(campaignId, status);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию архивной",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "archive"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "archive",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse archive(@ApiParam(value = "login", required = true)
                               @Nonnull @RequestParam(value = "login") String login,
                               @ApiParam(value = "campaignId", required = true)
                               @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().archiveCampaign(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию не архивной",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "unarchive"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "unarchive",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse unarchive(@ApiParam(value = "login", required = true)
                                 @Nonnull @RequestParam(value = "login") String login,
                                 @ApiParam(value = "campaignId", required = true)
                                 @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().unarchiveCampaign(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию активной",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "makeCampaignActive"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "makeCampaignActive",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse makeCampaignActive(@ApiParam(value = "login", required = true)
                                          @Nonnull @RequestParam(value = "login") String login,
                                          @ApiParam(value = "campaignId", required = true)
                                          @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().makeCampaignActive(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию готовой к отправке в БК",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "makeNewCampaignReadyForSendingToBS"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "makeNewCampaignReadyForSendingToBS",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse makeNewCampaignReadyForSendingToBS(@ApiParam(value = "login", required = true)
                                                          @Nonnull @RequestParam(value = "login") String login,
                                                          @ApiParam(value = "campaignId", required = true)
                                                          @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().makeNewCampaignReadyForSendingToBS(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию готовой к удалению",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "makeCampaignReadyForDelete"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "makeCampaignReadyForDelete",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse makeCampaignReadyForDelete(@ApiParam(value = "login", required = true)
                                                  @Nonnull @RequestParam(value = "login") String login,
                                                  @ApiParam(value = "campaignId", required = true)
                                                  @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().makeCampaignReadyForDelete(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию готовой к удалению в Груте",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "makeCampaignReadyForDeleteInGrut"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "makeCampaignReadyForDeleteInGrut",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse makeCampaignReadyForDeleteInGrut(@ApiParam(value = "login", required = true)
                                                  @Nonnull @RequestParam(value = "login") String login,
                                                  @ApiParam(value = "campaignId", required = true)
                                                  @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().makeCampaignReadyForDeleteInGrut(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию полностью промодерированной",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "makeCampaignFullyModerated"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "makeCampaignFullyModerated",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse makeCampaignFullyModerated(@ApiParam(value = "login", required = true)
                                                  @Nonnull @RequestParam(value = "login") String login,
                                                  @ApiParam(value = "campaignId", required = true)
                                                  @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().makeCampaignFullyModerated(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Сделать кампанию остановленной",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "suspendCampaign"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "suspendCampaign",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse suspendCampaign(@ApiParam(value = "login", required = true)
                                       @Nonnull @RequestParam(value = "login") String login,
                                       @ApiParam(value = "campaignId", required = true)
                                       @Nonnull @RequestParam(value = "campaignId") Long campaignId) {
        testStepsService.campaignSteps().suspendCampaign(campaignId);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Выставить кампании стратегию",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "setStrategy"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "setStrategy",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse setStrategy(@ApiParam(value = "login", required = true)
                                   @Nonnull @RequestParam(value = "login") String login,
                                   @ApiParam(value = "campaignId", required = true)
                                   @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                   @ApiParam(value = "strategyName", required = true)
                                   @Nonnull @RequestParam(value = "strategyName") StrategyName strategyName) {
        testStepsService.campaignSteps().setStrategy(campaignId, strategyName);
        return new TestStepsSuccessResponse();
    }

    @POST
    @ApiOperation(
            value = "Выставить дневной бюджет(количество выставлений )",
            tags = FAKE_CAMPAIGNS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "setDayBudget"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "setDayBudget",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody

    public WebResponse setDayBudget(@ApiParam(value = "login", required = true)
                                    @Nonnull @RequestParam(value = "login") String login,
                                    @ApiParam(value = "campaignId", required = true)
                                    @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                    @ApiParam(value = "sum", required = true)
                                    @Nonnull @RequestParam(value = "sum") BigDecimal sum,
                                    @ApiParam(value = "showMode")
                                    @RequestParam(value = "showMode") CampaignsDayBudgetShowMode showMode,
                                    @ApiParam(value = "changesCount")
                                    @RequestParam(value = "changesCount") Integer changesCount) {
        testStepsService.campaignSteps().setDayBudget(campaignId, sum, showMode, changesCount);
        return new TestStepsSuccessResponse();
    }

}
