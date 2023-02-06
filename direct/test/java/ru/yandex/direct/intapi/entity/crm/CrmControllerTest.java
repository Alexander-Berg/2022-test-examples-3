package ru.yandex.direct.intapi.entity.crm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.utils.JsonUtils;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@IntApiTest
@RunWith(SpringRunner.class)
public class CrmControllerTest {
    private static final String FAILBACK_DETAILS = "{\"enabled_by\":\"somelogin\", \"reason_type\":\"AUTO\", "
            + "\"reason_text\": \"IDM lag\", "
            + "\"solomon_link\":\"https://solomon.yandex-team.ru/?project=direct&graph=idm_requests\"}";
    @Autowired
    private Steps steps;

    @Autowired
    private CrmController crmController;

    @Autowired
    PpcPropertiesSupport propertiesSupport;

    private MockMvc mockMvc;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(crmController).build();
    }

    @Test
    public void getAdgroupInfoSmokeTest() throws Exception {
        AdGroupInfo textAdGroup = steps.adGroupSteps().createActiveTextAdGroup();
        AdGroupInfo mobileAdGroup = steps.adGroupSteps().createActiveMobileContentAdGroup();
        Long invalidId = mobileAdGroup.getAdGroupId() * 1000;

        mockMvc.perform(post("/crm/adgroups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("[%d,%d,%d]",
                        mobileAdGroup.getAdGroupId(), invalidId, textAdGroup.getAdGroupId()))
        ).andExpect(json().isEqualTo(
                "["
                        + "{\"adgroup_id\":" + mobileAdGroup.getAdGroupId() +
                        ",\"client_id\":" + mobileAdGroup.getClientId() +
                        ",\"campaign_id\":" + mobileAdGroup.getCampaignId() + "},"

                        + "{\"adgroup_id\":" + invalidId + ",\"error\":\"Not found\"},"

                        + "{\"adgroup_id\":" + textAdGroup.getAdGroupId()
                        + ",\"client_id\":" + textAdGroup.getClientId()
                        + ",\"campaign_id\":" + textAdGroup.getCampaignId() + "}"
                        + "]"));
    }

    @Test
    public void getFallbackStatusWhenFallbackDisabled() throws Exception {
        propertiesSupport.set(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS.getName(), "0");

        mockMvc.perform(get("/crm/fallback/status")).andExpect(json().isEqualTo("{\"enabled\":\"NO\"}"));
    }

    @Test
    public void getFallbackStatusWhenFallbackEnabled() throws Exception {
        propertiesSupport.set(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS.getName(), "1");
        propertiesSupport.set(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS_META.getName(), FAILBACK_DETAILS);

        mockMvc.perform(get("/crm/fallback/status")).andExpect(json().isEqualTo("{\"enabled\":\"YES\", \"details\": "
                + FAILBACK_DETAILS + "}"));
    }

    @Test
    public void enableFallback() throws Exception {
        propertiesSupport.set(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS.getName(), "0");

        mockMvc.perform(post("/crm/fallback/enable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FAILBACK_DETAILS)
        ).andExpect(json().isEqualTo("{\"success\": true}"));

        String propEnabled = propertiesSupport.get(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS.getName());
        String propMeta = propertiesSupport.get(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS_META.getName());

        assertEquals(propEnabled, "1");
        assertEquals(JsonUtils.fromJson(propMeta), JsonUtils.fromJson(FAILBACK_DETAILS));
    }

    @Test
    public void tryEnableFallbackWhenAlreadyEnabled() throws Exception {
        propertiesSupport.set(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS.getName(), "1");

        mockMvc.perform(post("/crm/fallback/enable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FAILBACK_DETAILS)
        ).andExpect(json().isEqualTo("{\"success\": false, "
                + "\"code\": null,\"text\": \"OPERATION_FAILED\",\"description\": \"fallback already enabled\"}"));
    }

    @Test
    public void disableFallback() throws Exception {
        propertiesSupport.set(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS.getName(), "1");
        propertiesSupport.set(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS_META.getName(), FAILBACK_DETAILS);

        mockMvc.perform(post("/crm/fallback/disable")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(json().isEqualTo("{\"success\": true}"));

        String propEnabled = propertiesSupport.get(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS.getName());
        String propMeta = propertiesSupport.get(PpcPropertyEnum.ENABLE_LIMITED_SUPPORT_READ_ALL_CLIENTS_META.getName());

        assertEquals(propEnabled, "0");
        assertEquals(propMeta, null);
    }
}
