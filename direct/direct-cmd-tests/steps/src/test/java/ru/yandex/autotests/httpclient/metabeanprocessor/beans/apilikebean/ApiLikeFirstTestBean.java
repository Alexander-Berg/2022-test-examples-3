package ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean;


public class ApiLikeFirstTestBean {

    private ApiLikeSecondTestBean nestedTestBean;

    private Boolean canUseDayBudget;

    private ApiLikeThirdTestBean[] thirdTestBeans;

    public ApiLikeSecondTestBean getNestedTestBean() {
        return nestedTestBean;
    }

    public void setNestedTestBean(ApiLikeSecondTestBean nestedTestBean) {
        this.nestedTestBean = nestedTestBean;
    }

    public Boolean getCanUseDayBudget() {
        return canUseDayBudget;
    }

    public void setCanUseDayBudget(Boolean canUseDayBudget) {
        this.canUseDayBudget = canUseDayBudget;
    }

    public ApiLikeThirdTestBean[] getThirdTestBeans() {
        return thirdTestBeans;
    }

    public void setThirdTestBeans(ApiLikeThirdTestBean[] thirdTestBeans) {
        this.thirdTestBeans = thirdTestBeans;
    }
}
