package ru.yandex.market.mbo.core.dashboard;

import ru.yandex.market.mbo.gwt.models.dashboard.DumpExtractorData;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpsLogsFilter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dmserebr
 * @date 15/07/2019
 */
public class DumpHistoryDaoMock implements DumpHistoryDao {

    private List<DumpGroupData> data = new ArrayList<>();

    public void addData(DumpGroupData item) {
        data.add(item);
    }

    public void addAllData(DumpGroupData... dataList) {
        Collections.addAll(data, dataList);
    }

    public void clear() {
        data.clear();
    }

    @Override
    public List<DumpGroupData> getDumpGroupData(DumpGroupFilter filter) {
        return data.stream()
            .filter(item -> filter.getType() == null || filter.getType().getTmsExecutorName().equals(item.getType()))
            .filter(item -> filter.getLastFinishedSince() == null || item.getFinishTime() == null ||
                item.getFinishTime().toInstant().isAfter(filter.getLastFinishedSince()))
            .collect(Collectors.toList());
    }

    @Override
    public List<DumpExtractorData> getDumpExtractorsLogs(DumpGroupData dumpGroupData) {
        return null;
    }

    @Override
    public List<DumpExtractorData> getDumpExtractorsLogs(DumpsLogsFilter filter,
                                                         @Nullable Integer offset,
                                                         @Nullable Integer length,
                                                         @Nullable DumpsLogsFilter.Field sortField,
                                                         @Nullable Boolean sortAsc) {
        return null;
    }

    @Override
    public Integer getDumpExtractorsLogsCount(DumpsLogsFilter filter) {
        return null;
    }

    @Override
    public List<String> loadDumpsExtractorsNames() {
        return null;
    }

    @Override
    public List<String> loadDumpsTypes() {
        return null;
    }

    @Override
    public void logStatusUpdateInHistory(DumpGroupData dumpGroup, String message) {

    }
}
