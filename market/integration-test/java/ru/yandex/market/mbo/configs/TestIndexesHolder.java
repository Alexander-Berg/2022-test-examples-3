package ru.yandex.market.mbo.configs;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.appcontext.UnstableInit;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelIndexWriter;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelMultipleIndexWriter;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelSingleIndexWriter;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelStore;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelUtil;
import ru.yandex.market.mbo.db.modelstorage.yt.fast.delivery.YtCardQueue;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.YtModelIndexReaders;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelCatalogIndexReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByAliasReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByBarcodeReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByCategoryVendorIdReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByGroupIdReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByIdReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByVendorCodeReader;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.YtTableService;
import ru.yandex.market.yt.util.table.constants.TabletState;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.rpcproxy.ETransactionType;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransactionOptions;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;


public class TestIndexesHolder implements InitializingBean, DisposableBean {

    public static final ApiServiceTransactionOptions TEST_TRANSACTION_OPTIONS =
        new ApiServiceTransactionOptions(ETransactionType.TT_TABLET)
            .setSticky(true);

    private static final String QUERY = "* FROM [%s]";

    private static final int THREAD_COUNT = 8;
    private static final int TERMINATION_TIMEOUT = 2;
    private static final long AWAIT_MOUNITNG_TIMEOUT_MILLIS = 3 * 60 * 1000L;
    private static final long RETRY_MILLIS = 1000L;

    private final YtModelStore ytModelStore;
    private final YtCardQueue ytCardQueue;
    private final Yt yt;
    private final YtTableService tableService;
    private final UnstableInit<YtClient> ytClient;
    private final UUID randomUUID;

    private YPath modelTablePath;
    private YtTableRpcApi modelTableRpcApi;
    private final YtTableModel modelTableModel;

    private YPath modelIndexByIdTablePath;
    private YtTableRpcApi modelIndexByIdTableRpcApi;
    private final YtTableModel modelIndexByIdTableModel;
    private final YtModelIndexWriter ytModelIndexByIdWriter;
    private final YtModelIndexByIdReader ytModelIndexByIdReader;

    private YPath modelIndexByCategoryVendorIdTablePath;
    private YtTableRpcApi modelIndexByCategoryVendorIdTableRpcApi;
    private final YtTableModel modelIndexByCategoryVendorIdTableModel;
    private final YtModelIndexWriter ytModelIndexByCategoryVendorIdWriter;
    private final YtModelIndexByCategoryVendorIdReader ytModelIndexByCategoryVendorIdReader;

    private YPath modelIndexByGroupIdTablePath;
    private YtTableRpcApi modelIndexByGroupIdTableRpcApi;
    private final YtTableModel modelIndexByGroupIdTableModel;
    private final YtModelIndexWriter ytModelIndexByGroupIdWriter;
    private final YtModelIndexByGroupIdReader ytModelIndexByGroupIdReader;

    private YPath modelIndexByBarcodeTablePath;
    private YtTableRpcApi modelIndexByBarcodeTableRpcApi;
    private final YtTableModel modelIndexByBarcodeTableModel;
    private final YtModelIndexWriter ytModelIndexByBarcodeWriter;
    private final YtModelIndexByBarcodeReader ytModelIndexByBarcodeReader;

    private YPath modelIndexByVendorCodeTablePath;
    private YtTableRpcApi modelIndexByVendorCodeTableRpcApi;
    private final YtTableModel modelIndexByVendorCodeTableModel;
    private final YtModelIndexWriter ytModelIndexByVendorCodeWriter;
    private final YtModelIndexByVendorCodeReader ytModelIndexByVendorCodeReader;

    private YPath modelIndexByAliasTablePath;
    private YtTableRpcApi modelIndexByAliasTableRpcApi;
    private final YtTableModel modelIndexByAliasTableModel;
    private final YtModelIndexWriter ytModelIndexByAliasWriter;
    private final YtModelIndexByAliasReader ytModelIndexByAliasReader;

