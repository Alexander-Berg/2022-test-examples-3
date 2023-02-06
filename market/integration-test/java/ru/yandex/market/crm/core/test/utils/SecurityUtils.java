package ru.yandex.market.crm.core.test.utils;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import ru.yandex.market.crm.http.security.BlackboxAuthentication;
import ru.yandex.market.crm.http.security.BlackboxProfile;

/**
 * @author apershukov
 */
public class SecurityUtils {

    public static BlackboxProfile profile(String login) {
        return profile(login, RandomUtils.nextLong(0, 100_000));
    }

    public static BlackboxProfile profile(String login, long uid) {
        return new BlackboxProfile(
                login,
                "Vasiliy Pupkin",
                uid,
                "iddqd",
                "market.yandex.ru",
                "",
                ""
        );
    }

    public static BlackboxProfile adminProfile() {
        // TODO Костыль. Убрать после ликвидации аналогичного костыля в UserPermissionsService.
        return profile("admin", 1120000000133262L);
    }

    public static void setAuthentication(BlackboxProfile profile) {
        BlackboxAuthentication authentication = new BlackboxAuthentication(profile);
        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
