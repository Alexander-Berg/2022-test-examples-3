package ru.yandex.market.pricelabs.tms.processing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.WithCreatedAt;
import ru.yandex.market.pricelabs.model.types.WithImportedAt;
import ru.yandex.market.pricelabs.model.types.WithStatus;
import ru.yandex.market.pricelabs.model.types.WithUpdatedAt;
import ru.yandex.market.pricelabs.processing.AbstractProcessor;
import ru.yandex.market.yt.binding.ProcessorCfg;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YTProxy;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.utils.Operations;

import static ru.yandex.market.pricelabs.misc.TimingUtils.getInstant;

@Slf4j
public class YtScenarioExecutor<Target> {

    private final ProcessorCfg<Target> cfg;
    private final YTBinder<Target> binder;
    private final ObjectModification<Target> modification;
    private final Collection<YtClientProxy> targetProxy;

    protected YtScenarioExecutor(@NonNull ProcessorCfg<Target> cfg, @NonNull ObjectModification<Target> modification) {
        this.cfg = cfg;
        this.binder = cfg.getBinder();
        this.modification = modification;
        this.targetProxy = binder.isDynamic() ? List.of(cfg.getClient()) :
                cfg.getClient().matchReplicas();
    }

    public ProcessorCfg<Target> getCfg() {
        return cfg;
    }

    public String getTable() {
        return cfg.getTable();
    }

    public YTBinder<Target> getBinder() {
        return binder;
    }

    public void removeTargetTable() {
        targetProxy.forEach(proxy -> {
            if (proxy.isPathExists(getTable())) {
                proxy.deletePath(getTable());
            }
        });
    }

    public void clearTargetTable() {
        targetProxy.forEach(proxy -> clearTable(proxy, getTable(), binder));
    }

    public void test(Runnable executor, List<Target> existingRows, List<Target> expectRows) {
        this.test(executor, existingRows, expectRows, Utils.emptyConsumer());
    }

    public void insert(List<Target> existingRows, Consumer<Target> updater) {
        existingRows.forEach(updater);
        insert(existingRows);
    }

    public void insert(List<Target> existingRows) {
        insert(existingRows, true);
    }

    public void insert(List<Target> existingRows, boolean needCleanup) {
        // target таблица не может не быть проинициализирована!
        targetProxy.forEach(proxy -> insertTableImpl(proxy, getTable(), binder, existingRows, needCleanup));
    }

    public void test(Runnable executor, List<Target> existingRows, List<Target> expectRows,
                     Consumer<Target> expectUpdate) {
        log.info("Preparing data (existing rows: {}, expect rows: {})", existingRows.size(), expectRows.size());

        this.insert(existingRows);
        Operations.executeOp("Run test", log, executor);

        log.info("Checking data...");

        this.verify(expectRows, expectUpdate);

        log.info("Checking complete");
    }

    public void verify(List<Target> expectRows) {
        this.verify(expectRows, Utils.emptyConsumer());
    }

    public static <T> void verify(AbstractProcessor<T> process, List<T> expectRows, Consumer<T> updateOp) {
        List<T> actualList = new ArrayList<>();
        YTBinder<T> targetBinder = process.getBinder();
        if (targetBinder.isDynamic()) {
            var query = String.format("* from [%s]", process.getTable());
            actualList.addAll(process.getCfg().getClient().selectRows(query, targetBinder));
        } else {
            process.getCfg().getClient().read(YPath.simple(process.getTable()), targetBinder, actualList::add);
        }

        CoreTestUtils.compare(expectRows, TmsTestUtils.update(actualList, updateOp));
    }

    public List<Target> selectTargetRows() {
        return targetProxy.stream().findAny().map(this::selectTargetRows).orElseThrow();
    }

    public List<Target> selectTargetRows(YtClientProxy proxy) {
        List<Target> actualList = new ArrayList<>();
        if (binder.isDynamic()) {
            var keys = getBinder().getKeys();
            var orderBy = keys.isEmpty() ? "" : (" order by " + String.join(",", keys) + " limit 1000");
            var query = String.format("* from [%s]%s", getTable(), orderBy);
            actualList.addAll(proxy.selectRows(query, binder));
        } else {
            proxy.read(YPath.simple(getTable()), binder, actualList::add);
        }
        return actualList;
    }

    public void verify(List<Target> expectRows, Consumer<Target> updateOp) {
        targetProxy.forEach(proxy -> {
            var actualList = selectTargetRows(proxy);
            CoreTestUtils.compare(expectRows, TmsTestUtils.update(actualList, updateOp));
        });
    }

    public List<Target> asCreated(Collection<Target> list) {
        return TmsTestUtils.map(list, this::asCreated);
    }

