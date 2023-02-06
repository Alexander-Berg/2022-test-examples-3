package ru.yandex.market.mboc.app.proto.mdm.pictures;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.ByteArrayMultipartFile;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.QualityDocumentPictureServiceImpl;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest.DocumentAddition;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class SupplierDocumentPicturesProcessorTest {

    private static final long SEED = 20181202L;
    private static final String CORRECT_SCAN_FILE_URL = "http://lolkek.com/wabulabudubda";
    private static final String CORRECT_SCAN_FILE_NAME = "4k.jpg";

    private EnhancedRandom random;
    private QualityDocumentPictureServiceImpl pictureServiceMock;
    private SupplierDocumentPicturesProcessor documentPicturesProcessorMock;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);
        pictureServiceMock = Mockito.mock(QualityDocumentPictureServiceImpl.class);
        documentPicturesProcessorMock = new SupplierDocumentPicturesProcessor(pictureServiceMock);
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(random);
    }

    private QualityDocument generateValidDocument() {
        return TestDataUtils.generateCorrectDocument(random);
    }

    private DocumentAddition.ScanFile createScanFile(@Nullable String url, @Nullable String fileName) {
        DocumentAddition.ScanFile.Builder builder = DocumentAddition.ScanFile.newBuilder();
        if (url != null) {
            builder.setUrl(url);
        }
        if (fileName != null) {
            builder.setFileName(fileName);
        }
        return builder.build();
    }

    @Test
    public void whenAddingScanFileWithoutFileNameShouldReturnError() {
        List<ErrorInfo> errorInfos = SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
            DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile("some.com", null))
                .build(), null);

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("Все файлы сертификатов должны иметь url и file_name."));
    }

    @Test
    public void whenAddingScanFileWithoutUrlShouldReturnError() {
        List<ErrorInfo> errorInfos = SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
            DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile(null, "test/no-extension"))
                .build(), null);

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("Все файлы сертификатов должны иметь url и file_name."));
    }


    @Test
    public void whenAddingScanFileWithoutExtensionInDocumentsShouldReturnErrorsInDocuments() {
        List<ErrorInfo> errorInfos = SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
            DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile("some.com", "test/no-extension"))
                .build(), null);

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("Все файлы должны иметь расширение: jpg, jpeg, png, pdf"));
    }

    @Test
    public void whenScanFileWithWrongExtensionInDocumentsShouldReturnErrorsInDocuments() {
        List<ErrorInfo> errorInfos = SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
            DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile("some.com", "test/wrong.extension"))
                .build(), null);

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("Все файлы должны иметь расширение: jpg, jpeg, png, pdf"));
    }

    @Test
    public void whenAddingScanFilesShouldCheckAllFilesExtensions() {
        List<ErrorInfo> errorInfos = SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
            DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile("some.com", "test/correct.jpg"))
                .addNewScanFile(createScanFile("some.com", "test/correct.jpg"))
                .addNewScanFile(createScanFile("some.com", "test/wrong.wrong"))
                .build(), null);

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("Все файлы должны иметь расширение: jpg, jpeg, png, pdf"));
    }

    @Test
    public void whenAddingNewDocumentWithEmptyPicturesShouldReturnError() {
        List<ErrorInfo> errorInfos =
            SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
                DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument())).build(),
                null);

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("При добавлении нового документа нужна хотя бы 1 картинка"));
    }

    @Test
    public void whenUpdatingDocumentWithEmptyPicturesShouldReturnError() {
        List<ErrorInfo> errorInfos =
            SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
                DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument())).build(),
                generateValidDocument().setPictures(Collections.emptyList()));

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("При обновлении документа должна остаться хотя бы 1 картинка"));
    }

    @Test
    public void whenDeletingAllPicturesShouldReturnError() {
        String pic = "to-delete-url";
        List<ErrorInfo> errorInfos =
            SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
                DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addDeletePictureMdmUrl(pic).build(),
                generateValidDocument().setPictures(Collections.singletonList(pic)));

        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("При обновлении документа должна остаться хотя бы 1 картинка"));
    }

    @Test
    public void whenAddingNewDocumentPicturesShouldNotReturnError() {
        String pic = "new-pic.jpg";
        List<ErrorInfo> errorInfos =
            SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
                DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addNewScanFile(createScanFile("some.com", pic))
                    .build(), null);

        Assertions.assertThat(errorInfos).isEmpty();
    }

    @Test
    public void whenAddingNewDocumentPicturesToExistingDocumentShouldNotReturnError() {
        String pic = "new-pic.jpg";
        List<ErrorInfo> errorInfos =
            SupplierDocumentPicturesProcessor.validateDocumentAdditionPictures(
                DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addNewScanFile(createScanFile("some.com", pic))
                    .build(), generateValidDocument().setPictures(Collections.emptyList()));

        Assertions.assertThat(errorInfos).isEmpty();
    }

    @Test
    public void whenPictureValidationFailedShouldReturnError() throws Exception {
        DocumentPictureProcessingResult result = documentPicturesProcessorMock
            .updateDocumentAdditionPictures(DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile("some.com", "test/wrong.wrong"))
                .build(), null);

        Mockito.verify(pictureServiceMock, Mockito.times(0))
            .downloadPicture(Mockito.anyString(), Mockito.anyString());
        assertSoftly(softly -> {
            softly.assertThat(result.hasErrors()).isTrue();
            softly.assertThat(result.getPictures()).isEmpty();
        });
    }

    @Test
    public void whenFailedToDownloadFileScanShouldReturnError() throws Exception {
        RuntimeException exception = new RuntimeException("Failed to load.");

        Mockito.when(pictureServiceMock
            .downloadPicture(Mockito.any(DocumentAddition.ScanFile.class)))
            .thenThrow(exception);

        DocumentPictureProcessingResult result = documentPicturesProcessorMock
            .updateDocumentAdditionPictures(DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile(CORRECT_SCAN_FILE_URL, CORRECT_SCAN_FILE_NAME))
                .build(), null);

        Mockito.verify(pictureServiceMock, Mockito.times(0))
            .saveDocumentPictures(Mockito.anyString(), Mockito.any());
        assertSoftly(softly -> {
            softly.assertThat(result.hasErrors()).isTrue();
            softly.assertThat(result.getPictures()).isEmpty();
            softly.assertThat(result.getErrors()).containsExactly(
                MbocErrors.get().qdAddSupplierDocumentsFailedToAddDocument(
                    "Ошибка при загрузке картинки сертификата:" + exception.getMessage()));
        });
    }

    @Test
    public void whenFailedToSavePicturesShouldReturnError() throws Exception {
        RuntimeException exception = new RuntimeException("Failed to save pic.");
        Mockito.when(pictureServiceMock
            .downloadPicture(Mockito.any(DocumentAddition.ScanFile.class)))
            .thenCallRealMethod();
        Mockito.when(pictureServiceMock
            .downloadPicture(Mockito.eq(CORRECT_SCAN_FILE_URL), Mockito.eq(CORRECT_SCAN_FILE_NAME)))
            .thenReturn(new ByteArrayMultipartFile(CORRECT_SCAN_FILE_URL, new byte[1]));
        Mockito.when(pictureServiceMock.saveDocumentPictures(Mockito.eq(CORRECT_SCAN_FILE_URL), Mockito.any()))
            .thenThrow(exception);

        DocumentPictureProcessingResult result = documentPicturesProcessorMock
            .updateDocumentAdditionPictures(DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                .addNewScanFile(createScanFile(CORRECT_SCAN_FILE_URL, CORRECT_SCAN_FILE_NAME))
                .build(), null);

        Mockito.verify(pictureServiceMock, Mockito.times(1))
            .downloadPicture(Mockito.eq(CORRECT_SCAN_FILE_URL), Mockito.eq(CORRECT_SCAN_FILE_NAME));
        assertSoftly(softly -> {
            softly.assertThat(result.hasErrors()).isTrue();
            softly.assertThat(result.getPictures()).isEmpty();
            softly.assertThat(result.getErrors()).containsExactly(
                MbocErrors.get().qdAddSupplierDocumentsFailedToAddDocument(
                    "Ошибка при загрузке картинки сертификата:" + exception.getMessage()));
        });
    }

    @Test
    public void whenUpdatingDocumentPicturesShouldDeletePics() {
        QualityDocument validDocument = generateValidDocument().setPictures(Arrays.asList("pic1", "pic2", "pic3"));

        DocumentPictureProcessingResult result = documentPicturesProcessorMock
            .updateDocumentAdditionPictures(DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(validDocument))
                .addAllDeletePictureMdmUrl(Arrays.asList("pic2", "pic3"))
                .build(), validDocument);

        assertSoftly(softly -> {
            softly.assertThat(result.hasErrors()).isFalse();
            softly.assertThat(result.getPictures()).containsExactly("pic1");
        });
    }

    @Test
    public void whenUpdatingDocumentShouldAddPics() throws Exception {
        QualityDocument validDocument = generateValidDocument().setPictures(Arrays.asList("pic1", "pic2"));
        Mockito.when(pictureServiceMock
            .downloadPicture(Mockito.any(DocumentAddition.ScanFile.class)))
            .thenCallRealMethod();
        Mockito.when(pictureServiceMock
            .downloadPicture(Mockito.eq(CORRECT_SCAN_FILE_URL), Mockito.eq(CORRECT_SCAN_FILE_NAME)))
            .thenReturn(new ByteArrayMultipartFile(CORRECT_SCAN_FILE_NAME, new byte[]{1}));
        Mockito.when(pictureServiceMock
            .saveDocumentPictures(Mockito.eq(CORRECT_SCAN_FILE_NAME), Mockito.any()))
            .thenReturn(Collections.singletonList("new-pic"));

        DocumentPictureProcessingResult result = documentPicturesProcessorMock
            .updateDocumentAdditionPictures(DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(validDocument))
                .addNewScanFile(createScanFile(CORRECT_SCAN_FILE_URL, CORRECT_SCAN_FILE_NAME))
                .build(), validDocument);

        assertSoftly(softly -> {
            softly.assertThat(result.hasErrors()).isFalse();
            softly.assertThat(result.getPictures()).containsExactly("pic1", "pic2", "new-pic");
        });
    }
}
