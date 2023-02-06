package ru.yandex.market.toloka;

import java.util.Date;

import ru.yandex.market.markup2.entries.yang.YangPoolStatusInfo;

/**
 * @author york
 * @since 24.06.2020
 */
public class YangResultsDownloaderStub extends YangResultsDownloader {

    private Date firstSubmittedDate;

    public void onStart() {
        initAllPools();
    }

    public void setFirstSubmittedDate(Date firstSubmittedDate) {
        this.firstSubmittedDate = firstSubmittedDate;
    }

    @Override
    protected Date getFirstSubmittedDate() {
        if (firstSubmittedDate != null) {
            return firstSubmittedDate;
        }
        return super.getFirstSubmittedDate();
    }



    @Override
    public void downloadResultsAllPools() {
        super.downloadResultsAllPools();
    }

    public void downloadPoolResults(int poolId) {
        downloadPoolResults((YangPoolStatusInfo) getYangPoolStatusInfo(poolId), true);
    }
}
