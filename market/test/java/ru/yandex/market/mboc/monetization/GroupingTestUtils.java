package ru.yandex.market.mboc.monetization;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mboc.app.controller.web.DisplayGroupedModel;
import ru.yandex.market.mboc.app.controller.web.DisplayGroupingConfig;
import ru.yandex.market.mboc.app.controller.web.DisplayModelGroup;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.ComputeType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.GroupType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupedModel;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ModelGroup;
import ru.yandex.market.mboc.monetization.service.grouping.YtGroupedModelNode;
import ru.yandex.market.mboc.monetization.service.grouping.YtModelGroupNode;

/**
 * @author danfertev
 * @since 25.10.2019
 */
public class GroupingTestUtils {
    public static final long CATEGORY_ID = 1L;
    private static final int SEED = 22402;
    private static final EnhancedRandom RANDOM = new EnhancedRandomBuilder().seed(SEED).build();
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0L);
    private static final String[] IGNORED_FIELDS = new String[]{"modifiedAt", "createdAt", "parameterValues"};

    private GroupingTestUtils() {
    }

    public static GroupingConfig simpleGroupingConfig() {
        return new GroupingConfig().setCategoryId(CATEGORY_ID);
    }

    public static DisplayGroupingConfig simpleDisplayConfig() {
        return new DisplayGroupingConfig()
            .setGroupingConfig(simpleGroupingConfig())
            .setConfigParameters(List.of());
    }

    public static ModelGroup simpleGroup() {
        return RANDOM.nextObject(ModelGroup.class, IGNORED_FIELDS).setCategoryId(CATEGORY_ID);
    }

    public static ModelGroup manualGroup() {
        return simpleGroup().setComputeType(ComputeType.MANUAL);
    }

    public static ModelGroup autoGroup() {
        return simpleGroup().setComputeType(ComputeType.AUTO);
    }

    public static ModelGroup manualGroup(GroupType groupType) {
        return manualGroup().setGroupType(groupType);
    }

    public static ModelGroup autoGroup(GroupType groupType) {
        return autoGroup().setGroupType(groupType);
    }

    public static GroupedModel simpleGroupedModel() {
        return RANDOM.nextObject(GroupedModel.class, IGNORED_FIELDS);
    }

    public static GroupedModel simpleGroupedModel(long modelGroupId) {
        return simpleGroupedModel().setModelGroupId(modelGroupId);
    }

    public static GroupedModel nextGroupedModel(ModelGroup modelGroup) {
        return simpleGroupedModel()
            .setModelId(ID_GENERATOR.incrementAndGet())
            .setModelGroupId(modelGroup.getId())
            .setGroupType(modelGroup.getGroupType());
    }

    public static GroupedModel nextGroupedModel(DisplayModelGroup displayModelGroup) {
        return nextGroupedModel(displayModelGroup.getModelGroup());
    }

    public static List<Long> getModelIds(GroupedModel... models) {
        return Stream.of(models).map(GroupedModel::getModelId).collect(Collectors.toList());
    }

    public static List<Long> getModelIds(List<DisplayGroupedModel> models) {
        return models.stream()
            .map(DisplayGroupedModel::getGroupedModel)
            .map(GroupedModel::getModelId)
            .collect(Collectors.toList());
    }

    public static List<Long> getGroupIds(List<DisplayModelGroup> groups) {
        return groups.stream()
            .map(DisplayModelGroup::getModelGroup)
            .map(ModelGroup::getId)
            .collect(Collectors.toList());
    }

    public static List<Long> getGroupIds(DisplayModelGroup... groups) {
        return getGroupIds(List.of(groups));
    }

    public static Iterator<YTreeMapNode> groupIteratorF(ModelGroup... groups) {
        return List.of(groups).stream()
            .map(YtModelGroupNode::buildRow)
            .iterator();
    }

    public static Iterator<YTreeMapNode> modelIteratorF(ModelGroup group, GroupedModel... models) {
        return List.of(models).stream()
            .map(m -> YtGroupedModelNode.buildRow(group, m))
            .iterator();
    }
}
