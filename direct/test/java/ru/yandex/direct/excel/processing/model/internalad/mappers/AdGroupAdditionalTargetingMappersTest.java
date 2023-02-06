package ru.yandex.direct.excel.processing.model.internalad.mappers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DesktopInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLangsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEngine;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserName;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceVendor;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceVendorsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamily;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsName;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.excel.processing.model.internalad.AdGroupAdditionalTargetingRepresentation;
import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.MapperTestUtils;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.allValidInternalAdAdditionalTargetingsForExcel;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validDeviceIdsTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validInternalNetworkTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validPlusUserSegmentsTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validSearchTextTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validSidsTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validUuidsTargeting;

public class AdGroupAdditionalTargetingMappersTest {

    private static final ExcelMapper<AdGroupAdditionalTargetingRepresentation> MAPPER =
            AdGroupAdditionalTargetingMapper.AD_GROUP_TARGETING_MAPPER;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void exportAndImportEmpty() {
        checkExportThenImport(Collections.emptyList());
    }

    @Test
    public void exportAndImportAdGroup_allValidInternalAdAdditionalTargetings() {
        var targetings = allValidInternalAdAdditionalTargetingsForExcel();
        checkExportThenImport(targetings);
    }

    @Test
    public void exportAndImportAdGroup_VersionedAdditionalTargeting() {
        OsFamily osFamily = new OsFamily()
                .withTargetingValueEntryId(2L)
                .withMaxVersion("321")
                .withMinVersion("123");
        var targetings = List.of(new OsFamiliesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(List.of(osFamily)));

        SheetRange sheetRange = MapperTestUtils.createEmptySheet();
        MAPPER.write(sheetRange, new AdGroupAdditionalTargetingRepresentation(targetings));

        // при импорте для версий добавляем .0, ядро ожидает число с точкой
        osFamily.setMaxVersion(osFamily.getMaxVersion() + ".0");
        osFamily.setMinVersion(osFamily.getMinVersion() + ".0");
        checkImport(sheetRange, targetings);
    }

    @Test
    public void exportAndImportAdGroup_AdditionalTargetingWithListOfStringsValue() {
        var targetings = List.of(validSidsTargeting(), validDeviceIdsTargeting(), validUuidsTargeting(),
                validPlusUserSegmentsTargeting(), validSearchTextTargeting());

        checkExportThenImport(targetings);
    }

    @Test
    public void exportAndImportAdGroup_AdditionalUatraitsTargeting() {
        var targetings = List.of(new OsNamesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(List.of(new OsName().withTargetingValueEntryId(2L))));

        checkExportThenImport(targetings);
    }

    @Test
    public void exportAndImportAdGroup_MultipleTargeting() {
        var targetings = List.of(
                new DeviceVendorsAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withValue(List.of(new DeviceVendor().withTargetingValueEntryId(3L))),
                new DeviceNamesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                        .withValue(List.of("value")),
                new BrowserEnginesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withValue(List.of(new BrowserEngine()
                                .withTargetingValueEntryId(2L)
                                .withMaxVersion("321.0")
                                .withMinVersion("123.0"))),
                new BrowserEnginesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                        .withValue(List.of(new BrowserEngine()
                                .withTargetingValueEntryId(3L))));

        checkExportThenImport(targetings);
    }

    @Test
    public void importAdGroup_IgnoreValueCaseForTargetingsWithChoices() {
        var targetings = List.of(
                validInternalNetworkTargeting(),
                new InterfaceLangsAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                        .withValue(Set.of(InterfaceLang.UK)),
                new DesktopInstalledAppsAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withValue(Set.of(1L)),
                new DeviceVendorsAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withValue(List.of(new DeviceVendor().withTargetingValueEntryId(4L))),
                new BrowserNamesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withValue(List.of(new BrowserName().withTargetingValueEntryId(6L))));

        SheetRange sheetRange = MapperTestUtils.createEmptySheet();
        MAPPER.write(sheetRange, new AdGroupAdditionalTargetingRepresentation(targetings));

        updateTargetingValueInCell(sheetRange, "дА", targetings.get(0));
        updateTargetingValueInCell(sheetRange, "uK", targetings.get(1));
        updateTargetingValueInCell(sheetRange, "bROwsEr", targetings.get(2));
        updateTargetingValueInCell(sheetRange, "ApPlE", targetings.get(3));
        updateTargetingValueInCell(sheetRange, "fiREfOX", targetings.get(4));

        checkImport(sheetRange, targetings);
    }

