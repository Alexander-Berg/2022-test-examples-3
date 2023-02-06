package ru.yandex.market.promo.dyn;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.promo.dao.PromoDao;
import ru.yandex.market.core.supplier.promo.model.PromoType;
import ru.yandex.market.core.supplier.promo.model.StrategyType;
import ru.yandex.market.core.supplier.promo.service.PromoService;
import ru.yandex.market.promo.model.*;
import ru.yandex.market.promo.service.PromoYTService;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.promo.model.ParsePromoYtData.isToday;

public class AnaplanPromoExportExecutorTest extends FunctionalTest {
    @Autowired
    private AnaplanPromoExportExecutor anaplanPromoExportExecutor;

    @Autowired
    private PromoYTService promoYTService;

    @Autowired
    private PromoService promoService;

    @Test
    public void getLastTablesTest() {
        Map<PromoDao.AnaplanTable, PromoplanYTTableInfo> result =
                anaplanPromoExportExecutor.getLastTables("cluster");
        assertEquals(result.size(), PromoDao.AnaplanTable.values().length);
        assertNull(result.get(PromoDao.AnaplanTable.PARENT));
        assertNull(result.get(PromoDao.AnaplanTable.CHANNELS));
        assertNull(result.get(PromoDao.AnaplanTable.OPERATIONAL));
        assertNull(result.get(PromoDao.AnaplanTable.RESTRICTION_MSKU));
        assertNull(result.get(PromoDao.AnaplanTable.RESTRICTION));
    }

    @Test
    public void getMinInstantOfTablesTest() {
        Instant minusInstant = Instant.now().minus(2, ChronoUnit.HOURS);
        Map<PromoDao.AnaplanTable, PromoplanYTTableInfo> tablePromoplanYtTableInfo =
                Map.of(
                        PromoDao.AnaplanTable.PARENT,
                        new PromoplanYTTableInfo("parent_path", Instant.EPOCH,
                                Instant.now().minus(1, ChronoUnit.HOURS), Instant.EPOCH),
                        PromoDao.AnaplanTable.CHANNELS,
                        new PromoplanYTTableInfo("parent_path", minusInstant, minusInstant, Instant.EPOCH),
                        PromoDao.AnaplanTable.OPERATIONAL, new PromoplanYTTableInfo("parent_path", Instant.now(),
                                Instant.now(), Instant.EPOCH),
                        PromoDao.AnaplanTable.RESTRICTION_MSKU, new PromoplanYTTableInfo("parent_path", Instant.now(),
                                Instant.now(), Instant.EPOCH),
                        PromoDao.AnaplanTable.RESTRICTION, new PromoplanYTTableInfo("parent_path", Instant.now(),
                                Instant.now(), Instant.EPOCH)
                );
        Instant instant = anaplanPromoExportExecutor.getMinInstantOfTables(tablePromoplanYtTableInfo);
        assertEquals(Instant.EPOCH, instant);
    }


    @Test
    public void testDoJob() {
        Map<String, ExportOperationalPromoYtTable> promos = new HashMap<>();

        String newPromoId = "#5001";
        String newPromoIdName = "newPromo";
        promos.put(newPromoId, new ExportOperationalPromoYtTable.Builder()
                .withId(newPromoId)
                .withName(newPromoIdName)
                .withType(PromoType.DISCOUNT)
                .withStatus(AnaplanPromoStatus.READY)
                .withParentPromoId("parentPromoId")
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate(LocalDateTime.now().minusDays(7))
                                .withEndDate(LocalDateTime.now().minusDays(1))
                                .build())
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withStrategyType(StrategyType.NATIONAL)
                                .withPiPublishDate(LocalDateTime.now().minusDays(10))
                                .withPromoUrl("promoUrl")
                                .withLinkText("linkText")
                                .withPromoRulesUrl("promoRulesUrl")
                                .withCompensation(CompensationType.MARKET)
                                .withComment("comment")
                                .withDeadlineDate(LocalDate.now().minusDays(3))
                                .withCreatedAt(LocalDate.now().minusDays(3))
                                .withUpdatedAt(LocalDate.now().minusDays(3))
                                .build())
                .withPromoResponsible(
                        new PromoResponsibleYt.Builder()
                                .withTm("tm")
                                .withAuthor("author")
                                .withMarcom("marcom")
                                .withMarcomLogin("marcomLogin")
                                .withTmLogin("tmLogin")
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .build());

