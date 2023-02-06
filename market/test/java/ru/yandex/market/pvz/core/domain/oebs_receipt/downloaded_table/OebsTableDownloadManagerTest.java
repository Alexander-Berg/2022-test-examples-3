package ru.yandex.market.pvz.core.domain.oebs_receipt.downloaded_table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptParams;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptParamsMapper;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOebsReceiptFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OebsTableDownloadManagerTest {

    private static final String TABLE_PATH = "//tmp/test/table";
    private static final Instant MODIFICATION_TIME = Instant.EPOCH;

    private final TestOebsReceiptFactory oebsReceiptFactory;

    private final TransactionTemplate transactionTemplate;
    private final OebsReceiptParamsMapper mapper;
    private final OebsReceiptRepository oebsReceiptRepository;
    private final OebsTableDownloadManager tableDownloadManager;
    private final OebsDownloadedTableRepository downloadedTableRepository;

    @BeforeEach
    void setup() {
        oebsReceiptRepository.deleteAll();
        downloadedTableRepository.deleteAll();
    }

    @Test
    void testDownload() {
        List<OebsReceiptParams> receipts = buildOebsReceipts(5);

        tableDownloadManager.downloadOebsReceipts(TABLE_PATH, MODIFICATION_TIME, receipts);

        checkTableIsDownloaded(TABLE_PATH, MODIFICATION_TIME);
        setTableId(receipts, TABLE_PATH);

        assertThat(getAllReceipts()).containsExactlyInAnyOrderElementsOf(receipts);
    }

    @Test
    void testUpdateDownloadedReceipts() {
        List<OebsReceiptParams> initialReceipts = buildOebsReceipts(3);
        List<OebsReceiptParams> updatedReceipts = new ArrayList<>(initialReceipts);
        updatedReceipts.addAll(buildOebsReceipts(3));

        tableDownloadManager.downloadOebsReceipts(TABLE_PATH, MODIFICATION_TIME, initialReceipts);
        tableDownloadManager.downloadOebsReceipts(TABLE_PATH, MODIFICATION_TIME.plusSeconds(1), updatedReceipts);

        checkTableIsDownloaded(TABLE_PATH, MODIFICATION_TIME.plusSeconds(1));
        setTableId(Iterables.concat(initialReceipts, updatedReceipts), TABLE_PATH);

        assertThat(getAllReceipts()).containsExactlyInAnyOrderElementsOf(updatedReceipts);
    }

    @Test
    void testDownloadAnotherTable() {
        List<OebsReceiptParams> receipts = buildOebsReceipts(3);
        List<OebsReceiptParams> anotherTableReceipts = buildOebsReceipts(3);

        String anotherTableName = TABLE_PATH + "123";
        tableDownloadManager.downloadOebsReceipts(TABLE_PATH, MODIFICATION_TIME, receipts);
        tableDownloadManager.downloadOebsReceipts(anotherTableName, MODIFICATION_TIME, anotherTableReceipts);

        checkTableIsDownloaded(TABLE_PATH, MODIFICATION_TIME);
        checkTableIsDownloaded(anotherTableName, MODIFICATION_TIME);
        setTableId(receipts, TABLE_PATH);
        setTableId(anotherTableReceipts, anotherTableName);

        assertThat(getAllReceipts())
                .containsExactlyInAnyOrderElementsOf(Iterables.concat(receipts, anotherTableReceipts));
    }

    @Test
    void testOverrideOebsReceiptsWithoutTable() {
        List<OebsReceiptParams> receipts = buildOebsReceipts(3);
        oebsReceiptRepository.saveAll(StreamEx.of(receipts).map(mapper::map).toList());

        receipts.addAll(buildOebsReceipts(2));
        tableDownloadManager.downloadOebsReceipts(TABLE_PATH, MODIFICATION_TIME, receipts);

        checkTableIsDownloaded(TABLE_PATH, MODIFICATION_TIME);
        setTableId(receipts, TABLE_PATH);


        assertThat(getAllReceipts()).containsExactlyInAnyOrderElementsOf(receipts);
    }


    private List<OebsReceiptParams> buildOebsReceipts(int n) {
        return StreamEx.generate(() -> oebsReceiptFactory.buildReceipt(
                TestOebsReceiptFactory.OebsReceiptTestParams.builder().build()))
                .limit(n)
                .toList();
    }

    private void checkTableIsDownloaded(String path, Instant modificationTime) {
        Optional<OebsDownloadedTable> tableO = downloadedTableRepository.findByTablePath(path);
        assertThat(tableO).isNotEmpty();
        assertThat(tableO.get().getTablePath()).isEqualTo(path);
        assertThat(tableO.get().getModificationTime()).isEqualTo(modificationTime);
    }

    private void setTableId(Iterable<OebsReceiptParams> oebsReceipts, String tablePath) {
        long tableId = downloadedTableRepository.findByTablePath(tablePath).orElseThrow().getId();
        oebsReceipts.forEach(r -> r.setDownloadedTableId(tableId));
    }

    private List<OebsReceiptParams> getAllReceipts() {
        return transactionTemplate.execute(s -> StreamEx.of(oebsReceiptRepository.findAll())
                .map(mapper::map)
                .toList());
    }

}
