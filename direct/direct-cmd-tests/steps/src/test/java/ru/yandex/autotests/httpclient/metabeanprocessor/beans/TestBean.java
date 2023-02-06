package ru.yandex.autotests.httpclient.metabeanprocessor.beans;


import org.junit.Ignore;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 27.01.15
 */
@Ignore
public class TestBean {

    @JsonPath(requestPath = "campaign", responsePath = "campaignResponse")
    private SecondTestBean secondTestBean;

    @JsonPath(requestPath = "client/can_use_day_budget")
    private String canUseDayBudget;

    @JsonPath(requestPath = "validEmails", responsePath = "validEmailsResponse")
    private ThirdTestBean[] thirdTestBeans;

    @JsonPath(responsePath = "validEmailsResponse/emailResponse")
    private List<String> emails;

    @JsonPath(requestPath = "campaign/tags/val")
    private List<BannerTagBean> tags;

    public TestBean() {}

    public static TestBean createTestBean(SecondTestBean secondTestBean, String canUseDayBudget, ThirdTestBean[] thirdTestBeans, List<String> emails, List<BannerTagBean> tags) {
        return new TestBeanBuilder().setSecondTestBean(secondTestBean).setCanUseDayBudget(canUseDayBudget).setThirdTestBeans(thirdTestBeans).setEmails(emails).setTags(tags).createTestBean();
    }

    public void setSecondTestBean(SecondTestBean secondTestBean) {
        this.secondTestBean = secondTestBean;
    }

    public void setCanUseDayBudget(String canUseDayBudget) {
        this.canUseDayBudget = canUseDayBudget;
    }

    public ThirdTestBean[] getThirdTestBeans() {
        return thirdTestBeans;
    }

    public void setThirdTestBeans(ThirdTestBean[] thirdTestBeans) {
        this.thirdTestBeans = thirdTestBeans;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public SecondTestBean getSecondTestBean() {
        return secondTestBean;
    }

    public String getCanUseDayBudget() {
        return canUseDayBudget;
    }

    public List<String> getEmails() {
        return emails;
    }

    public List<BannerTagBean> getTags() {
        return tags;
    }

    public void setTags(List<BannerTagBean> tags) {
        this.tags = tags;
    }

    TestBean(SecondTestBean secondTestBean, String canUseDayBudget, ThirdTestBean[] thirdTestBeans, List<String> emails, List<BannerTagBean> tags) {
        this.secondTestBean = secondTestBean;
        this.canUseDayBudget = canUseDayBudget;
        this.thirdTestBeans = thirdTestBeans;
        this.emails = emails;
        this.tags = tags;
    }
}
