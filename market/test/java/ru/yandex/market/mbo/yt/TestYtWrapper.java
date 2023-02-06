package ru.yandex.market.mbo.yt;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.acl.YtAcl;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.files.YtFiles;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.modelstorage.utils.ModelGroupIdUtils;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelUtil;
import ru.yandex.market.mbo.http.ModelStorage.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mbo wrapper above {@link TestYt} with more pretty methods.
 *
 * @author s-ermakov
 */
public class TestYtWrapper implements Yt {
    private final TestYt testYt;

    public TestYtWrapper() {
        this(new TestYt());
    }

    public TestYtWrapper(TestYt testYt) {
        this.testYt = testYt;
    }

    public TestYt yt() {
        return testYt;
    }

    @Override
    public TestCypress cypress() {
        return yt().cypress();
    }

    @Override
    public YtAcl acl() {
        return yt().acl();
    }

    @Override
    public YtFiles files() {
        return yt().files();
    }

    @Override
    public TestYtTables tables() {
        return yt().tables();
    }

    @Override
    public TestYtOperations operations() {
        return yt().operations();
    }

    @Override
    public TestYtTransactions transactions() {
        return yt().transactions();
    }

    public String pool() {
        return "unit-test-pool";
    }

    public String ytAccount() {
        return "unit-test-yt-account";
    }

    /**
     * Create model-storage models table.
     */
    public void createModelTable(YPath tablePath, Collection<Model> models) {
        createModelTable(tablePath, models, false);
    }

    /**
     * Create model-storage models table.
     */
    public void createModelTable(YPath tablePath, Collection<Model> models, boolean enableBrokenData) {
        List<YTreeMapNode> modelEntries = wrapToEntries(models, enableBrokenData);
        yt().tables().write(tablePath, YTableEntryTypes.YSON, DefaultIteratorF.wrap(modelEntries.iterator()));

        long time = new Date().getTime();
        yt().cypress().set(Option.empty(), false, tablePath.attribute("unflushed_timestamp"), time);
        yt().cypress().set(Option.empty(), false, tablePath.attribute("retained_timestamp"), time - 2);
    }

    /**
     * Can read any table, where model contains in {@link YtModelColumns#DATA} column.
     */
    public List<Model> readModelTable(YPath tablePath) {
        return readTable(tablePath, this::modelReader);
    }

    public <T> List<T> readTable(YPath yPath, Function<YTreeMapNode, T> mapFunction) {
        if (!yt().cypress().exists(Option.empty(), false, yPath)) {
            throw new IllegalStateException("Table " + yPath + " not exist");
        }

        List<T> result = new ArrayList<>();
        yt().tables().read(yPath, YTableEntryTypes.YSON, entry -> {
            T obj = mapFunction.apply(entry);
            result.add(obj);
        });
        return result;
    }

    public static List<YTreeMapNode> wrapToEntries(Collection<Model> models) {
        return wrapToEntries(models, false);
    }

    public static List<YTreeMapNode> wrapToEntries(Collection<Model> models, boolean enableBrokenData) {
        Map<Long, Long> modelIdToGroupId = ModelGroupIdUtils.computeGroupModelId(models, __ -> Collections.emptyMap());
        return models.stream()
            .map(Model::toBuilder)
            .peek(model -> {
                Long groupId = modelIdToGroupId.get(model.getId());
                if (groupId == null) {
                    if (!enableBrokenData) {
                        throw new IllegalStateException("Failed to compute group_model_id for model: " + model.getId() +
                            ". Check if your data is in consistent state");
                    }
                    groupId = model.getId();
                }
                model.setGroupModelId(groupId);
            })
            .map(YtModelUtil::modelToMapNode)
            .collect(Collectors.toList());
    }

    private Model modelReader(YTreeMapNode node) {
        byte[] bytes = node.getBytes(YtModelColumns.DATA);
        try {
            return Model.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
