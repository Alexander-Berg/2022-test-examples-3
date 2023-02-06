package ru.yandex.market.ff.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.model.entity.ShopRequestDocument;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.time.LocalDateTime.of;

/**
 * @author kotovdv 04/08/2017.
 */
public class ShopRequestDocumentRepositoryTest extends IntegrationTest {

    @Autowired
    private ShopRequestDocumentRepository repository;

    @Test
    @DatabaseSetup(value = "classpath:repository/shop-request-doc/before_save.xml")
    @ExpectedDatabase(value = "classpath:repository/shop-request-doc/after_save.xml", assertionMode = NON_STRICT)
    public void testSaveShopRequestDocument() throws Exception {
        ShopRequestDocument document = createDocument();

        repository.save(document);
    }

    private ShopRequestDocument createDocument() {
        ShopRequestDocument document = new ShopRequestDocument();

        document.setRequestId(1L);
        document.setType(DocumentType.SUPPLY);
        document.setCreatedAt(of(1999, 9, 9, 9, 9, 9));
        document.setFileUrl("FILE_URL");
        document.setExtension(FileExtension.CSV);

        return document;
    }
}
