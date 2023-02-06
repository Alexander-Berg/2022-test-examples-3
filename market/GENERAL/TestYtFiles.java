package ru.yandex.market.mbo.yt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.files.YtFiles;
import ru.yandex.misc.io.IoFunction1V;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ExistsNode;
import ru.yandex.yt.ytclient.proxy.request.TransactionalOptions;

public class TestYtFiles implements YtFiles {
    private final TestYt testYt;
    private final Map<YPath, byte[]> files = new HashMap<>();

    public TestYtFiles(TestYt testYt) {
        this.testYt = testYt;
    }

    @Override
    public void write(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path, InputStream content,
                      boolean computeMD5) {
        ExistsNode existsNode = new ExistsNode(path)
                .setTransactionalOptions(new TransactionalOptions().setTransactionId(transactionId.orElse(null)));
        CreateNode createNode = new CreateNode(path, CypressNodeType.FILE)
                .setTransactionalOptions(new TransactionalOptions().setTransactionId(transactionId.orElse(null)))
                .setRecursive(true);

        if (!testYt.cypress().exists(existsNode)) {
            testYt.cypress().create(createNode);
        }
        try {
            files.put(path.justPath(), IOUtils.toByteArray(content));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void read(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path,
                     IoFunction1V<InputStream> callback) {
        ExistsNode existsNode = new ExistsNode(path)
                .setTransactionalOptions(new TransactionalOptions().setTransactionId(transactionId.orElse(null)));
        if (!testYt.cypress().exists(existsNode) || !files.containsKey(path.justPath())) {
            throw new RuntimeException("File " + path + " not exist");
        }
        byte[] bytes = files.get(path.justPath());
        callback.apply(new ByteArrayInputStream(bytes));
    }
}
