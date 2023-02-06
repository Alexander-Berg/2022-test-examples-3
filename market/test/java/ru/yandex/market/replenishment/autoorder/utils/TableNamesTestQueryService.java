package ru.yandex.market.replenishment.autoorder.utils;

import freemarker.template.Configuration;
import org.springframework.stereotype.Service;

import ru.yandex.market.yql_query_service.service.QueryService;
import ru.yandex.market.yql_query_service.service.YqlTemplateVariablesManager;

@Service
public class TableNamesTestQueryService extends QueryService {

    private boolean notUseLocalTables = false;

    public TableNamesTestQueryService(Configuration yqlQueryServiceFreemarkerConfguration,
                                      YqlTemplateVariablesManager yqlTemplateVariablesManager) {
        super(yqlQueryServiceFreemarkerConfguration, yqlTemplateVariablesManager);
    }

    public void setNotUseLocalTables(boolean notUseLocalTables) {
        this.notUseLocalTables = notUseLocalTables;
    }

}
