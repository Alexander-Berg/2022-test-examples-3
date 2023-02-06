package ru.yandex.market.partner.campaign.model;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import ru.yandex.market.core.param.model.ParamType;

/**
 * @author Vadim Lyalin
 */
@XmlRootElement(name = "param-value")
@XmlAccessorType(XmlAccessType.FIELD)
public class ParamValueDTO {
    @XmlAttribute(name = "type")
    private ParamType type;
    @XmlValue
    private String value;

    public ParamValueDTO() {
    }

    public ParamValueDTO(ParamType type, String value) {
        this.type = type;
        this.value = value;
    }

    public ParamType getType() {
        return type;
    }

    public void setType(ParamType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParamValueDTO that = (ParamValueDTO) o;
        return type == that.type &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "ParamValueDTO{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }
}
