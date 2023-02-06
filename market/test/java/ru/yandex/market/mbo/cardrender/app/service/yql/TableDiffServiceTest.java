package ru.yandex.market.mbo.cardrender.app.service.yql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.cardrender.app.config.YtRenderPathConfig;
import ru.yandex.market.mbo.cardrender.app.model.table.ExportKeyInfo;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.yt.utils.UnstableInit;

/**
 * @author apluhin
 * @created 1/12/22
 */
public class TableDiffServiceTest extends BaseTest {

    private static final String DIFF_EXPORT_PATH = "//test";
    private static final String MBO_EXPORT_PATH = "//mbo";

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private YtRenderPathConfig ytRenderPathConfig;
    private JdbcTemplate yqlJdbcTemplate;
    private TableDiffService tableDiffService;
    private Cypress cypress;
    private Yt yt;


    @Before
    public void setUp() throws Exception {
        yt = Mockito.mock(Yt.class);
        cypress = Mockito.mock(Cypress.class);
        Mockito.when(yt.cypress()).thenReturn(cypress);

        yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        tableDiffService = new TableDiffService(
                storageKeyValueService,
                DIFF_EXPORT_PATH,
                MBO_EXPORT_PATH,
                yqlJdbcTemplate,
                ytRenderPathConfig,
                UnstableInit.simple(yt));
    }

    @Test
    public void testFindNextDiff() {
        YTreeNode oldStuff = buildResponse("20220102_1000", 1000L, true);
        YTreeNode newStuff = buildResponse("20220102_1200", 2000L, true);

        mockResponse(List.of(oldStuff, newStuff));

        ExportKeyInfo nextKey = tableDiffService.findNextKey(ExportKeyInfo.diff("20220102_1000"));
        Assertions.assertThat(nextKey.getExportKey()).isEqualTo("20220102_1200");
    }

    @Test
    public void testIgnoreFailedDiff() {
        YTreeNode oldStuff = buildResponse("20220102_1000", 1000L, true);
        YTreeNode failedStuff = buildResponse("20220102_1200", 2000L, false);
        YTreeNode newStuff = buildResponse("20220102_1400", 3000L, true);
        YTreeNode newStuff1 = buildResponse("20220102_1600", 4000L, true);

        mockResponse(List.of(oldStuff, newStuff, failedStuff, newStuff1));

        ExportKeyInfo nextKey = tableDiffService.findNextKey(ExportKeyInfo.diff("20220102_1000"));
        Assertions.assertThat(nextKey.getExportKey()).isEqualTo("20220102_1400");
    }

    @Test
    public void testIgnoreNextFailedDiff() {
        YTreeNode oldStuff = buildResponse("20220102_1000", 1000L, true);
        YTreeNode failedStuff = buildResponse("20220102_1200", 2000L, false);
        YTreeNode newStuff = buildResponse("20220102_1400", 3000L, false);

        mockResponse(List.of(oldStuff, newStuff, failedStuff));

        ExportKeyInfo nextKey = tableDiffService.findNextKey(ExportKeyInfo.diff("20220102_1000"));
        Assertions.assertThat(nextKey).isNull();
    }

    @Test
    public void testRenderDiffBatch() {
        tableDiffService.diffSnapshot(ExportKeyInfo.diff("20220102_1000"));
    }

    @Test
    public void testRenderFullBatch() {
        tableDiffService.fullSnapshot(ExportKeyInfo.full("20220102_1000"));
    }

    @Test
    public void testRenderDeleteBatch() {
        YTreeNode recent = buildResponse("recent", 1000L, true);
        Mockito.when(cypress.get(Mockito.any(), Mockito.anyCollection())).thenReturn(recent);
        tableDiffService.deleteSync();
    }

    private void mockResponse(List<YTreeNode> nodeList) {
        Map<String, YTreeNode> collect = nodeList.stream().collect(Collectors.toMap(it -> it.stringValue(), it -> it));
        Mockito.when(cypress.get(Mockito.any(), Mockito.anyCollection())).thenReturn(
                new YTreeMapNodeImpl(collect, null)
        );
    }

    private YTreeNode buildResponse(String key, Long completedTime, boolean isOk) {
        Map<String, YTreeNode> attributes = new HashMap<>();
        if (isOk) {
            attributes.put("status", new YTreeStringNodeImpl("OK", null));
        }
        attributes.put("complete_time", new YTreeIntegerNodeImpl(true, completedTime, null));
        attributes.put("key", new YTreeStringNodeImpl(key, null));
        return new YTreeStringNodeImpl(key, attributes);
    }
}
