package ru.yandex.market.loyalty.core.test;

import java.util.Collections;
import java.util.Date;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.service.blackbox.BlackboxClient;
import ru.yandex.market.loyalty.core.service.blackbox.UserInfoResponse;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class BlackboxUtils {

    public static void mockBlackbox(long uid, PerkType type, boolean purchased, RestTemplate blackboxRestTemplate) {
        mockBlackbox(uid, blackboxRestTemplate, mockBlackboxResponse(purchased, type));
    }

    public static void mockBlackbox(long uid, RestTemplate blackboxRestTemplate, UserInfoResponse response) {
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("uid=" + uid))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).thenReturn(ResponseEntity.ok(response));
    }

    public static void mockBlackbox(String login, PerkType type, boolean purchased, RestTemplate blackboxRestTemplate) {
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("login=" + login))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).thenReturn(ResponseEntity.ok(mockBlackboxResponse(purchased, type)));
    }

    public static void mockUserInfoResponse(RestTemplate blackboxRestTemplate) {
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("method=userinfo"))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).thenReturn(ResponseEntity.ok(new UserInfoResponse(Collections.emptyList())));
    }

    public static UserInfoResponse mockBlackboxResponse(boolean purchased, PerkType type) {
        return mockBlackboxResponse(null, Pair.of(type, purchased));
    }

    @SafeVarargs
    public static UserInfoResponse mockBlackboxResponse(Date regDate, Pair<PerkType, Boolean>... types) {
        ImmutableMap.Builder<String, String> dbfields = ImmutableMap.builder();
        ImmutableMap.Builder<String, String> attributes = ImmutableMap.builder();

        UserInfoResponse.User user = new UserInfoResponse.User();
        for (Pair<PerkType, Boolean> type : types) {
            switch (type.getKey()) {
                case YANDEX_PLUS:
                    attributes.put(BlackboxClient.YANDEX_PLUS_FLAG_NAME_MAGIC, type.getValue() ? "1" : "0");
                    break;
                case YANDEX_EMPLOYEE:
                    dbfields.put(BlackboxClient.STAFF_LOGIN, type.getValue() ? "test" : "");
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        if (regDate != null) {
            dbfields.put(BlackboxClient.REG_DATE, BlackboxClient.REG_DATE_FORMAT.format(regDate));
        }

        user.setAttributes(attributes.build());
        user.setDbfields(dbfields.build());


        return new UserInfoResponse(Collections.singletonList(user));
    }

    public static Object returnWithDelay(Object o, long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return o;
    }
}
