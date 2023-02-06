package ru.yandex.direct.web;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.direct.core.entity.freelancer.service.FreelancerClientAvatarService;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.freelancer.exception.FreelancerAvatarUploadException;
import ru.yandex.direct.web.entity.freelancer.model.FileDescription;
import ru.yandex.direct.web.entity.freelancer.model.FreelancerAvatarUploadResponse;
import ru.yandex.direct.web.entity.freelancer.service.FreelancerAvatarWebService;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.freelancer.service.validation.AvatarsDefects.unknownError;


@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerAvatarWebServiceTest {

    private static final int LARGE_FILE_SIZE = 1024 * 1024 * 11;
    private static final String UPLOAD_FILE = "new avatar";
    private static final MultipartFile CORRECT_FILE = new MockMultipartFile(UPLOAD_FILE, UPLOAD_FILE.getBytes());
    private static final String AVATAR_URL = "avatar_url";
    private static final long UPLOADED_AVATAR_ID = 10L;

    private final FreelancerClientAvatarService freelancerClientAvatarService =
            mock(FreelancerClientAvatarService.class);

    @Autowired
    private ValidationResultConversionService validationResultConversionService;

    private FreelancerAvatarWebService testedService;

    @Before
    public void setUp() {
        testedService =
                new FreelancerAvatarWebService(freelancerClientAvatarService, validationResultConversionService);
        when(freelancerClientAvatarService.saveAvatar(anyLong(), any()))
                .thenReturn(Result.successful(UPLOADED_AVATAR_ID));
        when(freelancerClientAvatarService.getUrlSize180(any())).thenReturn(AVATAR_URL);
    }

    @Test
    public void saveFile_success() {
        WebResponse webResponse = testedService.saveFile(1L, CORRECT_FILE);
        assertThat(webResponse.isSuccessful()).isTrue();
        assertSoftly(softly -> {
            FreelancerAvatarUploadResponse uploadAvatarResponse = (FreelancerAvatarUploadResponse) webResponse;
            FileDescription uploadResult = uploadAvatarResponse.getResult();
            softly.assertThat(uploadResult.getKey())
                    .describedAs("avatar id")
                    .isEqualTo(Long.toString(UPLOADED_AVATAR_ID));
            softly.assertThat(uploadResult.getUrl())
                    .describedAs("avatar url")
                    .isEqualTo(AVATAR_URL);
        });
    }

    @Test
    public void saveFile_exception_tooLarge() {
        byte[] largeFile = new byte[LARGE_FILE_SIZE];
        MultipartFile file = new MockMultipartFile(UPLOAD_FILE, largeFile);

        assertThatThrownBy(() -> testedService.saveFile(1L, file))
                .isInstanceOf(FreelancerAvatarUploadException.class).hasMessage("Too large size");
    }

    @Test
    public void saveFile_IOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        doThrow(IOException.class).when(file).getBytes();

        assertThatThrownBy(() -> testedService.saveFile(1L, file))
                .isInstanceOf(FreelancerAvatarUploadException.class)
                .hasMessage("Cannot process file")
                .hasCause(new IOException());
    }

    @Test
    public void saveFile_exception_unsuccessfulResult() {
        when(freelancerClientAvatarService.saveAvatar(anyLong(), any()))
                .thenReturn(Result.broken(ValidationResult.failed(1L, unknownError())));

        WebResponse webResponse = testedService.saveFile(1L, CORRECT_FILE);
        assertThat(webResponse.isSuccessful()).isFalse();
    }
}
