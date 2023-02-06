package ru.yandex.market.mbi.logprocessor;

import java.util.List;

import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Вспомогательный класс, который позволяет инициализировать динамическую таблицу из csv файла.
 */
public class YtInitializer {

    private final YtClientProxy ytProxy;

    public YtInitializer(YtClientProxy ytProxy) {
        this.ytProxy = ytProxy;
    }

    public <T> void initializeFromFile(String path, BindingTable<T> table) {
        List<T> entities = TestUtil.readBeanFromCsv(path, table.getMasterClass());
        ytProxy.insertRows(table.getTable(), YTBinder.getBinder(table.getMasterClass()), entities);
    }

    public <T> void cleanTable(BindingTable<T> table) {
        final var list = ytProxy.selectRows(String.format("* from [%s]", table.getTable()), YTBinder.getBinder(table.getMasterClass()));
        ytProxy.deleteRows(table.getTable(), YTBinder.getBinder(table.getMasterClass()), list);
    }
}