    private YPath modelCatalogIndexTablePath;
    private YtTableRpcApi catalogIndexTableModelRpcApi;
    private final YtTableModel catalogIndexTableModel;
    private final YtModelIndexWriter ytModelCatalogIndexWriter;
    private final YtModelCatalogIndexReader ytModelCatalogIndexReader;

    private final YtModelIndexReaders ytModelIndexReaders;

    @SuppressWarnings("checkstyle:parameterNumber")
    public TestIndexesHolder(YtModelStore ytModelStore,
                             YtCardQueue ytCardQueue,
                             Yt yt,
                             YtTableService tableService,
                             YtTableModel modelTableModel,
                             YtTableModel modelIndexByIdTableModel,
                             YtModelIndexWriter ytModelIndexByIdWriter,
                             YtTableModel modelIndexByCategoryVendorIdTableModel,
                             YtModelIndexWriter ytModelIndexByCategoryVendorIdWriter,
                             YtTableModel modelIndexByGroupIdTableModel,
                             YtModelIndexWriter ytModelIndexByGroupIdWriter,
                             YtTableModel modelIndexByBarcodeTableModel,
                             YtModelIndexWriter ytModelIndexByBarcodeWriter,
                             YtModelIndexByBarcodeReader ytModelIndexByBarcodeReader,
                             YtTableModel modelIndexByVendorCodeTableModel,
                             YtModelIndexWriter ytModelIndexByVendorCodeWriter,
                             YtTableModel modelIndexByAliasTableModel,
                             YtModelIndexWriter ytModelIndexByAliasWriter,
                             YtTableModel catalogIndexTableModel,
                             YtModelIndexWriter ytModelCatalogIndexWriter,
                             UnstableInit<YtClient> ytClient,
                             YtModelIndexByIdReader ytModelIndexByIdReader,
                             YtModelIndexByCategoryVendorIdReader ytModelIndexByCategoryVendorIdReader,
                             YtModelIndexByGroupIdReader ytModelIndexByGroupIdReader,
                             YtModelIndexByVendorCodeReader ytModelIndexByVendorCodeReader,
                             YtModelIndexByAliasReader ytModelIndexByAliasReader,
                             YtModelCatalogIndexReader ytModelCatalogIndexReader,
                             YtModelIndexReaders ytModelIndexReaders) {
        this.ytModelStore = ytModelStore;
        this.ytCardQueue = ytCardQueue;
        this.yt = yt;
        this.modelTableModel = modelTableModel;
        this.modelIndexByIdTableModel = modelIndexByIdTableModel;
        this.ytModelIndexByIdWriter = ytModelIndexByIdWriter;
        this.modelIndexByCategoryVendorIdTableModel = modelIndexByCategoryVendorIdTableModel;
        this.ytModelIndexByCategoryVendorIdWriter = ytModelIndexByCategoryVendorIdWriter;
        this.modelIndexByGroupIdTableModel = modelIndexByGroupIdTableModel;
        this.ytModelIndexByGroupIdWriter = ytModelIndexByGroupIdWriter;
        this.modelIndexByBarcodeTableModel = modelIndexByBarcodeTableModel;
        this.ytModelIndexByBarcodeWriter = ytModelIndexByBarcodeWriter;
        this.ytModelIndexByBarcodeReader = ytModelIndexByBarcodeReader;
        this.modelIndexByVendorCodeTableModel = modelIndexByVendorCodeTableModel;
        this.ytModelIndexByVendorCodeWriter = ytModelIndexByVendorCodeWriter;
        this.tableService = tableService;
        this.ytClient = ytClient;
        this.ytModelIndexByIdReader = ytModelIndexByIdReader;
        this.ytModelIndexByCategoryVendorIdReader = ytModelIndexByCategoryVendorIdReader;
        this.ytModelIndexByGroupIdReader = ytModelIndexByGroupIdReader;
        this.ytModelIndexByVendorCodeReader = ytModelIndexByVendorCodeReader;
        this.modelIndexByAliasTableModel = modelIndexByAliasTableModel;
        this.ytModelIndexByAliasWriter = ytModelIndexByAliasWriter;
        this.catalogIndexTableModel = catalogIndexTableModel;
        this.ytModelCatalogIndexWriter = ytModelCatalogIndexWriter;
        this.ytModelIndexByAliasReader = ytModelIndexByAliasReader;
        this.ytModelCatalogIndexReader = ytModelCatalogIndexReader;
        this.ytModelIndexReaders = ytModelIndexReaders;
        this.randomUUID = UUID.randomUUID();
    }

