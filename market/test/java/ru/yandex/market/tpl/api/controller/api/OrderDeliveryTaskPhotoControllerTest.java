package ru.yandex.market.tpl.api.controller.api;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.model.order.NoPhotoRequest;
import ru.yandex.market.tpl.api.model.order.PhotoDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.Photo;
import ru.yandex.market.tpl.core.domain.order.PhotoRepository;
import ru.yandex.market.tpl.core.domain.task.TaskError;
import ru.yandex.market.tpl.core.domain.task.TaskErrorRepository;
import ru.yandex.market.tpl.core.domain.task.TaskErrorType;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.external.avatarnica.model.AvatarnicaUploadResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderDeliveryTaskPhotoControllerTest extends BaseApiTest {
    private final PhotoRepository photoRepository;
    private final OrderGenerateService orderGenerateService;
    private final OrderDeliveryTaskPhotoController orderDeliveryTaskPhotoController;
    private final TaskErrorRepository taskErrorRepository;

    private Long taskId;
    private User user;


    @BeforeEach
    void setUp() {
        this.user = userHelper.findOrCreateUser(824125L);
        var order = orderGenerateService.createOrder();
        AtomicLong taskIdBox = new AtomicLong();
        userHelper.createShiftWithDeliveryTask(user, UserShiftStatus.SHIFT_OPEN, order, taskIdBox);
        this.taskId = taskIdBox.get();
    }

    @Test
    void delete() {
        byte[] imageData = new byte[1024 * 1024];
        AvatarnicaUploadResponse response = createAvatarnicaUploadResponse();
        when(avatarnicaClient.uploadImageData(eq(imageData))).thenReturn(response);
        MultipartFile file = new MockMultipartFile("name", imageData);

        PhotoDto photoDto = orderDeliveryTaskPhotoController.upload(taskId, file, user, new UUID(0xDEADBEAF, 0x13371337));
        Photo savedPhoto = photoRepository.findByIdOrThrow(photoDto.getId());
        assertNotNull(savedPhoto);
        verify(avatarnicaClient, times(1)).uploadImageData(eq(imageData));

        orderDeliveryTaskPhotoController.delete(taskId, photoDto.getId(), user);
        assertTrue(photoRepository.findById(photoDto.getId()).isEmpty());
        assertEquals(List.of(), photoRepository.findByTaskId(taskId));
        verify(avatarnicaClient, times(1))
                .delete(eq(savedPhoto.getAvatarsGroupId()), eq(savedPhoto.getAvatarsImagename()));
        verifyNoMoreInteractions(avatarnicaClient);
    }

    @Test
    void noPhoto() {
        NoPhotoRequest noPhotoRequest = new NoPhotoRequest();
        String userComment = "Some-comment";
        noPhotoRequest.setComment(userComment);
        TaskError response = orderDeliveryTaskPhotoController.noPhoto(taskId, noPhotoRequest, user, new UUID(0xDEADBEAF, 0x13371337));
        Assertions.assertThat(response).isNotNull();

        TaskError taskError = taskErrorRepository.findLastNoPhotoError(taskId).orElseThrow();
        assertEquals(TaskErrorType.NO_PHOTO, taskError.getErrorType());
        assertEquals(userComment, taskError.getUserComment());
        verifyNoInteractions(avatarnicaClient);
    }

}
