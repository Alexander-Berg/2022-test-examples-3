package ru.yandex.direct.intapi.entity.testusers;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ru.yandex.direct.core.entity.testuser.model.TestUser;
import ru.yandex.direct.core.entity.testuser.service.TestUsersService;
import ru.yandex.direct.tvm.AllowServices;
import ru.yandex.direct.utils.JsonRpcRequest;
import ru.yandex.direct.utils.JsonRpcResponse;

import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_PROD;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;

@Controller
@RequestMapping("jsonrpc/TestUsers")
@Api("API для получения тестовых суперпользователей")
@AllowServices(production = DIRECT_SCRIPTS_PROD, testing = {DIRECT_SCRIPTS_PROD, DIRECT_SCRIPTS_TEST})
public class TestUsersController {

    private TestUsersService testUsersService;

    @Autowired
    public TestUsersController(TestUsersService testUsersService) {
        this.testUsersService = testUsersService;
    }

    @ApiOperation(
            value = "Получить список тесторых суперпользователей",
            httpMethod = "POST"
    )
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public JsonRpcResponse<List<TestUser>> getAll(@RequestBody JsonRpcRequest<?> request) {
        return new JsonRpcResponse<>(request.getId(), testUsersService.getAll());
    }
}
