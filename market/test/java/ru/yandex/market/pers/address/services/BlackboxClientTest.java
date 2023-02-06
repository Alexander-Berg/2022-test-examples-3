package ru.yandex.market.pers.address.services;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.pers.address.factories.BlackboxUserFactory;
import ru.yandex.market.pers.address.services.blackbox.BlackboxClient;
import ru.yandex.market.pers.address.services.blackbox.UserFromBlackbox;
import ru.yandex.market.pers.address.services.blackbox.UserInfoResponse;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;

import java.net.URI;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

class BlackboxClientTest {
    private static final String BLACKBOX_URL = "https://pass-test.yandex.ru/blackbox";
    private static final long UID = 1000537205013L;
    private static final long SBER_ID = (1L << 61) - 1L;

    private BlackboxClient marketDataSyncClient;
    private RestTemplate restTemplateMock;
    private UserInfoService userInfoService;

    @BeforeEach
    void init() {
        restTemplateMock = mock(RestTemplate.class);
        userInfoService = mock(UserInfoService.class);
        marketDataSyncClient = new BlackboxClient(BLACKBOX_URL, restTemplateMock, userInfoService);
        mockUid();
    }

    @Test
    void shouldFetchName() {
        String lastName = "Иванов";
        String firstName = "Иван";
        mockResponse(BlackboxUserFactory.builder()
            .setLastName(lastName)
            .setFirstName(firstName)
            .build());

        UserFromBlackbox userFromBlackbox = marketDataSyncClient.userInfo(UID);
        assertEquals(lastName, userFromBlackbox.getLastName());
        assertEquals(firstName, userFromBlackbox.getFirstName());
    }

    @Test
    void shouldFetchPhone() {
        String phone = "+79651796146";
        mockResponse(BlackboxUserFactory.builder()
            .setPhone(phone)
            .build());

        UserFromBlackbox userFromBlackbox = marketDataSyncClient.userInfo(UID);
        assertEquals(phone, userFromBlackbox.getPhone());
    }

    @Test
    void shouldFetchEmailAddresses() {
        String yandexEmail = "user@yandex.ru";
        String anotherEmail = "user@rambler.ru";
        mockResponse(BlackboxUserFactory.builder()
            .addEmail(yandexEmail)
            .addEmail(anotherEmail)
            .build());

        UserFromBlackbox userFromBlackbox = marketDataSyncClient.userInfo(UID);
        assertThat(userFromBlackbox.getEmails(), containsInAnyOrder(
            yandexEmail, anotherEmail
        ));
    }

    @Test
    void shouldSquashYandexEmails() {
        String defaultYandexEmail = "user@yandex.ru";
        String shortYandexEmail = "user@ya.ru";
        String comYandexEmail = "user@yandex.com";
        String kzYandexEmail = "user@yandex.kz";
        mockResponse(BlackboxUserFactory.builder()
            .addEmail(defaultYandexEmail, true)
            .addEmail(shortYandexEmail, false)
            .addEmail(comYandexEmail, false)
            .addEmail(kzYandexEmail, false)
            .build());

        UserFromBlackbox userFromBlackbox = marketDataSyncClient.userInfo(UID);
        assertThat(userFromBlackbox.getEmails(), Matchers.<Collection<String>>allOf(
            contains(defaultYandexEmail),
            hasSize(1)
        ));
    }

    @Test
    void shouldReturnEmptyObjectForSberId() {
        UserFromBlackbox user = marketDataSyncClient.userInfo(SBER_ID);
        assertNotNull(user);
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getPhone());
        assertNotNull(user.getEmails());
        assertEquals(0, user.getEmails().size());
    }

    private void mockResponse(UserInfoResponse userInfo) {
        when(restTemplateMock.getForObject(urlMatcher(UID), eq(UserInfoResponse.class)))
            .thenReturn(userInfo);
    }

    private void mockUid() {
        when(userInfoService.resolve(UID))
                .thenReturn(Uid.ofPassport(UID));
        when(userInfoService.resolve(SBER_ID))
                .thenReturn(Uid.ofSberlog(UID));
    }

    private static URI urlMatcher(long uid) {
        return argThat(hasProperty("query", allOf(
            containsString("uid=" + uid),
            containsString("emails=getall"),
            containsString("attributes=" + BlackboxClient.ALL_FLAGS)
        )));
    }
}
