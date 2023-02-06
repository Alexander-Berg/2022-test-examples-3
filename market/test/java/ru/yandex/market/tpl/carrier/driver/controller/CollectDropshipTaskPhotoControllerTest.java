package ru.yandex.market.tpl.carrier.driver.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.tpl.carrier.core.domain.photo.Photo;
import ru.yandex.market.tpl.carrier.core.domain.photo.PhotoRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.photo.PhotoDto;
import ru.yandex.market.tpl.carrier.driver.service.photo.AvatarincaSaveResponse;
import ru.yandex.market.tpl.carrier.driver.service.photo.AvatarnicaPhotoSaver;
import ru.yandex.market.tpl.common.web.util.Idempotency;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CollectDropshipTaskPhotoControllerTest extends BaseDriverApiIntTest {
    private final PhotoRepository photoRepository;

    private final TestUserHelper userHelper;
    private final AvatarnicaPhotoSaver avatarnicaPhotoSaver;

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final ObjectMapper objectMapper;

    private long taskId;

    @BeforeEach
    public void setUp() {
        User user = userHelper.findOrCreateUser(UID);
        var transport = userHelper.findOrCreateTransport();
        Run run = runGenerator.generate();
        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        this.taskId = userShift.streamCollectDropshipTasks().findFirst().get().getId();

        Mockito.when(avatarnicaPhotoSaver.saveInAvatarnica(Mockito.any()))
                .thenReturn(new AvatarincaSaveResponse("url", 0L, "imageName", 3L));
    }

    @Test
    void uploadPhoto() throws Exception {
        UUID uuid = new UUID(0x1234ABCD, 0x5678EF90);

        Map<String, PhotoDto> response1 = performUploadRequest(uuid);
        Map<String, PhotoDto> response2 = performUploadRequest(uuid);

        Assertions.assertThat(response1).isEqualTo(response2);

        List<Photo> photos = photoRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(1);
    }

    @SneakyThrows
    @Test
    void uploadMultiplePhotos() {
        UUID uuid = new UUID(0x1234ABCD, 0x5678EF90);

        String response = mockMvc.perform(multipart("/api/tasks/collect-dropship/{taskId}/photo/upload", taskId)
                .part(new MockPart("photoFile1", "1", new byte[3]))
                .part(new MockPart("photoFile2", "2", new byte[3]))
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, uuid.toString())
                .contentType(MediaType.IMAGE_JPEG)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        Map<String, PhotoDto> result = objectMapper.readValue(response, new TypeReference<Map<String, PhotoDto>>() {});
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result.containsKey("photoFile1")).isTrue();
        Assertions.assertThat(result.containsKey("photoFile2")).isTrue();
    }

    private Map<String, PhotoDto> performUploadRequest(UUID uuid) throws Exception {
        MockHttpServletRequestBuilder builder = multipart("/api/tasks/collect-dropship/{taskId}/photo/upload", taskId)
                .part(new MockPart("photoFile", "1", new byte[3]))
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.IMAGE_JPEG);

        if (uuid != null) {
            builder.header(Idempotency.IDEMPOTENCY_HEADER_KEY, uuid.toString());
        }

        String response = mockMvc.perform(builder)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, new TypeReference<Map<String, PhotoDto>>() {});
    }
}
