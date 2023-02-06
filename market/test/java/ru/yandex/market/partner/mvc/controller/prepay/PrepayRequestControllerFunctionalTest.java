package ru.yandex.market.partner.mvc.controller.prepay;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.api.cpa.yam.service.PartnerApplicationDocumentsStorageService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType.SIGNED_APP_FORM;


/**
 * @author stani on 26.03.18.
 */
@DbUnitDataSet(before = "PrepayRequestControllerFunctionalTest.before.csv")
class PrepayRequestControllerFunctionalTest extends FunctionalTest {
    @Autowired
    private PrepayRequestService prepayRequestService;
    @Autowired
    private PartnerApplicationDocumentsStorageService partnerApplicationDocumentsStorageService;

    private File tempDir;

    @BeforeEach
    public void setUp() {
        tempDir = Files.createTempDir();
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }

    @DisplayName("Если уже нет привязанного документа, то не должно падать")
    @Test
    @DbUnitDataSet(after = "PrepayRequestControllerFunctionalTest.before.csv")
    void checkDeleteDocumentIfNotExists() {
        FunctionalTestHelper.delete(baseUrl + "/prepay-request/128426/document/18130?datasource_id=558618");
    }

    @DisplayName("Проверка загрузки документа")
    @Test
    void uploadSignedForm() throws IOException {
        //prepare mocks
        doReturn(Optional.of(new URL("http://some.pdf"))).when(partnerApplicationDocumentsStorageService)
                .uploadFile(any(ContentProvider.class), anyLong(), anyLong());

        doReturn(new URL("http://some.pdf")).when(partnerApplicationDocumentsStorageService)
                .getDownloadUrl(anyLong(), anyLong());

        File document = File.createTempFile("document", ".pdf", tempDir);
        FileUtils.write(document, "Some text", StandardCharsets.UTF_8);

        //prepare message
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("type", String.valueOf(SIGNED_APP_FORM.getId()));
        multipart.add("file", new FileSystemResource(document));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        //call tested code
        FunctionalTestHelper.post(baseUrl + "/prepay-request/128426/document?datasource_id=558618",
                new HttpEntity<>(multipart, headers));
        assertNotNull(prepayRequestService.findLastActiveRequest(558618).getStartDate());
    }
}
