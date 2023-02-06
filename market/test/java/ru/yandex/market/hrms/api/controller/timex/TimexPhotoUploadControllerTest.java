package ru.yandex.market.hrms.api.controller.timex;

import java.time.Instant;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.s3.S3Service;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;
import ru.yandex.market.hrms.core.service.timex.client.TimexSessionUpdater;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@DbUnitDataSet(before = "TimexPhotoUploadControllerTest.before.csv")
public class TimexPhotoUploadControllerTest extends AbstractApiTest {

    @SpyBean
    @MockBean
    private TimexApiFacadeNew timexApiFacadeNew;

    @MockBean
    private TimexSessionUpdater timexSessionUpdater;

    @MockBean
    private S3Service s3Service;

    @BeforeEach
    public void init() {
        mockClock(Instant.parse("2022-07-05T12:00:00Z"));
        doNothing().when(timexSessionUpdater).updateSession();
        when(timexApiFacadeNew.setPhotoToTimex(any(), any())).thenReturn(Optional.empty());
        doNothing().when(s3Service).putObject(any(), any(), any());
    }

    @AfterEach
    public void down() {
        clearInvocations(timexApiFacadeNew, timexSessionUpdater);
    }

    @Test
    @DbUnitDataSet(after = "TimexPhotoUploadControllerTest.staff.after.csv")
    public void shouldUploadPhotoForStaff() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                loadFileAsBytes("timex.jpg")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/upload-staff-photo-to-timex")
                        .file(file)
                        .queryParam("employeeId", "123")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(after = "TimexPhotoUploadControllerTest.old_outstaff.after.csv")
    public void shouldUploadPhotoForOldOutstaff() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                loadFileAsBytes("timex.jpg")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/upload-outstaff-photo-to-timex")
                        .file(file)
                        .queryParam("outstaffId", "567")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TimexPhotoUploadControllerTest.environment.before.csv")
    @DbUnitDataSet(after = "TimexPhotoUploadControllerTest.new_outstaff.after.csv")
    public void shouldUploadPhotoForNewOutstaff() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                loadFileAsBytes("timex.jpg")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/upload-outstaff-photo-to-timex")
                        .file(file)
                        .queryParam("outstaffId", "1")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());
    }
}
