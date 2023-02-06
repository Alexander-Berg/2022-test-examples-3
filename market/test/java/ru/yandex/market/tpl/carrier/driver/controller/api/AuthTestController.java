package ru.yandex.market.tpl.carrier.driver.controller.api;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.AUTH_HEADER;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.AUTH_HEADER_DATA_TYPE;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.BASE_PATH;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.HEADER;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.LEGACY_BASE_PATH;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.TAXI_PARK_ID_HEADER;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.TAXI_PASSPORT_UID_HEADER;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.TAXI_PROFILE_ID_HEADER;

@Slf4j
@RestController
@Profile("!" + TplProfiles.PRODUCTION)
public class AuthTestController {

    @GetMapping(LEGACY_BASE_PATH + "/test")
    @ApiImplicitParam(name = AUTH_HEADER, paramType = HEADER, dataType = AUTH_HEADER_DATA_TYPE)
    public String testLegacy(User user) {
        log.info("Authenticated!");
        return user.toString();
    }

    @GetMapping(BASE_PATH + "/test")
    @ApiImplicitParams({
        @ApiImplicitParam(name = TAXI_PROFILE_ID_HEADER, paramType = HEADER,
                dataType = AUTH_HEADER_DATA_TYPE, required = true),
        @ApiImplicitParam(name = TAXI_PARK_ID_HEADER, paramType = HEADER,
                dataType = AUTH_HEADER_DATA_TYPE, required = true),
        @ApiImplicitParam(name = TAXI_PASSPORT_UID_HEADER, paramType = HEADER,
                dataType = AUTH_HEADER_DATA_TYPE, required = true)
    })
    public String testNew(User user) {
        log.info("Authenticated!");
        return user.toString();
    }

    @GetMapping("/test")
    @ApiImplicitParams({
            @ApiImplicitParam(name = TAXI_PROFILE_ID_HEADER, paramType = HEADER, dataType = AUTH_HEADER_DATA_TYPE),
            @ApiImplicitParam(name = TAXI_PARK_ID_HEADER, paramType = HEADER, dataType = AUTH_HEADER_DATA_TYPE),
            @ApiImplicitParam(name = TAXI_PASSPORT_UID_HEADER, paramType = HEADER, dataType = AUTH_HEADER_DATA_TYPE),
            @ApiImplicitParam(name = AUTH_HEADER, paramType = HEADER, dataType = AUTH_HEADER_DATA_TYPE)
    })
    public String testUnknown(User user) {
        log.info("Authenticated!");
        return user.toString();
    }
}
