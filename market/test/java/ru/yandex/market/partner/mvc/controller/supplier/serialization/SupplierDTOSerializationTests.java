package ru.yandex.market.partner.mvc.controller.supplier.serialization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.core.upload.model.FileUpload;
import ru.yandex.market.partner.mvc.MvcTestSerializationConfig;
import ru.yandex.market.partner.mvc.controller.feed.model.AssortmentFeedDTO;
import ru.yandex.market.partner.mvc.controller.feed.model.FileUploadDTO;

/**
 * @author fbokovikov
 */
@SpringJUnitConfig(classes = MvcTestSerializationConfig.class)
class SupplierDTOSerializationTests {

    @Autowired
    private SerializationChecker checker;

    /**
     * Тест на сериализацию {@link FileUploadDTO}
     */
    @Test
    void testFeedUploadDTOSerialization() {
        AssortmentFeedDTO feedUploadDTO =
                AssortmentFeedDTO.builder().setUpload(FileUploadDTO.valueOf(
                        new FileUpload.Builder()
                                .setId(123)
                                .setUrl("http://mds.ru/supplier-feed.xls")
                                .setName("supplier-feed.xls")
                                .setSize(12345678L)
                                .setUploadDate(LocalDateTime.of(LocalDate.of(2017, 10, 19), LocalTime.of(15, 0)))
                                .build()))
                        .build();
        checker.testJsonSerialization(feedUploadDTO, "" +
                "{" +
                "    \"upload\": {" +
                "        \"fileName\":\"supplier-feed.xls\"," +
                "        \"fileSize\":12345678," +
                "        \"uploadDateTime\":1508414400000" +
                "    }," +
                "    \"isDefault\": false" +
                "}"
        );
    }

}
