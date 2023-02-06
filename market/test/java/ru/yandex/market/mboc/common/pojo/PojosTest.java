package ru.yandex.market.mboc.common.pojo;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.NewOffersData.SupplierInfo;
import ru.yandex.market.mboc.common.offers.ImportedOffer;


/**
 * @author masterj
 */
public class PojosTest {
    private static <T> void doAssertions(EqualsAndHashCodeAsserter<T> asserter) {
        SoftAssertions.assertSoftly(softly -> {
            asserter.assertThatAllFieldsAreUsedInEqualsAndHashCode(softly);
            asserter.assertThatEqualsAndHashCodeAreDefined(softly);
        });
    }

    @Test
    public void testMasterData() {
        doAssertions(new EqualsAndHashCodeAsserter<>(MasterData.class, MasterData::new,
            "modifiedTimestamp", "goldenRsl", "regNumbers", "datacampMasterDataVersion", "measurementState"));
    }

    @Test
    public void testSupplier() {
        doAssertions(new EqualsAndHashCodeAsserter<>(Supplier.class, Supplier::new,
            "realSupplierId", "type"));
    }

    @Test
    public void testQualityDocument() {
        doAssertions(new EqualsAndHashCodeAsserter<>(QualityDocument.class, QualityDocument::new));
    }

    @Test
    public void testQualityDocumentMetadata() {
        doAssertions(new EqualsAndHashCodeAsserter<>(QualityDocument.Metadata.class, QualityDocument.Metadata::new));
    }

    @Test
    public void testImportedOffer() {
        doAssertions(new EqualsAndHashCodeAsserter<>(ImportedOffer.class, ImportedOffer::new,
            "masterData", "unknowns", "model", "author", "avilability"));
    }

    @Test
    public void testSupplierInfo() {
        doAssertions(new EqualsAndHashCodeAsserter<>(SupplierInfo.class, SupplierInfo::new));
    }
}
