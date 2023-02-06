package ru.yandex.market.pers.notify.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.pers.notify.MobileAppInfoUtil;
import ru.yandex.market.pers.notify.api.controller.dto.mobile.MobileRegistrationRequest;
import ru.yandex.market.pers.notify.assertions.SubscriptionAssertions;
import ru.yandex.market.pers.notify.json.JsonSerializer;
import ru.yandex.market.pers.notify.model.Market;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus;
import ru.yandex.market.pers.notify.push.MobileAppInfoDAO;
import ru.yandex.market.pers.notify.subscription.SubscriptionService;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.notify.api.controller.MobileController.SUPPORTED_PLATFORMS;
import static ru.yandex.market.pers.notify.subscription.MobileAppSubscriptionsMigrator.DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES;
import static ru.yandex.market.pers.notify.subscription.MobileAppSubscriptionsMigrator.DEFAULT_PUSH_TYPES;

/**
 * @author ukhuvrus
 */
class MobileControllerTest extends MarketUtilsMockedDbTest {

    private static final Set<NotificationType> DEFAULT_TRANSACTIONAL_PUSH_TYPES
            = Set.of(NotificationType.STORE_PUSH_ORDER_STATUS);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionAssertions subscriptionAssertions;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonSerializer.mapper();

    @Test
    void testInvalidUnregister() throws Exception {
        mockMvc.perform(post("/mobile/unregister"))
            .andDo(print()).andExpect(status().isBadRequest())
            .andExpect(content().json(
                toJson(new Error("INVALID_FORMAT", "Required String parameter 'uuid' is not present", 400)))
            );
    }

    @Test
    void testUnregisterWithBadAppName() throws Exception {
        mockMvc.perform(post("/mobile/unregister")
            .param("uuid", "someUuid")
            .param("appName", "some_unsupported_app_name")
        ).andDo(print()).andExpect(status().isBadRequest())
        .andExpect(content().json(toJson(
            new Error("Bad Request", "Unsupported app name: some_unsupported_app_name", 400))));
    }

