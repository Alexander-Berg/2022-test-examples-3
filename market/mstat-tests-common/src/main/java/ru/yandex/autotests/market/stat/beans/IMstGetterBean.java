package ru.yandex.autotests.market.stat.beans;

import ru.yandex.autotests.market.common.differ.WithId;

/**
 * Created by entarrion on 20.08.15.
 */
public interface IMstGetterBean extends WithId, WithPeriod {
    Integer getPp();

    void setPp(Integer pp);

    String getCookie();

    void setCookie(String cookie);

    String getIp();

    void setIp(String ip);

    String getIp6();

    void setIp6(String ip6);

    String getUrl();

    void setUrl(String url);
}
