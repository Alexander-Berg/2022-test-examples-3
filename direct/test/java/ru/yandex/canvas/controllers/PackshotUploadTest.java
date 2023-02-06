package ru.yandex.canvas.controllers;

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.avatars.AvatarsPutCanvasResult;
import ru.yandex.canvas.model.direct.Privileges;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.FileStatus;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.VideoCreativeType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PackshotUploadTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StillageService stillageService;

    @Autowired
    private SessionParams sessionParams;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    private AvatarsService avatarsService;

    @Autowired
    private DirectService directService;

    static final String VALID_STILLAGE_RESPONSE = "{\n"
            + " \"id\":\"1213231\","
            + "             \"mimeType\": \"image/jpeg\",\n"
            + "             \"metadataInfo\": {\n"
            + "                 \"width\": 3840,\n"
            + "                 \"height\": 2160\n"
            + "             },\n"
            + "             \"url\": \"https://storage.mds.yandex"
            + ".net/get-bstor/42187/b8b8a1c4-b652-4641-a935-3a1c50f60719.jpeg\",\n"
            + "             \"md5Hash\": \"znJgpA6D216olZH1x56ERA==\",\n"
            + "             \"fileSize\": 1759499,\n"
            + "             \"contentGroup\": \"IMAGE\"\n"
            + "         }";

    @Test
    public void fileUploadSmokeTest() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        when(directService.getFeatures(any(), any())).thenReturn(Collections.emptySet());

        MockMultipartFile mockMultipartFile =
                new MockMultipartFile("file", "giraffe.jpg", "", "FAKE_JPG_FILE".getBytes());

        ObjectMapper mapper = new ObjectMapper();

        when(stillageService.uploadFile(anyString(), any(byte[].class))).thenReturn(mapper.readValue(VALID_STILLAGE_RESPONSE,
                StillageFileInfo.class));

        when(videoFilesRepository.save(any(VideoFiles.class))).thenAnswer((Answer<VideoFiles>) invocation -> {
            Object[] args = invocation.getArguments();
            VideoFiles record = (VideoFiles) args[0];

            record.setStatus(FileStatus.NEW);
            record.setId("1234");

            return record;
        });

        mockAvatars();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/video/files").file(mockMultipartFile)
                .param("client_id", String.valueOf(1L))
                .param("user_id", String.valueOf(2L))
                .param("type", "packshot")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(e -> System.err.println("!!! " + e.getResponse().getContentAsString()))
                // .andExpect(json().isEqual§To(expected))
                .andExpect(status().is(201));

    }

    void mockAvatars() {
        AvatarsPutCanvasResult avatarsPutCanvasResult = new AvatarsPutCanvasResult();
        AvatarsPutCanvasResult.SizesInfo sizesInfo = new AvatarsPutCanvasResult.SizesInfo();
        AvatarsPutCanvasResult.SizeInfo origInfo = new AvatarsPutCanvasResult.SizeInfo();
        origInfo.setHeight(1024);
        origInfo.setUrl("http://avatars.ru/orig.jpg");
        origInfo.setWidth(768);

        AvatarsPutCanvasResult.SizeInfo preview = new AvatarsPutCanvasResult.SizeInfo();
        preview.setWidth(360);
        preview.setHeight(720);
        preview.setUrl("http://avatars.ru/preview.jpg");

        sizesInfo.setOrig(origInfo);
        sizesInfo.setPreview480p(preview);
        sizesInfo.setThumbnail(preview);
        sizesInfo.setLargePreview(preview);
        sizesInfo.setPreview(preview);

        avatarsPutCanvasResult.setSizes(sizesInfo);

        when(avatarsService.upload("https://storage.mds.yandex.net/get-bstor/42187/b8b8a1c4-b652-4641-a935" +
                "-3a1c50f60719.jpeg"))
                .thenReturn(avatarsPutCanvasResult);
    }

}
