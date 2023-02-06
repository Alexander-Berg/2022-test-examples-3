package ru.yandex.market.ff.repository;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.model.entity.DocumentTemplate;
import ru.yandex.market.ff.model.entity.DocumentTemplateColumn;

import static ru.yandex.market.ff.client.enums.DocumentType.SUPPLY;
import static ru.yandex.market.ff.client.enums.DocumentType.WITHDRAW;

/**
 * @author kotovdv 01/08/2017.
 */
class DocumentTemplateRepositoryTest extends IntegrationTest {

    @Autowired
    private DocumentTemplateRepository documentTemplateRepository;

    private SoftAssertions softly;

    @BeforeEach
    void beforeTest() {
        softly = new SoftAssertions();
    }

    @AfterEach
    void afterTest() {
        softly.assertAll();
    }

    @Test
    void testGetExistingTemplateValidations() {

        DocumentType documentType = SUPPLY;
        List<DocumentTemplate> templateList = documentTemplateRepository.getDocumentTemplates(documentType);
        softly.assertThat(templateList).isNotNull();
        softly.assertThat(templateList.size()).isEqualTo(2);

        assertTemplateValid(documentType, templateList.get(0), 8);
        assertTemplateValid(documentType, templateList.get(1), 6);
    }

    @Test
    void testGetEmptyTemplateValidations() {

        DocumentType documentType = WITHDRAW;
        List<DocumentTemplate> templateList = documentTemplateRepository.getDocumentTemplates(documentType);
        softly.assertThat(templateList).isNotNull();
        softly.assertThat(templateList.size()).isEqualTo(1);

        assertTemplateValid(documentType, templateList.get(0), 3);
    }

    private void assertTemplateValid(DocumentType documentType, DocumentTemplate template, final int rowsCount) {

        softly.assertThat(template)
                .describedAs("Document template must not be null")
                .isNotNull();

        softly.assertThat(template.getDocumentType())
                .describedAs("Template name value assertion")
                .isEqualTo(documentType);

        final List<DocumentTemplateColumn> columns = template.getColumns();
        softly.assertThat(columns.size())
                .describedAs("Template columns size assertion")
                .isEqualTo(rowsCount);

        final DocumentTemplateColumn column = columns.get(0);

        softly.assertThat(column.getColumnIndex())
                .describedAs("Column index value assertion")
                .isEqualTo(0);

        softly.assertThat(column.getColumnName())
                .describedAs("Column name value assertion")
                .isEqualTo("Ваш SKU");

        softly.assertThat(column.isMandatory())
                .describedAs("Is mandatory value assertion")
                .isTrue();

        softly.assertThat(column.isMandatory())
                .describedAs("Is unique value assertion")
                .isTrue();

        softly.assertThat(column.getDataType())
                .describedAs("Data type value assertion")
                .isEqualTo("STRING[128]");

    }
}
