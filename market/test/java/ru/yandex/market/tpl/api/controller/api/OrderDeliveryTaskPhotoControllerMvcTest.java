package ru.yandex.market.tpl.api.controller.api;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.facade.OrderDeliveryTaskPhotoFacade;
import ru.yandex.market.tpl.api.service.PhotoDeleter;
import ru.yandex.market.tpl.api.service.PhotoSaver;
import ru.yandex.market.tpl.api.service.UserTaskOwnershipVerifier;
import ru.yandex.market.tpl.core.domain.order.Photo;
import ru.yandex.market.tpl.core.service.order.TaskErrorSaver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@WebLayerTest(value = {OrderDeliveryTaskPhotoController.class})
class OrderDeliveryTaskPhotoControllerMvcTest extends BaseShallowTest {
    private final Long orderId = 1L;
    @Autowired
    protected MockMvc mockMvc;
    @MockBean
    private PhotoSaver photoSaver;
    @MockBean
    private TaskErrorSaver taskErrorSaver;
    @MockBean
    private PhotoDeleter photoDeleter;
    @MockBean
    private UserTaskOwnershipVerifier userTaskOwnershipVerifier;
    @MockBean
    private OrderDeliveryTaskPhotoFacade orderDeliveryTaskPhotoFacade;

    @Test
    @SneakyThrows
    void uploadSuccessful() {
        byte[] data = new byte[3];
        String photoPath = "https://some/path";
        Photo photo = Photo.builder()
                .photoUrl(photoPath)
                .build();
        when(photoSaver.save(eq(orderId), any(), any())).thenReturn(photo);
        var request = MockMvcRequestBuilders.multipart(sf("/api/tasks/order-delivery/{}/photo/upload", orderId))
                .file("photoFile", data)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.IMAGE_JPEG);
        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @SneakyThrows
    void uploadSuccessfulSeveralFiles() {
        var files = List.of(
                new MockPart("photoFiles1", "1", new byte[3]),
                new MockPart("photoFiles2", "2", new byte[3]),
                new MockPart("photoFiles3", "3", new byte[3])
        );
        when(photoSaver.savePhotos(eq(orderId), any(), any(), eq(false))).thenReturn(Map.of());
        var request = MockMvcRequestBuilders.multipart(sf("/api/tasks/order-delivery/{}/photo/upload/v2", orderId));
        files.forEach(request::part);
        mockMvc.perform(
                request.header(AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.IMAGE_JPEG)
        )
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @SneakyThrows
    void batchUploadSuccessfulSeveralFiles() {
        byte[] tasksAsJson = "[123,456]".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile jsonPart = new MockMultipartFile("tasks", "tasks", "application/json", tasksAsJson);

        var request = MockMvcRequestBuilders.multipart("/api/tasks/order-delivery/photo/upload-batch")
                .file("files", new byte[3])
                .file("files", new byte[4])
                .file(jsonPart)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE);
        mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful());
    }
}
