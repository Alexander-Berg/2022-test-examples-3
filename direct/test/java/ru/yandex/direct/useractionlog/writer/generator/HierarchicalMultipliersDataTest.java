package ru.yandex.direct.useractionlog.writer.generator;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.matcher.AssertionMatcher;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.dbschema.ppc.enums.HierarchicalMultipliersType;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.model.HierarchicalMultipliersData;
import ru.yandex.direct.useractionlog.schema.ObjectPath;

/**
 * Последовательные вставки и удаления не должны ломать {@link HierarchicalMultipliersData}.
 */
@ParametersAreNonnullByDefault
public class HierarchicalMultipliersDataTest {
    private static final ObjectPath DUMMY_PATH = new ObjectPath.ClientPath(new ClientId(1));
    private static final String TYPE = HierarchicalMultipliersType.geo_multiplier.getLiteral();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void addingOfRelated() {
        HierarchicalMultipliersData.Root data = new HierarchicalMultipliersData.Root()
                .withIsEnabled("")
                .withLastChange("")
                .withMultiplierPct("")
                .withPath(DUMMY_PATH)
                .withType(TYPE);
        TestUtils.assumeThat("Just created object should be empty",
                null,
                new AssertionMatcher<String>() {
                    @Override
                    public void assertion(String actual) throws AssertionError {
                        Assertions.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                                .describedAs("Empty")
                                .containsEntry("related_ids", "");
                        Assertions.assertThat(data.relatedIsEmpty()).isTrue();
                        Assertions.assertThat(data.relatedSize()).isEqualTo(0);
                    }
                });

        data.putRelated(101, new HierarchicalMultipliersData.Geo()
                .withIsHidden("101")
                .withMultiplierPct("101")
                .withRegionId("101"));
        String description = "Added one related, id=101";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "0-101")
                .containsEntry("is_hidden_0", "101")
                .containsEntry("multiplier_pct_0", "101")
                .containsEntry("region_id_0", "101")
                .doesNotContainKey("is_hidden_1")
                .doesNotContainKey("multiplier_pct_1")
                .doesNotContainKey("region_id_1")
                .doesNotContainKey("is_hidden_2")
                .doesNotContainKey("multiplier_pct_2")
                .doesNotContainKey("region_id_2");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(1);