    @Override
    public void destroy() throws Exception {
        unmountTables();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        waitForInitOrThrow(aVoid -> ytClient.isAvailable(), "ytClient");
        deferredInitTables();
        initRpcApi();
    }

    /**
     * called in afterPropertiesSet.
     */
    private void initRpcApi() {
        YtClient client = ytClient.get();
        modelTableRpcApi = new YtTableRpcApi(tableService.getTable(modelTableModel), client);
        modelIndexByIdTableRpcApi = new YtTableRpcApi(tableService.getTable(modelIndexByIdTableModel), client);
        modelIndexByCategoryVendorIdTableRpcApi = new YtTableRpcApi(
            tableService.getTable(modelIndexByCategoryVendorIdTableModel), client);
        modelIndexByGroupIdTableRpcApi = new YtTableRpcApi(
            tableService.getTable(modelIndexByGroupIdTableModel), client);
        modelIndexByBarcodeTableRpcApi = new YtTableRpcApi(
            tableService.getTable(modelIndexByBarcodeTableModel), client);
        modelIndexByVendorCodeTableRpcApi = new YtTableRpcApi(
            tableService.getTable(modelIndexByVendorCodeTableModel), client);
        modelIndexByAliasTableRpcApi = new YtTableRpcApi(
            tableService.getTable(modelIndexByAliasTableModel), client);
        catalogIndexTableModelRpcApi = new YtTableRpcApi(
            tableService.getTable(catalogIndexTableModel), client);
    }

    private void initPaths() {
        modelTablePath = initRandomYPath(this.modelTableModel, randomUUID);
        modelIndexByIdTablePath = initRandomYPath(this.modelIndexByIdTableModel, randomUUID);
        modelIndexByCategoryVendorIdTablePath = initRandomYPath(this.modelIndexByCategoryVendorIdTableModel,
            randomUUID);
        modelIndexByGroupIdTablePath = initRandomYPath(this.modelIndexByGroupIdTableModel, randomUUID);
        modelIndexByBarcodeTablePath = initRandomYPath(this.modelIndexByBarcodeTableModel, randomUUID);
        modelIndexByVendorCodeTablePath = initRandomYPath(this.modelIndexByVendorCodeTableModel, randomUUID);
        modelIndexByAliasTablePath = initRandomYPath(this.modelIndexByAliasTableModel, randomUUID);
        modelCatalogIndexTablePath = initRandomYPath(this.catalogIndexTableModel, randomUUID);
    }

    private YPath initRandomYPath(YtTableModel ytTableModel, UUID uuid) {
        // несколько некрасивый способ сохранянть один и тот же рандомный путь на время тестов
        String tempPath = ytTableModel.getPath().endsWith("test") ?
            ytTableModel.getPath() :
            ytTableModel.getPath() + "_" + uuid + "_test";
        ytTableModel.setPath(tempPath);
        tableService.getTable(ytTableModel);
        return YPath.simple(tempPath);
    }

