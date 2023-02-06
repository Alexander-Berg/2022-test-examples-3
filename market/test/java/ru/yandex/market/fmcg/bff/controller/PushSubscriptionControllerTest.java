package ru.yandex.market.fmcg.bff.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;

import ru.yandex.market.fmcg.bff.connector.BlackboxConnector;
import ru.yandex.market.fmcg.bff.controller.dto.BlackboxOAuthResponse;
import ru.yandex.market.fmcg.bff.region.RegionService;
import ru.yandex.market.fmcg.bff.region.model.Coords;
import ru.yandex.market.fmcg.bff.region.model.GeobaseRegion;
import ru.yandex.market.fmcg.bff.test.FmcgBffTest;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author semin-serg
 */
class PushSubscriptionControllerTest extends FmcgBffTest {

    private static final String AUTH_TOKEN = "some-auth-token";
    private static final int USER_REGION_ID = 2;
    private static final GeobaseRegion USER_REGION = new GeobaseRegion(USER_REGION_ID, "", new Coords(0, 0), "", 1);
    private static final String UUID = "some-uuid";
    private static final String PUSH_TOKEN = "some-push-token";
    private static final String ANDROID_PLATFORM = "android";
    private static final String IOS_PLATFORM = "ios";
    private static final String APP_NAME = "some-app-name";
    private static final String USER_IP = "some-user-ip";
    private static final long UID = 25321;
    private static final String USER_TICKET = "some_user_ticket";
    private static final String DEFAULT_APP_NAME = "com.yandex.supercheck";

    @Autowired
    RegionService regionService;
    @Autowired
    BlackboxConnector authenticationService;
    @Autowired
    PersNotifyClient persNotifyClient;

    @Autowired
    MockMvc mockMvc;
    @Captor
    ArgumentCaptor<MobileAppInfo> mobileAppInfoArgumentCaptor;
    @Captor
    ArgumentCaptor<Long> longCaptor;
    @Captor
    ArgumentCaptor<String> stringCaptor1;
    @Captor
    ArgumentCaptor<String> stringCaptor2;

    @BeforeEach
    void processMockitoAnnotations() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    class SubscribeTest extends FmcgBffTest {

        @SneakyThrows
        @Test
        void authorized() {
            setupAuthenticationService();

            mockMvc.perform(addHeadersForAuth(createRequestBuilder(ANDROID_PLATFORM)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());

            verify(persNotifyClient).registerMobileAppInfo(mobileAppInfoArgumentCaptor.capture());
            MobileAppInfo expectedMobileAppInfo = createExpectedMobileAppInfo();
            expectedMobileAppInfo.setUid(UID);
            assertThat(mobileAppInfoArgumentCaptor.getValue())
                .isEqualToComparingFieldByField(expectedMobileAppInfo);
        }

        @SneakyThrows
        @Test
        void android() {
            platformTest(ANDROID_PLATFORM, MobilePlatform.ANDROID);
        }

        @SneakyThrows
        @Test
        void ios() {
            platformTest(IOS_PLATFORM, MobilePlatform.IPHONE);
        }

        @SneakyThrows
        @Test
        void mobileAppInfoWasNotRegistered() {
            when(persNotifyClient.registerMobileAppInfo(any())).thenReturn(false);

            mockMvc.perform(createRequestBuilder())
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @SneakyThrows
        @Test
        void geoId() {
            setupRegionService();

            mockMvc.perform(createRequestBuilder()
                .header("User-Region", USER_REGION_ID))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());

            verify(persNotifyClient).registerMobileAppInfo(mobileAppInfoArgumentCaptor.capture());
            MobileAppInfo expectedMobileAppInfo = createExpectedMobileAppInfo();
            expectedMobileAppInfo.setGeoId((long) USER_REGION_ID);
            assertThat(mobileAppInfoArgumentCaptor.getValue())
                .isEqualToComparingFieldByField(expectedMobileAppInfo);
        }

        @SneakyThrows
        private void platformTest(String platform, MobilePlatform expectedPlatform) {
            mockMvc.perform(createRequestBuilder(platform))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());

            verify(persNotifyClient).registerMobileAppInfo(mobileAppInfoArgumentCaptor.capture());
            assertThat(mobileAppInfoArgumentCaptor.getValue())
                .isEqualToComparingFieldByField(createExpectedMobileAppInfo(expectedPlatform));
        }

        private MockHttpServletRequestBuilder createRequestBuilder() {
            return createRequestBuilder(ANDROID_PLATFORM);
        }

        private MockHttpServletRequestBuilder createRequestBuilder(String platform) {
            return MockMvcRequestBuilders.get("/apiv1/b/push/subscribe")
                .param("uuid", UUID)
                .param("pushToken", PUSH_TOKEN)
                .param("platform", platform)
                .param("appName", APP_NAME);
        }

        private MobileAppInfo createExpectedMobileAppInfo() {
            return createExpectedMobileAppInfo(MobilePlatform.ANDROID);
        }

        private MobileAppInfo createExpectedMobileAppInfo(MobilePlatform mobilePlatform) {
            MobileAppInfo expectedMobileAppInfo = new MobileAppInfo();
            expectedMobileAppInfo.setUuid(UUID);
            expectedMobileAppInfo.setPushToken(PUSH_TOKEN);
            expectedMobileAppInfo.setPlatform(mobilePlatform);
            expectedMobileAppInfo.setAppName(APP_NAME);
            return expectedMobileAppInfo;
        }

        private void setupRegionService() {
            when(regionService.getFmcgRegion(USER_REGION_ID)).thenReturn(Optional.of(USER_REGION));
        }

    }

