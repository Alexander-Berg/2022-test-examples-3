package ru.yandex.market.mbo.configs;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.mbo.appcontext.UnstableInit;
import ru.yandex.market.mbo.configs.yt.YtHttpConfig;
import ru.yandex.market.mbo.configs.yt.YtModelStoreConfig;
import ru.yandex.market.mbo.configs.yt.YtRpcConfig;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelIndexWriter;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelStore;
import ru.yandex.market.mbo.db.modelstorage.yt.YtRangeIdGenerator;
import ru.yandex.market.mbo.db.modelstorage.yt.fast.delivery.YtCardQueue;
import ru.yandex.market.mbo.db.modelstorage.yt.fast.delivery.YtCardQueueTableConfig;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.YtModelIndexReaders;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelCatalogIndexReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByAliasReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByBarcodeReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByCategoryVendorIdReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByGroupIdReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByIdReader;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.readers.YtModelIndexByVendorCodeReader;
import ru.yandex.market.yt.util.table.YtTableService;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static ru.yandex.market.mbo.configs.yt.YtModelStoreConfig.MAX_SAFE_INTEGER;
import static ru.yandex.market.mbo.configs.yt.YtModelStoreConfig.RESERVED_FOR_SEARCH_32;
import static ru.yandex.market.mbo.configs.yt.YtModelStoreConfig.RESERVED_FOR_SEARCH_64;


/**
 * Тестовая конфигурация для Yt-тестов.
 */
@Import({TestConfiguration.class,
    YtHttpConfig.class,
    YtRpcConfig.class,
    YtModelStoreConfig.class,
})
public class YtTestConfiguration {

    //В таблице прописана политика отчистки по ttl
    private static final String YT_CARD_QUEUE_PATH =
        "//home/market/development/mbo/model-storage/model-render-queue";

    @Resource
    private UnstableInit<YtClient> ytClient;

    @Resource
    private YtTableService ytTableService;

    @Resource
    private YtTableModel idsTable32;

    @Resource
    private YtTableModel idsTable64;

    @Resource
    private YtModelStore ytModelStore;

    @Resource
    private Yt yt;
    @Resource
    private YtTableService tableService;
    @Resource
    private YtTableModel modelTableModel;
    @Resource
    private YtCardQueueTableConfig ytCardQueueTableConfig;

    @Resource
    private YtTableModel modelIndexByIdTableModel;
    @Resource
    private YtModelIndexWriter ytModelIndexByIdWriter;
    @Resource
    private YtModelIndexByIdReader ytModelIndexByIdReader;

    @Resource
    private YtTableModel modelIndexByCategoryVendorIdTableModel;
    @Resource
    private YtModelIndexWriter ytModelIndexByCategoryVendorIdWriter;
    @Resource
    private YtModelIndexByCategoryVendorIdReader ytModelIndexByCategoryVendorReader;

    @Resource
    private YtTableModel modelIndexByGroupIdTableModel;
    @Resource
    private YtModelIndexWriter ytModelIndexByGroupIdWriter;
    @Resource
    private YtModelIndexByGroupIdReader ytModelIndexByGroupModelReader;

    @Resource
    private YtTableModel modelIndexByBarcodeTableModel;
    @Resource
    private YtModelIndexWriter ytModelIndexByBarcodeWriter;
    @Resource
    YtModelIndexByBarcodeReader ytModelIndexByBarcodeReader;

    @Resource
    private YtTableModel modelIndexByVendorCodeTableModel;
    @Resource
    private YtModelIndexWriter ytModelIndexByVendorCodeWriter;
    @Resource
    private YtModelIndexByVendorCodeReader ytModelIndexByVendorCodeReader;

    @Resource
    private YtTableModel modelIndexByAliasTableModel;
    @Resource
    private YtModelIndexWriter ytModelIndexByAliasWriter;
    @Resource
    private YtModelIndexByAliasReader ytModelIndexByAliasReader;
    @Resource
    private YtCardQueue ytCardQueue;


    @Resource
    private YtTableModel catalogIndexTableModel;
    @Resource
    private YtModelIndexWriter ytModelCatalogIndexWriter;
    @Resource
    private YtModelCatalogIndexReader ytModelCatalogIndexReader;

    @Resource
    private YtModelIndexReaders ytModelIndexReaders;

    @Bean
    public YtRangeIdGenerator masterModelStorageIdSequence() {
        return new YtRangeIdGenerator(idsTable32, Integer.MAX_VALUE - RESERVED_FOR_SEARCH_32, ytClient);
    }

    @Bean
    public YtRangeIdGenerator masterModelStorageGeneratedIdSequence() {
        return new YtRangeIdGenerator(idsTable64, MAX_SAFE_INTEGER - RESERVED_FOR_SEARCH_64, ytClient);
    }

    @Bean
    public TestIndexesHolder testIndexesHolder() {
        ytCardQueueTableConfig.renderQueueTableModel().setPath(YT_CARD_QUEUE_PATH);
        return new TestIndexesHolder(ytModelStore, ytCardQueue, yt, tableService, modelTableModel,
            modelIndexByIdTableModel, ytModelIndexByIdWriter,
            modelIndexByCategoryVendorIdTableModel, ytModelIndexByCategoryVendorIdWriter,
            modelIndexByGroupIdTableModel, ytModelIndexByGroupIdWriter,
            modelIndexByBarcodeTableModel, ytModelIndexByBarcodeWriter, ytModelIndexByBarcodeReader,
            modelIndexByVendorCodeTableModel, ytModelIndexByVendorCodeWriter,
            modelIndexByAliasTableModel, ytModelIndexByAliasWriter,
            catalogIndexTableModel, ytModelCatalogIndexWriter,
            ytClient,
            ytModelIndexByIdReader,
            ytModelIndexByCategoryVendorReader,
            ytModelIndexByGroupModelReader,
            ytModelIndexByVendorCodeReader,
            ytModelIndexByAliasReader,
            ytModelCatalogIndexReader,
            ytModelIndexReaders);
    }
}
