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
 * Контроллер для степов объявлений
 */
@RestController
@RequestMapping("/ads")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для степов объявлений")
public class AdStepsController {
    private static final String FAKE_ADS_STEPS_TAG = "Fake ads steps";
    private static final String HONEST_ADS_STEPS_TAG = "Ads steps";
    private final TestStepsService testStepsService;

    @Autowired
    public AdStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }

    @POST
    @ApiOperation(
            value = "Создание дефолтного текстового объявления",
            tags = HONEST_ADS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createDefaultActiveTextAd"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createDefaultActiveTextAd",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createDefaultActiveTextAd(@ApiParam(value = "login", required = true)
                                                 @Nonnull @RequestParam(value = "login") String login,
                                                 @ApiParam(value = "campaignId", required = true)
                                                 @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                                 @ApiParam(value = "adGroupId", required = true)
                                                 @Nonnull @RequestParam(value = "adGroupId") Long adGroupId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adSteps().createDefaultActiveTextAd(login, campaignId, adGroupId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание текстового объявления",
            tags = HONEST_ADS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveTextAd"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveTextAd",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveTextAd(@ApiParam(value = "login", required = true)
                                          @Nonnull @RequestParam(value = "login") String login,
                                          @ApiParam(value = "campaignId", required = true)
                                          @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                          @ApiParam(value = "adGroupId", required = true)
                                          @Nonnull @RequestParam(value = "adGroupId") Long adGroupId,
                                          @ApiParam(value = "title", required = true)
                                          @Nonnull @RequestParam(value = "title") String title,
                                          @ApiParam(value = "titleExtension", required = true)
                                          @Nonnull @RequestParam(value = "titleExtension") String titleExtension,
                                          @ApiParam(value = "body", required = true)
                                          @Nonnull @RequestParam(value = "body") String body,
                                          @ApiParam(value = "href", required = true)
                                          @Nonnull @RequestParam(value = "href") String href) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adSteps().createActiveTextAd(login, campaignId, adGroupId, title, titleExtension, body, href));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание CPM BANNER объявления",
            tags = HONEST_ADS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveCpmBannerAd"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveCpmBannerAd",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveCpmBannerAd(@ApiParam(value = "login", required = true)
                                               @Nonnull @RequestParam(value = "login") String login,
                                               @ApiParam(value = "campaignId", required = true)
                                               @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                               @ApiParam(value = "adGroupId", required = true)
                                               @Nonnull @RequestParam(value = "adGroupId") Long adGroupId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adSteps().createActiveCpmBannerAd(login, campaignId,
                    adGroupId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание ДО объявления",
            tags = HONEST_ADS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveDynamicAd"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveDynamicAd",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveDynamicAd(@ApiParam(value = "login", required = true)
                                             @Nonnull @RequestParam(value = "login") String login,
                                             @ApiParam(value = "campaignId", required = true)
                                             @Nonnull @RequestParam(value = "campaignId") Long campaignId,
                                             @ApiParam(value = "adGroupId", required = true)
                                             @Nonnull @RequestParam(value = "adGroupId") Long adGroupId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adSteps().createActiveDynamicAd(login, campaignId,
                    adGroupId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Создание прайсового объявления",
            tags = HONEST_ADS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createActiveCpmPriceAd"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createActiveCpmPriceAd",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createActiveCpmPriceAd(@ApiParam(value = "login", required = true)
                                              @Nonnull @RequestParam(value = "login") String login,
                                              @ApiParam(value = "adGroupId", required = true)
                                              @Nonnull @RequestParam(value = "adGroupId") Long adGroupId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(testStepsService.adSteps().createActiveCpmPriceAd(login, adGroupId));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Удаление объявления.",
            tags = HONEST_ADS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "deleteAd"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "deleteAd",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse deleteAd(@ApiParam(value = "adId", required = true)
                                @Nonnull @RequestParam(value = "adId") Long adId) {
        try {
            testStepsService.adSteps().deleteAd(adId);
            return new TestStepsSuccessResponse();
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

}
