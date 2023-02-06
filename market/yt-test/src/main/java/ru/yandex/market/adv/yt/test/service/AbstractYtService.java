package ru.yandex.market.adv.yt.test.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

import ru.yandex.market.adv.yt.test.model.TableContent;
import ru.yandex.market.adv.yt.test.model.TableInfo;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Абстрактный класс для работы с YT.
 * Date: 12.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public abstract class AbstractYtService implements YtService {

    @Override
    public void createTable(TableInfo tableInfo) {
        TableContent tableContent = tableInfo.getTableContent();

        if (tableContent.getCreate()) {
            YtClientProxy ytClient = tableInfo.getYtClient();
            String path = tableInfo.getPath();

            YTBinder<Object> binder = getBinder(tableInfo);

            if (!ytClient.isPathExists(path)) {
                ytClient.createTable(path, binder, Map.of());
                write(ytClient, path, binder, new ArrayList<>(tableContent.getRows()));
            }
        }
    }

    @Override
    public void checkAndCleanTable(TableInfo tableInfo) {
        YtClientProxy ytClient = tableInfo.getYtClient();
        String path = tableInfo.getPath();
        TableContent tableContent = tableInfo.getTableContent();

        YTBinder<Object> binder = getBinder(tableInfo);
        boolean pathExists = ytClient.isPathExists(path);

        List<Object> selectRows;
        if (pathExists) {
            selectRows = read(ytClient, path, binder);
            deleteRows(ytClient, path, binder, selectRows);
            ytClient.deletePath(path);
        } else {
            selectRows = List.of();
        }

        Assertions.assertThat(pathExists)
                .overridingErrorMessage("Expecting: table '%s'%s exist", path, tableContent.getExist() ? "" : " not")
                .isEqualTo(tableContent.getExist());

        Assertions.assertThat(selectRows)
                .as("Table '%s' contains wrong data", path)
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields(tableInfo.getIgnoreColumns())
                        .build())
                .containsExactlyInAnyOrderElementsOf(tableContent.getExist() ? tableContent.getRows() : List.of());
    }

    /**
     * Получение биндера для маппинга модели приложения в таблицу YT.
     *
     * @param tableInfo информация по таблице
     * @return биндер
     */
    protected abstract YTBinder<Object> getBinder(TableInfo tableInfo);

    /**
     * Запись строк в таблицу.
     *
     * @param ytClient клиент YT
     * @param path     путь до таблицы
     * @param binder   биндер
     * @param rows     строки таблицы для удаления
     */
    protected abstract void write(YtClientProxy ytClient, String path, YTBinder<Object> binder, List<Object> rows);

    /**
     * Чтение всех данных из таблицы.
     *
     * @param ytClient клиент YT
     * @param path     путь до таблицы
     * @param binder   биндер
     * @return строки таблицы
     */
    protected abstract List<Object> read(YtClientProxy ytClient, String path, YTBinder<Object> binder);

    /**
     * Удаление строк из таблицы.
     *
     * @param ytClient клиент YT
     * @param path     путь до таблицы
     * @param binder   биндер
     * @param rows     строки таблицы для удаления
     */
    protected abstract void deleteRows(YtClientProxy ytClient, String path, YTBinder<Object> binder, List<Object> rows);
}