    @Nested
    class UnsubscribeTest extends FmcgBffTest {

        @SneakyThrows
        @Test
        void unauthorized() {
            mockMvc.perform(createRequestBuilder())
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());

            verify(persNotifyClient).unregisterMobileAppInfo(longCaptor.capture(), stringCaptor1.capture(),
                stringCaptor2.capture());
            assertThat(longCaptor.getValue()).isNull();
            assertThat(stringCaptor1.getValue()).isEqualTo(UUID);
            assertThat(stringCaptor2.getValue()).isEqualTo(DEFAULT_APP_NAME);
        }

        @SneakyThrows
        @Test
        void authorized() {
            setupAuthenticationService();

            mockMvc.perform(addHeadersForAuth(createRequestBuilder()))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());

            verify(persNotifyClient).unregisterMobileAppInfo(longCaptor.capture(), stringCaptor1.capture(),
                stringCaptor2.capture());
            assertThat(longCaptor.getValue()).isEqualTo(UID);
            assertThat(stringCaptor1.getValue()).isEqualTo(UUID);
            assertThat(stringCaptor2.getValue()).isEqualTo(DEFAULT_APP_NAME);
        }

        @SneakyThrows
        @Test
        void failureInMarketUtils() {
            when(persNotifyClient.unregisterMobileAppInfo(any(), any(), any())).thenReturn(false);

            mockMvc.perform(createRequestBuilder())
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        }

        private MockHttpServletRequestBuilder createRequestBuilder() {
            return MockMvcRequestBuilders.get("/apiv1/b/push/unsubscribe")
                .param("uuid", UUID);
        }

    }

    private void setupAuthenticationService() {
        BlackboxOAuthResponse blackboxResponse = new BlackboxOAuthResponse();
        BlackboxOAuthResponse.OAuth oAuth = new BlackboxOAuthResponse.OAuth();
        oAuth.setUid(UID);
        blackboxResponse.setOAuth(oAuth);
        blackboxResponse.setUserTicket(USER_TICKET);
        when(authenticationService.oauth(eq(AUTH_TOKEN), eq(USER_IP))).thenReturn(blackboxResponse);
    }

    private MockHttpServletRequestBuilder addHeadersForAuth(MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder
            .header("Authorization", "OAuth " + AUTH_TOKEN)
            .header("X-Real-IP", USER_IP);
    }

}
