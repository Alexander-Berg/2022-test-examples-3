package ru.yandex.autotests.market.stat.meta;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by entarrion on 01.04.15.
 */
public class MetaJdbcTemplateFactory {

    public static JdbcTemplate getInstanceReporting() {
        return ReportingJdbcSingletonHolder.INSTANCE;
    }

    public static JdbcTemplate getInstanceDictionariesYt() {
        return DictionariesYtJdbcSingletonHolder.INSTANCE;
    }

    private static class ReportingJdbcSingletonHolder {
        private static final JdbcTemplate INSTANCE = new MetaBaseConfig().reportingJdbcTemplate();
    }

    private static class DictionariesYtJdbcSingletonHolder {
        private static final JdbcTemplate INSTANCE = new MetaBaseConfig().dictionariesYtJdbcTemplate();
    }
}
