package ru.yandex.direct.jobs.advq.offline.processing;

import ru.yandex.direct.ytwrapper.tables.generated.YtBidsRow;
import ru.yandex.direct.ytwrapper.tables.generated.YtPhrasesRow;

class OfflineAdvqTestUtils {
    private static final int TI_SHARDS = 0;
    private static final int TI_PHRASES = 1;
    private static final int TI_BIDS = 2;
    private static final int TI_RESULT = 3;

    static YtPhrasesRow phrasesRow(Long pid, String geo) {
        YtPhrasesRow row = new YtPhrasesRow(OfflineAdvqProcessingMRSpec.getMRTablesRows().get(TI_PHRASES).getFields());

        row.setPid(pid);
        row.setGeo(geo);

        row.setTableIndex(TI_PHRASES);

        return row;
    }

    static YtBidsRow bidsRow(Long pid, Long id, String keyword, Long forecast) {
        YtBidsRow row = new YtBidsRow(OfflineAdvqProcessingMRSpec.getMRTablesRows().get(TI_BIDS).getFields());

        row.setPid(pid);
        row.setId(id);
        row.setPhrase(keyword);
        row.setShowsForecast(forecast);

        row.setTableIndex(TI_BIDS);

        return row;
    }

    static OfflineAdvqPreProcessingResultRow preProcessingRow(Long pid, Long shard) {
        OfflineAdvqPreProcessingResultRow row = new OfflineAdvqPreProcessingResultRow();

        row.setPid(pid);
        row.setShard(shard);

        row.setTableIndex(TI_SHARDS);

        return row;
    }

    static OfflineAdvqProcessingTemporaryTableRow resultRow(Long pid, Long id, String geo, String keyword,
                                                            Long forecast) {
        OfflineAdvqProcessingTemporaryTableRow row = new OfflineAdvqProcessingTemporaryTableRow();

        row.setPid(pid);
        row.setGroupId(pid);
        row.setGeo(geo);
        row.setId(id);
        row.setOriginalKeyword(keyword);
        row.setForecast(forecast);

        row.setTableIndex(TI_RESULT);

        return row;
    }

    static OfflineAdvqProcessingTemporaryTableRow resultRowNoPid(Long pid, Long id, String geo, String keyword,
                                                                 Long forecast) {
        OfflineAdvqProcessingTemporaryTableRow row = new OfflineAdvqProcessingTemporaryTableRow();

        row.setGroupId(pid);
        row.setGeo(geo);
        row.setId(id);
        row.setOriginalKeyword(keyword);
        row.setForecast(forecast);

        row.setTableIndex(TI_RESULT);

        return row;
    }
}
