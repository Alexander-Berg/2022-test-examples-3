package ru.yandex.autotests.market.stat.beans;

import ru.yandex.autotests.market.common.differ.WithId;

/**
 * Created by kateleb on 22.04.15.
 */
public interface WithFilter extends WithId {

    Integer getFilter();

    void setFilter(Integer filter);
}
