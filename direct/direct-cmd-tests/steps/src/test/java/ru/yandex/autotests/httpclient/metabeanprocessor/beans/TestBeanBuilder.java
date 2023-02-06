package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

import org.junit.Ignore;

import java.util.List;

@Ignore
public class TestBeanBuilder {
    private SecondTestBean secondTestBean;
    private String canUseDayBudget;
    private ThirdTestBean[] thirdTestBeans;
    private List<String> emails;
    private List<BannerTagBean> tags;

    public TestBeanBuilder() {}

    public TestBeanBuilder setSecondTestBean(SecondTestBean secondTestBean) {
        this.secondTestBean = secondTestBean;
        return this;
    }

    public TestBeanBuilder setCanUseDayBudget(String canUseDayBudget) {
        this.canUseDayBudget = canUseDayBudget;
        return this;
    }

    public TestBeanBuilder setThirdTestBeans(ThirdTestBean[] thirdTestBeans) {
        this.thirdTestBeans = thirdTestBeans;
        return this;
    }

    public TestBeanBuilder setEmails(List<String> emails) {
        this.emails = emails;
        return this;
    }

    public TestBeanBuilder setTags(List<BannerTagBean> tags) {
        this.tags = tags;
        return this;
    }

    public TestBean createTestBean() {
        return new TestBean(secondTestBean, canUseDayBudget, thirdTestBeans, emails, tags);
    }
}