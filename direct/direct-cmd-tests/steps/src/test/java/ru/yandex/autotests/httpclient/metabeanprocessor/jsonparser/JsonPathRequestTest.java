package ru.yandex.autotests.httpclient.metabeanprocessor.jsonparser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.02.15
 */
public class JsonPathRequestTest {

    private TestBean expectedTestBean;
    private String json;

    @Before
    public void before() {
        json = "{ \"campaign\": {" +
                "\"tags\":{\"val\":[{\"uses_count\":\"1\",\"value\":\"firstTag\",\"tag_id\":\"3268936\"}," +
                    "{\"uses_count\":\"0\",\"value\":\"secondTag\",\"tag_id\":\"3268937\"}]}, " +
                "\"fio\":[\"Иванов Иван Иванович\", \"Вася Пупкин\"], \"cid\":30824416," +
                "\"banners_status\":{\"running_unmoderated\":0}}, " +
                "\"validEmails\":[{\"email\": {\"ee\": \"at-direct-backend-c@yandex.ru\"},\"select\":\"\"}," +
                "{\"email\": {\"ee\": \"at-tester@yandex-team.ru\"},\"select\":\"\"}]," +
                "\"client\":{\"can_use_day_budget\":0},  \"unusedField\" : \"unusedValue\"}";

        List<String> fios = new ArrayList<>();
        fios.add("Иванов Иван Иванович");
        fios.add("Вася Пупкин");
        BannerTagBean bannerTagBeanFirst = new BannerTagBeanBuilder().setTagId("3268936").setUsesCount("1").
                setValue("firstTag").createBannerTagBean();
        List<BannerTagBean> bannerTagBeans = new LinkedList<>();
        bannerTagBeans.add(bannerTagBeanFirst);
        BannerTagBean bannerTagBeanSecond = new BannerTagBeanBuilder().setTagId("3268937").setUsesCount("0").
                setValue("secondTag").createBannerTagBean();
        bannerTagBeans.add(bannerTagBeanSecond);
        SecondTestBean secondTestBean = new SecondTestBeanBuilder().setFio(fios).setCid(30824416).
                setRunningUnmoderated("0").createSecondTestBean();
        FourthTestBean fourthTestBean = new FourthTestBeanBuilder().setEmail("at-direct-backend-c@yandex.ru").
                createFourthTestBean();
        ThirdTestBean thirdTestBean = new ThirdTestBeanBuilder().setEmail(fourthTestBean).
                createThirdTestBean();
        List<ThirdTestBean> list = new ArrayList<>();
        list.add(thirdTestBean);
        fourthTestBean = new FourthTestBeanBuilder().setEmail("at-tester@yandex-team.ru").
                createFourthTestBean();
        thirdTestBean = new ThirdTestBeanBuilder().setEmail(fourthTestBean).createThirdTestBean();
        list.add(thirdTestBean);

        expectedTestBean = new TestBeanBuilder().setCanUseDayBudget("0").setSecondTestBean(secondTestBean).
                setThirdTestBeans(list.toArray(new ThirdTestBean[2])).setTags(bannerTagBeans)
                .createTestBean();

    }
    @Test
    public void parseJsonToPlainBeans() {
        TestBean actualTestBean = JsonPathJSONPopulater.eval(json, new TestBeanBuilder().createTestBean(), BeanType.REQUEST);
        assertThat("Бины не одинаковые", actualTestBean, beanEquivalent(expectedTestBean));
    }
}
