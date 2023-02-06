package ru.yandex.market.hrms.api.controller.medical_examination.pdf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "../MedicalExaminationReferral.before.csv")
public class MedicalExaminationControllerPdfTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "MedicalExaminationControllerPdfTest.before.csv")
    void shouldReturnPdfViewInFile(@TempDir Path tempDir) throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));

        MvcResult result = mockMvc.perform(get("/lms/medical-examination-referral/3/pdf")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.LABOR_PROTECTION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andReturn();

        File actualResult = new File(tempDir.resolve("actual_referral_result.pdf").toUri());
        try (FileOutputStream fos = new FileOutputStream(actualResult)) {
            IOUtils.copy(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()), fos);
        }

        var expectedContent = extractContentFromPdf(loadFileAsBytes("expected_referral_result.pdf"));
        var actualContent = extractContentFromPdf(actualResult.getPath());

        Assertions.assertEquals("application/pdf", result.getResponse().getContentType());
        Assertions.assertArrayEquals(expectedContent.getBytes("cp1251"), actualContent.getBytes("cp1251"));
    }

    private String extractContentFromPdf(String path) throws IOException {
        PdfTextExtractor extractor = new PdfTextExtractor(new PdfReader(path));
        return extractor.getTextFromPage(1);
    }

    private String extractContentFromPdf(byte[] fileAsBytesArr) throws IOException {
        PdfTextExtractor extractor = new PdfTextExtractor(new PdfReader(fileAsBytesArr));
        return extractor.getTextFromPage(1);
    }
}