        String hahn = "hahn";
        ExportRestrictionYtTable.Builder restrictionYtTable =
                new ExportRestrictionYtTable.Builder(newPromoId);

        Map<String, ExportRestrictionYtTable.Builder> restrictionYtTableMap = new HashMap<>();
        restrictionYtTableMap.put(newPromoId, restrictionYtTable);
        when(promoYTService.getExportRestrictionPromo(Set.of(newPromoId), hahn)).thenReturn(restrictionYtTableMap);
        when(promoYTService.getCategoryRestriction(Set.of(newPromoId), hahn)).thenReturn(
                Map.of(newPromoId, new ExportRestrictionYtTable.OriginalCategoryRestriction("false")
                        .addCategory("1", "10"))
        );
        when(promoYTService.getHIDCategoryRestriction(Set.of(newPromoId), hahn)).thenReturn(
                Map.of(newPromoId, Map.of(1L,
                        new ExportRestrictionYtTable.HidCategory.Builder()
                                .withHid("1")
                                .withDiscount("10")
                                .build()))
        );
        Map<String, ExportRestrictionYtTable.Builder> restriction = new HashMap<>();
        restriction.put(newPromoId, restrictionYtTable);
        when(promoYTService.getExportRestrictionPromo(Set.of(newPromoId), hahn))
                .thenReturn(restriction);
        doNothing().when(promoService).addPostponedStepEvent(anyLong(), anyLong());

        mockCluster(hahn);

