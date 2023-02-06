package ru.yandex.market.core.stats;

import java.util.List;
import java.util.Map;

import ru.yandex.market.core.report.model.DictionaryReportMetaData;

/**
 * Базовый класс для тестов. Методы возвращают значения по умолчанию
 *
 * @author zoom
 */
class PPGroupRegistryAdapter implements PPGroupRegistry {

    private final List<Long> ppGroups;
    private final Map<Long, Long> ppMapGroupByPpCode;

    public PPGroupRegistryAdapter(List<Long> ppGroups, Map<Long, Long> ppMapGroupByPpCode) {
        this.ppGroups = ppGroups;
        this.ppMapGroupByPpCode = ppMapGroupByPpCode;
    }

    @Override
    public List<Long> getPpGroups() {
        return ppGroups;
    }

    @Override
    public Map<Long, Long> getPpGroupByPpCode() {
        return ppMapGroupByPpCode;
    }

    @Override
    public DictionaryReportMetaData getMetaData() {
        return null;
    }

    @Override
    public DictionaryReportMetaData getShowsMetaData() {
        return null;
    }
}