        data.putRelated(102, new HierarchicalMultipliersData.Geo()
                .withIsHidden("102")
                .withMultiplierPct("102")
                .withRegionId("102"));
        description = "Added two related, id=101, id=102";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "0-101,1-102")
                .containsEntry("is_hidden_0", "101")
                .containsEntry("multiplier_pct_0", "101")
                .containsEntry("region_id_0", "101")
                .containsEntry("is_hidden_1", "102")
                .containsEntry("multiplier_pct_1", "102")
                .containsEntry("region_id_1", "102")
                .doesNotContainKey("is_hidden_2")
                .doesNotContainKey("multiplier_pct_2")
                .doesNotContainKey("region_id_2");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(2);

        data.putRelated(103, new HierarchicalMultipliersData.Geo()
                .withIsHidden("103")
                .withMultiplierPct("103")
                .withRegionId("103"));
        description = "Added three related, id=101, id=102, id=103";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "0-101,1-102,2-103")
                .containsEntry("is_hidden_0", "101")
                .containsEntry("multiplier_pct_0", "101")
                .containsEntry("region_id_0", "101")
                .containsEntry("is_hidden_1", "102")
                .containsEntry("multiplier_pct_1", "102")
                .containsEntry("region_id_1", "102")
                .containsEntry("is_hidden_2", "103")
                .containsEntry("multiplier_pct_2", "103")
                .containsEntry("region_id_2", "103");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(3);
    }

    @Test
    public void deletingOfRelated() {
        HierarchicalMultipliersData.Root data = new HierarchicalMultipliersData.Root()
                .withIsEnabled("")
                .withLastChange("")
                .withMultiplierPct("")
                .withPath(DUMMY_PATH)
                .withType(TYPE);
        data.putRelated(101, new HierarchicalMultipliersData.Geo()
                .withIsHidden("101")
                .withMultiplierPct("101")
                .withRegionId("101"));
        data.putRelated(102, new HierarchicalMultipliersData.Geo()
                .withIsHidden("102")
                .withMultiplierPct("102")
                .withRegionId("102"));
        data.putRelated(103, new HierarchicalMultipliersData.Geo()
                .withIsHidden("103")
                .withMultiplierPct("103")
                .withRegionId("103"));
        data.putRelated(104, new HierarchicalMultipliersData.Geo()
                .withIsHidden("104")
                .withMultiplierPct("104")
                .withRegionId("104"));
        TestUtils.assumeThat("Should be four related objects after four putRelated",
                null,
                new AssertionMatcher<String>() {
                    @Override
                    public void assertion(String actual) throws AssertionError {
                        Assertions.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                                .containsEntry("related_ids", "0-101,1-102,2-103,3-104")
                                .containsEntry("is_hidden_0", "101")
                                .containsEntry("multiplier_pct_0", "101")
                                .containsEntry("region_id_0", "101")
                                .containsEntry("is_hidden_1", "102")
                                .containsEntry("multiplier_pct_1", "102")
                                .containsEntry("region_id_1", "102")
                                .containsEntry("is_hidden_2", "103")
                                .containsEntry("multiplier_pct_2", "103")
                                .containsEntry("region_id_2", "103")
                                .containsEntry("is_hidden_3", "104")
                                .containsEntry("multiplier_pct_3", "104")
                                .containsEntry("region_id_3", "104");
                        Assertions.assertThat(data.relatedIsEmpty()).isFalse();
                        Assertions.assertThat(data.relatedSize()).isEqualTo(4);
                    }
                });

        data.removeRelated(102);
        String description = "Deleted from the middle";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "0-101,2-103,3-104")
                .containsEntry("is_hidden_0", "101")
                .containsEntry("multiplier_pct_0", "101")
                .containsEntry("region_id_0", "101")
                .doesNotContainKey("is_hidden_1")
                .doesNotContainKey("multiplier_pct_1")
                .doesNotContainKey("region_id_1")
                .containsEntry("is_hidden_2", "103")
                .containsEntry("multiplier_pct_2", "103")
                .containsEntry("region_id_2", "103")
                .containsEntry("is_hidden_3", "104")
                .containsEntry("multiplier_pct_3", "104")
                .containsEntry("region_id_3", "104");

        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(3);

        data.removeRelated(101);
        description = "Deleted from the start";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "2-103,3-104")
                .doesNotContainKey("is_hidden_0")
                .doesNotContainKey("multiplier_pct_0")
                .doesNotContainKey("region_id_0")
                .doesNotContainKey("is_hidden_1")
                .doesNotContainKey("multiplier_pct_1")
                .doesNotContainKey("region_id_1")
                .containsEntry("is_hidden_2", "103")
                .containsEntry("multiplier_pct_2", "103")
                .containsEntry("region_id_2", "103")
                .containsEntry("is_hidden_3", "104")
                .containsEntry("multiplier_pct_3", "104")
                .containsEntry("region_id_3", "104");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(2);

        data.removeRelated(104);
        description = "Deleted from the end";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "2-103")
                .doesNotContainKey("is_hidden_0")
                .doesNotContainKey("multiplier_pct_0")
                .doesNotContainKey("region_id_0")
                .doesNotContainKey("is_hidden_1")
                .doesNotContainKey("multiplier_pct_1")
                .doesNotContainKey("region_id_1")
                .containsEntry("is_hidden_2", "103")
                .containsEntry("multiplier_pct_2", "103")
                .containsEntry("region_id_2", "103")
                .doesNotContainKey("is_hidden_3")
                .doesNotContainKey("multiplier_pct_3")
                .doesNotContainKey("region_id_3");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(1);

        data.removeRelated(103);
        description = "Deleted everything";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "")
                .doesNotContainKey("is_hidden_0")
                .doesNotContainKey("multiplier_pct_0")
                .doesNotContainKey("region_id_0")
                .doesNotContainKey("is_hidden_1")
                .doesNotContainKey("multiplier_pct_1")
                .doesNotContainKey("region_id_1")
                .doesNotContainKey("is_hidden_2")
                .doesNotContainKey("multiplier_pct_2")
                .doesNotContainKey("region_id_2")
                .doesNotContainKey("is_hidden_3")
                .doesNotContainKey("multiplier_pct_3")
                .doesNotContainKey("region_id_3");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isTrue();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(0);
    }

    @Test
    public void insertDeleteInsert() {
        HierarchicalMultipliersData.Root data = new HierarchicalMultipliersData.Root()
                .withIsEnabled("")
                .withLastChange("")
                .withMultiplierPct("")
                .withPath(DUMMY_PATH)
                .withType(TYPE);
        data.putRelated(101, new HierarchicalMultipliersData.Geo()
                .withIsHidden("101")
                .withMultiplierPct("101")
                .withRegionId("101"));
        data.putRelated(102, new HierarchicalMultipliersData.Geo()
                .withIsHidden("102")
                .withMultiplierPct("102")
                .withRegionId("102"));
        data.putRelated(103, new HierarchicalMultipliersData.Geo()
                .withIsHidden("103")
                .withMultiplierPct("103")
                .withRegionId("103"));
        data.putRelated(104, new HierarchicalMultipliersData.Geo()
                .withIsHidden("104")
                .withMultiplierPct("104")
                .withRegionId("104"));
        data.removeRelated(101);
        data.removeRelated(103);
        TestUtils.assumeThat(null,
                new AssertionMatcher<String>() {
                    @Override
                    public void assertion(String actual) throws AssertionError {
                        Assertions.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                                .containsEntry("related_ids", "1-102,3-104")
                                .doesNotContainKey("is_hidden_0")
                                .doesNotContainKey("multiplier_pct_0")
                                .doesNotContainKey("region_id_0")
                                .containsEntry("is_hidden_1", "102")
                                .containsEntry("multiplier_pct_1", "102")
                                .containsEntry("region_id_1", "102")
                                .doesNotContainKey("is_hidden_2")
                                .doesNotContainKey("multiplier_pct_2")
                                .doesNotContainKey("region_id_2")
                                .containsEntry("is_hidden_3", "104")
                                .containsEntry("multiplier_pct_3", "104")
                                .containsEntry("region_id_3", "104");
                        Assertions.assertThat(data.relatedIsEmpty()).isFalse();
                        Assertions.assertThat(data.relatedSize()).isEqualTo(2);
                    }
                });

        data.putRelated(105, new HierarchicalMultipliersData.Geo()
                .withIsHidden("105")
                .withMultiplierPct("105")
                .withRegionId("105"));
        String description = "Added one";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "0-105,1-102,3-104")
                .containsEntry("is_hidden_0", "105")
                .containsEntry("multiplier_pct_0", "105")
                .containsEntry("region_id_0", "105")
                .containsEntry("is_hidden_1", "102")
                .containsEntry("multiplier_pct_1", "102")
                .containsEntry("region_id_1", "102")
                .doesNotContainKey("is_hidden_2")
                .doesNotContainKey("multiplier_pct_2")
                .doesNotContainKey("region_id_2")
                .containsEntry("is_hidden_3", "104")
                .containsEntry("multiplier_pct_3", "104")
                .containsEntry("region_id_3", "104");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(3);

        data.putRelated(106, new HierarchicalMultipliersData.Geo()
                .withIsHidden("106")
                .withMultiplierPct("106")
                .withRegionId("106"));
        description = "Added two";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "0-105,1-102,2-106,3-104")
                .containsEntry("is_hidden_0", "105")
                .containsEntry("multiplier_pct_0", "105")
                .containsEntry("region_id_0", "105")
                .containsEntry("is_hidden_1", "102")
                .containsEntry("multiplier_pct_1", "102")
                .containsEntry("region_id_1", "102")
                .containsEntry("is_hidden_2", "106")
                .containsEntry("multiplier_pct_2", "106")
                .containsEntry("region_id_2", "106")
                .containsEntry("is_hidden_3", "104")
                .containsEntry("multiplier_pct_3", "104")
                .containsEntry("region_id_3", "104");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(4);

        data.putRelated(107, new HierarchicalMultipliersData.Geo()
                .withIsHidden("107")
                .withMultiplierPct("107")
                .withRegionId("107"));
        description = "Added three";
        softly.assertThat(HierarchicalMultipliersData.toFieldValueList(data, 1).toMap())
                .describedAs(description)
                .containsEntry("related_ids", "0-105,1-102,2-106,3-104,4-107")
                .containsEntry("is_hidden_0", "105")
                .containsEntry("multiplier_pct_0", "105")
                .containsEntry("region_id_0", "105")
                .containsEntry("is_hidden_1", "102")
                .containsEntry("multiplier_pct_1", "102")
                .containsEntry("region_id_1", "102")
                .containsEntry("is_hidden_2", "106")
                .containsEntry("multiplier_pct_2", "106")
                .containsEntry("region_id_2", "106")
                .containsEntry("is_hidden_3", "104")
                .containsEntry("multiplier_pct_3", "104")
                .containsEntry("region_id_3", "104")
                .containsEntry("is_hidden_4", "107")
                .containsEntry("multiplier_pct_4", "107")
                .containsEntry("region_id_4", "107");
        softly.assertThat(data.relatedIsEmpty())
                .describedAs(description)
                .isFalse();
        softly.assertThat(data.relatedSize())
                .describedAs(description)
                .isEqualTo(5);
    }
}
