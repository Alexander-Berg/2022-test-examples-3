package ru.yandex.market.mbo.db;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.tree.ExportTovarTree;
import ru.yandex.market.mbo.tree.TovarTreeService;
import ru.yandex.market.mbo.utils.WordProtoUtils;

import java.util.ArrayList;
import java.util.List;

public class TovarTreeProtoServiceMock implements TovarTreeService {
    private final TovarTreeDao tovarTreeDao;

    public TovarTreeProtoServiceMock(TovarTreeDao tovarTreeDao) {
        this.tovarTreeDao = tovarTreeDao;
    }

    @Override
    public ExportTovarTree.GetTovarTreeResponse getTovarTree(ExportTovarTree.GetTovarTreeRequest getTovarTreeRequest) {
        List<MboParameters.Category> result = new ArrayList<>();
        appendCategories(result, tovarTreeDao.loadTovarTree().getRoot());
        return ExportTovarTree.GetTovarTreeResponse.newBuilder()
            .addAllCategories(result)
            .build();
    }

    public void appendCategories(List<MboParameters.Category> result, TovarCategoryNode node) {
        result.add(fromTovarTreeNode(node));

        node.getChildren().forEach(c -> appendCategories(result, c));
    }

    private MboParameters.Category fromTovarTreeNode(TovarCategoryNode node) {
        return MboParameters.Category.newBuilder()
            .setHid(node.getHid())
            .setParentHid(node.getParentHid())
            .addName(WordProtoUtils.defaultWord(node.getName()))
            .addUniqueName(WordProtoUtils.defaultWord(node.getName()))
            .build();
    }

    @Override
    public MonitoringResult ping() {
        return null;
    }

    @Override
    public MonitoringResult monitoring() {
        return null;
    }
}
