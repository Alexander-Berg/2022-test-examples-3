package ru.yandex.autotests.httpclient.metabeanprocessor.jsonparser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.*;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
* @author : Alex Samokhin (alex-samo@yandex-team.ru)
*         Date: 25.02.15
*/
public class JsonPathResponseTest {

    private TestBean expectedTestBean;
    private String json;

    @Before
    public void before() {
        json = "{ \"campaignResponse\": { \"nameResponse\":[\"Иванов Иван Иванович\", \"Вася Пупкин\"], \"cidResponse\":30824416," +
                "\"banners_status\":{\"running_unmoderated\":0}}, " +
                "\"validEmailsResponse\":[{\"emailResponse\":\"at-direct-backend-c@yandex.ru\",\"select\":\"\"}," +
                "{\"emailResponse\":\"at-tester@yandex-team.ru\",\"select\":\"\"}]," +
                "\"client\":{\"can_use_day_budget\":0},  \"unusedField\" : \"unusedValue\"}";


        List<String> fios = new ArrayList<>();
        fios.add("Иванов Иван Иванович");
        fios.add("Вася Пупкин");
        SecondTestBean secondTestBean = new SecondTestBeanBuilder().setName(fios).setCid(30824416).createSecondTestBean();
        ThirdTestBean thirdTestBean = new ThirdTestBeanBuilder().createThirdTestBean();
        List<ThirdTestBean> list = new ArrayList<>();
        list.add(thirdTestBean);

        thirdTestBean = new ThirdTestBeanBuilder().createThirdTestBean();
        list.add(thirdTestBean);
        List<String> emails = new ArrayList<>();
        emails.add("at-direct-backend-c@yandex.ru");
        emails.add("at-tester@yandex-team.ru");
        expectedTestBean = new TestBeanBuilder().setSecondTestBean(secondTestBean).
                setThirdTestBeans(list.toArray(new ThirdTestBean[2])).setEmails(emails).createTestBean();
    }
    @Test
    public void parseJsonToPlainBeans() {
        TestBean actualTestBean =
                JsonPathJSONPopulater.eval(json, new TestBeanBuilder().createTestBean(), BeanType.RESPONSE);
        assertThat("Бины не одинаковые", actualTestBean, beanEquivalent(expectedTestBean));
    }
}
