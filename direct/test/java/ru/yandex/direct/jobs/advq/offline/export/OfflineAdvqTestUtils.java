package ru.yandex.direct.jobs.advq.offline.export;

import java.util.List;

import ru.yandex.direct.ytwrapper.tables.generated.YtBidsRow;
import ru.yandex.direct.ytwrapper.tables.generated.YtCampaignsRow;
import ru.yandex.direct.ytwrapper.tables.generated.YtPhrasesRow;

class OfflineAdvqTestUtils {
    private static final int TI_CAMPAIGNS = 0;
    private static final int TI_PHRASES = 1;
    private static final int TI_BIDS = 2;

    static YtCampaignsRow campaignsRow(Long cid, String archived) {
        YtCampaignsRow row = new YtCampaignsRow(OfflineAdvqMRSpec.getMRTablesRows().get(TI_CAMPAIGNS).getFields());

        row.setCid(cid);
        row.setArchived(archived);

        row.setTableIndex(TI_CAMPAIGNS);

        return row;
    }

    static YtPhrasesRow phrasesRow(Long cid, Long pid, String mwText, String adGroupType, String geo, String status) {
        YtPhrasesRow row = new YtPhrasesRow(OfflineAdvqMRSpec.getMRTablesRows().get(TI_PHRASES).getFields());

        row.setCid(cid);
        row.setPid(pid);
        row.setMwText(mwText);
        row.setAdgroupType(adGroupType);
        row.setGeo(geo);
        row.setStatusShowsForecast(status);

        row.setTableIndex(TI_PHRASES);

        return row;
    }

    static YtBidsRow bidsRow(Long cid, Long pid, Long id, String keyword) {
        YtBidsRow row = new YtBidsRow(OfflineAdvqMRSpec.getMRTablesRows().get(TI_BIDS).getFields());

        row.setCid(cid);
        row.setPid(pid);
        row.setId(id);
        row.setPhrase(keyword);

        row.setTableIndex(TI_BIDS);

        return row;
    }

    static OfflineAdvqExportOutputTableRow resultRow(Long id, Long pid, String original, String keyword, String geo,
                                                     List<String> devices,
                                                     List<String> minusWords) {
        OfflineAdvqExportOutputTableRow row = new OfflineAdvqExportOutputTableRow();
        row.setId(id);
        row.setGroupId(pid);
        row.setOriginalKeyword(original);
        row.setGeo(geo);
        row.setKeyword(keyword);
        row.setMinusWords(minusWords);
        row.setDevices(devices);
        return row;
    }
}
