package ru.yandex.market.pricelabs.tms.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.misc.TablePath;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YTProxy;
import ru.yandex.market.yt.utils.Operations;

@Slf4j
public class YtSourceTargetScenarioExecutor<Source, Target> extends YtScenarioExecutor<Target> {

    private final YTProxy source;
    private final YTBinder<Source> sourceBinder;
    private final Supplier<String> sourceTable;
    private final TablePath<Source> sourcePath;
    private final Map<String, String> sourcePathParams;

    protected YtSourceTargetScenarioExecutor(SourceTargetProcessorCfg<Source, Target> cfg,
                                             ObjectModification<Target> modification,
                                             @NonNull Supplier<String> sourceTable,
                                             @NonNull Map<String, String> sourcePathParams) {
        super(cfg, modification);
        // Всегда выбираем primary (в тестах у нас только один источник)
        this.source = cfg.getSourceClusters().getPrimary();
        this.sourceBinder = cfg.getSourceBinder();
        this.sourcePath = cfg.getSourcePath();
        this.sourcePathParams = sourcePathParams;
        this.sourceTable = sourceTable;
    }

    @Override
    public SourceTargetProcessorCfg<Source, Target> getCfg() {
        return (SourceTargetProcessorCfg<Source, Target>) super.getCfg();
    }

    private String getSourcePath() {
        return getSourcePath(sourceTable.get());
    }

    private String getSourcePath(@Nullable String table) {
        if (table != null) {
            return sourcePath.getPathPrefix(sourcePathParams) + "/" + table;
        } else {
            return sourcePath.getPath(sourcePathParams);
        }
    }

    public YTProxy getSource() {
        return this.source;
    }

    public String getSourcePrefix() {
        return sourcePath.getPathPrefix(sourcePathParams);
    }

    public void removeSourcePrefix() {
        var sourcePrefix = getSourcePrefix();
        if (getSource().isPathExists(sourcePrefix)) {
            getSource().deletePath(sourcePrefix);
        }
    }

    public void removeSourceTables() {
        var sourcePrefix = getSourcePrefix();
        if (getSource().isPathExists(sourcePrefix)) {
            getSource().deletePath(sourcePrefix);
        }
        getSource().createPath(sourcePrefix);
    }

    public void createSourceTable() {
        getSource().createTable(getSourcePath(), sourceBinder, Map.of());
    }

    public void createSourceTable(String table) {
        getSource().createTable(getSourcePath(table), sourceBinder, Map.of());
    }

    public void makeLink(String targetTable, String linkName) {
        getSource().makeLink(getSourcePath(targetTable), getSourcePath(linkName));
    }

    public void clearSourceTable() {
        clearTable(getSource(), getSourcePath(), sourceBinder);
    }

    public void clearSourceTable(String table) {
        clearTable(getSource(), getSourcePath(table), sourceBinder);
    }

    public List<Source> selectSourceRows() {
        List<Source> actualList = new ArrayList<>();
        getSource().read(YPath.simple(getSourcePath()), sourceBinder, actualList::add);
        return actualList;
    }

    public void test(Runnable executor, List<Source> newRows, List<Target> existingRows,
                     List<Target> expectRows) {
        this.test(executor, newRows, existingRows, expectRows, Utils.emptyConsumer());
    }

    public void insertSource(List<Source> newRows, String tableName) {
        insertTable(getSource(), getSourcePath(tableName), sourceBinder, newRows);
    }

    public void insertSource(List<Source> newRows) {
        insertTable(getSource(), getSourcePath(), sourceBinder, newRows);
    }

    public void insert(List<Source> newRows, List<Target> existingRows) {
        insertSource(newRows);
        insert(existingRows);
    }

    public void test(Runnable executor, List<Source> newRows, List<Target> existingRows,
                     List<Target> expectRows, Consumer<Target> expectUpdate) {
        log.info("Preparing data (new rows: {}, existing rows: {}, expect rows: {})",
                newRows.size(), existingRows.size(), expectRows.size());

        this.insert(newRows, existingRows);
        Operations.executeOp("Run test", log, executor);

        log.info("Checking data...");

        this.verify(expectRows, expectUpdate);

        log.info("Checking complete");
    }


    public Source copySource(Source obj) {
        return YTreeDeepCopier.deepCopyOf(obj);
    }

    protected Map<String, String> getSourcePathParams() {
        return sourcePathParams;
    }

    protected Supplier<String> getSourceTable() {
        return sourceTable;
    }

    protected YTBinder<Source> getSourceBinder() {
        return sourceBinder;
    }

    //

    public static <Source, Target> YtSourceTargetScenarioExecutor<Source, Target> from(
            SourceTargetProcessorCfg<Source, Target> cfg, ObjectModification<Target> modification) {
        return from(cfg, modification, () -> null);
    }

    public static <Source, Target> YtSourceTargetScenarioExecutor<Source, Target> from(
            SourceTargetProcessorCfg<Source, Target> cfg, ObjectModification<Target> modification,
            Supplier<String> sourceTable) {
        return new YtSourceTargetScenarioExecutor<>(cfg, modification, sourceTable, Map.of());
    }

    public static <Source, Target> YtSourceTargetScenarioExecutor<Source, Target> from(
            SourceTargetProcessorCfg<Source, Target> cfg, ObjectModification<Target> modification,
            Supplier<String> sourceTable, Map<String, String> sourcePathParams) {
        return new YtSourceTargetScenarioExecutor<>(cfg, modification, sourceTable, sourcePathParams);
    }

}
