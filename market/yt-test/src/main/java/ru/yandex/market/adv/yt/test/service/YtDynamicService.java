package ru.yandex.market.adv.yt.test.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.adv.yt.test.model.TableInfo;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Реализация для работы с динамическими таблицами YT.
 * Date: 12.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@SuppressWarnings("unchecked")
@ParametersAreNonnullByDefault
public class YtDynamicService extends AbstractYtService {

    @Override
    protected YTBinder<Object> getBinder(TableInfo tableInfo) {
        return (YTBinder<Object>) YTBinder.getBinder(tableInfo.getModel());
    }

    @Override
    protected void write(YtClientProxy ytClient, String path, YTBinder<Object> binder, List<Object> rows) {
        ytClient.mountTable(path);

        if (!rows.isEmpty()) {
            ytClient.insertRows(path, binder, rows);
        }
    }

    @Override
    protected List<Object> read(YtClientProxy ytClient, String path, YTBinder<Object> binder) {
        return ytClient.selectRows("* from [" + path + "]", binder);
    }

    @Override
    protected void deleteRows(YtClientProxy ytClient, String path, YTBinder<Object> binder, List<Object> rows) {
        if (!rows.isEmpty()) {
            ytClient.deleteRows(path, binder, rows);
        }
    }
}
