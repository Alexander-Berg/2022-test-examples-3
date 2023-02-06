package ru.yandex.market.tsum.tms.isolation.model;

import java.util.Objects;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 12/12/2018
 */
public class NamedBean {
    private final String name;
    private final Class<?> clazz;

    public NamedBean(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String getName() {
        return name;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NamedBean namedBean = (NamedBean) o;
        return Objects.equals(name, namedBean.name) &&
            Objects.equals(clazz, namedBean.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clazz);
    }
}
