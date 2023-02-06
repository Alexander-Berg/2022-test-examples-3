package ru.yandex.market.ydb.integration;

import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.market.ydb.integration.migration.TableTruncationAware;

@Component
public class DataCleaner {

    private final List<TableTruncationAware> daoList;

    public DataCleaner(List<TableTruncationAware> daoList) {
        this.daoList = daoList;
    }

    public void cleanData() {
        for (TableTruncationAware dao : daoList) {
            dao.truncateTable();
        }
    }
}
