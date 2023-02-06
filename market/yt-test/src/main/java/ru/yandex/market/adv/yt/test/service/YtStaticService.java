package ru.yandex.market.adv.yt.test.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.adv.yt.test.model.TableInfo;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Реализация для работы со статическими таблицами YT.
 * Date: 12.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@SuppressWarnings("unchecked")
@ParametersAreNonnullByDefault
public class YtStaticService extends AbstractYtService {

    @Override
    protected YTBinder<Object> getBinder(TableInfo tableInfo) {
        return (YTBinder<Object>) YTBinder.getStaticBinder(tableInfo.getModel());
    }

    @Override
    protected void write(YtClientProxy ytClient, String path, YTBinder<Object> binder, List<Object> rows) {
        if (!rows.isEmpty()) {
            ytClient.write(path, binder, rows);
        }
    }

    @Override
    protected List<Object> read(YtClientProxy ytClient, String path, YTBinder<Object> binder) {
        List<Object> selectRows = new ArrayList<>();
        ytClient.read(YPath.simple(path), binder, selectRows::add);
        return selectRows;
    }

    @Override
    protected void deleteRows(YtClientProxy ytClient, String path, YTBinder<Object> binder, List<Object> rows) {

    }
}
