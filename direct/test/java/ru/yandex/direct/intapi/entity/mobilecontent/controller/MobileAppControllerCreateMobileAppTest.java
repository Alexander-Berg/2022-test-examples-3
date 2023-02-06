package ru.yandex.direct.intapi.entity.mobilecontent.controller;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.mobileapp.service.MobileAppService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.mobilecontent.model.CreateMobileAppRequest;
import ru.yandex.direct.intapi.entity.mobilecontent.model.CreateMobileAppResponse;
import ru.yandex.direct.utils.JsonUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppControllerCreateMobileAppTest {
    private static final String INVALID_STORE_HREF = "https://itunes.apple.com/us/app";
    private static final String VALID_STORE_HREF1 = "https://itunes.apple.com/us/app/tumblr/id305343404?mt=8";
    private static final String VALID_STORE_HREF2 =
            "https://play.google.com/store/apps/details?id=com.apple.android.music&hl=ru";
    private static final String INVALID_TRACKER_URL = "bogus_url";
    private static final String VALID_TRACKER_URL1 = "https://appmetrica.yandex.net/12345";
    private static final String VALID_TRACKER_URL2 = "https://adjust.com/12345";

    @Autowired
    private Steps steps;

    @Autowired
    private MobileAppService mobileAppService;

    @Autowired
    private MobileAppController controller;

    private MockMvc mockMvc;

    private ClientId clientId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void invalidHttpMethod() throws Exception {
        mockMvc
                .perform(get("/mobile_app/createMobileApp"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void invalidStoreHref() throws Exception {
        CreateMobileAppResponse response = call(singleRequest(INVALID_STORE_HREF, null, null, null));
        assertThat(response.getResult(), contains(error(CreateMobileAppResponse.ErrorType.INVALID_STORE_HREF)));
    }

    @Test
    public void validHrefEmptyTrackerUrl() throws Exception {
        CreateMobileAppResponse response = call(singleRequest(VALID_STORE_HREF1, null, "", ""));
        assertThat(response.getResult(), contains(error(CreateMobileAppResponse.ErrorType.INVALID_TRACKER_URL)));
    }

    @Test
    public void validHrefBlankTrackerUrl() throws Exception {
        CreateMobileAppResponse response = call(singleRequest(VALID_STORE_HREF1, null, "  ", "  "));
        assertThat(response.getResult(), contains(error(CreateMobileAppResponse.ErrorType.INVALID_TRACKER_URL)));
    }

    @Test
    public void validHrefInvalidTrackerUrl() throws Exception {
        CreateMobileAppResponse response = call(singleRequest(VALID_STORE_HREF1, null, INVALID_TRACKER_URL, null));
        assertThat(response.getResult(), contains(error(CreateMobileAppResponse.ErrorType.INVALID_TRACKER_URL)));
    }

    @Test
    public void validHrefNoOtherData() throws Exception {
        CreateMobileAppResponse response = call(singleRequest(VALID_STORE_HREF1, null, null, null));
        assertThat(response.getResult(), contains(success()));

        Long mobileAppId = checkNotNull(response.getResult().get(0).getMobileAppId());
        MobileApp mobileApp = mobileAppService.getMobileApp(clientId, mobileAppId)
                .orElseThrow(IllegalStateException::new);

        assertThat(mobileApp.getStoreHref(), equalTo(VALID_STORE_HREF1));
        assertThat(mobileApp.getMinimalOperatingSystemVersion(), nullValue());
        assertThat(mobileApp.getTrackers(), empty());
    }

    @Test
    public void oneRequest_allDataFilled() throws Exception {
        String minimalOperatingSystemVersion = "5.0";
        CreateMobileAppResponse response =
                call(singleRequest(VALID_STORE_HREF1, minimalOperatingSystemVersion,
                                   VALID_TRACKER_URL1, VALID_TRACKER_URL1));

        assertThat(response.getResult(), contains(success()));

        Long mobileAppId = checkNotNull(response.getResult().get(0).getMobileAppId());
        MobileApp mobileApp = mobileAppService.getMobileApp(clientId, mobileAppId)
                .orElseThrow(IllegalStateException::new);

        assertThat(mobileApp.getStoreHref(), equalTo(VALID_STORE_HREF1));
        assertThat(mobileApp.getMinimalOperatingSystemVersion(), equalTo(minimalOperatingSystemVersion));
        assertThat(mobileApp.getTrackers(), hasSize(1));

        MobileAppTracker mobileAppTracker = mobileApp.getTrackers().get(0);
        assertThat(mobileAppTracker.getTrackingSystem(), equalTo(MobileAppTrackerTrackingSystem.OTHER));
        assertThat(mobileAppTracker.getUrl(), equalTo(VALID_TRACKER_URL1));
    }

    @Test
    public void twoRequests_allDataFilled() throws Exception {
        String minimalOperatingSystemVersion1 = "5.0";
        String minimalOperatingSystemVersion2 = "10.0";
        CreateMobileAppResponse response = call(asList(
                request(VALID_STORE_HREF1, minimalOperatingSystemVersion1, VALID_TRACKER_URL1, VALID_TRACKER_URL1),
                request(VALID_STORE_HREF2, minimalOperatingSystemVersion2, VALID_TRACKER_URL2, VALID_TRACKER_URL2)));

        assertThat(response.getResult(), contains(asList(success(), success())));

        Long mobileAppId1 = checkNotNull(response.getResult().get(0).getMobileAppId());
        MobileApp mobileApp1 = mobileAppService.getMobileApp(clientId, mobileAppId1)
                .orElseThrow(IllegalStateException::new);

        assertThat(mobileApp1.getStoreHref(), equalTo(VALID_STORE_HREF1));
        assertThat(mobileApp1.getMinimalOperatingSystemVersion(), equalTo(minimalOperatingSystemVersion1));
        assertThat(mobileApp1.getTrackers(), hasSize(1));

        MobileAppTracker mobileAppTracker1 = mobileApp1.getTrackers().get(0);
        assertThat(mobileAppTracker1.getTrackingSystem(), equalTo(MobileAppTrackerTrackingSystem.OTHER));
        assertThat(mobileAppTracker1.getUrl(), equalTo(VALID_TRACKER_URL1));

        Long mobileAppId2 = checkNotNull(response.getResult().get(1).getMobileAppId());
        MobileApp mobileApp2 = mobileAppService.getMobileApp(clientId, mobileAppId2)
                .orElseThrow(IllegalStateException::new);

        assertThat(mobileApp2.getStoreHref(), equalTo(VALID_STORE_HREF2));
        assertThat(mobileApp2.getMinimalOperatingSystemVersion(), equalTo(minimalOperatingSystemVersion2));
        assertThat(mobileApp2.getTrackers(), hasSize(1));

        MobileAppTracker mobileAppTracker2 = mobileApp2.getTrackers().get(0);
        assertThat(mobileAppTracker2.getTrackingSystem(), equalTo(MobileAppTrackerTrackingSystem.OTHER));
        assertThat(mobileAppTracker2.getUrl(), equalTo(VALID_TRACKER_URL2));
    }

    private CreateMobileAppRequest.Entity request(
            String storeHref, String minimalOperatingSystemVersion, String trackerUrl, String impressionUrl
    ) {
        return new CreateMobileAppRequest.Entity(storeHref, minimalOperatingSystemVersion, trackerUrl, impressionUrl);
    }

    private List<CreateMobileAppRequest.Entity> singleRequest(String storeHref, String minimalOperatingSystemVersion,
                                                              String trackerUrl, String impressionUrl) {
        CreateMobileAppRequest.Entity
                request = request(storeHref, minimalOperatingSystemVersion, trackerUrl, impressionUrl);
        return singletonList(request);
    }

    private CreateMobileAppResponse call(List<CreateMobileAppRequest.Entity> requests) throws Exception {
        String resultBody = mockMvc
                .perform(post("/mobile_app/createMobileApp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(new CreateMobileAppRequest(clientId, requests))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn().getResponse().getContentAsString();

        return JsonUtils.fromJson(resultBody, CreateMobileAppResponse.class);
    }

    private Matcher<CreateMobileAppResponse.Entry> error(CreateMobileAppResponse.ErrorType errorType) {
        return allOf(hasProperty("success", equalTo(false)),
                hasProperty("error", equalTo(errorType)),
                hasProperty("mobileAppId", nullValue()));
    }

    private Matcher<CreateMobileAppResponse.Entry> success() {
        return allOf(hasProperty("success", equalTo(true)),
                hasProperty("error", nullValue()),
                hasProperty("mobileAppId", not(nullValue())));
    }
}
