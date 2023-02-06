package ru.yandex.market.tpl.carrier.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;

import ru.yandex.market.tpl.carrier.core.domain.registry.Registry;
import ru.yandex.market.tpl.carrier.core.domain.registry.RegistryStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.service.photo.AvatarincaSaveResponse;
import ru.yandex.market.tpl.carrier.driver.service.photo.AvatarnicaPhotoSaver;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.BASE_PATH;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RegistryControllerTest extends BaseDriverApiIntTest {

    private final TestUserHelper userHelper;

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final AvatarnicaPhotoSaver avatarnicaPhotoSaver;
    private final ObjectMapper objectMapper;

    private long taskId;

    @BeforeEach
    public void setUp() {
        mockBlackboxClient(UID);
        User user = userHelper.findOrCreateUser(UID);
        var transport = userHelper.findOrCreateTransport();
        Run run = runGenerator.generate();
        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        this.taskId = userShift.streamCollectDropshipTasks().findFirst().get().getId();
        Mockito.when(avatarnicaPhotoSaver.saveInAvatarnica(Mockito.any()))
                .thenReturn(new AvatarincaSaveResponse("url", 0L, "imageName", 3L));
    }

    @Test
    void createRegistry() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/tasks/collect-dropship/{taskId}/registry", taskId)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RegistryStatus.CREATED.name()))
                .andExpect(jsonPath("$.sortables").value(Matchers.hasSize(0)));
        mockMvc.perform(get(BASE_PATH + "/tasks/collect-dropship/{taskId}/registry", taskId)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RegistryStatus.CREATED.name()))
                .andExpect(jsonPath("$.sortables").value(Matchers.hasSize(0)));
    }

    @Test
    void itemsFullCycle() throws Exception {
        var response = mockMvc.perform(post(BASE_PATH + "/tasks/collect-dropship/{taskId}/registry", taskId)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andReturn()
                .getResponse()
                .getContentAsString();

        Registry registry = objectMapper.readValue(response, Registry.class);
        response =
                mockMvc.perform(post(BASE_PATH + "/tasks/collect-dropship/registry/{registryId}/item", registry.getId())
                                .param("barcode", "SOME_BARCODE")
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                        ).andExpect(status().isOk())
                        .andExpect(jsonPath("$.sortables").value(Matchers.hasSize(1)))
                        .andExpect(jsonPath("$.sortables[0].sortableBarcode").value("SOME_BARCODE"))
                        .andReturn()
                        .getResponse().getContentAsString();

        registry = objectMapper.readValue(response, Registry.class);
        var sortable = registry.getSortables().stream().findFirst().get();

        mockMvc.perform(multipart(BASE_PATH + "/tasks/collect-dropship/registry/{registryId}/sortable/{sortableId}/photo",
                        registry.getId(), sortable.getId())
                        .part(new MockPart("photoFile1", "1", new byte[3]))
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .contentType(MediaType.IMAGE_JPEG)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortables[0].photo").value("url"));

        mockMvc.perform(delete(BASE_PATH + "/tasks/collect-dropship/registry/{registryId}/item", registry.getId())
                        .param("barcode", "SOME_BARCODE")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.sortables").value(Matchers.hasSize(0)));
    }

    @Test
    void finaliseRegistry() throws Exception {
        var response = mockMvc.perform(post(BASE_PATH + "/tasks/collect-dropship/{taskId}/registry", taskId)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andReturn()
                .getResponse()
                .getContentAsString();

        Registry registry = objectMapper.readValue(response, Registry.class);
        mockMvc.perform(post(BASE_PATH + "/tasks/collect-dropship/registry/{registryId}/finalise", registry.getId())
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RegistryStatus.FINALISED.name()));
        mockMvc.perform(post(BASE_PATH + "/tasks/collect-dropship/registry/{registryId}/item", registry.getId())
                .param("barcode", "SOME_BARCODE")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().is5xxServerError());
    }
}
