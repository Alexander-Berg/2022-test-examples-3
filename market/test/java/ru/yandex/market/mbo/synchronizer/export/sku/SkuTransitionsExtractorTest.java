package ru.yandex.market.mbo.synchronizer.export.sku;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.Magics;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.db.ModelStopWordsDao;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkService;
import ru.yandex.market.mbo.db.modelstorage.sku_transitions.SkuTransition;
import ru.yandex.market.mbo.db.modelstorage.sku_transitions.SkuTransitionsService;
import ru.yandex.market.mbo.sku_transition.ExportSkuTransition;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.ExporterUtils;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportMapReduceService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportModelsTableService;
import ru.yandex.market.mbo.yt.TestYtWrapper;
import ru.yandex.market.protobuf.tools.MagicChecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SkuTransitionsExtractorTest {

    private static final long TIMEOUT = 60L;
    private static final int ENRICHMENT_MAX_DATA_SIZE = 64;
    private static final long ENRICHMENT_JOB_COUNT = 10_000L;
    private static final long REDUCE_JOB_COUNT = 10_000L;
    private static final byte[] MAGIC_BYTES = MagicChecker.magicToBytes(Magics.MagicConstants.MBTR.name());

    private SkuTransitionsExtractor extractor;
    @Mock
    private SkuTransitionsService skuTransitionsService;
    @Mock
    private ValueLinkService valueLinkService;
    @Mock
    private ModelStopWordsDao modelStopWordsDao;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ExportRegistry registry;

    @Before
    public void setUp() throws Exception {

        YPath tmpPath = YPath.simple("//tmp");

        TestYtWrapper ytWrapper = new TestYtWrapper();
        ytWrapper.cypress().create(tmpPath, CypressNodeType.MAP);

        YtExportModelsTableService ytExportModelsTableService = new YtExportModelsTableService(
            ytWrapper, null, tmpPath, null, GUID.create());
        YtExportMapReduceService ytExportMapReduceService = new YtExportMapReduceService(ytWrapper,
            ytExportModelsTableService, null, null, ytWrapper.pool(),
            ENRICHMENT_MAX_DATA_SIZE, ENRICHMENT_JOB_COUNT, REDUCE_JOB_COUNT, ytWrapper.ytAccount(), null,
            null, null,
            TIMEOUT, TIMEOUT, valueLinkService, modelStopWordsDao, true, false, false);

        this.extractor = new SkuTransitionsExtractor(skuTransitionsService, ytExportMapReduceService, false);
        this.extractor.setYtExtractorPath("sku-transition");
        this.extractor.setOutputFileName("sku-transition.pb");
        this.extractor.setValidator(new SkuTransitionsValidator());

        this.registry = new ExportRegistry();
        this.registry.setRootPath(folder.newFolder().getAbsolutePath());
        this.registry.setYtExportPath("//home");
        this.registry.setMuteMode(true);
        this.registry.afterPropertiesSet();
        this.registry.processStart();
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        extractorWriterService.setYt(ytWrapper);
        this.extractor.setExtractorWriterService(extractorWriterService);
        this.extractor.setRegistry(registry);
        this.extractor.afterPropertiesSet();
    }

    @Test
    public void testExtract() throws Exception {
        SkuTransition skuTransition1 = generateSkuTransition();
        SkuTransition skuTransition2 = generateSkuTransition2();

        when(skuTransitionsService.findSkuTransitions(anyBoolean()))
            .thenReturn(Arrays.asList(skuTransition1, skuTransition2));

        extractor.perform("");

        List<ExportSkuTransition.SkuTransition> transitions = read();

        ExportSkuTransition.SkuTransition actualSkuTransition1 = findTransitionById(transitions,
            skuTransition1.getId());
        assertSkuTransition(actualSkuTransition1, skuTransition1);
        ExportSkuTransition.SkuTransition actualSkuTransition2 = findTransitionById(transitions,
            skuTransition2.getId());
        assertSkuTransition(actualSkuTransition2, skuTransition2);
    }

    private void assertSkuTransition(ExportSkuTransition.SkuTransition actual, SkuTransition expected) {
        assertNotNull(actual);
        assertEquals(actual.getActionId(), expected.getActionId().longValue());
        assertEquals(actual.getTimestamp(), Instant.ofEpochMilli(expected.getDate().getTime()).getEpochSecond());
        assertEquals(actual.getType().name(), expected.getType().name());
        assertEquals(actual.getReason().name(), expected.getReason().name());
        assertEquals(actual.getEntityType().name(), expected.getEntityType().name());
        assertEquals(actual.getOldEntityId(), expected.getOldEntityId().longValue());
        assertEquals(actual.getOldEntityDeleted(), expected.getOldEntityDeleted());
        assertEquals(actual.getNewEntityId(), expected.getNewEntityId().longValue());
        assertEquals(actual.getPrimaryTransition(), expected.getPrimaryTransition());
    }

    private ExportSkuTransition.SkuTransition findTransitionById(List<ExportSkuTransition.SkuTransition> transitions,
                                                                 Long id) {
        return transitions.stream()
            .filter(t -> t.getId() == id)
            .findFirst()
            .orElse(null);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private SkuTransition generateSkuTransition() {
        return new SkuTransition()
            .setId(2L)
            .setActionId(3L)
            .setDate(new Date())
            .setType(ModelTransitionType.DUPLICATE)
            .setReason(ModelTransitionReason.CLUSTERIZATION)
            .setEntityType(EntityType.SKU)
            .setOldEntityId(1L)
            .setOldEntityDeleted(true)
            .setNewEntityId(1L)
            .setPrimaryTransition(true);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private SkuTransition generateSkuTransition2() {
        return new SkuTransition()
            .setId(4L)
            .setActionId(5L)
            .setDate(new Date())
            .setType(ModelTransitionType.ERROR)
            .setReason(ModelTransitionReason.MODEL_SPLIT)
            .setEntityType(EntityType.MODEL)
            //simulation of nullable values
            .setOldEntityId(0L)
            .setOldEntityDeleted(false)
            .setNewEntityId(0L)
            .setPrimaryTransition(true);
    }

    private List<ExportSkuTransition.SkuTransition> read() throws IOException {
        File file = registry.getFile("sku-transition.pb");
        try (InputStream stream = ExporterUtils.getInputStream(file)) {
            List<ExportSkuTransition.SkuTransition> skuTransitions = new ArrayList<>();

            byte[] magic = new byte[MAGIC_BYTES.length];
            int read = stream.read(magic);
            Assertions.assertThat(read).isEqualTo(MAGIC_BYTES.length);
            Assertions.assertThat(magic).isEqualTo(MAGIC_BYTES);

            ExportSkuTransition.SkuTransition exported;
            while ((exported = ExportSkuTransition.SkuTransition.parseDelimitedFrom(stream)) != null) {
                skuTransitions.add(exported);
            }
            return skuTransitions;
        }
    }
}
