package ru.yandex.market.adv.content.manager.api;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.adv.content.manager.AbstractContentManagerMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Тесты на endpoint POST /v1/image.")
@MockServerSettings(ports = 12233)
class PostImageApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    PostImageApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Успешное сохранение изображения.")
    @DbUnitDataSet(
            after = "ImageApi/Post/csv/v1ImagePost_validImage_saveImage.after.csv"
    )
    @Test
    void v1ImagePost_validImage_saveImage() throws Exception {
        v1ImagePost("v1ImagePost_validImage_saveImage", status().isOk(),
                "img560x414.png", "CAROUSEL_BANNER");
    }

    @DisplayName("Успешное сохранение изображения без проверки на тип.")
    @DbUnitDataSet(
            after = "ImageApi/Post/csv/v1ImagePost_unknownType_saveIgnoreValidation.after.csv"
    )
    @Test
    void v1ImagePost_unknownType_saveIgnoreValidation() throws Exception {
        v1ImagePost("v1ImagePost_unknownType_saveIgnoreValidation", status().isOk(),
                "img270x200.png", "BANNER");
    }

    @DisplayName("Сохранение изображения недопустимой ширины и высоты возвращает ошибку.")
    @Test
    void v1ImagePost_invalidImageWidth_badRequest() throws Exception {
        v1ImagePost(status().isBadRequest(), "img270x200.png", "CAROUSEL_BANNER", "image/png")
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsString("Invalid image width: 270")))
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsString("Invalid image height: 200")))
                .andExpect(content()
                        .json(
                                loadFile("ImageApi/Post/json/response/" +
                                        "v1ImagePost_invalidImageWidth_badRequest.json")
                        )
                );

        server.verifyZeroInteractions();
    }

    @DisplayName("Сохранение изображения недопустимого content-type.")
    @Test
    void v1ImagePost_invalidContentType_badRequest() throws Exception {
        v1ImagePost(status().isBadRequest(), "img270x200.png", "CAROUSEL_BANNER", "text/plain")
                .andExpect(jsonPath("$.message")
                        .value("Unknown file format. Support image/png or image/jpeg content type."));

        server.verifyZeroInteractions();
        v1ImagePost("img270x200.png", "text/plain", "v1ImagePost_invalidContentType_badRequest");
    }

    @DisplayName("Сохранение файла с верно заданным content-type, но не являющегося изображением.")
    @Test
    void v1ImagePost_invalidFile_badRequest() throws Exception {
        v1ImagePost(status().isBadRequest(), "png.csv", "CAROUSEL_BANNER", "image/png")
                .andExpect(jsonPath("$.message")
                        .value("Unknown file format. Support image/png or image/jpeg content type."));

        server.verifyZeroInteractions();
        v1ImagePost("png.csv", "image/png", "v1ImagePost_invalidFile_badRequest");
    }

    @DisplayName("Сохранение изображения с недопустимым отношением ширины к высоте возвращает ошибку.")
    @Test
    void v1ImagePost_invalidImageWidthHeightRatio_badRequest() throws Exception {
        v1ImagePost("img295x210.png", "image/jpeg", "v1ImagePost_invalidImageWidthHeightRatio_badRequest");
    }

    private void v1ImagePost(String fileName, String contentType, String methodName) throws Exception {
        v1ImagePost(status().isBadRequest(), fileName, "CAROUSEL_BANNER", contentType)
                .andExpect(content().json(loadFile("ImageApi/Post/json/response/" + methodName + ".json")));

        server.verifyZeroInteractions();
    }

    @Nonnull
    private ResultActions v1ImagePost(ResultMatcher statusMatcher, String fileName,
                                      String type, String contentType) throws Exception {
        return mvc.perform(
                        MockMvcRequestBuilders.multipart("/v1/image?business_id=443&image_type=" + type)
                                .file(new MockMultipartFile("file", "", contentType,
                                        loadFileBinary("ImageApi/Post/img/" + fileName)))
                )
                .andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    private void v1ImagePost(String testName, ResultMatcher statusMatcher,
                             String fileName, String type) throws Exception {
        mockServerPath("POST",
                "/v1/upload",
                null,
                Map.of("userId", List.of("1513471018")),
                200,
                "ImageApi/Post/json/server/response/" + testName + ".json"
        );

        v1ImagePost(statusMatcher, fileName, type, "image/png")
                .andExpect(content().json(
                                loadFile("ImageApi/Post/json/response/" + testName + ".json")
                        )
                );

        server.verify(request()
                .withMethod("POST")
                .withPath("/v1/upload")
                .withQueryStringParameters(Map.of("userId", List.of("1513471018")))
        );
    }
}