    @Test
    void testRegisterWithAppName() throws Exception {
        String appName = "none";
        mockMvc.perform(post("/mobile/register")
            .param("uuid", "someUuid")
            .param("push_token", "somePushToken")
            .param("platform", MobilePlatform.IPHONE.name())
            .param("muid", "1")
            .param("yandexUid", "someYandexUid")
            .param("uid", "2")
            .param("appName", appName)
        )
            .andDo(print()).andExpect(status().isOk());

        MobileAppInfo info = mobileAppInfoDAO.getByUuid("someUuid");
        assertEquals(appName, info.getAppName());

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testRegisterWithBlueAppName() throws Exception {
        String appName = MobileAppInfoUtil.BLUE_AND_APPNAME;
        mockMvc.perform(post("/mobile/register")
                        .param("uuid", "someUuid")
                        .param("push_token", "somePushToken")
                        .param("platform", MobilePlatform.IPHONE.name())
                        .param("appName", appName)
                )
                .andDo(print()).andExpect(status().isOk());

        MobileAppInfo info = mobileAppInfoDAO.getByUuid("someUuid");
        assertEquals(appName, info.getAppName());

        subscriptionAssertions.assertSubscriptions(DEFAULT_PUSH_TYPES,"someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
    }

    @Test
    void testRegisterWithBadAppName() throws Exception {
        String appName = "some_unsupported_name";
        mockMvc.perform(post("/mobile/register")
            .param("uuid", "someUuid")
            .param("push_token", "somePushToken")
            .param("platform", MobilePlatform.IPHONE.name())
            .param("muid", "1")
            .param("yandexUid", "someYandexUid")
            .param("uid", "2")
            .param("appName", appName)
        )
            .andDo(print()).andExpect(status().isBadRequest())
            .andExpect(content().json(toJson(
                new Error("Bad Request", "Unsupported app name: some_unsupported_name", 400))));
    }

    @Test
    void testRegisterWithDefaultAppName() throws Exception {
        mockMvc.perform(post("/mobile/register")
            .param("uuid", "someUuid")
            .param("push_token", "somePushToken")
            .param("platform", MobilePlatform.IPHONE.name())
            .param("muid", "1")
            .param("yandexUid", "someYandexUid")
            .param("uid", "2")
        )
            .andDo(print()).andExpect(status().isOk());

        MobileAppInfo info = mobileAppInfoDAO.getByUuid("someUuid");
        assertNotNull(info.getAppName());

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testEmptyParam() throws Exception {
        mockMvc.perform(post("/mobile/register")
            .param("uuid", "someUuid")
            .param("push_token", "somePushToken")
            .param("platform", MobilePlatform.IPHONE.name())
            .param("muid", "")
        )
            .andDo(print()).andExpect(status().isOk());


        MobileAppInfo info = mobileAppInfoDAO.getByUuid("someUuid");
        assertNull(info.getMuid());
        assertEquals("someUuid", info.getUuid());

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testRegister() throws Exception {
        mockMvc.perform(post("/mobile/register")
            .param("uuid", "someUuid")
            .param("push_token", "somePushToken")
            .param("platform", MobilePlatform.IPHONE.name())
            .param("muid", "1")
            .param("yandexUid", "someYandexUid")
            .param("uid", "2")
        )
            .andDo(print()).andExpect(status().isOk());


        MobileAppInfo info = mobileAppInfoDAO.getByUuid("someUuid");

        assertEquals(mobileAppInfoDAO.getByUuid("someUuid"), mobileAppInfoDAO.getByYandexUid("someYandexUid"));
        assertEquals(mobileAppInfoDAO.getByUuid("someUuid"), mobileAppInfoDAO.getByMuid(1L));
        assertEquals(mobileAppInfoDAO.getByUuid("someUuid"), mobileAppInfoDAO.getByUid(2L).get(0));

        assertEquals("someUuid", info.getUuid());
        assertEquals("someYandexUid", info.getYandexUid());
        assertEquals(Long.valueOf(1), info.getMuid());
        assertEquals(Long.valueOf(2), info.getUid());
        assertEquals("somePushToken", info.getPushToken());
        assertEquals("IPHONE", MobilePlatform.IPHONE.name());

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testUnregister() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(null, "someUuid", "none", "somePushToken", MobilePlatform.ANDROID, false));

        mockMvc.perform(post("/mobile/unregister")
                        .param("uuid", "someUuid")
                )
                .andDo(print()).andExpect(status().isOk());

        MobileAppInfo info = mobileAppInfoDAO.getByUuid("someUuid");
        assertTrue(info.isUnregistered());

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testBlueUnregister() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));

        mockMvc.perform(post("/mobile/unregister")
                        .param("uuid", "someUuid")
                )
                .andDo(print()).andExpect(status().isOk());

        MobileAppInfo info = mobileAppInfoDAO.getByUuid("someUuid");
        assertTrue(info.isUnregistered());

        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId()))
        ));
    }

    @Test
    void testBlueRegisterUnregister() throws Exception {
        String appName = MobileAppInfoUtil.BLUE_AND_APPNAME;
        mockMvc.perform(post("/mobile/register")
                        .param("uuid", "someUuid")
                        .param("push_token", "somePushToken")
                        .param("platform", MobilePlatform.IPHONE.name())
                        .param("appName", appName)
                )
                .andDo(print()).andExpect(status().isOk());

        mockMvc.perform(post("/mobile/unregister")
                        .param("uuid", "someUuid")
                )
                .andDo(print()).andExpect(status().isOk());

        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId())),
                SubscriptionHistoryItem.updated(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId()))
        ));
    }

    @Test
    void testMigrate() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "registeredUuid", MobileAppInfoUtil.BLUE_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "otherUuid", MobileAppInfoUtil.GREEN_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "unregisteredUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, true
        ));
        mobileAppInfoDAO.unregister("unregisteredUuid");

        mockMvc.perform(post("/mobile/migrate")
                        .param("fromId", "0")
                        .param("count", "10")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=3"));

        subscriptionAssertions.assertSubscriptions(DEFAULT_PUSH_TYPES, "registeredUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertNoSubscriptions("otherUuid");
        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "unregisteredUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(DEFAULT_NON_TRANSACTIONAL_PUSH_TYPES, "unregisteredUuid", SubscriptionStatus.UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId()))
        ));
    }

    @Test
    void testLessThanOneCountMigrate() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));

        mockMvc.perform(post("/mobile/migrate")
                        .param("fromId", "0")
                        .param("count", "0")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(
                        new Error("INVALID_FORMAT", "Expected count more than 0, but actual is 0", 400)
                )));
    }

    @Test
    void testPartMigrate() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid2", MobileAppInfoUtil.BLUE_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));

        mockMvc.perform(post("/mobile/migrate")
                        .param("fromId", "0")
                        .param("count", "1")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=1"));

        subscriptionAssertions.assertSubscriptions(DEFAULT_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertNoSubscriptions("someUuid2");
    }

    @Test
    void testTooBigFromIdMigrate() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));

        mockMvc.perform(post("/mobile/migrate")
                        .param("fromId", "10")
                        .param("count", "10")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=10"));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testNoBlueSubscriptionsMigrate() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.GREEN_APPNAME, "somePushToken", MobilePlatform.ANDROID, false
        ));

        mockMvc.perform(post("/mobile/migrate")
                        .param("fromId", "0")
                        .param("count", "10")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=0"));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testMigrateExisted() throws Exception {
        String appName = MobileAppInfoUtil.BLUE_AND_APPNAME;

        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", appName, "somePushToken", MobilePlatform.ANDROID, false
        ));

        mockMvc.perform(post("/mobile/register")
                        .param("uuid", "migratedUuid")
                        .param("push_token", "somePushToken")
                        .param("platform", MobilePlatform.IPHONE.name())
                        .param("appName", appName)
                )
                .andDo(print()).andExpect(status().isOk());

        mockMvc.perform(post("/mobile/migrate")
                        .param("fromId", "0")
                        .param("count", "10")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=1"));

        subscriptionAssertions.assertSubscriptions(DEFAULT_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(DEFAULT_PUSH_TYPES, "migratedUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
    }

    @Test
    void testSubscribeTransactional() throws Exception {
        subscriptionService.saveOrUpdate(List.of(
                createSubscription("subscribedUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED),
                createSubscription("otherUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.UNSUBSCRIBED),
                createSubscription("unsubscribedUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED)
        ));

        mockMvc.perform(post("/mobile/subscribe-transactional")
                        .param("fromId", "0")
                        .param("count", "10")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=3"));

        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "subscribedUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(Set.of(NotificationType.STORE_PUSH_GENERAL_ADVERTISING), "otherUuid", SubscriptionStatus.UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "unsubscribedUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId())),
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
    }

    @Test
    void testLessThanOneCountSubscribeTransactional() throws Exception {
        mockMvc.perform(post("/mobile/subscribe-transactional")
                        .param("fromId", "0")
                        .param("count", "0")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(
                        new Error("INVALID_FORMAT", "Expected count more than 0, but actual is 0", 400)
                )));
    }

    @Test
    void testPartSubscribeTransactional() throws Exception {
        subscriptionService.saveOrUpdate(List.of(
                createSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED),
                createSubscription("someUuid2", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED)
        ));

        mockMvc.perform(post("/mobile/subscribe-transactional")
                        .param("fromId", "0")
                        .param("count", "1")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=1"));

        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId())),
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid2", SubscriptionStatus.UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId()))
        ));
    }

    @Test
    void testTooBigFromIdSubscribeTransactional() throws Exception {
        subscriptionService.saveOrUpdate(List.of(
                createSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED)
        ));

        mockMvc.perform(post("/mobile/subscribe-transactional")
                        .param("fromId", "10")
                        .param("count", "10")
                        .param("batchSize", "5000")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=10"));

        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.UNSUBSCRIBED.getId()))
        ));
    }

    @Test
    void testFixWrongAppName() throws Exception {
        jdbcTemplate.update("insert into FIX_WRONG_APP_NAME (UUID) values ('someUuid')");
        jdbcTemplate.update("insert into FIX_WRONG_APP_NAME (UUID) values ('otherUuid')");
        jdbcTemplate.update("insert into FIX_WRONG_APP_NAME (UUID) values ('anotherUuid')");

        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "appUuid", MobileAppInfoUtil.BLUE_IOS_APPNAME, "appPushToken", MobilePlatform.ANDROID, false));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.GREEN_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "otherUuid", MobileAppInfoUtil.GREEN_APPNAME, "otherPushToken", MobilePlatform.ANDROID, false));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "unregUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "unregPushToken", MobilePlatform.ANDROID, true));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "anotherUuid", MobileAppInfoUtil.GREEN_APPNAME, "anotherPushToken", MobilePlatform.ANDROID, false));
        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "invalidUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "invalidPushToken", MobilePlatform.ANDROID, false));

        mockMvc.perform(post("/mobile/fix-wrong-appname")
                        .param("fromId", "0")
                        .param("oldAppName", MobileAppInfoUtil.GREEN_APPNAME)
                        .param("newAppName", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                        .param("batchSize", "2")
                        .param("xivaBatchSize", "2")
                        .param("forcedAppName", "none")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=3"));

        assertMobileAppInfo(new MobileAppInfo(
                null, "appUuid", MobileAppInfoUtil.BLUE_IOS_APPNAME, "appPushToken", MobilePlatform.ANDROID, false));
        assertMobileAppInfo(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));
        assertMobileAppInfo(new MobileAppInfo(
                null, "otherUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "otherPushToken", MobilePlatform.ANDROID, false));
        assertMobileAppInfo(new MobileAppInfo(
                null, "unregUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "unregPushToken", MobilePlatform.ANDROID, true));
        assertMobileAppInfo(new MobileAppInfo(
                null, "anotherUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "anotherPushToken", MobilePlatform.ANDROID, false));
        assertMobileAppInfo(new MobileAppInfo(
                null, "invalidUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "invalidPushToken", MobilePlatform.ANDROID, false));

        subscriptionAssertions.assertNoSubscriptions("appUuid");
        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "otherUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertNoSubscriptions("unregUuid");
        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "anotherUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
        subscriptionAssertions.assertNoSubscriptions("invalidUuid");
    }

    @Test
    void testFailAndResumeFixWrongAppName() throws Exception {
        jdbcTemplate.update("insert into FIX_WRONG_APP_NAME (UUID) values ('someUuid')");

        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.GREEN_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));

        mockMvc.perform(post("/mobile/fix-wrong-appname")
                        .param("fromId", "0")
                        .param("oldAppName", MobileAppInfoUtil.GREEN_APPNAME)
                        .param("newAppName", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                        .param("batchSize", "5000")
                        .param("xivaBatchSize", "10")
                )
                .andDo(print())
                .andExpect(status().isInternalServerError());

        assertMobileAppInfo(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));

        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));

        int state = jdbcTemplate.queryForObject(
                "select STATE from FIX_WRONG_APP_NAME_BATCH where UUID = 'someUuid'",
                Integer.class
        );

        assertEquals(0, state);

        mockMvc.perform(post("/mobile/fix-wrong-appname")
                        .param("fromId", "0")
                        .param("oldAppName", MobileAppInfoUtil.GREEN_APPNAME)
                        .param("newAppName", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                        .param("batchSize", "5000")
                        .param("xivaBatchSize", "10")
                        .param("forcedAppName", "none")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=1"));

        assertMobileAppInfo(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));

        subscriptionAssertions.assertSubscriptions(DEFAULT_TRANSACTIONAL_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));

        long count = jdbcTemplate.queryForObject(
                "select count(*) from FIX_WRONG_APP_NAME_BATCH",
                Long.class
        );

        assertEquals(0, count);
    }

    @Test
    void testSkipFixWrongAppNameIfNoWrongAppName() throws Exception {
        jdbcTemplate.update("insert into FIX_WRONG_APP_NAME (UUID) values ('someUuid')");

        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));

        mockMvc.perform(post("/mobile/fix-wrong-appname")
                        .param("fromId", "0")
                        .param("oldAppName", MobileAppInfoUtil.GREEN_APPNAME)
                        .param("newAppName", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                        .param("batchSize", "5000")
                        .param("xivaBatchSize", "10")
                        .param("forcedAppName", "none")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=0"));

        assertMobileAppInfo(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @Test
    void testSkipFixWrongAppNameIfTooLargeLastId() throws Exception {
        jdbcTemplate.update("insert into FIX_WRONG_APP_NAME (UUID) values ('someUuid')");

        mobileAppInfoDAO.add(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.GREEN_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));

        mockMvc.perform(post("/mobile/fix-wrong-appname")
                        .param("fromId", "1")
                        .param("oldAppName", MobileAppInfoUtil.GREEN_APPNAME)
                        .param("newAppName", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                        .param("batchSize", "5000")
                        .param("xivaBatchSize", "10")
                        .param("forcedAppName", "none")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("lastId=1"));

        assertMobileAppInfo(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.GREEN_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));

        subscriptionAssertions.assertNoSubscriptions("someUuid");
    }

    @ParameterizedTest
    @MethodSource("testV2RegistrationFailedIfEmptyRequiredFieldsArgumentsProvider")
    void testV2RegistrationFailedIfEmptyRequiredFields(String pushToken,
                                                       String platform,
                                                       String appName) throws Exception {
        mockMvc.perform(put("/mobile/v2/register/someUuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MobileRegistrationRequest()
                                        .setPushToken(pushToken)
                                        .setPlatform(platform)
                                        .setAppName(appName)
                                )
                        )
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(new Error(
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        (pushToken == null ? "pushToken" : platform == null ? "platform" : "appName") + " is empty",
                        HttpStatus.BAD_REQUEST.value())
                )));
    }

    private static Stream<Arguments> testV2RegistrationFailedIfEmptyRequiredFieldsArgumentsProvider() {
        return Stream.of(
            arguments(null, MobilePlatform.ANDROID.name(), MobileAppInfoUtil.BLUE_NEW_AND_APPNAME),
            arguments("somePushToken", null, MobileAppInfoUtil.BLUE_NEW_AND_APPNAME),
            arguments("somePushToken", MobilePlatform.ANDROID.name(), null)
        );
    }

    @Test
    void testV2RegistrationFailedIfPlatformNotSupported() throws Exception {
        mockMvc.perform(put("/mobile/v2/register/someUuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MobileRegistrationRequest()
                                        .setPushToken("somePushToken")
                                        .setPlatform("WINDOWS")
                                        .setAppName(MobileAppInfoUtil.BLUE_NEW_AND_APPNAME))
                        )
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(new Error(
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "platform not supported. " + SUPPORTED_PLATFORMS + " expected",
                        HttpStatus.BAD_REQUEST.value())
                )));
    }

    @Test
    void testV2RegistrationFailedIfAppNameNotSupported() throws Exception {
        mockMvc.perform(put("/mobile/v2/register/someUuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MobileRegistrationRequest()
                                        .setPushToken("somePushToken")
                                        .setPlatform(Platform.ANDROID.name())
                                        .setAppName(MobileAppInfoUtil.GREEN_APPNAME)
                                )
                        )
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(toJson(new Error(
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "appName not supported. " + MobileAppInfoUtil.getAppNamesByMarket(Market.STORE) + " expected",
                        HttpStatus.BAD_REQUEST.value())
                )));
    }

    @Test
    void testV2RegistrationWithMinFields() throws Exception {
        mockMvc.perform(put("/mobile/v2/register/someUuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MobileRegistrationRequest()
                                        .setPushToken("somePushToken")
                                        .setPlatform(Platform.ANDROID.name())
                                        .setAppName(MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                                )
                        )
                )
                .andDo(print())
                .andExpect(status().isOk());

        assertMobileAppInfo(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "somePushToken", MobilePlatform.ANDROID, false));
        subscriptionAssertions.assertSubscriptions(DEFAULT_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
    }

    @Test
    void testV2RegistrationWithAllFields() throws Exception {
        mockMvc.perform(put("/mobile/v2/register/someUuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MobileRegistrationRequest()
                                        .setPushToken("somePushToken")
                                        .setPlatform(Platform.ANDROID.name())
                                        .setAppName(MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                                        .setPuid(123L)
                                        .setYandexuid("someYuid")
                                        .setMuid(456L)
                                        .setLoginTime(new Date().getTime())
                                        .setEnabledBySystem(true)
                                        .setRegionId(789L)
                                        .setAppVersion("v1")
                                        .setOsVersion("v2")
                                )
                        )
                )
                .andDo(print())
                .andExpect(status().isOk());

        assertMobileAppInfo(new MobileAppInfo(
                123L, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "someYuid", "somePushToken", MobilePlatform.ANDROID, false, new Date(), 789L, 456L, true));
        subscriptionAssertions.assertSubscriptions(DEFAULT_PUSH_TYPES, "someUuid", SubscriptionStatus.SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(1L, "status", String.valueOf(SubscriptionStatus.SUBSCRIBED.getId()))
        ));
    }

    @Test
    void testV2RegistrationWithUpdateFields() throws Exception {
        mobileAppInfoDAO.add(new MobileAppInfo(
                123L, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "someYuid", "somePushToken", MobilePlatform.ANDROID, true, new Date(), 789L, 456L, true));

        mockMvc.perform(put("/mobile/v2/register/someUuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                        new MobileRegistrationRequest()
                                                .setPushToken("otherPushToken")
                                                .setPlatform(Platform.ANDROID.name())
                                                .setAppName(MobileAppInfoUtil.BLUE_NEW_AND_APPNAME)
                                )
                        )
                )
                .andDo(print())
                .andExpect(status().isOk());

        assertMobileAppInfo(new MobileAppInfo(
                null, "someUuid", MobileAppInfoUtil.BLUE_NEW_AND_APPNAME, "otherPushToken", MobilePlatform.ANDROID, false));
        subscriptionAssertions.assertNoSubscriptions("someUUid");
    }

    private Subscription createSubscription(String uuid, NotificationType type, SubscriptionStatus status) {
        return new Subscription(new Uuid(uuid), NotificationTransportType.PUSH, type, status);
    }

    private void assertMobileAppInfo(MobileAppInfo expected) {
        var actual = mobileAppInfoDAO.getByUuid(expected.getUuid());

        assertEquals(actual.getUid(), expected.getUid());
        assertEquals(actual.getAppName(), expected.getAppName());
        assertEquals(actual.getUuid(), expected.getUuid());
        assertEquals(actual.getPushToken(), expected.getPushToken());
        assertEquals(actual.getMuid(), expected.getMuid());
        assertEquals(actual.getGeoId(), expected.getGeoId());
        assertEquals(actual.getYandexUid(), expected.getYandexUid());
        assertEquals(actual.getPlatform(), expected.getPlatform());
    }
}
