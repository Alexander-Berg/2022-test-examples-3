package ru.yandex.autotests.market.stat.meta;

/**
 * Created by entarrion on 02.04.15.
 */
public class TmsDaoFactory {

    public static TmsJbdcDao getTmsDaoForDictionariesYt() {
        return new TmsJbdcDao(MetaJdbcTemplateFactory.getInstanceDictionariesYt());
    }
}
