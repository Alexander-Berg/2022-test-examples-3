package ru.yandex.market.mboc.app.proto.mdm.pictures;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest.DocumentAddition;

public class DocumentProtoPictureConverterTest {
    private static final long SEED = 1612L;
    private static final int TRIES = 100;
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenRemovingOrigShouldRemoveOnlyLast() {
        List<String> strings = Arrays.asList("correct/orig", "/orig", null, "nothing");
        Assertions.assertThat(DocumentProtoPictureConverter
            .removeOrigSuffixFromPictures(strings))
            .containsExactly("correct", "", "nothing");
    }

    @Test
    public void whenAddingOrigShouldAddOnlyIfNotExists() {
        List<String> strings = Arrays.asList("correct", "", null, "exists/orig");
        Assertions.assertThat(DocumentProtoPictureConverter
            .addOrigSuffixToPictures(strings))
            .containsExactly("correct/orig", "/orig", "exists/orig");
    }

    @Test
    public void whenPreparingDocumentAdditionShouldReplacePicturesToDelete() {
        for (int i = 0; i < TRIES; i++) {
            DocumentAddition addition = DocumentAddition.newBuilder()
                .addAllDeletePictureMdmUrl(
                    random.objects(String.class, 2)
                        .collect(Collectors.toList()))
                .addAllNewScanFile(
                    Arrays.asList(
                        DocumentAddition.ScanFile.newBuilder()
                            .setFileName(random.nextObject(String.class))
                            .setUrl(random.nextObject(String.class))
                            .build(),
                        DocumentAddition.ScanFile.newBuilder()
                            .setFileName(random.nextObject(String.class))
                            .setUrl(random.nextObject(String.class))
                            .build()))
                .build();

            DocumentAddition newAddition = DocumentProtoPictureConverter
                .addOrigSuffixToDocumentAdditionPictures(addition);

            SoftAssertions.assertSoftly(softAssertions -> {
                softAssertions.assertThat(newAddition.getDeletePictureMdmUrlList())
                    .containsExactlyElementsOf(
                        addition.getDeletePictureMdmUrlList().stream()
                            .map(s -> s + DocumentProtoPictureConverter.ORIG)
                            .collect(Collectors.toList()));
                softAssertions.assertThat(newAddition.getNewScanFileList())
                    .containsExactlyElementsOf(addition.getNewScanFileList());
            });
        }
    }

    @Test
    public void whenPreparingQualityDocumentShouldReplacePictures() {
        for (int i = 0; i < TRIES; i++) {
            QualityDocument document = TestDataUtils.generateDocument(random)
                .setPictures(random.objects(String.class, 2)
                    .map(s -> s + DocumentProtoPictureConverter.ORIG)
                    .collect(Collectors.toList()));

            QualityDocument newDocument = DocumentProtoPictureConverter.removeOrigSuffixFromDocumentsPictures(document);

            SoftAssertions.assertSoftly(softAssertions -> {
                softAssertions.assertThat(newDocument.getPictures())
                    .containsExactlyElementsOf(
                        document.getPictures().stream()
                            .map(s -> s.replace(DocumentProtoPictureConverter.ORIG, ""))
                            .collect(Collectors.toList()));
                softAssertions.assertThat(newDocument)
                    .isEqualToIgnoringGivenFields(document, "pictures");
            });
        }
    }
}
