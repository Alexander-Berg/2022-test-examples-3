package ru.yandex.market.mbo.cardrender.app.service.yql;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.cardrender.app.model.table.ExportKeyInfo;
import ru.yandex.market.mbo.cardrender.app.model.table.TableInfo;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 12/14/21
 */
public class ExportCoordinatorTest extends BaseTest {

    private ExportCoordinator exportCoordinator;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    private TableDiffService tableDiffService;
    private FullStuffReader fullStuffReader;
    private DiffStuffReader diffStuffReader;

    @Before
    public void setUp() throws Exception {
        tableDiffService = Mockito.mock(TableDiffService.class);
        fullStuffReader = Mockito.mock(FullStuffReader.class);
        diffStuffReader = Mockito.mock(DiffStuffReader.class);
        exportCoordinator = new ExportCoordinator(
                tableDiffService,
                storageKeyValueService,
                fullStuffReader,
                diffStuffReader);
    }

    public List<ExportKeyInfo> convert(List<String> exportKeys) {
        return exportKeys.stream().map(ExportKeyInfo::extractPojo).collect(Collectors.toList());
    }

    @Test
    public void testImportDiff() {
        ExportKeyInfo workKey = ExportKeyInfo.full("20211205_1903");
        String newKey = "20211205_2003";
        ExportKeyInfo nextKey = ExportKeyInfo.diff(newKey);
        storageKeyValueService.putValue("last_calculated_diff_key", workKey.toString());
        Mockito.when(tableDiffService.findNextKey(Mockito.eq(workKey))).thenReturn(nextKey);
        TableInfo path = TableInfo.info("some_path", nextKey.getExportKey(), 20L);
        Mockito.when(tableDiffService.diffSnapshot(Mockito.eq(nextKey))).thenReturn(path);
        Mockito.when(tableDiffService.exportKey("recent")).thenReturn(newKey);
        exportCoordinator.initializeDiffExport();
        Mockito.verify(diffStuffReader, Mockito.times(1)).handleTable(
                Mockito.eq(path.getFullClusterPath())
        );
        Assertions.assertThat(storageKeyValueService.getString("last_calculated_diff_key", null))
                .isEqualTo(newKey);
    }

    @Test(expected = RuntimeException.class)
    public void testCheckThreshold() {
        ExportKeyInfo workKey = ExportKeyInfo.full("20211205_1903");
        String newKey = "20211205_2003";
        ExportKeyInfo nextKey = ExportKeyInfo.diff(newKey);
        storageKeyValueService.putValue("last_calculated_diff_key", workKey.toString());
        Mockito.when(tableDiffService.findNextKey(Mockito.eq(workKey))).thenReturn(nextKey);
        Mockito.when(tableDiffService.exportKey("recent")).thenReturn(newKey);
        TableInfo path = TableInfo.info("some_path", nextKey.getExportKey(), 5_000_000L);
        Mockito.when(tableDiffService.diffSnapshot(Mockito.eq(nextKey))).thenReturn(path);
        exportCoordinator.initializeDiffExport();
    }

    @Test
    public void testClearThreshold() {
        ExportKeyInfo workKey = ExportKeyInfo.full("20211205_1903");
        String newKey = "20211205_2003";
        ExportKeyInfo nextKey = ExportKeyInfo.diff(newKey);
        storageKeyValueService.putValue("last_calculated_diff_key", workKey.toString());
        storageKeyValueService.putValue("diff_limit_threshold", 5_000_000L);
        Mockito.when(tableDiffService.findNextKey(Mockito.eq(workKey))).thenReturn(nextKey);
        TableInfo path = TableInfo.info("some_path", nextKey.getExportKey(), 5_000_000L);
        Mockito.when(tableDiffService.diffSnapshot(Mockito.eq(nextKey))).thenReturn(path);
        Mockito.when(tableDiffService.exportKey("recent")).thenReturn(newKey);
        exportCoordinator.initializeDiffExport();
        Assertions.assertThat(storageKeyValueService.getLong("diff_limit_threshold", 0L))
                .isEqualTo(0L);
    }


    @Test(expected = RuntimeException.class)
    public void testWrongDirectionForDiff() {
        ExportKeyInfo oldKey = ExportKeyInfo.diff("20211205_1903");
        ExportKeyInfo nextKey = ExportKeyInfo.diff("20211204_2003");
        storageKeyValueService.putValue("last_calculated_diff_key", oldKey.toString());
        Mockito.when(tableDiffService.findNextKey(Mockito.eq(oldKey))).thenReturn(nextKey);
        Mockito.when(tableDiffService.exportKey("recent")).thenReturn(nextKey.getExportKey());
        exportCoordinator.initializeDiffExport();
    }

