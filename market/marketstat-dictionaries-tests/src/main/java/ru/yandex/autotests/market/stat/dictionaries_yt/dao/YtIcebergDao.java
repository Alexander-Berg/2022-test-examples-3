package ru.yandex.autotests.market.stat.dictionaries_yt.dao;

import org.apache.commons.lang3.RandomUtils;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictType;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.DictionaryRecord;
import ru.yandex.autotests.market.stat.date.DatePatterns;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.impl.common.YtException;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Created by kateleb on 06.06.17.
 */
public class YtIcebergDao {

    private final Yt yt;
    private final YTDictionariesConfig config;

    private YtIcebergDao() {
        config = new YTDictionariesConfig();
        yt = YtUtils.http(config.getYtProxy(), config.getYtToken());
    }

    public static YtIcebergDao newInstance() {
        return YtIcebergDao.YtIcebergDaoSingletonHolder.INSTANCE;
    }

    private static class YtIcebergDaoSingletonHolder {
        private static final YtIcebergDao INSTANCE = new YtIcebergDao();
    }

    public long getRowCount(DictType dictionaryType) {
        return getRowCount(dictionaryType, "latest");
    }

    public long getRowCount(DictType dictionaryType, LocalDateTime day) {
        return getRowCount(dictionaryType, DatePatterns.HIVE_DAY_PARTITION.format(day));
    }

    public long getRowCount(DictType dictionaryType, String subfolder) {
        try {
            YPath tablepath = getTablePath(dictionaryType, subfolder);
            return getAttribute(tablepath, "row_count").integerNode().getLong();
        } catch (YtException e) {
            return 0L;
        }
    }

    public Map<String, String> getSchema(DictType dictionaryType, String subfolder) {
        YPath tablepath = getTablePath(dictionaryType, subfolder);
        YTreeListNode schema = getAttribute(tablepath, "schema").listNode();
        return schema.asList().stream().map(YTreeNode::asMap)
            .collect(toMap(map -> map.getO("name").get().stringValue(), map -> map.getO("type").get().stringValue()));
    }

    public YPath getTablePath(DictType dictionaryType, String subfolder) {
        YPath tablepath;
        if (dictionaryType.getTableName().equals("offers")) {
            tablepath = YPath.simple(config.getYtOffersPath()).child(subfolder);
        } else {
            tablepath = YPath.simple(config.getYtPath()).child(dictionaryType.getTableName()).child(subfolder);
        }
        return tablepath;
    }

    public boolean latestPathExists(DictType type) {
        return pathExists(getTablePath(type, "latest"));
    }

    private long getRowCount(YPath tablePath) {
        try {
            return getAttribute(tablePath, "row_count").integerNode().getLong();
        } catch (YtException e) {
            return 0L;
        }
    }

    private YTreeNode getAttribute(YPath path, String attribute) {
        return yt.cypress().get(path.attribute(attribute));
    }

    public boolean pathExists(YPath path) {
        return yt.cypress().exists(path);
    }

    public List<YPath> list(YPath path) {
        ListF<YTreeStringNode> nodes = yt.cypress().list(path);
        return nodes.stream()
            .map(node -> path.child(node.stringValue())).collect(toList());
    }

    public <T extends DictionaryRecord> List<T> readFromTable(DictType dictionaryType, LocalDateTime day, int limit, long ytRowCount) {
        return readFromTable(getTablePathFor(dictionaryType, DatePatterns.HIVE_DAY_PARTITION.format(day), limit, ytRowCount),
            ytreeIterator -> YtParseUtils.parseData(ytreeIterator, (Class) (Class<T>) dictionaryType.getDataClass()));
    }

    public <T extends DictionaryRecord> List<T> readFromTable(DictType dictionaryType, LocalDateTime day, int limit) {
        YPath tablepath = YPath.simple(config.getYtPath()).child(dictionaryType.getTableName()).child(DatePatterns.HIVE_DAY_PARTITION.format(day) + "[#0:#" + limit + "]");
        return readFromTable(tablepath, ytreeIterator -> YtParseUtils.parseData(ytreeIterator, (Class) (Class<T>) dictionaryType.getDataClass()));
    }

    public Set<String> getTableFields(DictType dictionaryType, LocalDateTime ytPartition) {
        return new HashSet<>(getSchema(dictionaryType, DatePatterns.HIVE_DAY_PARTITION.format(ytPartition)).keySet());
    }

    public <T extends DictionaryRecord> List<T> readFromLatestPartition(DictType dictionaryType, int limit, long ytRowCount) {
        String latestPartition = dictionaryType.getTableName().equals("offers") ? "recent" : "latest";
        return readFromTable(getTablePathFor(dictionaryType, latestPartition, limit, ytRowCount),
            ytreeIterator -> YtParseUtils.parseData(ytreeIterator, (Class) (Class<T>) dictionaryType.getDataClass()));
    }

    private YPath getTablePathFor(DictType dictionaryType, String partition, int limit, long ytRowCount) {
        YPath tablepath;
        YPath basePath = dictionaryType.getTableName().equals("offers") ?
            YPath.simple(config.getYtOffersPath()) :
            YPath.simple(config.getYtPath()).child(dictionaryType.getTableName());

        if (ytRowCount < limit) {
            tablepath = basePath.child(partition);
        } else {
            int startIndex = RandomUtils.nextInt(0, (int) (ytRowCount - limit));
            tablepath = basePath
                .child(partition + "[#" + startIndex + ":#" + (startIndex + limit) + "]");
        }
        return tablepath;
    }

    private <T> T readFromTable(YPath tablePath, Function<IteratorF<YTreeMapNode>, T> function) {
        return yt.tables().read(tablePath, YTableEntryTypes.YSON, function);
    }
}
