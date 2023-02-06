package ru.yandex.market.tpl.api.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.model.order.PhotoDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.common.util.exception.TplException;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.Photo;
import ru.yandex.market.tpl.core.domain.order.PhotoRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.external.avatarnica.model.AvatarnicaUploadResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PhotoSaverTest extends BaseApiTest {
    private final PhotoSaver photoSaver;
    private final PhotoDeleter photoDeleter;
    private final PhotoRepository photoRepository;
    private final OrderGenerateService orderGenerateService;


    private Long taskId;

    @Value("${order.max-photo-count}")
    private Integer maxPhotoCount;

    @BeforeEach
    void setUp() {
        User user = userHelper.findOrCreateUser(824125L);
        var order = orderGenerateService.createOrder();
        AtomicLong taskIdBox = new AtomicLong();
        userHelper.createShiftWithDeliveryTask(user, UserShiftStatus.SHIFT_OPEN, order, taskIdBox);
        this.taskId = taskIdBox.get();
    }

    @Test
    void saveSuccessfully() {
        byte[] imageData = new byte[1024 * 1024];
        AvatarnicaUploadResponse response = createAvatarnicaUploadResponse();
        when(avatarnicaClient.uploadImageData(eq(imageData))).thenReturn(response);
        MultipartFile file = new MockMultipartFile("name", imageData);
        Photo photo = photoSaver.save(taskId, file, new UUID(0xDEADBEEF, 0x13371337));

        assertEquals(response.getGroupId(), photo.getAvatarsGroupId());
        assertEquals(response.getImagename(), photo.getAvatarsImagename());
        assertEquals(taskId, photo.getTaskId());
        assertEquals(photo, photoRepository.findById(photo.getId()).orElseThrow());
        verify(avatarnicaClient, times(1)).uploadImageData(eq(imageData));
        verifyNoMoreInteractions(avatarnicaClient);
    }

    @Test
    void tooManyPhotos() {
        byte[] bytes = new byte[1024 * 1024];
        AvatarnicaUploadResponse response = createAvatarnicaUploadResponse();
        when(avatarnicaClient.uploadImageData(eq(bytes))).thenReturn(response);
        MultipartFile file = new MockMultipartFile("name", bytes);

        IntStream.range(0, maxPhotoCount)
                .forEach(i -> photoSaver.save(taskId, file, new UUID(0xDEADBEEF, 0x13371337 + i)));
        assertThrows(TplException.class, () -> photoSaver.save(taskId, file, new UUID(0xDEADBEEF, 0x73317331)));

        assertEquals(maxPhotoCount, photoRepository.findByTaskId(taskId).size());
        verify(avatarnicaClient, times(maxPhotoCount)).uploadImageData(eq(bytes));
        verifyNoMoreInteractions(avatarnicaClient);
    }

    @Test
    void saveSuccessfullySeveralFiles() {
        AvatarnicaUploadResponse response = createAvatarnicaUploadResponse();
        when(avatarnicaClient.uploadImageData(any())).thenReturn(response);
        Map<String, MultipartFile> files = Map.of(
                "file1", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file2", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file3", new MockMultipartFile("name", new byte[1024 * 1024])
        );

        var result = photoSaver.savePhotos(taskId, files, new UUID(0xDEADBEEF, 0x13371337), false);

        assertThat(result.keySet()).containsAll(files.keySet());
        var photos = photoRepository.findByTaskId(taskId);
        assertThat(photos).hasSize(3);
        verify(avatarnicaClient, times(3)).uploadImageData(any());
        verifyNoMoreInteractions(avatarnicaClient);
    }

    @Test
    void tooManyPhotosInOneTask() {
        AvatarnicaUploadResponse response = createAvatarnicaUploadResponse();
        when(avatarnicaClient.uploadImageData(any())).thenReturn(response);
        Map<String, MultipartFile> files = Map.of(
                "file1", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file2", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file3", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file4", new MockMultipartFile("name", new byte[1024 * 1024])
        );

        assertThrows(TplException.class, () -> photoSaver.savePhotos(taskId, files, new UUID(0xDEADBEEF, 0x13371337),
                false));
        var photos = photoRepository.findByTaskId(taskId);
        assertThat(photos).hasSize(0);
        verify(avatarnicaClient, times(0)).uploadImageData(any());
        verifyNoMoreInteractions(avatarnicaClient);
    }

    @Test
    void tooManyPhotosInOneTaskTwoRequests() {
        AvatarnicaUploadResponse response = createAvatarnicaUploadResponse();
        when(avatarnicaClient.uploadImageData(any())).thenReturn(response);
        Map<String, MultipartFile> files = Map.of(
                "file1", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file2", new MockMultipartFile("name", new byte[1024 * 1024])
        );

        photoSaver.savePhotos(taskId, files, new UUID(0xDEADBEAF, 0x13371337), false);
        assertThrows(TplException.class, () -> photoSaver.savePhotos(taskId, files, new UUID(0xDEADBEEF, 0x13371337), false));
        var photos = photoRepository.findByTaskId(taskId);
        assertThat(photos).hasSize(2);
        verify(avatarnicaClient, times(2)).uploadImageData(any());
        verifyNoMoreInteractions(avatarnicaClient);
    }

    @Test
    void saveSuccessfullyAfterDeletingPhotos() {
        AvatarnicaUploadResponse response = createAvatarnicaUploadResponse();
        when(avatarnicaClient.uploadImageData(any())).thenReturn(response);
        Map<String, MultipartFile> files = Map.of(
                "file1", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file2", new MockMultipartFile("name", new byte[1024 * 1024]),
                "file3", new MockMultipartFile("name", new byte[1024 * 1024])
        );

        var photoDtos = photoSaver.savePhotos(taskId, files, new UUID(0xDEADBEEF, 0x13371337), false);
        photoDtos.values().stream().map(PhotoDto::getId)
                .forEach(photoDeleter::delete);
        photoSaver.savePhotos(taskId, files, new UUID(0xDEADBEEF, 0x13371337), false);

        var photos = photoRepository.findByTaskId(taskId);
        assertThat(photos).hasSize(3);
        verify(avatarnicaClient, times(3)).delete(any(), any());
        verify(avatarnicaClient, times(6)).uploadImageData(any());
        verifyNoMoreInteractions(avatarnicaClient);
    }
}
