package ru.yandex.autotests.direct.cmd.steps.auth;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

public class AuthUtils {

    static Cookie getFirstCookie(CookieStore cookieStore, String cookieName) {
        for (Cookie cookie : cookieStore.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}
