package ru.yandex.market.yql_test;

import java.util.Map;

import freemarker.template.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ru.yandex.market.yql_query_service.service.QueryService;
import ru.yandex.market.yql_query_service.service.YqlTemplateVariablesManager;
import ru.yandex.market.yql_test.service.QuerySuffixesService;

@Service
@Primary
public class YqlTestQueryService extends QueryService {

    private final QuerySuffixesService querySuffixesService;

    public YqlTestQueryService(Configuration yqlQueryServiceFreemarkerConfiguration,
                               YqlTemplateVariablesManager yqlTemplateVariablesManager,
                               QuerySuffixesService querySuffixesService) {
        super(yqlQueryServiceFreemarkerConfiguration, yqlTemplateVariablesManager);
        this.querySuffixesService = querySuffixesService;
    }

    @Override
    public String wrapQuery(String query, Map<String, Object> params) {
        return querySuffixesService.processQuery(query, super.wrapQuery(query, params));
    }

    @Override
    public String getQuery(String name, Map<String, Object> params) {
        return querySuffixesService.processQuery(name, super.getQuery(name, params));
    }
}
