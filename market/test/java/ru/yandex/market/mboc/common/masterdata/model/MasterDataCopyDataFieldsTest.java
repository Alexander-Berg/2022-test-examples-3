package ru.yandex.market.mboc.common.masterdata.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertFalse;

/**
 * @author jkt on 01.08.18.
 */
public class MasterDataCopyDataFieldsTest {

    private static final long SEED = 1359;
    private static final String[] NOT_COPYING_FIELDS = {"shopSku", "supplierId", "qualityDocuments", "categoryId"};
    private static final String[] NOT_UPDATING_FIELDS = {"shopSku", "supplierId", "modifiedTimestamp", "categoryId"};
    private MasterData masterData;
    private MasterData anotherMasterData;

    @Before
    public void initData() {
        EnhancedRandom random = TestDataUtils.defaultRandom(SEED);
        masterData = TestDataUtils.generateMasterData(
            "sku1", 1, random, TestDataUtils.generateDocument(random)
        );
        anotherMasterData = TestDataUtils.generateMasterData(
            "sku2", 2, random, TestDataUtils.generateCorrectDocument(random)
        );
    }

    @Test
    public void testCopyDataFields() {
        masterData.copyDataFieldsFrom(anotherMasterData);
        assertThat(masterData).isEqualToIgnoringGivenFields(anotherMasterData, NOT_COPYING_FIELDS);
    }

    @Test
    public void shouldNotCopyIdFieldsAndDocuments() {
        String shopSku = masterData.getShopSku();
        int supplierId = masterData.getSupplierId();
        List<QualityDocument> qualityDocuments = masterData.getQualityDocuments();

        masterData.copyDataFieldsFrom(anotherMasterData);
        assertSoftly(softly -> {
            softly.assertThat(masterData.getShopSku()).isEqualTo(shopSku);
            softly.assertThat(masterData.getSupplierId()).isEqualTo(supplierId);
            softly.assertThat(masterData.getQualityDocuments())
                .containsExactly(qualityDocuments.toArray(new QualityDocument[0]));
        });
    }

    @Test
    public void whenArgumentIsNullShouldNotFail() {
        assertThatCode(() -> masterData.copyDataFieldsFrom(null))
            .doesNotThrowAnyException();
        assertThatCode(() -> masterData.updateFrom(null))
            .doesNotThrowAnyException();
    }

    @Test
    public void whenUpdateDataFieldsShouldSetCorrectValues() {
        anotherMasterData.setManufacturerCountries(Collections.singletonList("Китай"));
        anotherMasterData.setSupplySchedule(Arrays.asList(
            new SupplyEvent(DayOfWeek.MONDAY), new SupplyEvent(DayOfWeek.THURSDAY), new SupplyEvent(DayOfWeek.FRIDAY)
        ));

        masterData.updateFrom(anotherMasterData);
        assertThat(masterData).isEqualToIgnoringGivenFields(anotherMasterData, NOT_UPDATING_FIELDS);
    }

    @Test
    public void whenUpdateCollectionFieldByEmptyCollectionShouldNotUpdate() {
        anotherMasterData.getManufacturerCountries().clear();
        anotherMasterData.getSupplySchedule().clear();
        anotherMasterData.getQualityDocuments().clear();

        List<String> countriesBefore = masterData.getManufacturerCountries();
        List<SupplyEvent> scheduleBefore = masterData.getSupplySchedule();
        List<QualityDocument> documentsBefore = masterData.getQualityDocuments();

        masterData.updateFrom(anotherMasterData);
        assertSoftly(softly -> {
            softly.assertThat(masterData.getManufacturerCountries())
                .containsExactlyInAnyOrderElementsOf(countriesBefore);
            softly.assertThat(masterData.getSupplySchedule()).containsExactlyInAnyOrderElementsOf(scheduleBefore);
            softly.assertThat(masterData.getQualityDocuments()).containsExactlyInAnyOrderElementsOf(documentsBefore);
        });
    }

    @Test
    public void shouldNotUpdateIdFieldsAndInternalParams() {
        String shopSku = masterData.getShopSku();
        int supplierId = masterData.getSupplierId();
        LocalDateTime modifiedTimestamp = masterData.getModifiedTimestamp();

        masterData.updateFrom(anotherMasterData);
        assertSoftly(softly -> {
            softly.assertThat(masterData.getShopSku()).isEqualTo(shopSku);
            softly.assertThat(masterData.getSupplierId()).isEqualTo(supplierId);
            softly.assertThat(masterData.getModifiedTimestamp()).isEqualTo(modifiedTimestamp);
        });
    }

    @Test
    public void shouldOnlyUpdateExistingFields() {
        anotherMasterData.setMinShipment(MasterData.NO_VALUE);
        anotherMasterData.setLifeTime(null);
        anotherMasterData.setManufacturer("");
        int minShipment = masterData.getMinShipment();
        TimeInUnits lifeTime = masterData.getLifeTime();
        String manufacturer = masterData.getManufacturer();

        assertFalse(anotherMasterData.hasMinShipment());
        assertFalse(anotherMasterData.hasLifeTime());
        assertFalse(anotherMasterData.hasManufacturer());

        masterData.updateFrom(anotherMasterData);
        List<String> ignoredFields = new ArrayList<>(Arrays.asList(NOT_UPDATING_FIELDS));
        ignoredFields.add("minShipment");
        ignoredFields.add("lifeTime");
        ignoredFields.add("manufacturer");
        assertThat(masterData).isEqualToIgnoringGivenFields(anotherMasterData, ignoredFields.toArray(new String[0]));
        assertThat(masterData.getMinShipment()).isEqualTo(minShipment);
        assertThat(masterData.getLifeTime()).isEqualTo(lifeTime);
        assertThat(masterData.getManufacturer()).isEqualTo(manufacturer);
    }
}
