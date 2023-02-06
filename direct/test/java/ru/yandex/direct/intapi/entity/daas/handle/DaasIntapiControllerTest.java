package ru.yandex.direct.intapi.entity.daas.handle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.daas.ClientStatus;
import ru.yandex.direct.intapi.entity.daas.DaasAvailabilityRequest;
import ru.yandex.direct.intapi.entity.daas.DaasAvailabilityResult;
import ru.yandex.direct.intapi.entity.daas.ItemStatus;
import ru.yandex.direct.intapi.entity.daas.ResponseItem;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.intapi.entity.daas.handle.DaasIntapiController.PROTOBUF_MEDIA_TYPE;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DaasIntapiControllerTest {
    private static final String EXTERNAL_ID = "id1";
    private static final DaasAvailabilityResult FORBIDDEN_RESPONSE = DaasAvailabilityResult.newBuilder()
            .addResponseItems(ResponseItem.newBuilder()
                    .setItemStatus(ItemStatus.FORBIDDEN)
                    .setExternalId(EXTERNAL_ID)
                    .build())
            .setClientStatus(ClientStatus.FORBIDDEN_CLIENT)
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private MockMvc mockMvc;

    @Before
    public void before() {
        DaasIntapiController daasIntapiController = new DaasIntapiController();
        mockMvc = MockMvcBuilders.standaloneSetup(daasIntapiController)
                .setMessageConverters(new ProtobufHttpMessageConverter())
                .build();
    }

    @Test
    public void organizationsRequestTest() throws Exception {
        mockMvc
                .perform(post("/daas/get_client_item_statuses/organizations")
                        .contentType(PROTOBUF_MEDIA_TYPE)
                        .content(createRequest(12345L).toByteArray())
                        .accept(PROTOBUF_MEDIA_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FORBIDDEN_RESPONSE.toByteArray()));
    }

    @Test
    public void collectionsRequestTest() throws Exception {
        mockMvc
                .perform(post("/daas/get_client_item_statuses/organizations")
                        .contentType(PROTOBUF_MEDIA_TYPE)
                        .content(createRequest(12345L).toByteArray())
                        .accept(PROTOBUF_MEDIA_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FORBIDDEN_RESPONSE.toByteArray()));
    }

    private DaasAvailabilityRequest createRequest(Long uid) {
        return DaasAvailabilityRequest.newBuilder()
                .addExternalIds(EXTERNAL_ID)
                .setUid(uid)
                .build();
    }
}