        Mockito.when(promoYTService.getExportOperationalPromo(
                LocalDate.of(2021, 1, 28)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC), hahn)).thenReturn(promos);
        doReturn(Collections.emptyList())
                .when(promoYTService).selectAllErrors();
        doNothing().when(promoYTService).upsertPromoDescriptionError(anyList());

        var promoDescriptionErrors =
                ArgumentCaptor.forClass(List.class);
        anaplanPromoExportExecutor.doJob(null);
        Mockito.verify(promoYTService, times(1)).upsertPromoDescriptionError(promoDescriptionErrors.capture());

        var eventPromoDescriptionErrors = promoDescriptionErrors.getValue();
        assertEquals(0, eventPromoDescriptionErrors.size());
    }


    // для промо у которого sendPromoPi = false  и для "Самый дешевый в подарок" или ПС формируется ассортимент только для 1P
    @Test
    public void testDoJobPromoNotForPi() {
        Map<String, ExportOperationalPromoYtTable> promos = new HashMap<>();
        String promoId = "#6001";
        String name = "PromoForKi";
        promos.put(promoId, new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withName(name)
                .withType(PromoType.DISCOUNT)
                .withStatus(AnaplanPromoStatus.READY)
                .withParentPromoId("parentPromoId")
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate(LocalDateTime.now().minusDays(7))
                                .withEndDate(LocalDateTime.now().minusDays(1))
                                .build())
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withStrategyType(StrategyType.NATIONAL)
                                .withPiPublishDate(LocalDateTime.now().minusDays(10))
                                .withPromoUrl("promoUrl")
                                .withLinkText("linkText")
                                .withPromoRulesUrl("promoRulesUrl")
                                .withCompensation(CompensationType.MARKET)
                                .withComment("comment")
                                .withDeadlineDate(LocalDate.now().minusDays(3))
                                .withCreatedAt(LocalDate.now().minusDays(3))
                                .withUpdatedAt(LocalDate.now().minusDays(3))
                                .withSendPromoPi(false)
                                .build())
                .withPromoResponsible(
                        new PromoResponsibleYt.Builder()
                                .withTm("tm")
                                .withAuthor("author")
                                .withMarcom("marcom")
                                .withMarcomLogin("marcomLogin")
                                .withTmLogin("tmLogin")
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .build());
        String hahn = "hahn";

        ExportRestrictionYtTable.Builder restrictionYtTable =
                new ExportRestrictionYtTable.Builder(promoId);

        Map<String, ExportRestrictionYtTable.Builder> restrictionYtTableMap = new HashMap<>();
        restrictionYtTableMap.put(promoId, restrictionYtTable);
        when(promoYTService.getExportRestrictionPromo(Set.of(promoId), hahn)).thenReturn(restrictionYtTableMap);
        when(promoYTService.getCategoryRestriction(Set.of(promoId), hahn)).thenReturn(
                Map.of(promoId, new ExportRestrictionYtTable.OriginalCategoryRestriction("false")
                        .addCategory("1", "10"))
        );
        when(promoYTService.getHIDCategoryRestriction(Set.of(promoId), hahn)).thenReturn(
                Map.of(promoId, Map.of(1L,
                        new ExportRestrictionYtTable.HidCategory.Builder()
                                .withHid("1")
                                .withDiscount("10")
                                .build()))
        );
        mockCluster(hahn);
        Mockito.when(promoYTService.getExportOperationalPromo(
                LocalDate.of(2021, 1, 28)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC), hahn)).thenReturn(promos);
        doReturn(Collections.emptyList())
                .when(promoYTService).selectAllErrors();
        doNothing().when(promoYTService).upsertPromoDescriptionError(anyList());
        doNothing().when(promoService).addPostponedStepEvent(anyLong(), anyLong());

        var promoDescriptionErrors =
                ArgumentCaptor.forClass(List.class);
        anaplanPromoExportExecutor.doJob(null);
        Mockito.verify(promoYTService, times(1)).upsertPromoDescriptionError(promoDescriptionErrors.capture());

        var eventPromoDescriptionErrors = promoDescriptionErrors.getValue();
        assertEquals(0, eventPromoDescriptionErrors.size());
    }

    // для промо у которого sendPromoPi = true и вендорского формируется ассортимент только для 1P
    @Test
    public void testDoJobPromoNotForPiVendor() {
        Map<String, ExportOperationalPromoYtTable> promos = new HashMap<>();
        String promoId = "#6001";
        String name = "PromoForKi";
        promos.put(promoId, new ExportOperationalPromoYtTable.Builder()
                .withId(promoId)
                .withName(name)
                .withType(PromoType.DISCOUNT)
                .withStatus(AnaplanPromoStatus.READY)
                .withParentPromoId("parentPromoId")
                .withPromoConstraints(
                        new PromoConstraintsYt.Builder()
                                .withStartDate(LocalDateTime.now().minusDays(7))
                                .withEndDate(LocalDateTime.now().minusDays(1))
                                .build())
                .withAdditionalInfo(
                        new AdditionalInfoYt.Builder()
                                .withStrategyType(StrategyType.VENDOR)
                                .withPiPublishDate(LocalDateTime.now().minusDays(10))
                                .withPromoUrl("promoUrl")
                                .withLinkText("linkText")
                                .withPromoRulesUrl("promoRulesUrl")
                                .withCompensation(CompensationType.MARKET)
                                .withComment("comment")
                                .withDeadlineDate(LocalDate.now().minusDays(3))
                                .withCreatedAt(LocalDate.now().minusDays(3))
                                .withUpdatedAt(LocalDate.now().minusDays(3))
                                .withSendPromoPi(true)
                                .build())
                .withPromoResponsible(
                        new PromoResponsibleYt.Builder()
                                .withTm("tm")
                                .withAuthor("author")
                                .withMarcom("marcom")
                                .withMarcomLogin("marcomLogin")
                                .withTmLogin("tmLogin")
                                .build())
                .withPromoCheckInfo(getPromoCheckInfo())
                .build());
        String hahn = "hahn";

        ExportRestrictionYtTable.Builder restrictionYtTable =
                new ExportRestrictionYtTable.Builder(promoId);

        Map<String, ExportRestrictionYtTable.Builder> restrictionYtTableMap = new HashMap<>();
        restrictionYtTableMap.put(promoId, restrictionYtTable);
        when(promoYTService.getExportRestrictionPromo(Set.of(promoId), hahn)).thenReturn(restrictionYtTableMap);
        when(promoYTService.getCategoryRestriction(Set.of(promoId), hahn)).thenReturn(
                Map.of(promoId, new ExportRestrictionYtTable.OriginalCategoryRestriction("false")
                        .addCategory("1", "10"))
        );
        when(promoYTService.getHIDCategoryRestriction(Set.of(promoId), hahn)).thenReturn(
                Map.of(promoId, Map.of(1L,
                        new ExportRestrictionYtTable.HidCategory.Builder()
                                .withHid("1")
                                .withDiscount("10")
                                .build()))
        );
        mockCluster(hahn);
        Mockito.when(promoYTService.getExportOperationalPromo(
                LocalDate.of(2021, 1, 28)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC), hahn)).thenReturn(promos);
        doReturn(Collections.emptyList())
                .when(promoYTService).selectAllErrors();
        doNothing().when(promoYTService).upsertPromoDescriptionError(anyList());
        doNothing().when(promoService).addPostponedStepEvent(anyLong(), anyLong());

        var promoDescriptionErrors =
                ArgumentCaptor.forClass(List.class);
        anaplanPromoExportExecutor.doJob(null);
        Mockito.verify(promoYTService, times(1)).upsertPromoDescriptionError(promoDescriptionErrors.capture());

        var eventPromoDescriptionErrors = promoDescriptionErrors.getValue();
        assertEquals(0, eventPromoDescriptionErrors.size());
    }

    private void mockCluster(String cluster) {
        Mockito.when(promoYTService.getRandomCluster()).thenReturn(cluster);
        PromoplanYTTableInfo promoplanPath = new PromoplanYTTableInfo("promoplan_path", Instant.EPOCH,
                Instant.EPOCH, Instant.EPOCH);
        Mockito.when(promoYTService.getPromoplanTablePathIfChanged(PromoDao.AnaplanTable.RESTRICTION_MSKU,
                Instant.EPOCH, cluster))
                .thenReturn(promoplanPath);
        Mockito.when(promoYTService.getPromoplanTablePathIfChanged(PromoDao.AnaplanTable.RESTRICTION, Instant.EPOCH,
                cluster))
                .thenReturn(promoplanPath);
        Mockito.when(promoYTService.getPromoplanTablePathIfChanged(PromoDao.AnaplanTable.CHANNELS, Instant.EPOCH,
                cluster))
                .thenReturn(promoplanPath);
        Mockito.when(promoYTService.getPromoplanTablePathIfChanged(PromoDao.AnaplanTable.OPERATIONAL, Instant.EPOCH,
                cluster))
                .thenReturn(promoplanPath);
        Mockito.when(promoYTService.getPromoplanTablePathIfChanged(PromoDao.AnaplanTable.PARENT, Instant.EPOCH, cluster))
                .thenReturn(promoplanPath);
    }

    @Test
    public void testIsToday() {
        Instant instantDayBefore = Instant.now().minus(Duration.ofDays(1));
        assertFalse(isToday(instantDayBefore));
        Instant instantNow = Instant.now();
        assertTrue(isToday(instantNow));
    }

    private PromoCheckInfo getPromoCheckInfo() {
        return new PromoCheckInfo.Builder()
                .withPromoId("#1")
                .withOriginalRestrictionCategoryCount("1")
                .withOriginalExcludeRestrictionCategoryCount("0")
                .withOriginalBrandCount("0")
                .withOriginalExcludeBrandCount("0")
                .withChannelsCount("0")
                .withMskuCount("0")
                .withMskuExcludeCount("0")
                .withSupplierCount("0")
                .withSupplierExcludeCount("")
                .withWarehouseCount("0")
                .withWarehouseExcludeCount("0")
                .withVendorTriggerCount("")
                .withVendorTriggerExcludeCount("0")
                .withHidTriggerCount("0")
                .withHidTriggerExcludeCount("0")
                .withRegionCount("0")
                .withRegionExcludeCount("0")
                .withRegionTriggerCount("0")
                .withRegionTriggerExcludeCount("0")
                .build();
    }
}
