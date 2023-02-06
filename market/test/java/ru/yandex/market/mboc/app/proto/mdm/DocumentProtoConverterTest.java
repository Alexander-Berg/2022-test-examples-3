package ru.yandex.market.mboc.app.proto.mdm;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.converter.ConversionAsserter;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mdm.http.MdmDocument;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class DocumentProtoConverterTest {
    private static final int RETIRES_COUNT = 256;

    private static final Long SEED = 788L;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenConvertingProtoDocumentShouldReturnCorrectQualityDocument() {
        for (int i = 0; i < RETIRES_COUNT; i++) {
            MdmDocument.Document document = generateProtoDocument()
                .toBuilder()
                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_ORIGIN)
                .build();
            QualityDocument converted = DocumentProtoConverter.createQualityDocument(document);
            assertSoftly(softly -> {
                softly.assertThat(converted.getRegistrationNumber()).isEqualTo(document.getRegistrationNumber());
                softly.assertThat(converted.getType())
                    .isEqualTo(QualityDocument.QualityDocumentType.CERTIFICATE_OF_ORIGIN);
                softly.assertThat(converted.getStartDate()).isEqualTo(LocalDate.ofEpochDay(document.getStartDate()));
                softly.assertThat(converted.getEndDate()).isEqualTo(LocalDate.ofEpochDay(document.getEndDate()));
                softly.assertThat(converted.getSerialNumber()).isEqualTo(document.getSerialNumber());
                softly.assertThat(converted.getRequirements()).isEqualTo(document.getRequirements());
                softly.assertThat(converted.getCertificationOrgRegNumber())
                    .isEqualTo(document.getCertificationOrgRegNumber());
                softly.assertThat(converted.getPictures())
                    .containsExactly(document.getPictureList().toArray(new String[0]));
                softly.assertThat(converted.getCustomsCommodityCodes())
                    .containsExactly(document.getCustomsCommodityCodesList().toArray(new String[0]));
                softly.assertThat(converted.getMetadata())
                    .isEqualTo(DocumentProtoConverter.createQualityMetadata(document.getMetadata()));
            });
        }
    }

    @Test
    public void whenConvertingProtoDocumentWithoutEndDateShouldConvertToEndlessDate() {
        MdmDocument.Document protoWithoutEndDate = generateProtoDocument().toBuilder().clearEndDate().build();
        Assertions.assertThat(DocumentProtoConverter.createQualityDocument(protoWithoutEndDate).getEndDate())
            .isEqualTo(QualityDocument.UNLIMITED_END_DATE);
    }

    @Test
    public void whenConvertingQualityDocumentWithEndlessDateShouldConvertToNullDateInProtoDocument() {
        QualityDocument qualityDocument = TestDataUtils.generateFullDocument(random)
            .setEndDate(QualityDocument.UNLIMITED_END_DATE);
        Assertions.assertThat(DocumentProtoConverter.createProtoDocument(qualityDocument).hasEndDate())
            .isFalse();
    }

    private MdmDocument.Document generateProtoDocument() {
        EnhancedRandom enhancedRandom = random;
        return MdmDocument.Document.newBuilder()
            .setRegistrationNumber(enhancedRandom.nextObject(String.class))
            .setType(enhancedRandom.nextObject(MdmDocument.Document.DocumentType.class))
            .setStartDate(enhancedRandom.nextObject(LocalDate.class).toEpochDay())
            .setEndDate(enhancedRandom.nextObject(LocalDate.class).toEpochDay())
            .setCertificationOrgRegNumber(enhancedRandom.nextObject(String.class))
            .setRequirements(enhancedRandom.nextObject(String.class))
            .addAllPicture(enhancedRandom
                .objects(String.class, enhancedRandom.nextInt(2) + 1)
                .collect(Collectors.toList()))
            .setSerialNumber(enhancedRandom.nextObject(String.class))
            .addAllCustomsCommodityCodes(enhancedRandom
                .objects(String.class, enhancedRandom.nextInt(2) + 1)
                .collect(Collectors.toList()))
            .setMetadata(MdmDocument.Document.Metadata.newBuilder()
                .setCreatedBy(String.valueOf(enhancedRandom.nextInt()))
                .setSource(MdmDocument.Document.Metadata.Source.SUPPLIER)
                .build())
            .build();
    }

    @Test
    public void testQualityDocument() {
        new ConversionAsserter<>(
            () -> MdmDocument.Document.newBuilder().build(),
            () -> new QualityDocument(),
            () -> TestDataUtils.generateCorrectDocument(random),
            DocumentProtoConverter::createProtoDocument,
            DocumentProtoConverter::createQualityDocument
        ).doAssertions();
    }

    @Test
    public void testQualityDocumentMetadata() {
        AtomicLong seedCounter = new AtomicLong(SEED);
        new ConversionAsserter<>(
            () -> MdmDocument.Document.Metadata.newBuilder().build(),
            () -> new QualityDocument.Metadata(),
            () -> TestDataUtils.generate(QualityDocument.Metadata.class, random),
            DocumentProtoConverter::createProtoMetadata,
            DocumentProtoConverter::createQualityMetadata
        ).doAssertions();
    }
}