    @Test
    public void importAdGroup_InvalidValueForTargetingsWithChoices() {
        var desktopInstalledAppsTargeting = new DesktopInstalledAppsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of(1L, 2L));
        var deviceVendorsTargeting = new DeviceVendorsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(List.of(new DeviceVendor().withTargetingValueEntryId(4L),
                        new DeviceVendor().withTargetingValueEntryId(5L),
                        new DeviceVendor().withTargetingValueEntryId(6L)));
        var browserNamesTargeting = new BrowserNamesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(List.of(new BrowserName().withTargetingValueEntryId(10L),
                        new BrowserName().withTargetingValueEntryId(11L),
                        new BrowserName()
                                .withTargetingValueEntryId(12L)
                                .withMaxVersion("12.4")
                                .withMinVersion("7.0")));
        var targetings = List.of(desktopInstalledAppsTargeting, deviceVendorsTargeting, browserNamesTargeting);

        SheetRange sheetRange = MapperTestUtils.createEmptySheet();
        MAPPER.write(sheetRange, new AdGroupAdditionalTargetingRepresentation(targetings));

        updateTargetingValueInCell(sheetRange, "invalid_DesktopInstalled1", 0, desktopInstalledAppsTargeting);
        updateTargetingValueInCell(sheetRange, "invalid_DesktopInstalled2", 1, desktopInstalledAppsTargeting);
        desktopInstalledAppsTargeting.setValue(Collections.singleton(null));

        updateTargetingValueInCell(sheetRange, "invalid_DeviceVendor1", 0, deviceVendorsTargeting);
        updateTargetingValueInCell(sheetRange, "invalid_DeviceVendor2", 1, deviceVendorsTargeting);
        deviceVendorsTargeting.getValue().get(0).setTargetingValueEntryId(null);
        deviceVendorsTargeting.getValue().get(1).setTargetingValueEntryId(null);

        updateTargetingValueInCell(sheetRange, "invalid_BrowserName1", 0, browserNamesTargeting);
        updateTargetingValueInCell(sheetRange, "invalid_BrowserName2", 1, browserNamesTargeting);
        browserNamesTargeting.getValue().get(0).setTargetingValueEntryId(null);
        browserNamesTargeting.getValue().get(1).setTargetingValueEntryId(null);

        SheetRange readSheetRange = MapperTestUtils.createStringSheetFromLists(
                MapperTestUtils.sheetToLists(sheetRange, MAPPER.getMeta().getWidth()));

        var firstTargetingColumnTitle = AdGroupAdditionalTargetingMapperSettings.getTargetingTitle(targetings.get(0));
        thrown.expect(allOf(
                isA(CantReadFormatException.class),
                hasProperty("columns", equalTo(List.of(firstTargetingColumnTitle))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(MAPPER.getMeta().getColumns().indexOf(firstTargetingColumnTitle)))
        ));
        MAPPER.read(readSheetRange);
    }


    private static void updateTargetingValueInCell(SheetRange sheetRange, String value,
                                                   AdGroupAdditionalTargeting targeting) {
        updateTargetingValueInCell(sheetRange, value, 0, targeting);
    }

    /**
     * Обновляет значение ячейки на указанное.
     * Номер столбца берется по заголовку таргетинга
     */
    private static void updateTargetingValueInCell(SheetRange sheetRange, String value, int row,
                                                   AdGroupAdditionalTargeting targeting) {
        String targetingTitle = AdGroupAdditionalTargetingMapperSettings.getTargetingTitle(targeting);
        int column = MAPPER.getMeta().getColumns().indexOf(targetingTitle);
        checkState(column >= 0);

        sheetRange.getCell(row, column).setCellValue(value);
    }

    private static void checkExportThenImport(Collection<? extends AdGroupAdditionalTargeting> additionalTargetings) {
        SheetRange sheetRange = MapperTestUtils.createEmptySheet();
        MAPPER.write(sheetRange, new AdGroupAdditionalTargetingRepresentation(additionalTargetings));

        checkImport(sheetRange, additionalTargetings);
    }

    /**
     * Порядок таргетингов в additionalTargetings должен соответствовать порядку в маппере
     * {@link AdGroupAdditionalTargetingMapper#AD_GROUP_TARGETING_MAPPER}
     */
    private static void checkImport(SheetRange sheetRange,
                                    Collection<? extends AdGroupAdditionalTargeting> additionalTargetings) {
        SheetRange readSheetRange = MapperTestUtils.createStringSheetFromLists(
                MapperTestUtils.sheetToLists(sheetRange, MAPPER.getMeta().getWidth()));
        AdGroupAdditionalTargetingRepresentation actualValue = MAPPER.read(readSheetRange).getValue();

        assertThat(actualValue.getTargetingList())
                .hasSize(additionalTargetings.size())
                .containsExactlyInAnyOrder(additionalTargetings.toArray(AdGroupAdditionalTargeting[]::new));
    }
}
