package ru.yandex.market.yql_test.configuration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ru.yandex.market.yql_query_service.YqlTemplateVariable;
import ru.yandex.market.yql_query_service.service.YqlTemplateVariablesManager;
import ru.yandex.market.yql_test.YqlTestTemplateVariable;

@Service
@Primary
public class YqlTemplateVariablesTestManager extends YqlTemplateVariablesManager {

    public YqlTemplateVariablesTestManager(Collection<YqlTemplateVariable> variables) {
        super(filter(variables));
    }

    private static Collection<YqlTemplateVariable> filter(Collection<YqlTemplateVariable> variables) {
        Map<String, YqlTemplateVariable> filtered = new HashMap<>();
        variables.stream()
                .filter(yqlTemplateVariable -> !(yqlTemplateVariable instanceof YqlTestTemplateVariable))
                .forEach(yqlTemplateVariable -> filtered.put(yqlTemplateVariable.name(), yqlTemplateVariable));
        variables.stream()
                .filter(yqlTemplateVariable -> (yqlTemplateVariable instanceof YqlTestTemplateVariable))
                .forEach(yqlTemplateVariable -> filtered.put(yqlTemplateVariable.name(), yqlTemplateVariable));
        return filtered.values();
    }
}
