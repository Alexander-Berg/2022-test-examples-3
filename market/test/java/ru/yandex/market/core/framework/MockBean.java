package ru.yandex.market.core.framework;

import ru.yandex.market.core.annotations.ConverterClass;
import ru.yandex.market.core.framework.converter.BeanElementConverter;

/**
 * @author Alexey Shevenkov ashevenkov@yandex-team.ru
 */

@ConverterClass(BeanElementConverter.class)
public class MockBean {

    private String property;
    private int anotherProperty;

    public MockBean() {
    }

    public MockBean(String property, int anotherProperty) {
        this.property = property;
        this.anotherProperty = anotherProperty;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public int getAnotherProperty() {
        return anotherProperty;
    }

    public void setAnotherProperty(int anotherProperty) {
        this.anotherProperty = anotherProperty;
    }
}
