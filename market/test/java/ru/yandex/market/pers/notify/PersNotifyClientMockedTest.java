package ru.yandex.market.pers.notify;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.notify.model.EmailSubscriptionWriteRequest;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.sk.SecretKeyData;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatusDto;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-bean.xml")
public class PersNotifyClientMockedTest {

    private static final long UID = 12345;
    private static String EMAIL = "foo@bar.com";

    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private RestTemplate persNotifyRestTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(persNotifyRestTemplate);
    }

    @Test
    public void register() {
        MobileAppInfo info = mobileAppInfo();
        mockServer.expect(requestTo("http://localhost:35826/api/mobile/register?" + mobileAppQueryParams(info)))
            .andRespond(withStatus(HttpStatus.OK));
        assertTrue(persNotifyClient.registerMobileAppInfo(info));
    }

    @Test
    public void registerUnauthorized() {
        MobileAppInfo info = mobileAppInfoUnauthorized();
        mockServer.expect(requestTo("http://localhost:35826/api/mobile/register?" + mobileAppQueryParams(info)))
            .andRespond(withStatus(HttpStatus.OK));
        assertTrue(persNotifyClient.registerMobileAppInfo(info));
    }

    @Test
    public void unregister() {
        mockServer.expect(requestTo("http://localhost:35826/api/mobile/unregister" +
            "?uuid=uuid_1&uid=12321&appName=app_name_2"))
            .andRespond(withStatus(HttpStatus.OK));
        assertTrue(persNotifyClient.unregisterMobileAppInfo(12321L, "uuid_1", "app_name_2"));
    }

    @Test
    public void unsubscribe() throws Exception {
        mockServer.expect(requestTo("http://localhost:35826/api/subscription/type" +
            "?subscriptionType=ADVERTISING&email=valter@valter.ru"))
            .andRespond(withStatus(HttpStatus.OK));
        persNotifyClient.unsubscribeNotParametric("valter@valter.ru", NotificationType.ADVERTISING);
    }

    @Test
    public void isSubscribed() throws Exception {
        mockServer.expect(requestTo("http://localhost:35826/api/subscription/type" +
            "?subscriptionType=ADVERTISING&email=valter@valter.ru"))
            .andRespond(withSuccess("[{\"subscriptionType\":\"ADVERTISING\",\"isSubscribed\":true}]",
                MediaType.APPLICATION_JSON));
        assertEquals(Collections.singletonList(
            new EmailSubscriptionStatusDto(NotificationType.ADVERTISING, true)
        ), persNotifyClient.isSubscribed("valter@valter.ru", Collections.singletonList(NotificationType.ADVERTISING)));
    }

    @Test
    public void clientMustHandleEmptyResponse() throws Exception {
        respondToAddEventWith(HttpStatus.CREATED);
        persNotifyClient.createEvent(someEvent());
    }

    @Test
    public void clientMustThrowExceptionForServerError() throws Exception {
        assertThrows(PersNotifyClientException.class, () -> {
            respondToAddEventWith(HttpStatus.INTERNAL_SERVER_ERROR);
            persNotifyClient.createEvent(someEvent());
        });
    }

    @Test
    public void clientMustThrowExceptionForNotFound() throws Exception {
        assertThrows(PersNotifyClientException.class, () -> {
            respondToAddEventWith(HttpStatus.NOT_FOUND);
            persNotifyClient.createEvent(someEvent());
        });
    }

    @Test
    public void clientMustThrowExceptionForBadRequest() throws Exception {
        assertThrows(PersNotifyClientException.class, () -> {
            respondToAddEventWith(HttpStatus.BAD_REQUEST);
            persNotifyClient.createEvent(someEvent());
        });
    }

    @Test
    public void testGetEmails() throws Exception {
        mockServer.expect(requestTo("http://localhost:35826/api/settings/UID/" + UID + "/emails"))
                .andRespond(withSuccess("[{\"email\":\"" + EMAIL + "\",\"active\":true}]", MediaType.APPLICATION_JSON));
        Set<Email> emails = persNotifyClient.getEmails(UID);
        assertEquals(1, emails.size());
        Email email = emails.iterator().next();
        assertEquals(EMAIL, email.getEmail());
        assertTrue(email.isActive());
    }

    @Test
    public void testGenerateLink() throws Exception {
        NotificationType type = NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING;

        String action = "asb";
        String sk = "zxcb";
        mockServer.expect(requestTo("http://localhost:35826/api/verification/" +
                "?expectedEmail=" + EMAIL + "&expectedType=" + type.name() + "&expectedUid=" + UID))
                .andRespond(withSuccess("action=" + action + "&sk=" + sk, MediaType.TEXT_PLAIN));

        String link = persNotifyClient.generateLink(type, UID, EMAIL);
        assertNotNull(link);
        assertEquals("action=" + action + "&sk=" + sk, link);
    }

    @Test
    public void testActivateByLink() throws Exception {
        NotificationType type = NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING;
        String action = "asb";
        String sk = "zxcb";
        int regionId = 213;
        mockServer.expect(requestTo("http://localhost:35826/api/verification/" +
                "?action=" + action + "&sk=" + sk + "&command=unsubscribe&userAgent&userIp&regionId=" + regionId))
                .andRespond(withSuccess("{\n" +
                                "    \"email\": \"" + EMAIL + "\",\n" +
                                "    \"type\": \"" + type.name() + "\",\n" +
                                "    \"success\": true\n" +
                                "}",
                        MediaType.APPLICATION_JSON));
        SecretKeyData secretKeyData = persNotifyClient.activateByLink(null, null, action, sk, false, regionId);
        assertNotNull(secretKeyData);
        assertTrue(secretKeyData.isSuccess());
        assertEquals(EMAIL, secretKeyData.getEmail());
        assertEquals(type, secretKeyData.getType());
    }

    private EmailSubscription buildSubs(NotificationType type, Map<String, String> params) {
        final EmailSubscription emailSubscription = new EmailSubscription();
        emailSubscription.setSubscriptionType(type);
        emailSubscription.setParameters(params);
        return emailSubscription;
    }

    @Test
    public void testCreateSubscription() throws Exception {
        NotificationType type = NotificationType.QA_NEW_ANSWERS;
        String userAgent = "Mozilla/5.0 (Linux; Android 5.1; SANTIN #Candy U7 Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Mobile Safari/537.36";
        String expectedUserAgent = "Mozilla/5.0%2520(Linux;%2520Android%25205.1;%2520SANTIN%2520%2523Candy%2520U7%2520Build/LMY47D)%2520AppleWebKit/537.36%2520(KHTML,%2520like%2520Gecko)%2520Chrome/69.0.3497.100%2520Mobile%2520Safari/537.36";
        String ip = "85.93.59.19";
        String email = "email@email.ru";
        mockServer.expect(requestTo(String.format("http://localhost:35826/api/subscription/?uid=%s&userIp=%s" +
            "&userAgent=%s&email=%s", UID, ip, expectedUserAgent, email)))
            .andRespond(withStatus(HttpStatus.CREATED));
        final EmailSubscriptionWriteRequest request = new EmailSubscriptionWriteRequest();
        request.setUid(UID);
        request.setEmail(email);
        request.setUserAgent(userAgent);
        request.setUserIp(ip);

        request.setEmailSubscriptions(Collections.singletonList(buildSubs(type, Collections.singletonMap("question_id", "1"))));
        persNotifyClient.createSubscriptions(request);
    }

    private void respondToAddEventWith(HttpStatus status) {
        mockServer.expect(requestTo("http://localhost:35826/api/event/add")).andRespond(withStatus(status));
    }

    private static NotificationEventSource someEvent() {
        return NotificationEventSource.fromUid(12345L, NotificationSubtype.GRADE_MODEL_AFTER_ORDER).build();
    }

    private String mobileAppQueryParams(MobileAppInfo info) {
        StringBuilder result = new StringBuilder(String.format("" +
            "uuid=%s&push_token=%s&platform=%s",
            info.getUuid(), info.getPushToken(), info.getPlatform()));
        if (info.getUid() != null) {
            result.append("&uid=").append(info.getUid());
        }
        if (info.getLoginTime() != null) {
            result.append("&login_time=").append(info.getLoginTime().getTime());
        }
        if (info.getGeoId() != null) {
            result.append("&geo_id=").append(info.getGeoId());
        }

        if (info.getAppName() != null) {
            result.append("&appName=").append(info.getAppName());
        }
        return result.toString();
    }

    private MobileAppInfo mobileAppInfo() {
        return new MobileAppInfo(123L, "uuid_1", "app_name_2",
            "yandex_uid_3", "push_token_4", MobilePlatform.IPHONE, false,
            new Date(), 213L, 124L);
    }

    private MobileAppInfo mobileAppInfoUnauthorized() {
        return new MobileAppInfo(null, "uuid_1", "app_name_2",
            "yandex_uid_3", "push_token_4", MobilePlatform.IPHONE, false,
            new Date(), 213L, 124L);
    }
}
