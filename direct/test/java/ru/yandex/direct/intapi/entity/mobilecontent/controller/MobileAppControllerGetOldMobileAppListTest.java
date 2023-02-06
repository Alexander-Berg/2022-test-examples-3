package ru.yandex.direct.intapi.entity.mobilecontent.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppControllerGetOldMobileAppListTest {

    public static final String STORE_CONTENT_ID1 = "ru.yandex.dummy.aa111111111111";
    public static final String HTTPS_TRACKER1 = "https://tracker1.ru/111111111111";
    public static final String STORE_CONTENT_ID2 = "ru.yandex.dummy.aa222222222222";
    public static final String HTTPS_TRACKER2 = "https://tracker1.ru/222222222222";
    @Autowired
    private Steps steps;

    @Autowired
    private MobileAppController controller;

    private MockMvc mockMvc;

    private Long clientId;
    private Long mobileContentId1;
    private Long mobileContentId2;

    @Before
    public void before() throws Exception {
        MobileContentInfo mobileContentInfo1 = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent().withStoreContentId(STORE_CONTENT_ID1)));
        mobileContentId1 = mobileContentInfo1.getMobileContentId();
        AdGroupInfo adGroupInfo1 = steps.adGroupSteps().createActiveMobileContentAdGroup(mobileContentInfo1);
        steps.bannerSteps().createBanner(activeTextBanner().withHref(HTTPS_TRACKER1), adGroupInfo1);


        MobileContentInfo mobileContentInfo2 = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent().withStoreContentId(STORE_CONTENT_ID2))
                        .withClientInfo(mobileContentInfo1.getClientInfo()));
        mobileContentId2 = mobileContentInfo2.getMobileContentId();
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveMobileContentAdGroup(mobileContentInfo2);
        steps.bannerSteps().createBanner(activeTextBanner().withHref(HTTPS_TRACKER2), adGroupInfo2);

        clientId = mobileContentInfo1.getClientId().asLong();

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getOldMobileAppList() throws Exception {
        mockMvc
                .perform(get("/mobile_app/mobile_content_list").param("client_id", Long.toString(clientId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(
                        String.format("{'success': true, 'result': [{"
                                        + "'mobileContentId': %s,"
                                        + "'storeHref': '%s',"
                                        + "'mobileContent': {'store_content_id': %s},"
                                        + "'trackers': [{'url': '%s'}]"
                                        + "}, {"
                                        + "'mobileContentId': %s,"
                                        + "'storeHref': '%s',"
                                        + "'mobileContent': {'store_content_id': %s},"
                                        + "'trackers': [{'url': '%s'}]"
                                        + "}]}",
                                mobileContentId1,
                                TestGroups.getDefaultStoreHref(STORE_CONTENT_ID1),
                                STORE_CONTENT_ID1,
                                HTTPS_TRACKER1,
                                mobileContentId2,
                                TestGroups.getDefaultStoreHref(STORE_CONTENT_ID2),
                                STORE_CONTENT_ID2,
                                HTTPS_TRACKER2
                        )
                ));
    }
}
