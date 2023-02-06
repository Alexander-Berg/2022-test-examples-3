package ru.yandex.market.delivery.rupostintegrationapp.service.component.getattacheddocs;

import java.io.ByteArrayInputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import utils.FixtureRepository;

import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;

class LetterBuilderTest extends BaseContextualTest {
    private LetterData letterData;

    @Autowired
    private LetterBuilder builder;

    @BeforeEach
    void init() {
        initLetterData();
    }

    private void initLetterData() {
        String[] arr = new String[]{
            "ООО АПЕЛЬСИН",
            "ОГРН: 1112233",
            "144 (Партия № 5)",
            "2016-02-15",
            "2016-02-15"
        };

        letterData = new LetterData()
            .setIncorporation(arr[0])
            .setRequisite(arr[1])
            .setBatchNum(arr[2])
            .setBatchDate(arr[3])
            .setDocumentDate(arr[4]);
    }

    @Test
    void testBuilder() throws Exception {
        byte[] builtBdf = builder.buildPdf(letterData);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(builtBdf);

        PDDocument pdDocument = PDDocument.load(inputStream);
        PDFTextStripper pdfStripper = new PDFTextStripper();

        softly.assertThat(pdfStripper.getText(pdDocument))
            .as("Built letter pdf text is invalid")
            .isEqualTo(FixtureRepository.getLetterContentSample());

        pdDocument.close();
    }
}
