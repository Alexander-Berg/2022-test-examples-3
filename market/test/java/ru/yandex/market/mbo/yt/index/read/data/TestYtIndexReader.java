package ru.yandex.market.mbo.yt.index.read.data;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.ir.yt.util.tables.YtClientWrapper;
import ru.yandex.market.mbo.yt.index.LongKey;
import ru.yandex.market.mbo.yt.index.read.AbstractYtIndexReader;
import ru.yandex.market.mbo.yt.index.read.SearchFilter;
import ru.yandex.market.mbo.yt.index.read.YtIndexQuery;
import ru.yandex.market.mbo.yt.utils.UnstableInit;
import ru.yandex.market.yt.util.table.YtTableService;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

/**
 * @author apluhin
 * @created 7/12/21
 *
 * Пример построения читателя индекса
 */
public class TestYtIndexReader extends AbstractYtIndexReader<LongKey> {

    public TestYtIndexReader(UnstableInit<YtClientWrapper> ytClientWrapper,
                             UnstableInit<YtTableService> ytTableService, YtTableModel indexTableModel) {
        super(ytClientWrapper, ytTableService, indexTableModel);
    }

    @Override
    protected YtIndexQuery convertToYtQuery(SearchFilter filter) {
        return new TestIndexQuery(filter);
    }

    /**
     * Конвертация rowset-индекса в ожидаемым приложением структуру
     */
    @Override
    public List<LongKey> convertRows(UnversionedRowset rowSet) {
        List<LongKey> res = new ArrayList<>();
        for (UnversionedRow row : rowSet.getRows()) {
            if (row != null) {
                UnversionedValue key = rpcApi.getValue(row, "key");

                res.add(new LongKey(key.longValue()));
            }
        }
        return res;
    }

    /**
     * Логика для проверки возможности использовать созданный фильтр под текущий индекс
     */
    @Override
    public Boolean isSupportFilter(SearchFilter filter) {
        return TestIndexQuery.isSupportFilter(filter);
    }

    /**
     *  Приоритет для CompositeIndexDecider
     */
    @Override
    public Integer priority() {
        return 50;
    }
}
