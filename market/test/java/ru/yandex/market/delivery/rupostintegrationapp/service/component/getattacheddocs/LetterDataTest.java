package ru.yandex.market.delivery.rupostintegrationapp.service.component.getattacheddocs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

class LetterDataTest extends BaseTest {
    private static final String PLACEHOLDER = "placeholder";
    private static final String INCORPORATION = "ООО АПЕЛЬСИН";
    private static final String REQUISITE = "ОГРН: 1112233";
    private static final String BATCH_NUM = "144";
    private static final String BATCH_DATE = "2016-02-15";
    private static final String DOCUMENT_DATE = "2016-02-15";
    private LetterData letterData;

    @BeforeEach
    void init() {
        letterData = new LetterData()
            .setIncorporation(INCORPORATION)
            .setRequisite(REQUISITE)
            .setBatchNum(BATCH_NUM)
            .setBatchDate(BATCH_DATE)
            .setDocumentDate(DOCUMENT_DATE);
    }

    @Test
    void testParameterWriting() {
        softly.assertThat(letterData.getMap().get(LetterData.INCORPORATION))
            .as("Incorporation was not filled successfully")
            .isEqualTo(INCORPORATION);

        softly.assertThat(letterData.getMap().get(LetterData.REQUISITE))
            .as("Requisites was not filled successfully")
            .isEqualTo(REQUISITE);

        softly.assertThat(letterData.getMap().get(LetterData.BATCH_NUM))
            .as("Batch number was not filled successfully")
            .isEqualTo(BATCH_NUM);

        softly.assertThat(letterData.getMap().get(LetterData.BATCH_DATE))
            .as("Batch date was not filled successfully")
            .isEqualTo(BATCH_DATE);

        softly.assertThat(letterData.getMap().get(LetterData.DOCUMENT_DATE))
            .as("Document date was not filled successfully")
            .isEqualTo(DOCUMENT_DATE);
    }

    @Test
    void testParameterRewriting() {
        letterData = letterData
            .setIncorporation(PLACEHOLDER)
            .setRequisite(PLACEHOLDER)
            .setBatchNum(PLACEHOLDER)
            .setBatchDate(PLACEHOLDER)
            .setDocumentDate(PLACEHOLDER);

        softly.assertThat(letterData.getMap().get(LetterData.INCORPORATION))
            .as("Incorporation was not filled successfully")
            .isEqualTo(PLACEHOLDER);

        softly.assertThat(letterData.getMap().get(LetterData.REQUISITE))
            .as("Requisites was not filled successfully")
            .isEqualTo(PLACEHOLDER);

        softly.assertThat(letterData.getMap().get(LetterData.BATCH_NUM))
            .as("Batch number was not filled successfully")
            .isEqualTo(PLACEHOLDER);

        softly.assertThat(letterData.getMap().get(LetterData.BATCH_DATE))
            .as("Batch date was not filled successfully")
            .isEqualTo(PLACEHOLDER);

        softly.assertThat(letterData.getMap().get(LetterData.DOCUMENT_DATE))
            .as("Document date was not filled successfully")
            .isEqualTo(PLACEHOLDER);
    }
}
