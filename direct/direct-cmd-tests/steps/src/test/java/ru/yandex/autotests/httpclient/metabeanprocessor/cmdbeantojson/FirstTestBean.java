package ru.yandex.autotests.httpclient.metabeanprocessor.cmdbeantojson;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 16.04.15.
 */
public class FirstTestBean {

    @JsonPath(requestPath = "first/property")
    private String firstProperty;

    @JsonPath(requestPath = "second/list/property")
    private List<Integer> secondListProperty;

    @JsonPath(requestPath = "third/array/property")
    private int[] thirdArrayProperty;

    @JsonPath(requestPath = "inner/bean")
    private InnerBean innerBeanWithSlashedPath;

    @JsonPath(requestPath = "inner/boolean")
    private Float fieldOnInnerLevel;

    @JsonPath(requestPath = "inner-bean")
    private InnerBean innerBeanWithNormalPath;

    @JsonPath(requestPath = "normal_property")
    private String normalProperty;

    private String fieldToBeIgnored;

    @JsonPath(responsePath = "ignored_property")
    private String responseFieldToBeIgnored;

    public InnerBean getInnerBeanWithNormalPath() {
        return innerBeanWithNormalPath;
    }

    public void setInnerBeanWithNormalPath(InnerBean innerBeanWithNormalPath) {
        this.innerBeanWithNormalPath = innerBeanWithNormalPath;
    }

    public int[] getThirdArrayProperty() {
        return thirdArrayProperty;
    }

    public void setThirdArrayProperty(int[] thirdArrayProperty) {
        this.thirdArrayProperty = thirdArrayProperty;
    }

    public List<Integer> getSecondListProperty() {
        return secondListProperty;
    }

    public void setSecondListProperty(List<Integer> secondListProperty) {
        this.secondListProperty = secondListProperty;
    }

    public String getFirstProperty() {
        return firstProperty;
    }

    public void setFirstProperty(String firstProperty) {
        this.firstProperty = firstProperty;
    }

    public String getNormalProperty() {
        return normalProperty;
    }

    public void setNormalProperty(String normalProperty) {
        this.normalProperty = normalProperty;
    }

    public InnerBean getInnerBeanWithSlashedPath() {
        return innerBeanWithSlashedPath;
    }

    public void setInnerBeanWithSlashedPath(InnerBean innerBeanWithSlashedPath) {
        this.innerBeanWithSlashedPath = innerBeanWithSlashedPath;
    }

    public String getFieldToBeIgnored() {
        return fieldToBeIgnored;
    }

    public void setFieldToBeIgnored(String fieldToBeIgnored) {
        this.fieldToBeIgnored = fieldToBeIgnored;
    }

    public String getResponseFieldToBeIgnored() {
        return responseFieldToBeIgnored;
    }

    public void setResponseFieldToBeIgnored(String responseFieldToBeIgnored) {
        this.responseFieldToBeIgnored = responseFieldToBeIgnored;
    }

    public Float isFieldOnInnerLevel() {
        return fieldOnInnerLevel;
    }

    public void setFieldOnInnerLevel(Float fieldOnInnerLevel) {
        this.fieldOnInnerLevel = fieldOnInnerLevel;
    }
}
