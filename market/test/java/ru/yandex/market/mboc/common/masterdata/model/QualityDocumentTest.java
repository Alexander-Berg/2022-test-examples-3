package ru.yandex.market.mboc.common.masterdata.model;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class QualityDocumentTest {

    private static final long SEED = 1899L;
    private static final int TRIES_COUNT = 100;
    private EnhancedRandom random;


    @Before
    public void setup() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(SEED)
            .build();
    }

    @Test
    public void whenQualityDocumentCopiedShouldBeEqualToOriginal() {
        for (int i = 0; i < TRIES_COUNT; i++) {
            QualityDocument qualityDocument = random.nextObject(QualityDocument.class);
            Assertions.assertThat(qualityDocument)
                .isEqualToComparingFieldByField(new QualityDocument(qualityDocument));
        }
    }

    @Test
    public void whenMetadataCopiedShouldBeEqualToOriginal() {
        for (int i = 0; i < TRIES_COUNT; i++) {
            QualityDocument.Metadata metadata = random.nextObject(QualityDocument.Metadata.class);
            Assertions.assertThat(metadata)
                .isEqualToComparingFieldByField(new QualityDocument.Metadata(metadata));
        }
    }

    @Test
    public void whenCreatedBySupplierIsCreatedByOrUnknownShouldReturnTrue() {
        int supplierId = 1;
        QualityDocument.Metadata metadata = random.nextObject(QualityDocument.Metadata.class).setCreatedBy(supplierId);
        Assertions.assertThat(metadata.isCreatedBy(supplierId)).isTrue();
    }

    @Test
    public void whenCreatedByNullIsCreatedByOrUnknownShouldReturnFalse() {
        int supplierId = 1;
        QualityDocument.Metadata metadata = random.nextObject(QualityDocument.Metadata.class).setCreatedBy(null);
        Assertions.assertThat(metadata.isCreatedBy(supplierId)).isFalse();
    }

    @Test
    public void whenCreatedByOtherSupplierIsCreatedByOrUnknownShouldReturnFalse() {
        int supplierId = 1;
        int otherSupplierId = 2;
        QualityDocument.Metadata metadata = random.nextObject(QualityDocument.Metadata.class)
            .setCreatedBy(otherSupplierId);
        Assertions.assertThat(metadata.isCreatedBy(supplierId)).isFalse();
    }
}