    public List<Target> asUpdated(Collection<Target> list) {
        return TmsTestUtils.map(list, this::asUpdated);
    }

    public List<Target> asDeleted(Collection<Target> list) {
        return TmsTestUtils.map(list, this::asDeleted);
    }

    public List<Target> asImported(Collection<Target> list) {
        return TmsTestUtils.map(list, this::asImported);
    }

    public List<Target> asCopy(Collection<Target> list, Consumer<Target> update) {
        return TmsTestUtils.map(list, this::asCopy, update);
    }

    public Target asDeleted(Target obj) {
        modification.markUpdated(obj);
        modification.markDeleted(obj);
        return obj;
    }

    public Target asCreated(Target obj) {
        modification.markUpdated(obj);
        modification.markCreated(obj);
        modification.markImported(obj);
        return obj;
    }

    public Target asUpdated(Target obj) {
        modification.markUpdated(obj);
        return obj;
    }

    public Target asImported(Target obj) {
        modification.markImported(obj);
        return obj;
    }

    public Target asCopy(Target obj) {
        return YTreeDeepCopier.deepCopyOf(obj);
    }

    public ObjectModification<Target> getModification() {
        return modification;
    }

    public static <T> void clearTable(ProcessorCfg<T> cfg) {
        clearTable(cfg.getClient(), cfg.getTable(), cfg.getBinder());
    }

    public static <T> void clearTable(YTProxy proxy, String path, YTBinder<T> binder) {
        if (!proxy.isPathExists(path)) {
            proxy.createTable(path, binder, Map.of());
            return; // ---
        }
        clearTableImpl(proxy, path, binder);
    }

    public static <T> void clearTableImpl(YTProxy proxy, String path, YTBinder<T> binder) {
        if (binder.isDynamic()) {
            var list = proxy.selectRows(String.format("* from [%s]", path), binder);
            if (!list.isEmpty()) {
                proxy.deleteRows(path, binder, list);
            }
        } else {
            // Очистить статическую таблицу через запись в нее пустого массива нельзя
            if (proxy.get(path, "row_count").intValue() > 0) {
                proxy.deletePath(path);
                proxy.createTable(path, binder, Map.of());
            }
        }
    }

    static <T> void insertTable(YTProxy proxy, String path, YTBinder<T> binder, List<T> list) {
        boolean needCleanup;
        if (!proxy.isPathExists(path)) {
            proxy.createTable(path, binder, Map.of());
            needCleanup = false;
        } else {
            needCleanup = true;
        }
        insertTableImpl(proxy, path, binder, list, needCleanup);
    }

    public static <T> void insertTableImpl(YTProxy proxy, String path, YTBinder<T> binder, List<T> list,
                                           boolean needCleanup) {
        if (needCleanup) {
            clearTableImpl(proxy, path, binder);
        }
        if (!list.isEmpty()) {
            if (binder.isDynamic()) {
                proxy.insertRows(path, binder, list);
            } else {
                proxy.write(path, binder, list);
            }
        }
    }

    public interface ObjectModification<Obj> {

        default void markCreated(Obj object) {
            // do nothing
        }

        default void markUpdated(Obj object) {
            // do nothing
        }

        default void markDeleted(Obj object) {
            // do nothing
        }

        default void markImported(Obj object) {
            // do nothing
        }


        static <T> ObjectModification<T> matched() {
            return matched(Utils.emptyConsumer());
        }

        static <T, F> ObjectModification<T> matched(Supplier<F> fieldSource, BiConsumer<T, F> fieldSetter) {
            return matched(value -> {
                F fieldValue = fieldSource.get();
                if (fieldValue != null) {
                    fieldSetter.accept(value, fieldValue);
                }
            });
        }

        static <T> ObjectModification<T> matched(Consumer<T> update) {
            return new ObjectModification<>() {

                @Override
                public void markCreated(T object) {
                    if (object instanceof WithCreatedAt) {
                        ((WithCreatedAt) object).setCreated_at(getInstant());
                    }
                }

                @Override
                public void markUpdated(T object) {
                    if (object instanceof WithUpdatedAt) {
                        ((WithUpdatedAt) object).setUpdated_at(getInstant());
                    }
                    update.accept(object);
                }

                @Override
                public void markDeleted(T object) {
                    if (object instanceof WithStatus) {
                        ((WithStatus) object).setStatus(Status.DELETED);
                    }
                }

                @Override
                public void markImported(T object) {
                    if (object instanceof WithImportedAt) {
                        ((WithImportedAt) object).setImported_at(getInstant());
                    }
                }
            };
        }

    }

    public static <T> YtScenarioExecutor<T> from(ProcessorCfg<T> cfg) {
        return new YtScenarioExecutor<>(cfg, YtScenarioExecutor.ObjectModification.matched());
    }

}
