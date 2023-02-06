package ru.yandex.market.core.testing;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import ru.yandex.market.core.annotations.ConverterClass;
import ru.yandex.market.core.framework.converter.SmartStandartBeanElementConverter;

/**
 * @author ashevenkov
 */
@ConverterClass(SmartStandartBeanElementConverter.class)
@XmlRootElement(name = "testing-param-status")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestingParamStatus implements Serializable {

    private boolean status;
    private String description;
    private int paramTypeId;

    protected TestingParamStatus() {
    }

    public TestingParamStatus(boolean status, String description, int paramTypeId) {
        this.status = status;
        this.description = description;
        this.paramTypeId = paramTypeId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getParamTypeId() {
        return paramTypeId;
    }

    public void setParamTypeId(int paramTypeId) {
        this.paramTypeId = paramTypeId;
    }

    @Override
    public String toString() {
        return "TestingParamStatus{" +
                "status=" + status +
                ", description='" + description + '\'' +
                ", paramTypeId=" + paramTypeId +
                '}';
    }
}
