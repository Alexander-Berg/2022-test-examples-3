package ru.yandex.market.crm.mapreduce.yt.reducers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUser;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserRow;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelType;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.ModelBlockData;
import ru.yandex.market.crm.mapreduce.util.Constants;
import ru.yandex.market.crm.mapreduce.util.Json;
import ru.yandex.market.crm.mapreduce.yt.YieldImpl;
import ru.yandex.market.crm.mapreduce.yt.reducers.SendingDataReducer.VariantInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Created by vdorogin on 24.07.17.
 */
@SuppressWarnings("ConstantName")
public class SendingDataReducerTest {

    private static final ModelBlockConf DEFAULT_BLOCK_TYPE = new ModelBlockConf("def_id", "A0", 4);
    private static final int DEFAULT_BLOCK_TABLE_INDEX = 1;
    private static final ModelBlockConf BLOCK_TYPE1 = new ModelBlockConf("id1", "A1", 4);
    private static final int BLOCK_TYPE1_TABLE_INDEX = 2;
    private static final ModelBlockConf BLOCK_TYPE2 = new ModelBlockConf("id2", "A2", 4);
    private static final int BLOCK_TYPE2_TABLE_INDEX = 3;
    private static final ModelBlockConf EMPTY_BLOCK_TYPE = new ModelBlockConf("id_empty", null, 4);
    private static final int MAX_MODEL_BLOCKS = 2;
    private static final String REDUCE_KEY = "email";
    private static final String TEST_EMAIL = "email1@yandex.ru";
    private static final String MARKET_LINK = "https://market.yandex.ru";

    private static final Long modelId1 = 1001L;
    private static final Long modelId2 = 1002L;
    private static final Long modelId3 = 1003L;
    private static final Long modelId4 = 1004L;
    private static final Long modelId5 = 1005L;
    private static final Long modelId6 = 1006L;
    private static final Long modelId7 = 1007L;

    private static final String USER_VARIANT = "variant";
    private static final Set<Long> staticModelIds = Sets.newHashSet(modelId6, modelId7);

    private SendingDataReducer reducer;
    private YieldImpl<CampaignUserRow> collector;
    private Statistics statistics;

    @Before
    public void init() {
        SendingDataReducer.BlockInfo defaultBlockInfo = new SendingDataReducer.BlockInfo(
                USER_VARIANT,
                DEFAULT_BLOCK_TYPE.getId(),
                DEFAULT_BLOCK_TYPE.getAlgorithm(),
                MARKET_LINK,
                DEFAULT_BLOCK_TYPE.getModelCount()
        );

        SendingDataReducer.BlockInfo blockType1Info = new SendingDataReducer.BlockInfo(
                USER_VARIANT,
                BLOCK_TYPE1.getId(),
                BLOCK_TYPE1.getAlgorithm(),
                MARKET_LINK,
                BLOCK_TYPE1.getModelCount()
        );

        SendingDataReducer.BlockInfo blockType2Info = new SendingDataReducer.BlockInfo(
                USER_VARIANT,
                BLOCK_TYPE2.getId(),
                BLOCK_TYPE2.getAlgorithm(),
                MARKET_LINK,
                BLOCK_TYPE2.getModelCount()
        );

        reducer = new SendingDataReducer(
                asList(defaultBlockInfo, blockType1Info, blockType2Info),
                Collections.singletonMap(USER_VARIANT, new VariantInfo(
                        staticModelIds.stream().map(String::valueOf).collect(Collectors.toSet()),
                        MAX_MODEL_BLOCKS
                )),
                false,
                Collections.emptySet()
        );

        collector = new YieldImpl<>();
        statistics = null;
    }

