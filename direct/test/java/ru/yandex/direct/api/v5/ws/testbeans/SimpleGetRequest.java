package ru.yandex.direct.api.v5.ws.testbeans;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "fieldNames"
})
@XmlRootElement(name = "GetRequest")
public class SimpleGetRequest {
    @XmlElement(name = "FieldNames", required = true)
    private List<SimpleFieldNamesEnum> fieldNames;

    @XmlElement(name = "SingleEnum", required = true)
    private SimpleFieldNamesEnum singleEnum;

    @XmlElement(name = "LongValue", required = true)
    private Long longValue;
    @XmlElement(name = "IntValue", required = true)
    private Integer intValue;

    @XmlElement(name = "JaxbIntValue", required = false)
    private JAXBElement<Integer> jaxbIntValue;

    @XmlElement(name = "InnerBean", required = true)
    private SimpleGetRequest innerBean;

    @XmlElement(name = "ListOfLists", required = true)
    private List<List<Long>> listOfLists;

    @XmlElement(name = "ListOfLong", required = true)
    private List<Long> listOfLong;

    public List<SimpleFieldNamesEnum> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(
            List<SimpleFieldNamesEnum> fieldNames) {
        this.fieldNames = fieldNames;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }

    public JAXBElement<Integer> getJaxbIntValue() {
        return jaxbIntValue;
    }

    public void setJaxbIntValue(JAXBElement<Integer> jaxbIntValue) {
        this.jaxbIntValue = jaxbIntValue;
    }

    public SimpleGetRequest getInnerBean() {
        return innerBean;
    }

    public void setInnerBean(SimpleGetRequest innerBean) {
        this.innerBean = innerBean;
    }

    public List<List<Long>> getListOfLists() {
        return listOfLists;
    }

    public void setListOfLists(List<List<Long>> listOfLists) {
        this.listOfLists = listOfLists;
    }

    public SimpleGetRequest withFieldNames(
            final List<SimpleFieldNamesEnum> fieldNames) {
        setFieldNames(fieldNames);
        return this;
    }

    public SimpleGetRequest withLongValue(final Long longValue) {
        setLongValue(longValue);
        return this;
    }

    public SimpleGetRequest withIntValue(final Integer intValue) {
        setIntValue(intValue);
        return this;
    }

    public SimpleGetRequest withInnerBean(final SimpleGetRequest innerBean) {
        setInnerBean(innerBean);
        return this;
    }

    public SimpleGetRequest withListOfLists(final List<List<Long>> listOfLists) {
        setListOfLists(listOfLists);
        return this;
    }

    public SimpleGetRequest withJaxbIntValue(final JAXBElement<Integer> jaxbIntValue) {
        setJaxbIntValue(jaxbIntValue);
        return this;
    }
}
