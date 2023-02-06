package ru.yandex.autotests.direct.cmd.common;

/**
 * Набор ссылок на мобильные приложения в обёртке appmetrica'и.
 * Могут инвалидироваться. Рецепт починки описан тут:
 * <a href="https://st.yandex-team.ru/TESTIRT-12857">TESTIRT-12857: Аппметрика инвалидирует ссылки без внешних кликов</a>
 * <p>
 * Коротко: нужно заходить в https://appmetrica.yandex.ru/campaign/list?appId=279525
 * под at-direct-super и восстанавливать трекеры.
 */
public interface AppMetricaHrefs {
    String HREF_ONE = "https://appmetrica.yandex.com/serve/961854457400830276/?click_id={LOGID}";
    String HREF_THREE = "https://appmetrica.yandex.com/serve/385393718435299181/?click_id={LOGID}";
}
