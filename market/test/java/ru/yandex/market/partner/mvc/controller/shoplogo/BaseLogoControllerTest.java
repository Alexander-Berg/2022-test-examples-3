package ru.yandex.market.partner.mvc.controller.shoplogo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.core.avatars.AvatarsClient;
import ru.yandex.market.core.avatars.model.AvatarsImageDTO;
import ru.yandex.market.core.avatars.model.MetaDTO;
import ru.yandex.market.core.avatars.model.ThumbnailDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Базовый класс для тестов загрузки логотипа.
 *
 * @author Vadim Lyalin
 */
public abstract class BaseLogoControllerTest extends FunctionalTest {
    public static final String TEST_DIR = "ru/yandex/market/partner/mvc/controller/shoplogo/images/";

    protected static final int HEIGHT = 14;
    protected static final int WIDTH = 100;

    protected static final int GROUP_ID = 123;
    protected static final String IMAGE_NAME = "imagename";
    protected static final String CORRECT_PNG_LOGO = "correct_logo.png";

    @Autowired
    protected AvatarsClient avatarsClient;

    protected ResponseEntity<String> doMultipartFileRequest(String url, String fileName) {
        var entity = FunctionalTestHelper.createMultipartHttpEntity(
                "file",
                new ClassPathResource(TEST_DIR + fileName),
                (params) -> params.add("type", String.valueOf(PartnerApplicationDocumentType.OTHER.getId()))
        );
        return FunctionalTestHelper.post(url, entity);
    }

    protected AvatarsImageDTO mockAvatarsResponse(boolean isTemporary, String format, int height, int width) {
        MetaDTO meta = new MetaDTO(false, format, 10);
        Map<String, ThumbnailDTO> sizes = new HashMap<>();
        sizes.put("orig", new ThumbnailDTO(height, width, "path/orig"));
        sizes.put("small", new ThumbnailDTO(height / 2, width / 2, "path/small"));
        AvatarsImageDTO avatarsResponse = new AvatarsImageDTO(GROUP_ID, IMAGE_NAME, meta, sizes);
        doReturn(avatarsResponse).when(avatarsClient).uploadImage(any(MultipartFile.class), eq(isTemporary));
        return avatarsResponse;
    }
}
