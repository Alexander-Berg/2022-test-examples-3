package ru.yandex.market.mboc.common.pojo;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;


/**
 * @author masterj
 */
public class MdmPojosTest {
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
    public void testQualityDocument() {
        doAssertions(new EqualsAndHashCodeAsserter<>(QualityDocument.class, QualityDocument::new));
    }

    @Test
    public void testQualityDocumentMetadata() {
        doAssertions(new EqualsAndHashCodeAsserter<>(QualityDocument.Metadata.class, QualityDocument.Metadata::new));
    }

}
