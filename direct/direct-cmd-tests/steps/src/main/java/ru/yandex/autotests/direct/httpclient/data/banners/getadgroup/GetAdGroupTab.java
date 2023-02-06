package ru.yandex.autotests.direct.httpclient.data.banners.getadgroup;


/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.03.15
 */
public enum GetAdGroupTab {

    BASE,
    SEARCH,
    CONTEXT,
    CONTEXT_POSTER;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
