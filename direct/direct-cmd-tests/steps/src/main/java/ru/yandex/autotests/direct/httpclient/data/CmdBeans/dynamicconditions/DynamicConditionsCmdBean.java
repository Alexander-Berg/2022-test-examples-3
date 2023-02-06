package ru.yandex.autotests.direct.httpclient.data.CmdBeans.dynamicconditions;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Бин, отражающий структуру динамических условий нацеливания в ответе контроллеров adAdGroups
 */
public class DynamicConditionsCmdBean {

    @JsonPath(requestPath = "dyn_id", responsePath = "dyn_id")
    private String dynamicConditionId;

    @JsonPath(requestPath = "condition_name", responsePath = "condition_name")
    private String dynamicConditionName;

    @JsonPath(requestPath = "type", responsePath = "type")
    private String type;

    @JsonPath(requestPath = "price", responsePath = "price")
    private String price;

    @JsonPath(requestPath = "price_context", responsePath = "price_context")
    private String priceContext;

    @JsonPath(requestPath = "autobudgetPriority", responsePath = "autobudgetPriority")
    private String autobudgetPriority;

    @JsonPath(requestPath = "condition", responsePath = "condition")
    private List<ConditionCmdBean> dynamicCondition;


    public String getDynamicConditionId() {
        return dynamicConditionId;
    }

    public void setDynamicConditionId(String dynamicConditionId) {
        this.dynamicConditionId = dynamicConditionId;
    }

    public String getDynamicConditionName() {
        return dynamicConditionName;
    }

    public void setDynamicConditionName(String dynamicConditionName) {
        this.dynamicConditionName = dynamicConditionName;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPriceContext() {
        return priceContext;
    }

    public void setPriceContext(String priceContext) {
        this.priceContext = priceContext;
    }

    public List<ConditionCmdBean> getDynamicCondition() {
        return dynamicCondition;
    }

    public void setDynamicCondition(List<ConditionCmdBean> dynamicCondition) {
        this.dynamicCondition = dynamicCondition;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public void setAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
    }
}
