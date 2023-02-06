package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.BsExportMultipliersObject;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.DeleteInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.TimeTargetChangedInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.UpsertInfo;

@ContextConfiguration(classes = BsExportMultipliersServiceTestIgnored.EssConfiguration.class)
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@Disabled("Запускать вручную с выставленным -Dyandex.environment.type=development")
public class BsExportMultipliersServiceTestIgnored {
    @Autowired
    private BsExportMultipliersService bsExportBidsService;

    @Test
    public void upsertMobileDesktopTest() {
        BsExportMultipliersObject mobile = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, 508588601L), 0L, "", "");
        BsExportMultipliersObject mobileCamp = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, 508575836L), 0L, "", "");
        BsExportMultipliersObject mobile2 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, 460256919L), 0L, "", "");
        BsExportMultipliersObject desktop = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, 508588596L), 0L, "", "");
        BsExportMultipliersObject desktopCamp = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, 475286227L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(mobile, mobileCamp, mobile2, desktop, desktopCamp));
    }

    @Test
    public void deleteMobileDesktopTest() {
        BsExportMultipliersObject mobile = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEVICE, 233404986L, 4627284771L), 0L, "", "");
        BsExportMultipliersObject mobileCamp = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEVICE, 181166171L, null), 0L, "", "");
        BsExportMultipliersObject mobile2 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEVICE, 47444009L, 4018375273L), 0L, "", "");
        BsExportMultipliersObject desktop = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEVICE, 233404986L, 4627284771L), 0L, "", "");
        BsExportMultipliersObject desktopCamp = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEVICE, 51867605L, null), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(mobile, mobileCamp, mobile2, desktop, desktopCamp));
    }

    @Test
    public void upsertWeatherTest() {
        BsExportMultipliersObject o = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.WEATHER, 403218688L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, Collections.singletonList(o));
    }

    @Test
    public void deleteWeatherTest() {
        BsExportMultipliersObject o = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.WEATHER, 42181454L, 3764381205L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, Collections.singletonList(o));
    }

    @Test
    public void upsertExpressionTest() {
        BsExportMultipliersObject o = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.TRAFFIC, 460356697L), 0L, "", "");
        BsExportMultipliersObject o2 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.CONTENT_DURATION, 508584621L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(o, o2));
    }

    @Test
    public void deleteExpressionTest() {
        BsExportMultipliersObject o = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.TRAFFIC, 49952632L, 4121701709L), 0L, "", "");
        BsExportMultipliersObject o2 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.CONTENT_DURATION, 37347431L, 4626926986L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(o, o2));
    }

    @Test
    public void upsertDemographicTest() {
        BsExportMultipliersObject d1 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEMOGRAPHY, 89614383L), 0L, "", "");
        BsExportMultipliersObject d2 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEMOGRAPHY, 34667734L), 0L, "", "");
        BsExportMultipliersObject d3 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEMOGRAPHY, 34688505L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(d1, d2, d3));
    }

    @Test
    public void deleteDemographicTest() {
        BsExportMultipliersObject d1 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEMOGRAPHY, 14209184L, 890024945L), 0L, "", "");
        BsExportMultipliersObject d2 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEMOGRAPHY, 14010019L, null), 0L, "", "");
        BsExportMultipliersObject d3 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEMOGRAPHY, 14010129L, null), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(d1, d2, d3));
    }

    @Test
    public void upsertRetargetingTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.RETARGETING, 508575851L), 0L, "", "");
        BsExportMultipliersObject m2 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.RETARGETING, 508575811L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(m1, m2));
    }

    @Test
    public void deleteRetargetingTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.RETARGETING, 158747038L, 4626153891L), 0L, "", "");
        BsExportMultipliersObject m2 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.RETARGETING, 181166171L, null), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(m1, m2));
    }

    @Test
    public void upsertInaccessibleRetargetingTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.RETARGETING, 387306770L), 0L, "", "");
        bsExportBidsService.updateMultipliers(17, List.of(m1));
    }

    @Test
    public void deleteInaccessibleRetargetingTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.RETARGETING, 40547857L, null), 0L, "", "");
        bsExportBidsService.updateMultipliers(17, List.of(m1));
    }

    @Test
    public void upsertGeoTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.GEO, 479967040L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(m1));
    }

    @Test
    public void deleteGeoTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.GEO, 51858241L, null), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(m1));
    }

    @Test
    public void demographicRaceTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEMOGRAPHY, 34688505L), 0L, "", "");
        BsExportMultipliersObject m2 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.DEMOGRAPHY, 14010129L, null), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(m1, m2));
    }

    @Test
    public void timeTargetTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.timeTargetChanged(
                new TimeTargetChangedInfo(43260909L), 0L, "", "");
        bsExportBidsService.updateMultipliers(19, List.of(m1));
    }


    @Test
    public void upsertInventoryTypeTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.INVENTORY, 393327019L), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(m1));
    }

    @Test
    public void deleteInventoryTest() {
        BsExportMultipliersObject m1 = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.INVENTORY, 41420045L, null), 0L, "", "");
        bsExportBidsService.updateMultipliers(1, List.of(m1));
    }

    @Configuration
    @Import({
            CoreConfiguration.class,
    })
    @ComponentScan(
            basePackages = {"ru.yandex.direct.logicprocessor", "ru.yandex.direct.bstransport"},
            excludeFilters = {
                    @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION),
            }
    )
    public static class EssConfiguration {

    }
}
