package ru.yandex.direct.teststeps.controller;

import java.util.List;

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
 * Контроллер для степов ключевых фраз
 */
@RestController
@RequestMapping("/minuswords")
@AllowedSubjectRoles({SUPER, SUPERREADER, SUPPORT, PLACER, MEDIA, MANAGER, AGENCY,
        INTERNAL_AD_ADMIN, INTERNAL_AD_MANAGER, INTERNAL_AD_SUPERREADER, CLIENT})
@Api(value = "Контроллер для степов минус фраз")
public class MinusPhrasesPackStepsController {

    private final TestStepsService testStepsService;
    private static final String MINUS_WORDS_STEPS_TAG = "Minus words steps";

    @Autowired
    public MinusPhrasesPackStepsController(TestStepsService testStepsService) {
        this.testStepsService = testStepsService;
    }

    @POST
    @ApiOperation(
            value = "Создание пакета библиотечных минус фраз для клиента.",
            tags = MINUS_WORDS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "createLibraryMinusWordsPack"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsIdResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "createLibraryMinusWordsPack",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse createLibraryMinusWordsPack(@ApiParam(value = "login", required = true)
                                                   @Nonnull @RequestParam(value = "login") String login,
                                                   @ApiParam(value = "minusWords", required = true)
                                                   @Nonnull @RequestParam(value = "minusWords") List<String> minusWords,
                                                   @ApiParam(value = "packName", required = true)
                                                   @Nonnull @RequestParam(value = "packName") String packName) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            return new TestStepsIdResponse(
                    testStepsService.minusPhrasesPackService().createLibraryMinusWordsPack(login, minusWords,
                            packName));
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Удаление пакета минус фраз.",
            tags = MINUS_WORDS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "deleteMinusWordsPack"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "deleteMinusWordsPack",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse deleteMinusWordsPack(@ApiParam(value = "login", required = true)
                                            @Nonnull @RequestParam(value = "login") String login,
                                            @ApiParam(value = "id", required = true)
                                            @Nonnull @RequestParam(value = "id") Long id) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {

            testStepsService.minusPhrasesPackService().deleteMinusWordsPack(login, id);
            return new TestStepsSuccessResponse();
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }

    @POST
    @ApiOperation(
            value = "Связать библиотечный пакет минус фраз с группой.",
            tags = MINUS_WORDS_STEPS_TAG,
            httpMethod = "POST",
            nickname = "linkMinusWordsPackToAdGroup"
    )
    @ApiResponses(
            {
                    @ApiResponse(code = 200, message = "Ok", response = TestStepsSuccessResponse.class),
                    @ApiResponse(code = 500, message = "Error", response = WebErrorResponse.class)
            }
    )
    @AllowNetworks({INTERNAL})
    @PostMapping(
            path = "linkMinusWordsPackToAdGroup",
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON
    )
    @ResponseBody
    public WebResponse linkMinusWordsPackToAdGroup(@ApiParam(value = "login", required = true)
                                                   @Nonnull @RequestParam(value = "login") String login,
                                                   @ApiParam(value = "id", required = true)
                                                   @Nonnull @RequestParam(value = "id") Long id,
                                                   @ApiParam(value = "adGroupId", required = true)
                                                   @Nonnull @RequestParam(value = "adGroupId") Long adGroupId) {
        if (Strings.isBlank(login)) {
            return new WebErrorResponse(500, "'login' can't be empty'");
        }
        try {
            testStepsService.minusPhrasesPackService().linkLibraryMinusKeywordPackToAdGroup(login, id, adGroupId);
            return new TestStepsSuccessResponse();
        } catch (IllegalArgumentException e) {
            return new WebErrorResponse(500, e.getMessage());
        }
    }
}
