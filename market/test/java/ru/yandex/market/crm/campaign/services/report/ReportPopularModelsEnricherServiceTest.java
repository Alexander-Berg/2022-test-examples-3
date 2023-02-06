package ru.yandex.market.crm.campaign.services.report;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.sending.conf.ModelBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.StaticModelBlockConf;
import ru.yandex.market.crm.core.services.report.ReportService;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.BlockType;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.ModelBlockData;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by vdorogin on 02.08.17.
 */
public class ReportPopularModelsEnricherServiceTest {

    private static final String BLOCK_ID1 = "model_block_id1";
    private static final String BLOCK_ID2 = "model_block_id2";
    private static final String BLOCK_ID3 = "model_block_id3";
    private static final String BLOCK_ID4 = "model_block_id4";
    @InjectMocks
    private ReportPopularModelsEnricherService reportPopularModelsEnricher;
    @Mock
    private ReportService reportService;

    private static ModelBlockConf modelBlockConf(String id, int modelCount) {
        ModelBlockConf conf = new ModelBlockConf();
        conf.setId(id);
        conf.setModelCount(modelCount);
        conf.setColor(Color.GREEN);
        return conf;
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Обогащение блока с одной моделью
     */
    @Test
    public void enrichWithOneModel() {
        Assert.assertEquals(
                Arrays.asList("3", "2", "4", "5"),
                actualList(
                        Arrays.asList(1L, 3L, 6L, 8L),
                        Arrays.asList(3L),
                        Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L),
                        4
                )
        );
        verify(reportService, times(1)).getPopularModels(any(), anyInt(), any(), any());
    }

    /**
     * Не должны добавиться популярные модели, которые уже есть в блоке.
     */
    @Test
    public void shouldNotEnrichAlreadyExistedModels() {
        Assert.assertEquals(
                Arrays.asList("2", "3", "4", "5"),
                actualList(
                        Arrays.asList(1L),
                        Arrays.asList(2L, 3L),
                        Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L),
                        4
                )
        );
        verify(reportService, times(1)).getPopularModels(any(), anyInt(), any(), any());
    }

    /**
     * Обогащение блока с полным списком моделей
     */
    @Test
    public void enrichWithFullBlock() {
        Assert.assertEquals(
                Arrays.asList("4", "3", "1", "2"),
                actualList(
                        Arrays.asList(1L, 2L, 3L, 4L, 5L),
                        Arrays.asList(4L, 3L, 1L, 2L),
                        Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L),
                        4
                )
        );
        verifyZeroInteractions(reportService);
    }

    /**
     * Обогащение блока с пустым списком популярных моделей
     */
    @Test
    public void enrichWithEmptyPopulars() {
        Assert.assertEquals(
                Arrays.asList("3", "1"),
                actualList(
                        Arrays.asList(1L, 3L, 6L, 8L),
                        Arrays.asList(3L, 1L),
                        Collections.emptyList(),
                        4
                )
        );
        verify(reportService, times(2)).getPopularModels(any(), anyInt(), any(), any());
    }

    /**
     * Обогащение нескольких блоков
     */
    @Test
    public void enrichSeveralBlocks() {
        List<Long> sentModelIds = Arrays.asList(4L, 3L, 1L, 2L);
        CampaignUserData data = new CampaignUserData();
        data.setBlocks(Arrays.asList(
                createModelBlock(BLOCK_ID1, Collections.singletonList(5L), BlockType.MODEL),
                createModelBlock(BLOCK_ID2, Arrays.asList(6L, 7L), BlockType.MODEL),
                createModelBlock(BLOCK_ID3, Arrays.asList(20L), BlockType.STATIC_MODEL),
                createModelBlock(BLOCK_ID4, Arrays.asList(8L, 9L, 10L), BlockType.MODEL)
        ));

        EmailSendingVariantConf variantConf = new EmailSendingVariantConf();
        variantConf.setBlocks(Arrays.asList(
                modelBlockConf(BLOCK_ID1, 3),
                modelBlockConf(BLOCK_ID2, 2),
                new StaticModelBlockConf(BLOCK_ID3),
                modelBlockConf(BLOCK_ID4, 8)
        ));

        List<Long> popularModelIds = Arrays.asList(1L, 2L, 5L, 6L, 9L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L);
        List<ModelInfo> models = popularModelIds.stream().map(this::createModel).collect(Collectors.toList());
        when(reportService.getPopularModels(any(), anyInt(), any(), any())).thenReturn(models);

        data.setExcludedModels(sentModelIds.stream().map(String::valueOf).collect(Collectors.toList()));
        reportPopularModelsEnricher.enrichBlocks(variantConf, data);
        verify(reportService).getPopularModels(any(), anyInt(), any(), any());

        Assert.assertEquals(4, data.getBlocks().size());
        checkModelBlock(data.getBlocks().get(0), 5L, 11L, 12L);
        checkModelBlock(data.getBlocks().get(1), 6L, 7L);
        checkModelBlock(data.getBlocks().get(2), 20L);
        checkModelBlock(data.getBlocks().get(3), 8L, 9L, 10L, 13L, 14L, 15L, 16L, 17L);
    }

    private void checkModelBlock(BlockData block, Long... modelIds) {
        Assert.assertTrue(block instanceof ModelBlockData);
        ModelBlockData modelBlock = (ModelBlockData) block;
        List<String> actual = modelBlock.getModels().stream().map(ModelInfo::getId).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(modelIds).stream().map(String::valueOf).collect(Collectors.toList()), actual);
    }

    private List<String> actualList(List<Long> sentModelIds,
                                    List<Long> modelIdsInBlock,
                                    List<Long> popularModelIds,
                                    int modelCount) {

        List<ModelInfo> models = popularModelIds.stream().map(this::createModel).collect(Collectors.toList());
        when(reportService.getPopularModels(any(), anyInt(), any(), any())).thenReturn(models);

        CampaignUserData data = createData(BLOCK_ID1, modelIdsInBlock);
        data.setExcludedModels(sentModelIds.stream().map(String::valueOf).collect(Collectors.toList()));
        reportPopularModelsEnricher.enrichBlocks(createVariantConf(modelCount), data);
        return ((ModelBlockData) data.getBlocks().get(0)).getModels().stream()
                .map(ModelInfo::getId)
                .collect(Collectors.toList());
    }

    private EmailSendingVariantConf createVariantConf(int modelCount) {
        EmailSendingVariantConf variantConf = new EmailSendingVariantConf();
        variantConf.setBlocks(Collections.singletonList(
                modelBlockConf(BLOCK_ID1, modelCount)
        ));
        return variantConf;
    }

    private CampaignUserData createData(String blockId, List<Long> modelIds) {
        CampaignUserData data = new CampaignUserData();
        data.setBlocks(Collections.singletonList(
                createModelBlock(blockId, modelIds, BlockType.MODEL)
        ));
        return data;
    }

    private ModelBlockData createModelBlock(String blockId, List<Long> modelIds, BlockType type) {
        ModelBlockData block = new ModelBlockData();
        block.setModels(modelIds.stream().map(this::createModel).collect(Collectors.toList())
        );
        block.setId(blockId);
        block.setType(type);
        return block;
    }

    private ModelInfo createModel(Long id) {
        ModelInfo model = new ModelInfo(id.toString());
        model.setImg("//url");
        return model;
    }
}
