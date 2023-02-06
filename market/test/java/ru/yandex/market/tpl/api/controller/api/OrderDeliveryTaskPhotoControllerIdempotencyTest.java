package ru.yandex.market.tpl.api.controller.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.NoPhotoRequest;
import ru.yandex.market.tpl.api.model.order.PhotoDto;
import ru.yandex.market.tpl.common.web.util.Idempotency;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.Photo;
import ru.yandex.market.tpl.core.domain.order.PhotoRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.TaskError;
import ru.yandex.market.tpl.core.domain.task.TaskErrorRepository;
import ru.yandex.market.tpl.core.external.avatarnica.AvatarnicaClient;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderDeliveryTaskPhotoControllerIdempotencyTest extends BaseApiIntTest {
    @Autowired
    protected TestUserHelper userHelper;
    @Autowired
    private OrderGenerateService orderGenerateService;
    @Autowired
    private AvatarnicaClient avatarnicaClient;
    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private TaskErrorRepository taskErrorRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private long taskId;
    private long taskId2;

    @BeforeEach
    public void setUp() {
        User user = userHelper.findOrCreateUser(UID);

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);

        var order = orderGenerateService.createOrder();
        this.taskId = userHelper.addDeliveryTaskToShift(user, userShift, order).getId();

        var order2 = orderGenerateService.createOrder();
        this.taskId2 = userHelper.addDeliveryTaskToShift(user, userShift, order2).getId();

        Mockito.when(avatarnicaClient.uploadImageData(Mockito.any()).getMeta().getOrigSizeBytes()).thenReturn(3L);
    }

    @Test
    void uploadPhoto() throws Exception {
        UUID uuid = new UUID(0x1234ABCD, 0x5678EF90);

        PhotoDto response1 = performUploadRequest(uuid);
        PhotoDto response2 = performUploadRequest(uuid);

        Assertions.assertThat(response1).isEqualTo(response2);

        List<Photo> photos = photoRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(1);
    }

    @Test
    void uploadPhotoWithoutIdempotencyKey() throws Exception {
        UUID uuid = null;

        PhotoDto response1 = performUploadRequest(uuid);
        PhotoDto response2 = performUploadRequest(uuid);

        Assertions.assertThat(response1).isNotEqualTo(response2);

        List<Photo> photos = photoRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(2);
    }

    @Test
    void noPhoto() throws Exception {
        UUID uuid = new UUID(0x1234ABCD, 0x5678EF90);

        TaskError response1 = performNoPhotoRequest(uuid);
        TaskError response2 = performNoPhotoRequest(uuid);

        Assertions.assertThat(response1).isEqualTo(response2);

        List<TaskError> photos = taskErrorRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(1);
    }

    @Test
    void noPhotoWithoutIdempotencyKey() throws Exception {
        UUID uuid = null;

        TaskError response1 = performNoPhotoRequest(uuid);
        TaskError response2 = performNoPhotoRequest(uuid);

        Assertions.assertThat(response1).isNotEqualTo(response2);

        List<TaskError> photos = taskErrorRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(2);
    }


    @Test
    void noPhotoBatch() throws Exception {
        UUID uuid = new UUID(0x1234ABCD, 0x5678EF90);

        Map<Long, TaskError> response1 = performNoPhotoBatchRequest(uuid);
        Map<Long, TaskError> response2 = performNoPhotoBatchRequest(uuid);

        Assertions.assertThat(response1).isEqualTo(response2);

        List<TaskError> errors = taskErrorRepository.findByTaskId(taskId);
        Assertions.assertThat(errors).hasSize(1);
        List<TaskError> errors2 = taskErrorRepository.findByTaskId(taskId2);
        Assertions.assertThat(errors2).hasSize(1);
    }

    @Test
    void noPhotoBatchWithoutIdempotencyKey() throws Exception {
        UUID uuid = null;

        Map<Long, TaskError> response1 = performNoPhotoBatchRequest(uuid);
        Map<Long, TaskError> response2 = performNoPhotoBatchRequest(uuid);

        Assertions.assertThat(response1).isNotEqualTo(response2);

        List<TaskError> photos = taskErrorRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(2);
        List<TaskError> photos2 = taskErrorRepository.findByTaskId(taskId2);
        Assertions.assertThat(photos2).hasSize(2);
    }

    @Test
    void uploadPhotos() throws Exception {
        UUID uuid = new UUID(0x1234ABCD, 0x5678EF90);

        var files = List.of(
                new MockPart("photoFiles1", "1", new byte[3]),
                new MockPart("photoFiles2", "2", new byte[3]),
                new MockPart("photoFiles3", "3", new byte[3])
        );

        Map<String, PhotoDto> response1 = performUploadV2Request(uuid, files);
        Map<String, PhotoDto> response2 = performUploadV2Request(uuid, files);

        Assertions.assertThat(response1).isEqualTo(response2);

        List<Photo> photos = photoRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(3);
    }

    @Test
    void uploadPhotosWithoutIdempotencyKey() throws Exception {
        UUID uuid = null;

        var files = List.of(
                new MockPart("photoFiles1", "1", new byte[3])
        );

        Map<String, PhotoDto> response1 = performUploadV2Request(uuid, files);
        Map<String, PhotoDto> response2 = performUploadV2Request(uuid, files);

        Assertions.assertThat(response1).isNotEqualTo(response2);

        List<Photo> photos = photoRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(2);
    }

    @Test
    void batchUploadPhotos() throws Exception {
        UUID uuid = new UUID(0x1234ABCD, 0x5678EF90);

        var files = List.of(
                new MockMultipartFile("files", "1", MediaType.IMAGE_JPEG_VALUE, new byte[3])
        );

        Map<String, List<PhotoDto>> response1 = performUploadBatchRequest(uuid, files);
        Map<String, List<PhotoDto>> response2 = performUploadBatchRequest(uuid, files);

        Assertions.assertThat(response1).isEqualTo(response2);

        List<Photo> photos = photoRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(1);
        List<Photo> photos2 = photoRepository.findByTaskId(taskId2);
        Assertions.assertThat(photos2).hasSize(1);
    }

    @Test
    void batchUploadPhotosNullIdempotencyKey() throws Exception {
        UUID uuid = null;

        var files = List.of(
                new MockMultipartFile("files", "1", MediaType.IMAGE_JPEG_VALUE, new byte[3])
        );

        Map<String, List<PhotoDto>> response1 = performUploadBatchRequest(uuid, files);
        Map<String, List<PhotoDto>> response2 = performUploadBatchRequest(uuid, files);

        Assertions.assertThat(response1).isNotEqualTo(response2);

        List<Photo> photos = photoRepository.findByTaskId(taskId);
        Assertions.assertThat(photos).hasSize(2);
        List<Photo> photos2 = photoRepository.findByTaskId(taskId2);
        Assertions.assertThat(photos2).hasSize(2);
    }

    TaskError performNoPhotoRequest(@Nullable UUID uuid) throws Exception {
        NoPhotoRequest requestBean = new NoPhotoRequest();
        requestBean.setTaskIds(List.of(taskId));
        requestBean.setComment("Comment");

        MockHttpServletRequestBuilder builder = post("/api/tasks/order-delivery/{taskId}/no-photo", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBean));

        if (uuid != null) {
            mockMvc.perform(builder.header(Idempotency.IDEMPOTENCY_HEADER_KEY, uuid.toString()));
        }

        String response = mockMvc.perform(builder)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, TaskError.class);
    }

    Map<Long, TaskError> performNoPhotoBatchRequest(@Nullable UUID uuid) throws Exception {
        NoPhotoRequest requestBean = new NoPhotoRequest();
        requestBean.setTaskIds(List.of(taskId, taskId2));
        requestBean.setComment("Comment");

        MockHttpServletRequestBuilder builder = post("/api/tasks/order-delivery/no-photo-batch")
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBean));

        if (uuid != null) {
            builder.header(Idempotency.IDEMPOTENCY_HEADER_KEY, uuid.toString());
        }

        String response = mockMvc.perform(builder)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, new TypeReference<Map<Long, TaskError>>() {
        });
    }

    PhotoDto performUploadRequest(UUID uuid) throws Exception {
        MockHttpServletRequestBuilder builder = multipart("/api/tasks/order-delivery/{taskId}/photo/upload", taskId)
                .part(new MockPart("photoFile", "1", new byte[3]))
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.IMAGE_JPEG);

        if (uuid != null) {
            builder.header(Idempotency.IDEMPOTENCY_HEADER_KEY, uuid.toString());
        }

        String response = mockMvc.perform(builder)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, PhotoDto.class);
    }


    Map<String, PhotoDto> performUploadV2Request(@Nullable UUID uuid, List<MockPart> files) throws Exception {
        var request = multipart("/api/tasks/order-delivery/{taskId}/photo/upload/v2", taskId);
        files.forEach(request::part);

        MockHttpServletRequestBuilder builder = request
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.IMAGE_JPEG);

        if (uuid != null) {
            builder.header(Idempotency.IDEMPOTENCY_HEADER_KEY, uuid.toString());
        }

        String response = mockMvc.perform(builder)
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, new TypeReference<Map<String, PhotoDto>>() {
        });
    }

    Map<String, List<PhotoDto>> performUploadBatchRequest(UUID uuid, List<MockMultipartFile> files) throws Exception {
        byte[] tasksAsJson = objectMapper.writeValueAsBytes(List.of(taskId, taskId2));
        MockMultipartFile jsonPart = new MockMultipartFile("tasks", "tasks", "application/json", tasksAsJson);

        MockMultipartHttpServletRequestBuilder builder = multipart("/api/tasks/order-delivery/photo/upload-batch", taskId)
                .file(jsonPart);
        files.forEach(builder::file);

        builder.header(AUTHORIZATION, AUTH_HEADER_VALUE);

        if (uuid != null) {
            builder.header(Idempotency.IDEMPOTENCY_HEADER_KEY, uuid.toString());
        }

        String response = mockMvc.perform(builder)
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, new TypeReference<Map<String, List<PhotoDto>>>() {
        });
    }

}
