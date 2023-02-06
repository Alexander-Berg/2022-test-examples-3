package ru.yandex.market.mboc.common.services.msku.sync;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

public class YtImportOperationsMock extends YtImportOperationsImpl {
    private static final Logger log = LoggerFactory.getLogger(YtImportOperationsMock.class);
    private String currentYtId;
    private List<YTreeMapNode> mskuNodes;

    public YtImportOperationsMock(StorageKeyValueService storageKeyValueService) {
        super(null, null, storageKeyValueService);
    }

    @Override
    public <ImportResult> void importFromYt(String sessionId,
                                            int batchSize,
                                            Function<List<YTreeMapNode>, List<ImportResult>> batchImportFunction,
                                            Consumer<List<ImportResult>> batchResultConsumer) {
        log.debug("+importFromYt()");
        importFromYt(mskuNodes.iterator(), batchSize, batchImportFunction, batchResultConsumer);
        log.debug("-importFromYt()");
    }

    @Override
    public String getCurrentYtSessionId() {
        return currentYtId;
    }

    public YtImportOperationsMock setCurrentYtId(String currentYtId) {
        this.currentYtId = currentYtId;
        return this;
    }

    public YtImportOperationsMock setMskuNodes(List<YTreeMapNode> mskuNodes) {
        this.mskuNodes = mskuNodes;
        return this;
    }
}
