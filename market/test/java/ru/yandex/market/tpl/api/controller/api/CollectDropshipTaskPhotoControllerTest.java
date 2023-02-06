package ru.yandex.market.tpl.api.controller.api;

import java.time.LocalDate;
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

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.PhotoDto;
import ru.yandex.market.tpl.common.web.util.Idempotency;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Photo;
import ru.yandex.market.tpl.core.domain.order.PhotoRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.external.avatarnica.AvatarnicaClient;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CollectDropshipTaskPhotoControllerTest extends BaseApiIntTest {
    private final PhotoRepository photoRepository;

    private final TestUserHelper userHelper;
    private final AvatarnicaClient avatarnicaClient;

    private final MovementGenerator movementGenerator;
    private final TestDataFactory testDataFactory;
    private final ObjectMapper objectMapper;

    private long taskId;
    private long taskId2;

    @BeforeEach
    public void setUp() {
        User user = userHelper.findOrCreateUser(UID);

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);

        Movement movement = movementGenerator.generate(MovementCommand.Create.builder().build());
        this.taskId = testDataFactory.addDropshipTask(userShift.getId(), movement).getId();

        Movement movement2 = movementGenerator.generate(MovementCommand.Create.builder().build());
        this.taskId2 = testDataFactory.addDropshipTask(userShift.getId(), movement2).getId();

        Mockito.when(avatarnicaClient.uploadImageData(Mockito.any()).getMeta().getOrigSizeBytes()).thenReturn(3L);
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
