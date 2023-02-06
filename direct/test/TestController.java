package ru.yandex.direct.web.entity.test;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ru.yandex.direct.common.spring.TestingComponent;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceProfile;
import ru.yandex.direct.web.annotations.AllowedOperatorRoles;
import ru.yandex.direct.web.annotations.AllowedSubjectRoles;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.model.WebSuccessResponse;
import ru.yandex.direct.web.entity.deal.controller.DealTestController;

@Api("Отладочный контроллер")
@TestingComponent
@Controller
@RequestMapping(path = "/testing", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class TestController {
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @ApiOperation(
            value = "пустой метод, с профайлингом",
            httpMethod = "GET"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Ok", response = DealTestController.DealsResponse.class)
    })
    @RequestMapping(path = "/dummy",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @AllowedSubjectRoles({RbacRole.SUPER, RbacRole.SUPERREADER, RbacRole.SUPPORT, RbacRole.PLACER,
            RbacRole.MEDIA, RbacRole.MANAGER, RbacRole.AGENCY, RbacRole.CLIENT, RbacRole.EMPTY})
    @AllowedOperatorRoles({RbacRole.SUPER, RbacRole.SUPERREADER, RbacRole.SUPPORT, RbacRole.PLACER,
            RbacRole.MEDIA, RbacRole.MANAGER, RbacRole.AGENCY, RbacRole.CLIENT, RbacRole.EMPTY})
    @ResponseBody
    public WebResponse dummy() {
        // позволяет прицепить трейс-интерсептор
        try (TraceProfile ignored = Trace.current().profile("testing:dummy", "first")) {
            logger.trace("in first profile");
        }

        try (TraceProfile ignored = Trace.current().profile("testing:dummy", "second")) {
            logger.trace("in second profile");
        }

        return new WebSuccessResponse();
    }
}
