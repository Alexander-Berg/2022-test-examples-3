package ru.yandex.market.pricelabs.tms.processing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.pricelabs.misc.TablePath;

@Slf4j
public class YtSourceTargetDiffScenarioExecutor<Source, Target>
        extends YtSourceTargetScenarioExecutor<Source, Target> {

    private final TablePath<Source> sourceDiffPath;

    protected YtSourceTargetDiffScenarioExecutor(SourceTargetDiffProcessorCfg<Source, Target> cfg,
                                                 ObjectModification<Target> modification,
                                                 @NonNull Supplier<String> sourceTable,
                                                 @NonNull Map<String, String> sourcePathParams) {
        super(cfg, modification, sourceTable, sourcePathParams);
        this.sourceDiffPath = cfg.getSourceDiffPath();
    }

    public static <Source, Target> YtSourceTargetDiffScenarioExecutor<Source, Target> from(
            SourceTargetDiffProcessorCfg<Source, Target> cfg, ObjectModification<Target> modification,
            Supplier<String> sourceTable) {
        return new YtSourceTargetDiffScenarioExecutor<>(cfg, modification, sourceTable, Map.of());
    }

    @Override
    public SourceTargetDiffProcessorCfg<Source, Target> getCfg() {
        return (SourceTargetDiffProcessorCfg<Source, Target>) super.getCfg();
    }

    public String getSourceDiffPrefix() {
        return sourceDiffPath.getPathPrefix(getSourcePathParams());
    }

    public void removeSourceDiffPrefix() {
        var sourceDiffPrefix = getSourceDiffPrefix();
        if (getSource().isPathExists(sourceDiffPrefix)) {
            getSource().deletePath(sourceDiffPrefix);
        }
    }

    public void removeSourceDiffTables() {
        var sourceDiffPrefix = getSourceDiffPrefix();
        if (getSource().isPathExists(sourceDiffPrefix)) {
            getSource().deletePath(sourceDiffPrefix);
        }
        getSource().createPath(sourceDiffPrefix);
    }

    public void createSourceDiffTable() {
        getSource().createTable(getSourceDiffPath(), getSourceBinder(), Map.of());
    }

    public void createSourceDiffTable(String table) {
        getSource().createTable(getSourceDiffPath(table), getSourceBinder(), Map.of());
    }

    public void clearSourceDiffTable() {
        clearTable(getSource(), getSourceDiffPath(), getSourceBinder());
    }

    public void clearSourceTable(String table) {
        clearTable(getSource(), getSourceDiffPath(table), getSourceBinder());
    }

    public void insertSourceDiff(List<Source> newRows, String tableName) {
        insertTable(getSource(), getSourceDiffPath(tableName), getSourceBinder(), newRows);
    }

    public void insertSourceDiff(List<Source> newRows) {
        insertTable(getSource(), getSourceDiffPath(), getSourceBinder(), newRows);
    }

    private String getSourceDiffPath() {
        return getSourceDiffPath(getSourceTable().get());
    }

    private String getSourceDiffPath(@Nullable String table) {
        if (table != null) {
            return sourceDiffPath.getPathPrefix(getSourcePathParams()) + "/" + table;
        } else {
            return sourceDiffPath.getPath(getSourcePathParams());
        }
    }
}