    /**
     * Удаление моделей-дублей
     */
    @Test
    public void deleteDublicateModels() {
        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL, BLOCK_TYPE1, modelId1, modelId4),
                createModelRow(TEST_EMAIL, modelId1),
                createModelRow(TEST_EMAIL, modelId2),
                createModelRow(TEST_EMAIL, modelId3),
                createModelRow(TEST_EMAIL, modelId4),
                createModelRow(TEST_EMAIL, modelId1),
                createModelRow(TEST_EMAIL, modelId2),
                createModelRow(TEST_EMAIL, modelId5)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, DEFAULT_BLOCK_TYPE, modelId2, modelId3, modelId5);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE1, modelId1, modelId4);
    }

    /**
     * пользователя не было в таблице сегментов, ничего не пишем в выходную таблицу
     */
    @Test
    public void deleteDublicateModelsByUserWithoutSegment() {

        IteratorF<YTreeMapNode> entries = Cf.list(
                createModelRow(TEST_EMAIL, modelId1),
                createModelRow(TEST_EMAIL, modelId2)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        Assert.assertTrue("Нечего не должно быть выведено", collector.get().isEmpty());
    }

    /**
     * Удаление пустого блока
     */
    @Test
    public void deleteEmptyBlock() {
        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL, BLOCK_TYPE1, modelId1, modelId2, modelId4),
                createModelRow(TEST_EMAIL, modelId4, BLOCK_TYPE1_TABLE_INDEX),
                createModelRow(TEST_EMAIL, modelId2, BLOCK_TYPE1_TABLE_INDEX)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkBlock(TEST_EMAIL, USER_VARIANT, 1, BLOCK_TYPE1, modelId1, modelId2, modelId4);
    }

    /**
     * Ограничение кол-ва блоков
     */
    @Test
    public void maxBlocks() {
        IteratorF<YTreeMapNode> entries = Cf.list(
                UserDataBuilder.create(TEST_EMAIL, USER_VARIANT)
                        .block(BLOCK_TYPE1, modelId4, modelId3)
                        .block(BLOCK_TYPE2, modelId1)
                        .build(),
                createModelRow(TEST_EMAIL, modelId5),
                createModelRow(TEST_EMAIL, modelId4),
                createModelRow(TEST_EMAIL, modelId3),
                createModelRow(TEST_EMAIL, modelId2),
                createModelRow(TEST_EMAIL, modelId1)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE1, modelId4, modelId3);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE2, modelId1);
    }

    /**
     * Ограниечние кол-ва моделей в блоке
     */
    @Test
    public void maxModelsInBlock() throws IOException {
        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL),
                createModelRow(TEST_EMAIL, modelId1),
                createModelRow(TEST_EMAIL, modelId2),
                createModelRow(TEST_EMAIL, modelId3),
                createModelRow(TEST_EMAIL, modelId4),
                createModelRow(TEST_EMAIL, modelId5)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkOneBlock(TEST_EMAIL, USER_VARIANT, modelId1, modelId2, modelId3, modelId4);
    }

    /**
     * Блок с пустым списком моделей
     */
    @Test
    public void specialyBlock() {
        SendingDataReducer.BlockInfo blockInfo = new SendingDataReducer.BlockInfo(
                USER_VARIANT,
                EMPTY_BLOCK_TYPE.getId(),
                EMPTY_BLOCK_TYPE.getAlgorithm(),
                MARKET_LINK,
                EMPTY_BLOCK_TYPE.getModelCount()
        );

        reducer = new SendingDataReducer(
                singletonList(blockInfo),
                Collections.singletonMap(USER_VARIANT, new VariantInfo(
                        staticModelIds.stream().map(String::valueOf).collect(Collectors.toSet()),
                        MAX_MODEL_BLOCKS
                )),
                false,
                Collections.emptySet()
        );

        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL, BLOCK_TYPE1, modelId1, modelId4)
        ).iterator();

        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE1, modelId1, modelId4);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, EMPTY_BLOCK_TYPE);
    }

    /**
     * Пользователь был в таблице сегментов и модели из статических блоков
     */
    @Test
    public void userWithSegmentAndModelsFromStaticBlocks() throws IOException {
        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL),
                createModelRow(TEST_EMAIL, modelId1),
                createModelRow(TEST_EMAIL, modelId7),
                createModelRow(TEST_EMAIL, modelId2),
                createModelRow(TEST_EMAIL, modelId6),
                createModelRow(TEST_EMAIL, modelId3)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkOneBlock(TEST_EMAIL, USER_VARIANT, modelId1, modelId2, modelId3);
    }

    /**
     * Пользователь был в таблице сегментов и нет моделей
     */
    @Test
    public void userWithSegmentAndNoModels() throws IOException {
        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkBlock(TEST_EMAIL, USER_VARIANT, 0, DEFAULT_BLOCK_TYPE);
    }

    /**
     * Пользователь был в таблице сегментов и одна модель
     */
    @Test
    public void userWithSegmentAndOneModel() throws IOException {
        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL),
                createModelRow(TEST_EMAIL, modelId1)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkOneBlock(TEST_EMAIL, USER_VARIANT, modelId1);
    }

    /**
     * Пользователь был в таблице сегментов и несколько моделей
     */
    @Test
    public void userWithSegmentAndSomeModels() throws IOException {
        IteratorF<YTreeMapNode> entries = Cf.list(
                createUserDataRow(TEST_EMAIL),
                createModelRow(TEST_EMAIL, modelId1),
                createModelRow(TEST_EMAIL, modelId2),
                createModelRow(TEST_EMAIL, modelId3)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkOneBlock(TEST_EMAIL, USER_VARIANT, modelId1, modelId2, modelId3);
    }

    /**
     * Блок заполняется моделями своей таблицы.
     */
    @Test
    public void blockIsFilledFromAppropriateTable() {
        IteratorF<YTreeMapNode> entries = Cf.list(
                UserDataBuilder.create(TEST_EMAIL, USER_VARIANT)
                        .block(BLOCK_TYPE1, modelId1)
                        .block(BLOCK_TYPE2, modelId2)
                        .build(),
                createModelRow(TEST_EMAIL, modelId3, BLOCK_TYPE1_TABLE_INDEX),
                createModelRow(TEST_EMAIL, modelId4, BLOCK_TYPE2_TABLE_INDEX),
                createModelRow(TEST_EMAIL, modelId5, BLOCK_TYPE1_TABLE_INDEX),
                createModelRow(TEST_EMAIL, modelId6, BLOCK_TYPE2_TABLE_INDEX)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE1, modelId1, modelId3, modelId5);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE2, modelId2, modelId4);
    }

    /**
     * Новый блок заполняется моделями из своей таблицы.
     */
    @Test
    public void newBlockIsFilledFromItsTable() {
        IteratorF<YTreeMapNode> entries = Cf.list(
                UserDataBuilder.create(TEST_EMAIL, USER_VARIANT)
                        .block(BLOCK_TYPE1, modelId1)
                        .build(),
                createModelRow(TEST_EMAIL, modelId2, BLOCK_TYPE2_TABLE_INDEX),
                createModelRow(TEST_EMAIL, modelId3, BLOCK_TYPE2_TABLE_INDEX),
                createModelRow(TEST_EMAIL, modelId4, BLOCK_TYPE2_TABLE_INDEX),
                createModelRow(TEST_EMAIL, modelId5, BLOCK_TYPE2_TABLE_INDEX)
        ).iterator();
        reducer.reduce(REDUCE_KEY, entries, collector, statistics);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE1, modelId1);
        checkBlock(TEST_EMAIL, USER_VARIANT, 2, BLOCK_TYPE2, modelId2, modelId3, modelId4, modelId5);
    }

    private void checkBlock(String email, String segment, int blockCount, ModelBlockConf blockType, Long... modelIds) {
        CampaignUserRow result = collector.get(0);
        Assert.assertNotNull(result);
        Assert.assertEquals(email, result.getEmail());

        String jsonData = result.getData();
        Assert.assertNotNull(jsonData);
        CampaignUserData data = Json.fromJson(CampaignUserData.class, jsonData);

        CampaignUser userInfo = data.getUserInfo();
        Assert.assertNotNull(userInfo);
        Assert.assertEquals(segment, userInfo.getVariant());

        Assert.assertNotNull(data.getBlocks());
        Assert.assertEquals(blockCount, data.getBlocks().size());
        Optional<ModelBlockData> blockOptional = data.getBlocks().stream()
                .filter(ModelBlockData.class::isInstance)
                .map(ModelBlockData.class::cast)
                .filter(b -> blockType.getId().equals(b.getId()))
                .findFirst();
        Assert.assertEquals(blockCount > 0, blockOptional.isPresent());

        if (blockCount > 0) {
            ModelBlockData modelBlock = blockOptional.get();

            Assert.assertNotNull(modelBlock.getModels());
            Assert.assertEquals(modelIds.length, modelBlock.getModels().size());

            for (int i = 0; i < modelIds.length; i++) {
                Assert.assertEquals(String.valueOf(modelIds[i]), modelBlock.getModels().get(i).getId());
            }
        }
    }

    private void checkOneBlock(String email, String segment, Long... modelIds) {
        checkBlock(email, segment, modelIds.length == 0 ? 0 : 1, DEFAULT_BLOCK_TYPE, modelIds);
    }

    private YTreeMapNode createModelRow(String email, long modelId) {
        return createModelRow(email, modelId, DEFAULT_BLOCK_TABLE_INDEX);
    }

    private YTreeMapNode createModelRow(String email, long modelId, int tableIndex) {
        return (YTreeMapNode) new YTreeBuilder()
                .beginAttributes()
                .key("table_index").value(tableIndex)
                .endAttributes()
                .beginMap()
                .key("email").value(email)
                .key("id").value(String.valueOf(modelId))
                .key("hid").value(123)
                .key("name").value("title")
                .key("price").value("123.0")
                .key("img").value("https://market.yandex.ru/product/" + modelId)
                .key("type").value(ModelType.MODEL.name())
                .endMap()
                .build();
    }

    private YTreeMapNode createUserDataRow(String email, Long... modelIds) throws JsonProcessingException {
        return createUserDataRow(email, DEFAULT_BLOCK_TYPE, modelIds);
    }

    private YTreeMapNode createUserDataRow(String email, ModelBlockConf blockType, Long... modelIds) {
        return UserDataBuilder.create(email, USER_VARIANT)
                .block(blockType, modelIds)
                .build();
    }

    private static class UserDataBuilder {
        private List<BlockData> blocks;
        private String email;
        private String segment;

        private UserDataBuilder(String email, String segment) {
            this.email = email;
            this.segment = segment;
            blocks = new ArrayList<>();
        }

        public static UserDataBuilder create(String email, String segment) {
            return new UserDataBuilder(email, segment);
        }

        public UserDataBuilder block(ModelBlockConf blockConf, Long... modelIds) {
            if (modelIds.length > 0) {
                ModelBlockData modelBlock = new ModelBlockData();
                modelBlock.setId(blockConf.getId());
                List<ModelInfo> models = Stream.of(modelIds)
                        .map(String::valueOf)
                        .map(ModelInfo::new)
                        .collect(Collectors.toList());
                blocks.add(modelBlock.setModels(models));
            }
            return this;
        }

        public YTreeMapNode build() {
            CampaignUserData data = new CampaignUserData()
                    .setUserInfo(new CampaignUser(segment))
                    .setBlocks(blocks.isEmpty() ? null : blocks);

            return (YTreeMapNode) YTree.builder()
                    .beginAttributes()
                        .key(Constants.TABLE_INDEX).value(0)
                    .endAttributes()
                    .beginMap()
                        .key("email").value(email)
                        .key("data").value(Json.toJson(data))
                    .endMap()
                    .build();
        }
    }

    private static class ModelBlockConf {
        private String id;
        private String algorithm;
        private int modelCount;

        private ModelBlockConf(String id, String algorithm, int modelCount) {
            this.id = id;
            this.algorithm = algorithm;
            this.modelCount = modelCount;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public int getModelCount() {
            return modelCount;
        }

        public void setModelCount(int modelCount) {
            this.modelCount = modelCount;
        }
    }
}
