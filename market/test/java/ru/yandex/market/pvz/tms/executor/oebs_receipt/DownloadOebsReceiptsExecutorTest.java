package ru.yandex.market.pvz.tms.executor.oebs_receipt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptParams;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptParamsMapper;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptRepository;
import ru.yandex.market.pvz.core.domain.oebs_receipt.downloaded_table.OebsDownloadedTable;
import ru.yandex.market.pvz.core.domain.oebs_receipt.downloaded_table.OebsDownloadedTableRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.tms.executor.oebs_receipt.model.OebsPaymentYtModel;
import ru.yandex.market.tpl.common.yt.tables.download.YtBrowserEntry;
import ru.yandex.market.tpl.common.yt.tables.download.YtDownloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.OEBS_INCORRECT;

@TransactionlessEmbeddedDbTest
@Import({DownloadOebsReceiptsExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DownloadOebsReceiptsExecutorTest {

    private static final String FOLDER_PATH = "//tmp/oebs/payments";
    private static final String TABLE_PATH = FOLDER_PATH + "/table";

    private final TransactionTemplate transactionTemplate;
    private final OebsReceiptParamsMapper mapper;
    private final DownloadOebsReceiptsExecutor executor;

    private final OebsDownloadedTableRepository downloadedTableRepository;
    private final OebsReceiptRepository oebsReceiptRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @MockBean
    private YtDownloader ytDownloader;

    @BeforeEach
    void setup() {
        oebsReceiptRepository.deleteAll();
        downloadedTableRepository.deleteAll();
    }

    @Test
    void testDownloadTable() {
        YtBrowserEntry table = buildFakeTable(TABLE_PATH, Instant.EPOCH);
        OebsPaymentYtModel row = buildYtModel();
        mockTable(table, List.of(row));

        executor.doRealJob(null);

        checkTableIsDownloaded(table.getPath(), table.getModificationTime());

        assertThat(getAllReceipts()).hasSize(1);
        assertThat(getAllReceipts().get(0).getOebsNumber()).isEqualTo(row.getDocNumber());
    }

    @Test
    void testDownloadUpdatedTable() {
        YtBrowserEntry table = buildFakeTable(TABLE_PATH, Instant.EPOCH);
        OebsPaymentYtModel row1 = buildYtModel();
        mockTable(table, List.of(row1));

        executor.doRealJob(null);

        table = buildFakeTable(TABLE_PATH, Instant.EPOCH.plusSeconds(1));
        OebsPaymentYtModel row2 = buildYtModel();
        mockTable(table, List.of(row1, row2));

        executor.doRealJob(null);

        checkTableIsDownloaded(table.getPath(), table.getModificationTime());

        List<String> actual = StreamEx.of(getAllReceipts())
                .map(OebsReceiptParams::getOebsNumber)
                .toList();

        List<String> expected = List.of(row1.getDocNumber(), row2.getDocNumber());

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void testDontDownloadIfNotModified() {
        testDownloadTable();

        executor.doRealJob(null);

        verify(ytDownloader, times(1))
                .downloadTable(any(YtBrowserEntry.class), eq(OebsPaymentYtModel.class), any());
    }

    private YtBrowserEntry buildFakeTable(String path, Instant modificationTime) {
        YtBrowserEntry entry = mock(YtBrowserEntry.class);
        when(entry.isFolder()).thenReturn(false);
        when(entry.isTable()).thenReturn(true);
        when(entry.getPath()).thenReturn(path);
        when(entry.getModificationTime()).thenReturn(modificationTime);
        return entry;
    }

    private YtBrowserEntry buildFakeFolder(String path, List<YtBrowserEntry> entries) {
        YtBrowserEntry entry = mock(YtBrowserEntry.class);
        when(entry.isFolder()).thenReturn(true);
        when(entry.isTable()).thenReturn(false);
        when(entry.getPath()).thenReturn(path);
        when(entry.browseEntries()).thenReturn(entries);
        when(entry.browseEntriesToMap()).thenCallRealMethod();
        return entry;
    }

    private OebsPaymentYtModel buildYtModel() {
        return OebsPaymentYtModel.builder()
                .docNumber(String.valueOf(RandomUtils.nextLong()))
                .docAmountRub(RandomUtils.nextDouble(1.0, 100_000.0))
                .trxNumber("TINKOFFFF_123")
                .paydocNumber("431242")
                .paydocDate("2020-09-03")
                .build();
    }

    private void mockTable(YtBrowserEntry table, List<OebsPaymentYtModel> rows) {
        YtBrowserEntry folder = buildFakeFolder(FOLDER_PATH, List.of(table));

        when(ytDownloader.browseFolder(any())).thenReturn(folder);
        when(ytDownloader.downloadTable(any(YtBrowserEntry.class), eq(OebsPaymentYtModel.class), any()))
                .thenReturn(rows);
    }

    private List<OebsReceiptParams> getAllReceipts() {
        return transactionTemplate.execute(s -> StreamEx.of(oebsReceiptRepository.findAll())
                .map(mapper::map)
                .toList());
    }

    private void checkTableIsDownloaded(String path, Instant modificationTime) {
        Optional<OebsDownloadedTable> tableO = downloadedTableRepository.findByTablePath(path);
        assertThat(tableO).isNotEmpty();
        assertThat(tableO.get().getTablePath()).isEqualTo(path);
        assertThat(tableO.get().getModificationTime()).isEqualTo(modificationTime);
    }

    @Test
    void downloadTableWithIncorrectOebsAndInvalidPaydocDate() {
        configurationGlobalCommandService.setValue(OEBS_INCORRECT, true);
        YtBrowserEntry table = buildFakeTable(TABLE_PATH, Instant.EPOCH);
        OebsPaymentYtModel row = OebsPaymentYtModel.builder()
                .docNumber(String.valueOf(RandomUtils.nextLong()))
                .docAmountRub(RandomUtils.nextDouble(1.0, 100_000.0))
                .trxNumber("TINKOFFFF_123")
                .paydocNumber("431242")
                .paydocDate("")
                .build();
        mockTable(table, List.of(row));

        executor.doRealJob(null);

        checkTableIsDownloaded(table.getPath(), table.getModificationTime());

        assertThat(getAllReceipts()).isEmpty();
    }

    @Test
    void downloadTableWithInvalidPaydocDate() {
        YtBrowserEntry table = buildFakeTable(TABLE_PATH, Instant.EPOCH);
        OebsPaymentYtModel row = OebsPaymentYtModel.builder()
                .docNumber(String.valueOf(RandomUtils.nextLong()))
                .docAmountRub(RandomUtils.nextDouble(1.0, 100_000.0))
                .trxNumber("TINKOFFFF_123")
                .paydocNumber("431242")
                .paydocDate("")
                .build();
        mockTable(table, List.of(row));

        assertThatThrownBy(() -> executor.doRealJob(null))
                .isExactlyInstanceOf(RuntimeException.class);

        Optional<OebsDownloadedTable> tableO = downloadedTableRepository.findByTablePath(table.getPath());
        assertThat(tableO).isEmpty();
        assertThat(getAllReceipts()).isEmpty();
    }
}
