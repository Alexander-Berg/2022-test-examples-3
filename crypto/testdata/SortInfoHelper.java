package ru.yandex.crypta.graph2.dao.yt.local.fastyt.testdata;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph2.dao.yt.local.fastyt.fs.LocalYtDataLayer;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

public class SortInfoHelper {

    private final LocalYtDataLayer dataLayer;

    public SortInfoHelper(LocalYtDataLayer dataLayer) {
        this.dataLayer = dataLayer;
    }

    public void setSortedBy(YPath target, ListF<String> keys) {
        dataLayer.setMetadata(target, YTree.builder().value(
                Cf.map("sorted_by", keys)
        ).build().mapNode());
    }

    public List<String> getSortedBy(YPath target) {
        Option<YTreeMapNode> maybeMetadata = dataLayer.getMetadata(target);
        if (maybeMetadata.isPresent()) {
            YTreeMapNode metadata = maybeMetadata.get();
            Optional<YTreeNode> sortedByAttr = metadata.get("sorted_by");
            return sortedByAttr.stream()
                    .flatMap(sortedBy -> sortedBy.asList().stream().map(YTreeNode::stringValue))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