    /**
     * parallel init tables if needed.
     * called in afterPropertiesSet.
     */
    public void deferredInitTables() {
        initPaths();
        // parallel init
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        executorService.submit(ytModelStore::deferredInit);
        executorService.submit(ytCardQueue::deferredInit);
        executorService.submit(() -> {
            ytModelIndexByIdWriter.deferredInit();
            ytModelIndexByIdReader.deferredInit();
        });
        executorService.submit(() -> {
            ytModelIndexByCategoryVendorIdWriter.deferredInit();
            ytModelIndexByCategoryVendorIdReader.deferredInit();
        });
        executorService.submit(() -> {
            ytModelIndexByGroupIdWriter.deferredInit();
            ytModelIndexByGroupIdReader.deferredInit();
        });
        executorService.submit(() -> {
            ytModelIndexByBarcodeWriter.deferredInit();
            ytModelIndexByBarcodeReader.deferredInit();
        });
        executorService.submit(() -> {
            ytModelIndexByVendorCodeWriter.deferredInit();
            ytModelIndexByVendorCodeReader.deferredInit();
        });
        executorService.submit(() -> {
            ytModelIndexByAliasWriter.deferredInit();
            ytModelIndexByAliasReader.deferredInit();
        });
        executorService.submit(() -> {
            ytModelCatalogIndexWriter.deferredInit();
            ytModelCatalogIndexReader.deferredInit();
        });
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
        }
        // check if all tables are created and mounted
        waitForInitOrThrow(aVoid -> allTablesEnabled(), "deferredInitTables");
    }

    private void waitForInitOrThrow(Predicate<Void> condition, String s) {
        long start = System.currentTimeMillis();
        while (!condition.test(null)) {
            if ((System.currentTimeMillis() - start) > AWAIT_MOUNITNG_TIMEOUT_MILLIS) {
                throw new RuntimeException("Tired of waiting for " + s + ", failing");
            }
            try {
                Thread.sleep(RETRY_MILLIS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean allTablesEnabled() {
        boolean allExist = yt.cypress().exists(modelTablePath) &&
            yt.cypress().exists(modelIndexByIdTablePath) &&
            yt.cypress().exists(modelIndexByCategoryVendorIdTablePath) &&
            yt.cypress().exists(modelIndexByGroupIdTablePath) &&
            yt.cypress().exists(modelIndexByBarcodeTablePath) &&
            yt.cypress().exists(modelIndexByVendorCodeTablePath) &&
            yt.cypress().exists(modelIndexByAliasTablePath) &&
            yt.cypress().exists(modelCatalogIndexTablePath);

        return allExist &&
            tableIsMounted(modelTablePath) &&
            tableIsMounted(modelIndexByIdTablePath) &&
            tableIsMounted(modelIndexByCategoryVendorIdTablePath) &&
            tableIsMounted(modelIndexByGroupIdTablePath) &&
            tableIsMounted(modelIndexByBarcodeTablePath) &&
            tableIsMounted(modelIndexByVendorCodeTablePath) &&
            tableIsMounted(modelIndexByAliasTablePath) &&
            tableIsMounted(modelCatalogIndexTablePath);
    }

    private boolean tableIsMounted(YPath path) {
        Optional<YTreeNode> attribute = yt.cypress()
            .get(path, Cf.set("tablet_state"))
            .getAttribute("tablet_state");
        YTreeNode nodeOrNull = attribute.orElse(null);
        String state = nodeOrNull == null ? null : nodeOrNull.stringValue().toLowerCase();
        return TabletState.MOUNTED.name().toLowerCase().equals(state);
    }

    public void clearTables() {
        List<Map<String, Object>> modelsKeyColumns = new ArrayList<>();
        List<ModelStorage.Model> models = new ArrayList<>();
        yt.tables().selectRows(String.format(QUERY, modelTableModel.getPath()), YTableEntryTypes.YSON, row -> {
            modelsKeyColumns.add(YtModelUtil.toLookupMap(row.getLong(YtModelColumns.MODEL_ID),
                row.getLong(YtModelColumns.CATEGORY_ID)));
            models.add(YtModelUtil.extractModel(row));
        });
        modelTableRpcApi.doInTransaction(TEST_TRANSACTION_OPTIONS, tr -> {
            ModifyRowsRequest modelsDeleteRequest = modelTableRpcApi.createModifyRowRequest();
            modelsKeyColumns.forEach(modelsDeleteRequest::addDelete);
            tr.modifyRows(modelsDeleteRequest).join();

            clearIndexTable(modelIndexByIdTableRpcApi, ytModelIndexByIdWriter, models, tr);
            clearIndexTable(modelIndexByCategoryVendorIdTableRpcApi, ytModelIndexByCategoryVendorIdWriter, models, tr);
            clearIndexTable(modelIndexByGroupIdTableRpcApi, ytModelIndexByGroupIdWriter, models, tr);
            clearIndexTable(modelIndexByBarcodeTableRpcApi, ytModelIndexByBarcodeWriter, models, tr);
            clearIndexTable(modelIndexByVendorCodeTableRpcApi, ytModelIndexByVendorCodeWriter, models, tr);
            clearIndexTable(modelIndexByAliasTableRpcApi, ytModelIndexByAliasWriter, models, tr);
            clearIndexTable(catalogIndexTableModelRpcApi, ytModelCatalogIndexWriter, models, tr);
            return true;
        });

    }

    private void clearIndexTable(YtTableRpcApi rpcApi, YtModelIndexWriter writer, List<ModelStorage.Model> models,
                                 ApiServiceTransaction tr) {
        ModifyRowsRequest modifyRowRequest = rpcApi.createModifyRowRequest();
        models.forEach(model -> {
            List<String> keyColumns = writer.getKeyColumns();
            List<Map<String, Object>> indexRows = getIndexRows(writer, model);
            indexRows.forEach(rowMap -> {
                Map<String, Object> toDelete = new HashMap<>();
                rowMap.entrySet().stream()
                    .filter(stringObjectEntry -> keyColumns.contains(stringObjectEntry.getKey()))
                    .forEach(entry -> toDelete.put(entry.getKey(), entry.getValue()));
                modifyRowRequest.addDelete(toDelete);
            });
        });
        tr.modifyRows(modifyRowRequest).join();

    }

    private List<Map<String, Object>> getIndexRows(YtModelIndexWriter writer, ModelStorage.Model model) {
        if (writer instanceof YtModelSingleIndexWriter) {
            return Collections.singletonList(
                ((YtModelSingleIndexWriter) writer).getIndexFieldsExtractor().apply(model)
            );
        }
        if (writer instanceof YtModelMultipleIndexWriter) {
            return ((YtModelMultipleIndexWriter) writer).getIndexFieldsExtractor().apply(model);
        }
        throw new IllegalStateException("Unknown index writer type");
    }

    /**
     * delete all tables after tests.
     * called in destroy.
     */
    public void unmountTables() {
        // таблица
        unmountTable(modelTablePath);
        // индексы
        unmountTable(modelIndexByIdTablePath);
        unmountTable(modelIndexByCategoryVendorIdTablePath);
        unmountTable(modelIndexByGroupIdTablePath);
        unmountTable(modelIndexByBarcodeTablePath);
        unmountTable(modelIndexByVendorCodeTablePath);
        unmountTable(modelIndexByAliasTablePath);
        unmountTable(modelCatalogIndexTablePath);

    }

    private void unmountTable(YPath path) {
        if (yt.cypress().exists(path)) {
            yt.tables().unmount(path);
            yt.cypress().remove(path);
        }
    }

    public Yt yt() {
        return yt;
    }

    public YtTableRpcApi rpcApi() {
        return modelTableRpcApi;
    }

    public YtModelStore ytModelStore() {
        return ytModelStore;
    }

    public YPath modelTablePath() {
        return modelTablePath;
    }

    public YPath modelIndexByIdTablePath() {
        return modelIndexByIdTablePath;
    }

    public YPath modelIndexByCategoryVendorIdTablePath() {
        return modelIndexByCategoryVendorIdTablePath;
    }

    public YPath modelIndexByGroupIdTablePath() {
        return modelIndexByGroupIdTablePath;
    }

    public YPath modelCatalogIndexTablePath() {
        return modelCatalogIndexTablePath;
    }

    public YtModelIndexWriter ytModelIndexByBarcodeWriter() {
        return ytModelIndexByBarcodeWriter;
    }

    public YtModelIndexReaders ytModelIndexReaders() {
        return ytModelIndexReaders;
    }

    public YtModelIndexWriter ytModelIndexByVendorCodeWriter() {
        return ytModelIndexByVendorCodeWriter;
    }

    public YtModelIndexWriter ytModelCatalogIndexWriter() {
        return ytModelCatalogIndexWriter;
    }

    public YtModelIndexWriter ytModelIndexByAliasWriter() {
        return ytModelIndexByAliasWriter;
    }
}
