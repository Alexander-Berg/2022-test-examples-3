package ru.yandex.market.core.feature.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.DuplicateCutoffException;
import ru.yandex.market.core.error.EntityNotFoundException;
import ru.yandex.market.core.feature.db.FeatureCutoffDao;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffReason;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP_BY_SELLER;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_SELF_DELIVERY;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class FeatureCutoffDaoTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FeatureCutoffDao featureCutoffDao;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    @DbUnitDataSet(before = "datasource.csv", after = "singleCutoff.csv")
    void openCutoff() {
        FeatureCutoffInfo cutoffInfo = featureCutoffDao.openCutoff(
                buildFeatureCutoffInfo(
                        1,
                        FeatureType.SUBSIDIES,
                        FeatureCutoffType.QUALITY,
                        "q",
                        new Date(),
                        FeatureCutoffReason.PERCENT_CANCELLED_ORDERS),
                100501
        );
        assertNotNull("Expected ID was set", cutoffInfo.getId());
        assertTrue("Expected ID was set", cutoffInfo.getId() > 0);
        assertEquals("Opened disabled period",
                Integer.valueOf(1), jdbcTemplate.queryForObject(
                        "select count(*) from shops_web.feature_disabled_period where active = 1", Integer.class));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"})
    void openDuplicateCutoff() {
        Assertions.assertThrows(
                DuplicateCutoffException.class,
                () -> featureCutoffDao.openCutoff(
                        buildFeatureCutoffInfo(
                                1,
                                FeatureType.SUBSIDIES,
                                FeatureCutoffType.QUALITY,
                                "q",
                                new Date(),
                                FeatureCutoffReason.PERCENT_CANCELLED_ORDERS),
                        1
                )
        );
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv", "disabledPeriod.csv"}, after = "singleClosedCutoff.csv")
    void closeCutoff() {
        featureCutoffDao.closeCutoff(1, FeatureType.SUBSIDIES, FeatureCutoffType.QUALITY, 100501);
        assertEquals("Expected no open cutoffs",
                0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "shops_web.feature_open_cutoff"));
        assertEquals("Closed disabled period",
                Integer.valueOf(0), jdbcTemplate.queryForObject(
                        "select count(*) from shops_web.feature_disabled_period where active = 1", Integer.class));
    }

    @Test
    @DbUnitDataSet
    void closeCutoffNonExistent() {
        Assertions.assertThrows(
                EntityNotFoundException.class,
                () -> featureCutoffDao.closeCutoff(1, FeatureType.SUBSIDIES, FeatureCutoffType.QUALITY, 1)
        );
    }

    @Test
    @DbUnitDataSet(before = "datasource.csv", after = "singleCutoff.csv")
    void ensureCutoffOpen() {
        featureCutoffDao.ensureCutoffOpen(
                buildFeatureCutoffInfo(
                        1,
                        FeatureType.SUBSIDIES,
                        FeatureCutoffType.QUALITY,
                        "q",
                        new Date(),
                        FeatureCutoffReason.PERCENT_CANCELLED_ORDERS),
                100501
        );
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"}, after = "singleCutoff.csv")
    void ensureCutoffOpenWhenExists() throws Exception {
        featureCutoffDao.ensureCutoffOpen(
                buildFeatureCutoffInfo(
                        1,
                        FeatureType.SUBSIDIES,
                        FeatureCutoffType.QUALITY,
                        "q",
                        new Date(),
                        FeatureCutoffReason.PERCENT_CANCELLED_ORDERS),
                100501
        );
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"}, after = "empty.csv")
    void ensureCutoffClosed() {
        featureCutoffDao.ensureCutoffClosed(1, FeatureType.SUBSIDIES, FeatureCutoffType.QUALITY, 100501);
    }

    @Test
    @DbUnitDataSet(after = "empty.csv")
    void ensureCutoffClosedWhenAbsent() {
        featureCutoffDao.ensureCutoffClosed(1, FeatureType.SUBSIDIES, FeatureCutoffType.QUALITY, 1);
    }

    @Test
    @DbUnitDataSet
    void hasCutoff() {
        assertFalse(featureCutoffDao.hasCutoff(1, FeatureType.SUBSIDIES));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"})
    void hasCutoffExists() {
        assertTrue(featureCutoffDao.hasCutoff(1, FeatureType.SUBSIDIES));
    }

    @Test
    @DbUnitDataSet
    void hasCutoffType() {
        assertFalse(featureCutoffDao.hasCutoff(1, FeatureType.SUBSIDIES, FeatureCutoffType.QUALITY));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"})
    void hasCutoffTypeExists() {
        assertTrue(featureCutoffDao.hasCutoff(1, FeatureType.SUBSIDIES, FeatureCutoffType.QUALITY));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"})
    void getCutoffById() {
        FeatureCutoffInfo cutoffInfo = featureCutoffDao.getCutoff(1);
        assertThat(cutoffInfo, hasProperty("id", is(1L)));
        assertThat(cutoffInfo, hasProperty("datasourceId", is(1L)));
        assertThat(cutoffInfo, hasProperty("featureCutoffType", is(FeatureCutoffType.QUALITY)));
        assertThat(cutoffInfo, hasProperty("featureType", is(FeatureType.SUBSIDIES)));
        assertThat(cutoffInfo, hasProperty("comment", is("q")));
        assertThat(cutoffInfo, hasProperty("startDate", notNullValue()));
        assertThat(cutoffInfo, hasProperty("reason", equalTo(FeatureCutoffReason.PERCENT_CANCELLED_ORDERS)));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"})
    void getCutoffByParameters() {
        FeatureCutoffInfo cutoffInfo =
                featureCutoffDao.getCutoffs(1, FeatureType.SUBSIDIES).stream()
                        .filter(c -> c.getFeatureCutoffType() == FeatureCutoffType.QUALITY).findAny()
                        .orElseThrow(() -> new RuntimeException("Cutoff not found"));
        assertThat(cutoffInfo, hasProperty("datasourceId", is(1L)));
        assertThat(cutoffInfo, hasProperty("featureCutoffType", is(FeatureCutoffType.QUALITY)));
        assertThat(cutoffInfo, hasProperty("featureType", is(FeatureType.SUBSIDIES)));
        assertThat(cutoffInfo, hasProperty("comment", is("q")));
        assertThat(cutoffInfo, hasProperty("startDate", notNullValue()));
        assertThat(cutoffInfo, hasProperty("reason", equalTo(FeatureCutoffReason.PERCENT_CANCELLED_ORDERS)));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"})
    void getCutoffs() {
        Collection<FeatureCutoffInfo> cutoffs =
                featureCutoffDao.getCutoffs(1, FeatureType.SUBSIDIES);
        assertEquals("One cutoff", 1, cutoffs.size());
        FeatureCutoffInfo cutoffInfo = cutoffs.iterator().next();
        assertThat(cutoffInfo, hasProperty("datasourceId", is(1L)));
        assertThat(cutoffInfo, hasProperty("featureCutoffType", is(FeatureCutoffType.QUALITY)));
        assertThat(cutoffInfo, hasProperty("featureType", is(FeatureType.SUBSIDIES)));
        assertThat(cutoffInfo, hasProperty("comment", is("q")));
        assertThat(cutoffInfo, hasProperty("startDate", notNullValue()));
        assertThat(cutoffInfo, hasProperty("reason", equalTo(FeatureCutoffReason.PERCENT_CANCELLED_ORDERS)));
    }

    @Test
    @DbUnitDataSet(before = {"datasourceCutoff.csv", "reopenCutoff.csv"}, after = "afterReopenCutoff.csv")
    void reopenCutoff() {
        Clock fixedClock = Clock.fixed(
                LocalDate.of(2022, 6, 23)
                        .atTime(10,0,0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        when(clock.instant()).thenReturn(fixedClock.instant());
        featureCutoffDao.reopenCutoff(4438660, 100501, Set.of(
                MARKETPLACE_SELF_DELIVERY,
                DROPSHIP,
                MARKETPLACE,
                DROPSHIP_BY_SELLER
                )
        );
    }


    private FeatureCutoffInfo buildFeatureCutoffInfo(long shopId,
                                                     FeatureType featureType,
                                                     FeatureCustomCutoffType featureCutoffType,
                                                     String comment,
                                                     Date startDate,
                                                     FeatureCutoffReason reason) {
        return new FeatureCutoffInfo.Builder()
                .setDatasourceId(shopId)
                .setFeatureType(featureType)
                .setFeatureCutoffType(featureCutoffType)
                .setComment(comment)
                .setStartDate(startDate)
                .setReason(reason)
                .build();
    }
}
