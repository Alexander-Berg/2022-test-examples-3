package ru.yandex.market.toloka;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import ru.yandex.market.toloka.model.ResultItem;
import ru.yandex.market.toloka.model.ResultItemStatus;

public class ReadthroughResultsDownloader extends YangResultsDownloader {

    @Override
    public List<ResultItem> getResults(int poolId, String taskSuiteId, String taskId) {
        return tolokaApi.getTaskResult(poolId, taskId);
    }

    @Override
    public boolean hasDownloadedResult(String taskSuiteId) {
        return false;
    }

    @Override
    protected List<ResultItem> getActiveOrFinishedResults(int poolId) {
        return tolokaApi.getResult(poolId, Optional.empty(), Optional.empty(), Optional.empty(),
                ResultItemStatus.SUBMITTED, ResultItemStatus.ACCEPTED, ResultItemStatus.ACTIVE);
    }

    public void hackAllPoolsDownloaded() {
        String now = TolokaApi.DATE_FORMAT.format(new Date());
        poolInfos.values().forEach(v -> v.setLastSubmittedTS(now));
    }
}
