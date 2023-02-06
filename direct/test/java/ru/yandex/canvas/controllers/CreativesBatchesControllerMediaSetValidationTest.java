package ru.yandex.canvas.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.File;
import ru.yandex.canvas.model.elements.ElementType;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.CreativesService;
import ru.yandex.canvas.service.FileService;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checking media set preset-based validators:
 * <li> {@link ru.yandex.canvas.model.validation.presetbased.creative.AllowedDomainsValidator}
 * <li> {@link ru.yandex.canvas.model.validation.presetbased.creative.CroppedImageValidator}
 * <li> {@link ru.yandex.canvas.model.validation.presetbased.creative.ImageFileSizeValidator}
 * <li> {@link ru.yandex.canvas.model.validation.presetbased.creative.ImageSizeValidator}
 * <li> {@link ru.yandex.canvas.model.validation.presetbased.creative.ValidImageValidator}
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativesBatchesControllerMediaSetValidationTest {
    private final String fileId = "5bd19fb4159036001aebf91e";
    private final String croppedFileId = "5bd19ff40112fc001a4ee848";

    private final int maxValidFileSize = 512000;
    private final int maxValidLogoFileSize = 40960;
    private final int maxValidLogoHeight = 300;
    private final int maxValidLogoWidth = 300;

    private final String errorMsgSizeLimit = String.format(
            "Logo file should be less than %d bytes and fit %dx%d pixels",
            maxValidLogoFileSize, maxValidLogoWidth, maxValidLogoHeight);

    @Autowired
    private AvatarsService avatarsService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private CreativesService creativesService;

    @Before
    public void initAvatarsService() {
        when(avatarsService.getReadServiceHost()).thenReturn("avatars.mds.yandex.net");
    }

    // If this test fails, then it could concleal lack of validion on other tests
    @Test
    public void okBatchAndFilesPassesValidation() throws Exception {
        File okLogoFile = minValidLogoFile(fileId);
        File okImageFile = minValidImageFile(croppedFileId);
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(okLogoFile));
        when(fileService.getByIdInternal(croppedFileId)).thenReturn(Optional.of(okImageFile));

        JSONObject batch = batchWithImageAndLogo();

        createBatch(batch).andExpect(status().isCreated());
        verify(creativesService).createBatch(any(), eq(MockedTest.CLIENT_ID));
    }

    @Test
    public void testCreativeDataTooBigCroppedImageFileSize() throws Exception {
        File tooBigFile = minValidImageFile(croppedFileId);
        tooBigFile.getStillageFileInfo().setFileSize(maxValidFileSize + 1);
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(minValidImageFile(fileId)));
        when(fileService.getByIdInternal(croppedFileId)).thenReturn(Optional.of(tooBigFile));

        JSONObject batch = batchWithImage();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties", hasEntry(is("items[0].data.elements[0]"),
                        contains(is("Image is too big")))));
    }

    @Test
    public void testCreativeDataTooBigLogoFileSize() throws Exception {
        File tooBigFile = minValidLogoFile(fileId);
        tooBigFile.getStillageFileInfo().setFileSize(maxValidLogoFileSize + 1);
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(tooBigFile));

        JSONObject batch = batchWithLogo();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties", hasEntry(is("items[0].data.elements[0]"),
                        contains(is(errorMsgSizeLimit)))));
    }

    @Test
    public void testCreativeDataTooWideLogo() throws Exception {
        File tooWideFile = minValidLogoFile(fileId);
        tooWideFile.getStillageFileInfo().getMetadataInfo().put("width", maxValidLogoWidth + 1);
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(tooWideFile));

        JSONObject batch = batchWithLogo();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties", hasEntry(is("items[0].data.elements[0]"),
                        contains(is(errorMsgSizeLimit)))));
    }

    @Test
    public void testCreativeDataTooTallLogo() throws Exception {
        File tooTallFile = minValidLogoFile(fileId);
        tooTallFile.getStillageFileInfo().getMetadataInfo().put("height", maxValidLogoHeight + 1);
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(tooTallFile));

        JSONObject batch = batchWithLogo();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties", hasEntry(is("items[0].data.elements[0]"),
                        contains(is(errorMsgSizeLimit)))));
    }

    @Test
    public void testCreativeDataTallWideBigLogo() throws Exception {
        File file = minValidLogoFile(fileId);
        Map<String, Object> metadataInfo = file.getStillageFileInfo().getMetadataInfo();
        metadataInfo.put("height", maxValidLogoHeight + 1);
        metadataInfo.put("width", maxValidLogoWidth + 1);
        file.getStillageFileInfo().setFileSize(maxValidLogoFileSize + 1);
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(file));

        JSONObject batch = batchWithLogo(11);

        createBatch(batch).andExpect(status().isCreated());
        verify(creativesService).createBatch(any(), eq(MockedTest.CLIENT_ID));
    }

    @Test
    public void testGeoPinCreativeDataValidLogoSize() throws Exception {
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(minValidLogoFile(fileId)));

        JSONObject batch = batchWithLogo(11);

        createBatch(batch).andExpect(status().isCreated());
        verify(creativesService).createBatch(any(), eq(MockedTest.CLIENT_ID));
    }

    @Test
    public void testCreativeDataNotCroppedImage() throws Exception {
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(minValidImageFile(fileId)));

        JSONObject batch = batchWithNonCroppedImage();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0]"),
                                contains(is("Image is not cropped for 300x250")))));
    }

    @Test
    public void testCreativeDataImageDisallowedDomain() throws Exception {
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(minValidImageFile(fileId)));

        JSONObject batch = batchWithDisallowedImageUrl();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0]"),
                                hasItem(is("Error adding image, please try again")))));
    }

    @Test
    public void testCreativeDataLogoDisallowedDomain() throws Exception {
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(minValidLogoFile(fileId)));

        JSONObject batch = batchWithDisallowedLogoUrl();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0]"),
                                contains(is("Error adding image, please try again")))));
    }

    @Test
    public void testCreativeDataLogoInvalidImage() throws Exception {
        when(fileService.getByIdInternal(fileId)).thenReturn(Optional.of(minValidLogoFile(fileId)));

        JSONObject batch = batchWithNullImage();

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0]"),
                                contains(is("Upload an image")))));
    }

    @NotNull
    private File minValidImageFile(String fileId) {
        Map<String, Object> metadataInfo = new HashMap<>();
        metadataInfo.put("width", 300);
        metadataInfo.put("height", 250);

        StillageFileInfo stillageFileInfo = new StillageFileInfo();
        stillageFileInfo.setId("123");
        stillageFileInfo.setFileSize(maxValidFileSize);
        stillageFileInfo.setMetadataInfo(metadataInfo);

        File file = new File();
        file.setId(fileId);
        file.setStillageFileInfo(stillageFileInfo);
        return file;
    }

    @NotNull
    private File minValidLogoFile(String fileId) {
        File file = minValidImageFile(fileId);
        file.getStillageFileInfo().setFileSize(maxValidLogoFileSize);
        return file;
    }

    private ResultActions createAndExpectBadRequest(JSONObject batch) throws Exception {
        return createBatch(batch)
                .andExpect(e -> System.err.println("!!!!" + e.getResponse().getContentType()))
                .andExpect(e -> System.err.println("!!!!" + e.getResponse().getContentAsString()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    private ResultActions createBatch(JSONObject batch) throws Exception {
        return this.mockMvc.perform(post("/creatives-batches").content(batch.toString())
                .param("client_id", String.valueOf(MockedTest.CLIENT_ID))
                .param("user_id", String.valueOf(MockedTest.USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));
    }

    private JSONObject batchWithLogo() {
        return batchWithLogo(1);
    }

    private JSONObject batchWithLogo(int presetId) {
        JSONObject batch = minValidBatch(presetId);
        addLogoElementToBatch(batch);
        addLogoMediaSetToBatch(batch);
        return batch;
    }

    private JSONObject batchWithImage() {
        JSONObject batch = minValidBatch();
        addImageMediaSetToBatch(batch);
        addImageElementToBatch(batch);
        addButtonElementToBatch(batch);
        return batch;
    }

    private JSONObject batchWithImageAndLogo() {
        JSONObject batch = minValidBatch();
        addImageAndLogoMediaSetToBatch(batch);
        addImageElementToBatch(batch);
        addLogoElementToBatch(batch);
        addButtonElementToBatch(batch);
        return batch;
    }

    private JSONObject batchWithNonCroppedImage() {
        JSONObject batch = batchWithImage();
        var image = batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .getJSONObject("mediaSets").getJSONObject("image").getJSONArray("items")
                .getJSONObject(0).getJSONArray("items").getJSONObject(0);
        image.put("croppedFileId", (String) null);
        return batch;
    }

    private JSONObject batchWithDisallowedImageUrl() {
        JSONObject batch = batchWithImage();
        var image = batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .getJSONObject("mediaSets").getJSONObject("image").getJSONArray("items")
                .getJSONObject(0).getJSONArray("items").getJSONObject(0);
        image.put("url", "https://bad-domain.ru/get-canvas/168188/2a00000166aad8d02b8b052381a09fd957b0/cropSource");
        return batch;
    }

    private JSONObject batchWithDisallowedLogoUrl() {
        JSONObject batch = batchWithLogo();
        var logo = batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .getJSONObject("mediaSets").getJSONObject("logo").getJSONArray("items")
                .getJSONObject(0).getJSONArray("items").getJSONObject(0);
        logo.put("url", "https://bad-domain.ru/get-canvas/168188/2a00000166aad8d02b8b052381a09fd957b0");
        return batch;
    }

    private JSONObject batchWithNullImage() {
        JSONObject batch = batchWithLogo();
        var image = batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .getJSONObject("mediaSets").getJSONObject("logo").getJSONArray("items")
                .getJSONObject(0).getJSONArray("items").getJSONObject(0);
        image.put("fileId", (String) null);
        return batch;
    }

    private JSONObject minValidBatch() {
        return minValidBatch(1);
    }

    private JSONObject minValidBatch(int presetId) {
        return new JSONObject()
                .put("name", "test_batch")
                .put("items", new JSONArray()
                        .put(new JSONObject()
                                .put("presetId", presetId)
                                .put("data", new JSONObject()
                                        .put("width", 300)
                                        .put("height", 250)
                                        .put("bundle", new JSONObject()
                                                .put("name", "media-banner_theme_divided")
                                                .put("version", 1))
                                        .put("options", new JSONObject()
                                                .put("borderColor", "#FF0000") // RED
                                                .put("backgroundColor", "#0000FF")) // BLUE
                                        .put("elements", new JSONArray())
                                        .put("mediaSets", new JSONObject()))));
    }

    private JSONObject element(final String type, final Map<String, String> options) {
        return new JSONObject()
                .put("type", type)
                .put("options", new JSONObject(options));
    }

    private void addImageMediaSetToBatch(JSONObject batch) {
        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .put("mediaSets", new JSONObject().put("image",
                        new JSONObject().put("items", new JSONArray()
                                .put(imageMediaSetItem()))));
    }

    private JSONObject imageMediaSetItem() {
        return new JSONObject().put("type", "image")
                .put("items", new JSONArray().put(new JSONObject()
                        .put("fileId", fileId)
                        .put("croppedFileId", croppedFileId)
                        .put("width", 300)
                        .put("height", 250)
                        .put("alias", "normal")
                        .put("url",
                                "https://avatars.mds.yandex"
                                        + ".net/get-canvas/168188/2a00000166aad8d02b8b052381a09fd957b0/cropSource")
                ));
    }

    private void addLogoMediaSetToBatch(JSONObject batch) {
        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .put("mediaSets", new JSONObject()
                        .put("logo", new JSONObject().put("items", new JSONArray().put(logoMediaSetItem()))
                        ));
    }

    private JSONObject logoMediaSetItem() {
        return new JSONObject().put("type", "logo")
                .put("items", new JSONArray().put(new JSONObject()
                        .put("fileId", fileId)
                        .put("width", 100)
                        .put("height", 200)
                        .put("alias", "normal")
                        .put("url",
                                "https://avatars.mds.yandex.net/get-canvas/168188/2a00000166aad8d02b8b052381a09fd957b0")
                ));
    }

    private void addImageAndLogoMediaSetToBatch(JSONObject batch) {
        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .put("mediaSets", new JSONObject()
                        .put("image",
                                new JSONObject().put("items", new JSONArray()
                                        .put(imageMediaSetItem())))
                        .put("logo",
                                new JSONObject().put("items", new JSONArray()
                                        .put(logoMediaSetItem()))));
    }

    private void addLogoElementToBatch(JSONObject batch) {
        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data").getJSONArray("elements")
                .put(new JSONObject()
                        .put("type", "logo")
                        .put("mediaSet", "logo")
                        .put("options", new JSONObject(ImmutableMap.of(
                                "width", 200,
                                "height", 100))));
    }

    private void addImageElementToBatch(JSONObject batch) {
        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data").getJSONArray("elements")
                .put(new JSONObject()
                        .put("type", "image")
                        .put("mediaSet", "image")
                        .put("options", new JSONObject(ImmutableMap.of(
                                "width", 300,
                                "height", 250))));
    }

    private void addButtonElementToBatch(JSONObject batch) {
        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data").getJSONArray("elements")
                .put(element(ElementType.BUTTON, ImmutableMap.of("content", "magic_button", "color", "#DEADAB")));
    }
}
