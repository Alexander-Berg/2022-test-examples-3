package ru.yandex.direct.intapi.entity.display.canvas;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DisplayCanvasAuthenticateTest {
    @Autowired
    private Steps steps;

    @Autowired
    private DisplayCanvasController displayCanvasController;

    private MockMvc mockMvc;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(displayCanvasController).build();
    }

    @Test
    public void displayCanvasAuthSmokeTest() throws Exception {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        Long uid = userInfo.getUid();
        ClientId clientId = userInfo.getClientInfo().getClientId();


        mockMvc.perform(get("/DisplayCanvas/authenticate")
                .param("client_id", clientId.toString())
                .param("user_id", uid.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(json().when(Option.IGNORING_ARRAY_ORDER)
                        .isEqualTo("{\"permissions\":[\"preview\",\"creative_get\",\"creative_create\"," +
                                "\"idea\",\"video_addition\", \"turbo_landing\"]}"));

        //Unknown user
        mockMvc.perform(get("/DisplayCanvas/authenticate")
                .param("client_id", "121212")
                .param("user_id", "35436715")
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(json().isEqualTo("{\"permissions\":[]}"));

        //Known uid, but incorrect clientId
        mockMvc.perform(get("/DisplayCanvas/authenticate")
                .param("user_id", uid.toString())
                .param("client_id", "12")
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(json().isEqualTo("{\"permissions\":[]}"));


        //ClientID wasn't passed
        mockMvc.perform(get("/DisplayCanvas/authenticate")
                .param("client_id", "")
                .param("user_id", uid.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(json().isEqualTo("{\"permissions\":[\"preview\"]}"));

        //Both of params weren't passed
        mockMvc.perform(get("/DisplayCanvas/authenticate")
                .param("client_id", "")
                .param("user_id", "")
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(json().isEqualTo("{\"permissions\":[\"preview\"]}"));

    }
}
