package ru.yandex.crypta.graph2.dao.yt.local.fastyt.testdata;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtImpl;
import ru.yandex.inside.yt.kosher.impl.transactions.utils.YtTransactionsUtils;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.misc.ExceptionUtils;

import static ru.yandex.inside.yt.kosher.tables.YTableEntryTypes.YSON;

public class DataHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DataHelper.class);

    private YtImpl realYt;
    private Yt localFsYt;

    public DataHelper(YtImpl realYt, Yt localFsYt) {
        this.realYt = realYt;
        this.localFsYt = localFsYt;
    }

    public void dumpTableToFile(YPath table) {
        dumpTableToFile(table, table, null);
    }

    public void dumpTableToFile(YPath src, YPath dst, Integer nRecs) {
        Duration transactionTimeout = realYt.getConfiguration().getSnapshotTransactionTimeout();
        Duration pingTimeout = realYt.getConfiguration().getSnapshotTransactionPingPeriod();

        YtTransactionsUtils.withSnapshotLock(realYt,
                transactionTimeout,
                Optional.empty(),
                true,
                Optional.of(pingTimeout),
                src, (tx, path) -> {
                    Optional<GUID> txId = Optional.of(tx.getId());
                    try (CloseableIterator<YTreeMapNode> allRecs = realYt.tables().read(txId, true, path, YSON)) {

                        IteratorF<YTreeMapNode> recs = Cf.wrap(new LoggingIterator<>(allRecs, 100000));

                        if (nRecs != null) {
                            recs = recs.take(nRecs);
                        }

                        LOG.info("Dumping recs from {} to {}", src, dst);

                        localFsYt.tables().write(dst, YSON, recs);
                        return null;
                    } catch (Exception e) {
                        throw ExceptionUtils.translate(e);
                    }
                }
        );

    }

    public void uploadLocalTableToYt(YPath src, YPath dst) {
        Duration transactionTimeout = realYt.getConfiguration().getSnapshotTransactionTimeout();
        Duration pingTimeout = realYt.getConfiguration().getSnapshotTransactionPingPeriod();

        YtTransactionsUtils.withTransaction(realYt,
                transactionTimeout,
                Optional.empty(),
                true,
                Optional.of(pingTimeout),
                (tx) -> {
                    Optional<GUID> txId = Optional.of(tx.getId());
                    try (CloseableIterator<YTreeMapNode> allRecs = localFsYt.tables().read(Optional.empty(), true, src, YSON)) {

                        IteratorF<YTreeMapNode> recs = Cf.wrap(new LoggingIterator<>(allRecs, 100000));

                        LOG.info("Uploading recs from {} to {}", src, dst);

                        realYt.cypress().create(dst, CypressNodeType.TABLE, true, true);
                        realYt.tables().write(txId, true, dst, YSON, recs);
                        return null;
                    } catch (Exception e) {
                        throw ExceptionUtils.translate(e);
                    }
                }
        );

    }


    public void dumpTableToFile(YPath table, int nRecs) {
        dumpTableToFile(table, table, nRecs);
    }

    public YtImpl getRealYt() {
        return realYt;
    }

    public Yt getLocalFsYt() {
        return localFsYt;
    }
}
