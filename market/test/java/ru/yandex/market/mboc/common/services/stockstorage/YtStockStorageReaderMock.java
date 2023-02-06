package ru.yandex.market.mboc.common.services.stockstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class YtStockStorageReaderMock implements YtStockStorageReader {

    private List<YtStockInfo> infos = new ArrayList<>();

    public void prepareStockInfo(List<YtStockInfo> infos) {
        this.infos = infos;
    }

    @Override
    public void readStockInfo(Consumer<List<YtStockInfo>> batchConsumer) {
        batchConsumer.accept(infos);
    }
}
