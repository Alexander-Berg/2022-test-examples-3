package ru.yandex.market.clab.tms.service;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RawPhoto;
import ru.yandex.market.clab.test.BaseIntegrationTest;
import ru.yandex.market.clab.tms.config.AmazonS3Config;

/**
 * @author anmalysh
 * @since 1/11/2019
 */
@SpringBootTest(classes = {
    AmazonS3Config.class
})
@Ignore
public class RawPhotoS3ServiceTest extends BaseIntegrationTest {

    @Autowired
    RawPhotoS3Service rawPhotoS3Service;

    @Test
    public void testFailingPhotos() {
        Good good = new Good()
            .setId(8L)
            .setSupplierId(465852L)
            .setSupplierSkuId("000025.Б0026865")
            .setMskuId(14252873L);
        RawPhoto photo = new RawPhoto()
            .setId(84L)
            .setBarcode("4210201166764")
            .setPhoto("421020116676400020.CR2");
        rawPhotoS3Service.uploadRawPhoto(good, photo);
    }
}
