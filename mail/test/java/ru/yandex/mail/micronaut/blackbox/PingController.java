package ru.yandex.mail.micronaut.blackbox;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

@Controller
public class PingController {
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/ping")
    public String ping() {
        return "pong";
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/auth")
    public String auth(BlackboxUid uid) {
        return "auth " + uid;
    }
}