    @Test
    public void testDiffIgnore() {
        ExportKeyInfo oldKey = ExportKeyInfo.diff("20211205_1903");
        storageKeyValueService.putValue("last_calculated_diff_key", oldKey.toString());
        Mockito.when(tableDiffService.findNextKey(Mockito.eq(oldKey))).thenReturn(null);
        Mockito.when(tableDiffService.exportKey("recent")).thenReturn(oldKey.getExportKey());
        exportCoordinator.initializeDiffExport();

        Mockito.verify(diffStuffReader, Mockito.times(0)).handleTable(
                Mockito.anyString()
        );
    }

    @Test(expected = RuntimeException.class)
    public void testDiffWithoutOldKey() {
        storageKeyValueService.putValue("last_calculated_diff_key", null);
        exportCoordinator.initializeDiffExport();
    }

    @Test
    public void testImportFull() {
        ExportKeyInfo oldKey = ExportKeyInfo.diff("20211205_2003");
        ExportKeyInfo newKey = ExportKeyInfo.full("20211205_2103");
        storageKeyValueService.putValue("last_calculated_diff_key", oldKey.toString());

        TableInfo path = TableInfo.info("some_path", newKey.toString(), 20L);
        Mockito.when(tableDiffService.fullSnapshot(newKey)).thenReturn(
                path
        );
        exportCoordinator.initializeFullExport(newKey.getExportKey());
        Mockito.verify(fullStuffReader, Mockito.times(1)).handleTable(
                Mockito.eq(path.getFullClusterPath())
        );
    }

    @Test
    public void testImportSame() {
        ExportKeyInfo oldKey = ExportKeyInfo.diff("20211205_2003");
        storageKeyValueService.putValue("last_calculated_diff_key", oldKey.toString());

        Mockito.when(tableDiffService.fullSnapshot(Mockito.any())).thenReturn(
                TableInfo.info("some_path", oldKey.toString(), 20L)
        );

        exportCoordinator.initializeFullExport(oldKey.getExportKey());
        Mockito.verify(fullStuffReader, Mockito.times(1)).handleTable(
                Mockito.anyString()
        );
    }

    @Test
    public void testImportFromOffset() {
        ExportKeyInfo oldKey = ExportKeyInfo.diff("20211205_2003");
        ExportKeyInfo newKey = ExportKeyInfo.full("20211205_2103");
        long offset = 1_000_000L;
        storageKeyValueService.putValue("last_calculated_diff_key", oldKey.toString());
        storageKeyValueService.putValue("save_point_offset_FULL", offset);

        Mockito.when(tableDiffService.fullSnapshot(newKey)).thenReturn(
                TableInfo.info("some_path", newKey.toString(), 20L)
        );
        exportCoordinator.initializeFullExport(newKey.getExportKey());
        Mockito.verify(fullStuffReader, Mockito.times(1)).handleTable(
                Mockito.anyString()
        );
    }

    @Test
    public void testImportDiffFromOffset() {
        ExportKeyInfo oldKey = ExportKeyInfo.diff("20211205_2003");
        ExportKeyInfo newKey = ExportKeyInfo.diff("20211205_2103");
        long offset = 1_000_000L;
        storageKeyValueService.putValue("last_calculated_diff_key", oldKey.toString());
        storageKeyValueService.putValue("save_point_offset_DIFF", offset);

        Mockito.when(tableDiffService.findNextKey(oldKey)).thenReturn(newKey);
        TableInfo value = TableInfo.info("20211205_2103", newKey.toString(), 20L);
        Mockito.when(tableDiffService.exportKey("recent")).thenReturn(
                newKey.getExportKey()
        );
        Mockito.when(tableDiffService.diffSnapshot(newKey)).thenReturn(
                value
        );
        exportCoordinator.initializeDiffExport();

        Mockito.verify(diffStuffReader, Mockito.times(1)).handleTable(
                Mockito.eq(value.getFullClusterPath())
        );
    }


    @Test
    public void testFirstImport() {
        ExportKeyInfo newKey = ExportKeyInfo.full("20211205_2003");
        Mockito.when(tableDiffService.fullSnapshot(Mockito.eq(newKey))).thenReturn(
                TableInfo.info("some_path", newKey.toString(), 20L)
        );
        exportCoordinator.initializeFullExport(newKey.getExportKey());
        Mockito.verify(fullStuffReader, Mockito.times(1)).handleTable(
                Mockito.anyString()
        );
    }

}
